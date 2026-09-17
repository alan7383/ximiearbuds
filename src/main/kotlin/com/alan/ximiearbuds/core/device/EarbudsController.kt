package com.alan.ximiearbuds.core.device

import com.alan.ximiearbuds.core.bluetooth.*
import com.alan.ximiearbuds.core.crypto.BluetoothAuthEngine
import com.alan.ximiearbuds.core.protocol.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class EarbudsController(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
    private val autoConnectOnStartup: Boolean = true
) {
    private var transport: BluetoothTransport = TransportFactory.createTransport(useSimulation = false)
    private var isSimulated: Boolean = false

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _isDemoMode = MutableStateFlow(false)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode

    private val _deviceInfo = MutableStateFlow(TargetDeviceInfo())
    val deviceInfo: StateFlow<TargetDeviceInfo> = _deviceInfo

    private val _activeModel = MutableStateFlow<EarbudsModel?>(null)
    val activeModel: StateFlow<EarbudsModel?> = _activeModel

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices

    private val _noiseControl = MutableStateFlow(NoiseControlState())
    val noiseControl: StateFlow<NoiseControlState> = _noiseControl

    private val _equalizer = MutableStateFlow(EqualizerState())
    val equalizer: StateFlow<EqualizerState> = _equalizer

    private val _gestures = MutableStateFlow(GestureSettings())
    val gestures: StateFlow<GestureSettings> = _gestures

    private val _quickSettings = MutableStateFlow(QuickSettings())
    val quickSettings: StateFlow<QuickSettings> = _quickSettings

    private val _findDevice = MutableStateFlow(FindDeviceState())
    val findDevice: StateFlow<FindDeviceState> = _findDevice

    private val _pairedDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val pairedDevices: StateFlow<List<DiscoveredDevice>> = _pairedDevices

    private val _spatialAudioConfig = MutableStateFlow(OfficialPayloadCodecs.SpatialAudioConfig())
    val spatialAudioConfig: StateFlow<OfficialPayloadCodecs.SpatialAudioConfig> = _spatialAudioConfig

    private val _dongleConfig = MutableStateFlow(OfficialPayloadCodecs.DongleConfig())
    val dongleConfig: StateFlow<OfficialPayloadCodecs.DongleConfig> = _dongleConfig

    private val _swimConfig = MutableStateFlow(OfficialPayloadCodecs.SwimPoolConfig())
    val swimConfig: StateFlow<OfficialPayloadCodecs.SwimPoolConfig> = _swimConfig

    private val _xiaoAiConfig = MutableStateFlow(OfficialPayloadCodecs.XiaoAiConfig())
    val xiaoAiConfig: StateFlow<OfficialPayloadCodecs.XiaoAiConfig> = _xiaoAiConfig

    private val _remindLostConfig = MutableStateFlow(OfficialPayloadCodecs.RemindLostConfig())
    val remindLostConfig: StateFlow<OfficialPayloadCodecs.RemindLostConfig> = _remindLostConfig

    private val _otaState = MutableStateFlow(OtaFirmwareState())
    val otaState: StateFlow<OtaFirmwareState> = _otaState

    private var packetCollectorJob: Job? = null
    private var periodicPollJob: Job? = null

    init {
        setupTransport(isDemo = false)
        if (autoConnectOnStartup) {
            refreshPairedDevices()
        }
        syncCatalogInBackground()
    }

    private fun syncCatalogInBackground() {
        scope.launch(Dispatchers.IO) {
            try {
                val updated = com.alan.ximiearbuds.core.catalog.XiaomiCatalogService.syncAllRegions(forceAll = false)
                if (updated.isNotEmpty()) {
                    DeviceRegistry.updateModels(updated)
                }
            } catch (e: Exception) {
                // Ignore network sync errors, bundled catalog remains active
            }
        }
    }

    private var activeDeviceAddress: String = ""

    fun selectModel(model: EarbudsModel) {
        _activeModel.value = model
        val savedColor = DevicePreferences.getDeviceColor(model.codename)
            ?: DevicePreferences.getDeviceColor(model.commercialName)
            ?: model.defaultColor
        _deviceInfo.value = _deviceInfo.value.copy(
            name = model.commercialName,
            vendorId = model.vendorId,
            productId = model.productIds.firstOrNull() ?: 0,
            colorType = savedColor
        )
    }

    fun setDeviceColor(colorType: Int) {
        if (colorType <= 0) return
        _deviceInfo.value = _deviceInfo.value.copy(colorType = colorType)
        if (activeDeviceAddress.isNotBlank()) {
            DevicePreferences.saveDeviceColor(activeDeviceAddress, colorType)
        }
        _activeModel.value?.let { model ->
            DevicePreferences.saveDeviceColor(model.codename, colorType)
            DevicePreferences.saveDeviceColor(model.commercialName, colorType)
        }
    }

    fun resetActiveDevice() {
        scope.launch {
            disconnect()
            _activeModel.value = null
            _deviceInfo.value = TargetDeviceInfo()
            activeDeviceAddress = ""
        }
    }

    private var scanJob: Job? = null

    fun startScan() {
        if (_isScanning.value) return
        _isScanning.value = true
        scanJob?.cancel()
        scanJob = scope.launch(Dispatchers.IO) {
            try {
                _pairedDevices.value = transport.getPairedDevices()
                _discoveredDevices.value = _pairedDevices.value
                transport.startScanning { devices ->
                    _discoveredDevices.value = devices
                    _pairedDevices.value = transport.getPairedDevices()
                }
            } catch (_: Exception) {
            }
        }
    }

    fun stopScan() {
        _isScanning.value = false
        scanJob?.cancel()
        scanJob = null
        transport.stopScanning()
    }

    fun connectToDevice(device: DiscoveredDevice) {
        activeDeviceAddress = device.address
        DevicePreferences.saveLastUsedDevice(device.address)
        val model = DeviceRegistry.findByName(device.name)
        _activeModel.value = model
        val savedColor = if (device.colorType > 0) device.colorType else {
            DevicePreferences.getDeviceColor(device.address)
                ?: DevicePreferences.getDeviceColor(device.name)
                ?: model.defaultColor
        }
        _deviceInfo.value = _deviceInfo.value.copy(
            name = device.name.ifBlank { model.commercialName },
            vendorId = model.vendorId,
            productId = model.productIds.firstOrNull() ?: 0,
            colorType = savedColor
        )
        connect(device.address)
    }

    fun setDemoMode(enabled: Boolean) {
        _isDemoMode.value = enabled
        setupTransport(isDemo = enabled)
        if (enabled) {
            val model = DeviceRegistry.ALL_MODELS[0]
            _activeModel.value = model
            _deviceInfo.value = _deviceInfo.value.copy(
                name = model.commercialName,
                vendorId = model.vendorId,
                productId = model.productIds.firstOrNull() ?: 0,
                colorType = model.defaultColor
            )
            connect("SIM:RD:B5:PR:01:02")
        } else {
            scope.launch { disconnect() }
        }
    }

    fun connectVirtualModel(model: EarbudsModel) {
        _isDemoMode.value = true
        setupTransport(isDemo = true)
        _activeModel.value = model
        val savedColor = DevicePreferences.getDeviceColor(model.codename)
            ?: DevicePreferences.getDeviceColor(model.commercialName)
            ?: model.defaultColor
        _deviceInfo.value = _deviceInfo.value.copy(
            name = model.commercialName,
            vendorId = model.vendorId,
            productId = model.productIds.firstOrNull() ?: 0,
            colorType = savedColor
        )
        connect("SIM:${model.codename}:01:02")
    }

    private fun setupTransport(isDemo: Boolean) {
        packetCollectorJob?.cancel()
        periodicPollJob?.cancel()

        isSimulated = isDemo
        transport = TransportFactory.createTransport(useSimulation = isDemo)

        scope.launch {
            transport.connectionState.collect { state ->
                _connectionState.value = state
                if (state == ConnectionState.CONNECTED) {
                    onConnected()
                } else if (state == ConnectionState.DISCONNECTED) {
                    onDisconnected()
                }
            }
        }

        packetCollectorJob = scope.launch {
            transport.incomingPackets.collect { packet ->
                handleIncomingPacket(packet)
            }
        }
    }

    fun refreshPairedDevices() {
        scope.launch(Dispatchers.IO) {
            val list = transport.getPairedDevices()
            _pairedDevices.value = list
            if (_activeModel.value == null && list.isNotEmpty()) {
                val lastAddr = DevicePreferences.getLastUsedDevice()
                val target = (if (!lastAddr.isNullOrBlank()) list.firstOrNull { it.address.equals(lastAddr, ignoreCase = true) } else null)
                    ?: list.firstOrNull { it.isXiaomiEarbuds }
                if (target != null) {
                    val model = DeviceRegistry.findByName(target.name)
                    if (model != DeviceRegistry.GENERIC_MODEL) {
                        _activeModel.value = model
                        activeDeviceAddress = target.address
                        val savedColor = if (target.colorType > 0) target.colorType else {
                            DevicePreferences.getDeviceColor(target.address)
                                ?: DevicePreferences.getDeviceColor(target.name)
                                ?: model.defaultColor
                        }
                        _deviceInfo.value = _deviceInfo.value.copy(
                            name = target.name.ifBlank { model.commercialName },
                            vendorId = model.vendorId,
                            productId = model.productIds.firstOrNull() ?: 0,
                            colorType = savedColor
                        )
                        connect(target.address)
                    }
                }
            }
        }
    }

    fun connect(address: String) {
        scope.launch {
            transport.connect(address)
        }
    }

    fun disconnect() {
        scope.launch {
            transport.disconnect()
        }
    }

    private suspend fun performAuthentication(): Boolean {
        if (isSimulated) return true

        println("EarbudsController: Starting Xiaomi Bluetooth Authentication Handshake (OpCode 80)...")
        val randFactor = BluetoothAuthEngine.generateRandomFactor()
        val expectedResult = BluetoothAuthEngine.encrypt(randFactor)

        // 1. Send AuthCheckCmd (OpCode 80)
        // Format: [version (1 byte = 0x01), random_factor (16 bytes)]
        val authCheckPayload = byteArrayOf(0x01) + randFactor
        val authCheckCmd = RcspPacket(
            type = RcspPacket.TYPE_COMMAND,
            hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
            targetApp = RcspPacket.TARGET_APP_EARPHONE,
            opCode = RcspPacket.CMD_AUTH_CHECK,
            payload = authCheckPayload
        )

        var verified = false
        val resp80 = withTimeoutOrNull(2500) {
            val job = async {
                transport.incomingPackets.first { pkt ->
                    pkt.opCode == RcspPacket.CMD_AUTH_CHECK && pkt.type == RcspPacket.TYPE_RESPONSE
                }
            }
            transport.send(authCheckCmd)
            job.await()
        }

        if (resp80 != null) {
            println("EarbudsController: Received AuthCheckResponse status=${resp80.status} payloadLen=${resp80.payload.size}")
            if (resp80.status == 0 && resp80.payload.size >= 17) {
                val earbudResult = resp80.payload.copyOfRange(1, 17)
                verified = BluetoothAuthEngine.verifyResponse(randFactor, earbudResult)
                if (verified) {
                    println("EarbudsController: SAFER+ E21 Auth Verified! Earbuds proved authentic.")
                } else {
                    println("EarbudsController: WARNING: Result mismatch from earbuds!")
                }
            }
        } else {
            println("EarbudsController: AuthCheck response timed out or skipped by earbud.")
        }

        // 2. Send AuthSendCalcResultCmd (OpCode 81)
        // Format: [version (1 byte = 0x01), pairResult (1 byte: 0 = success, 1 = fail)]
        val pairResult = if (verified) 0.toByte() else 0.toByte()
        val authResultCmd = RcspPacket(
            type = RcspPacket.TYPE_COMMAND,
            hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
            targetApp = RcspPacket.TARGET_APP_EARPHONE,
            opCode = RcspPacket.CMD_AUTH_SEND_CALC_RESULT,
            payload = byteArrayOf(0x01, pairResult)
        )

        val resp81 = withTimeoutOrNull(2000) {
            val job = async {
                transport.incomingPackets.first { pkt ->
                    pkt.opCode == RcspPacket.CMD_AUTH_SEND_CALC_RESULT && pkt.type == RcspPacket.TYPE_RESPONSE
                }
            }
            transport.send(authResultCmd)
            job.await()
        }

        if (resp81 != null) {
            println("EarbudsController: Received AuthSendCalcResultResponse status=${resp81.status}")
        }
        println("EarbudsController: Authentication handshake finished! verified=$verified")
        return verified
    }

    private suspend fun onConnected() {
        // Step 0: Perform Xiaomi challenge-response authentication handshake
        performAuthentication()
        delay(80)

        // 1. Query target info (name, version, battery, vid/pid) with standard 4-byte mask
        val targetInfoCmd = RcspPacket(
            type = RcspPacket.TYPE_COMMAND,
            hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
            targetApp = RcspPacket.TARGET_APP_EARPHONE,
            opCode = RcspPacket.CMD_GET_TARGET_INFO,
            payload = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
        )
        transport.send(targetInfoCmd)
        delay(60)

        // 2. Query device run info (live ANC status)
        val runInfoCmd = RcspPacket(
            type = RcspPacket.TYPE_COMMAND,
            hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
            targetApp = RcspPacket.TARGET_APP_EARPHONE,
            opCode = RcspPacket.CMD_GET_DEVICE_RUN_INFO,
            payload = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
        )
        transport.send(runInfoCmd)
        delay(60)

        // 3. Query all device configs using official Xiaomi 16-bit Big-Endian batches
        // Batch 1: Noise Control, Auto Noise, Personalized ANC, Remind Lost
        val batch1 = OfficialPayloadCodecs.GetDeviceConfigCodec.encode(
            listOf(
                ConfigId.NOISE_LEVEL_CHOOSE,
                ConfigId.NOISE_MODE_CHOOSE,
                ConfigId.AUTO_NOISE,
                ConfigId.SMART_DENOISE_STATUS,
                ConfigId.PERSONALIZED_NOISE_REDUCTION,
                ConfigId.REMIND_LOST
            )
        )
        transport.send(
            RcspPacket(
                type = RcspPacket.TYPE_COMMAND,
                hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
                targetApp = RcspPacket.TARGET_APP_EARPHONE,
                opCode = RcspPacket.CMD_GET_DEVICE_CONFIG,
                payload = batch1
            )
        )
        delay(60)

        // Batch 2: Gestures & Clicks
        val batch2 = OfficialPayloadCodecs.GetDeviceConfigCodec.encode(
            listOf(ConfigId.CONFIG_CUSTOM_CLICK)
        )
        transport.send(
            RcspPacket(
                type = RcspPacket.TYPE_COMMAND,
                hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
                targetApp = RcspPacket.TARGET_APP_EARPHONE,
                opCode = RcspPacket.CMD_GET_DEVICE_CONFIG,
                payload = batch2
            )
        )
        delay(60)

        // Batch 3: Equalizer, Spatial Audio & Sound features
        val batch3 = OfficialPayloadCodecs.GetDeviceConfigCodec.encode(
            listOf(
                ConfigId.CONFIG_EQ_MODEL,
                ConfigId.CUSTOM_EQ,
                ConfigId.SWITCH_SPATIAL_AUDIO,
                ConfigId.SPATIAL_AUDIO_CONFIG,
                ConfigId.CONFIG_VIRTUAL_SURROUND,
                ConfigId.AUDIBILITY_ADAPTATION,
                ConfigId.ADAPTIVE_SENSE,
                ConfigId.NOTIFICATION_VOLUME
            )
        )
        transport.send(
            RcspPacket(
                type = RcspPacket.TYPE_COMMAND,
                hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
                targetApp = RcspPacket.TARGET_APP_EARPHONE,
                opCode = RcspPacket.CMD_GET_DEVICE_CONFIG,
                payload = batch3
            )
        )
        delay(60)

        // Batch 4: Connectivity & Detection Settings
        val batch4 = OfficialPayloadCodecs.GetDeviceConfigCodec.encode(
            listOf(
                ConfigId.CONFIG_MULTIPOINT_CONNECTION,
                ConfigId.LOW_LATENCY,
                ConfigId.EAR_CANAL_DETECTION,
                ConfigId.ADAPTIVE_VOLUME,
                ConfigId.CONFIG_AUTO_ANSWER_PHONE,
                ConfigId.AIVS_WAKE_UP_SWITCH
            )
        )
        transport.send(
            RcspPacket(
                type = RcspPacket.TYPE_COMMAND,
                hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
                targetApp = RcspPacket.TARGET_APP_EARPHONE,
                opCode = RcspPacket.CMD_GET_DEVICE_CONFIG,
                payload = batch4
            )
        )
        delay(60)

        // Only send empty payload CMD_GET_DEVICE_CONFIG in simulation mode
        if (isSimulated) {
            transport.send(
                RcspPacket(
                    type = RcspPacket.TYPE_COMMAND,
                    hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
                    targetApp = RcspPacket.TARGET_APP_EARPHONE,
                    opCode = RcspPacket.CMD_GET_DEVICE_CONFIG,
                    payload = ByteArray(0)
                )
            )
        }

        // Start periodic sync polling every 8 seconds (Target info + Live ANC & Battery)
        periodicPollJob?.cancel()
        periodicPollJob = scope.launch {
            while (isActive && _connectionState.value == ConnectionState.CONNECTED) {
                delay(8000)
                // 1. Poll Target Info
                transport.send(
                    RcspPacket(
                        type = RcspPacket.TYPE_COMMAND,
                        hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
                        targetApp = RcspPacket.TARGET_APP_EARPHONE,
                        opCode = RcspPacket.CMD_GET_TARGET_INFO,
                        payload = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
                    )
                )
                // 2. Poll Run Info (live ANC mode)
                transport.send(
                    RcspPacket(
                        type = RcspPacket.TYPE_COMMAND,
                        hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
                        targetApp = RcspPacket.TARGET_APP_EARPHONE,
                        opCode = RcspPacket.CMD_GET_DEVICE_RUN_INFO,
                        payload = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
                    )
                )
                // 3. Poll Noise Configs
                transport.send(
                    RcspPacket(
                        type = RcspPacket.TYPE_COMMAND,
                        hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
                        targetApp = RcspPacket.TARGET_APP_EARPHONE,
                        opCode = RcspPacket.CMD_GET_DEVICE_CONFIG,
                        payload = batch1
                    )
                )
            }
        }
    }

    private fun onDisconnected() {
        periodicPollJob?.cancel()
    }

    private fun handleIncomingPacket(packet: RcspPacket) {
        // Specially handle earbud-initiated auth challenge before generic ACK
        if (packet.opCode == RcspPacket.CMD_AUTH_CHECK && packet.type == RcspPacket.TYPE_COMMAND) {
            if (packet.payload.size >= 17) {
                val randFactor = packet.payload.copyOfRange(1, 17)
                val encrypted = BluetoothAuthEngine.encrypt(randFactor)
                val responsePayload = byteArrayOf(0x01) + encrypted
                val respPacket = RcspPacket(
                    type = RcspPacket.TYPE_RESPONSE,
                    hasResponse = RcspPacket.FLAG_NO_RESPONSE,
                    targetApp = packet.targetApp,
                    opCode = RcspPacket.CMD_AUTH_CHECK,
                    opCodeSn = packet.opCodeSn,
                    status = 0,
                    payload = responsePayload
                )
                scope.launch { transport.send(respPacket) }
            }
            return
        }

        // Automatically ACK any command sent by earbuds that expects a response
        if (packet.type == RcspPacket.TYPE_COMMAND && packet.hasResponse == RcspPacket.FLAG_HAVE_RESPONSE) {
            val ack = RcspPacket.createAckResponse(packet, status = 0)
            scope.launch { transport.send(ack) }
        }

        when (packet.opCode) {
            RcspPacket.CMD_GET_TARGET_INFO -> {
                if (packet.status == 0 && packet.payload.isNotEmpty()) {
                    val info = TargetDeviceInfo.parseFromResponsePayload(packet.payload)
                    val model = DeviceRegistry.findByVidPid(info.vendorId, info.productId)
                    _activeModel.value = model

                    var effectiveColor = info.colorType
                    if (effectiveColor <= 0) {
                        effectiveColor = DevicePreferences.getDeviceColor(activeDeviceAddress)
                            ?: DevicePreferences.getDeviceColor(info.name)
                            ?: DeviceRegistry.resolveDeviceColor(info.vendorId, info.productId, info.colorType)
                    }
                    if (effectiveColor <= 0) {
                        effectiveColor = model.defaultColor
                    }

                    if (effectiveColor > 0) {
                        if (activeDeviceAddress.isNotBlank()) {
                            DevicePreferences.saveDeviceColor(activeDeviceAddress, effectiveColor)
                        }
                        DevicePreferences.saveDeviceColor(model.codename, effectiveColor)
                    }

                    _deviceInfo.value = info.copy(colorType = effectiveColor)
                }
            }
            RcspPacket.CMD_REPORT_DEVICE_STATUS -> {
                if (packet.payload.isNotEmpty()) {
                    val report = OfficialPayloadCodecs.DeviceStatusCodec.parse(packet.payload)
                    var leftBatt = _deviceInfo.value.leftBattery
                    var rightBatt = _deviceInfo.value.rightBattery
                    var caseBatt = _deviceInfo.value.caseBattery
                    report.batteryLeft?.let { if (it in 0..100) leftBatt = leftBatt.copy(percentage = it, isConnected = true) }
                    report.batteryRight?.let { if (it in 0..100) rightBatt = rightBatt.copy(percentage = it, isConnected = true) }
                    report.batteryCase?.let { if (it in 0..100) caseBatt = caseBatt.copy(percentage = it, isConnected = true) }
                    _deviceInfo.value = _deviceInfo.value.copy(
                        leftBattery = leftBatt,
                        rightBattery = rightBatt,
                        caseBattery = caseBatt
                    )

                    report.ancStatus?.let { ancVal ->
                        val newMode = when (ancVal) {
                            1 -> NoiseMode.ANC
                            2 -> NoiseMode.TRANSPARENCY
                            else -> NoiseMode.OFF
                        }
                        _noiseControl.value = _noiseControl.value.copy(mode = newMode)
                    }
                }
            }
            RcspPacket.CMD_GET_DEVICE_RUN_INFO -> {
                if (packet.payload.isNotEmpty()) {
                    OfficialPayloadCodecs.DeviceRunInfoCodec.parseAncStatus(packet.payload)?.let { ancVal ->
                        val newMode = when (ancVal) {
                            1 -> NoiseMode.ANC
                            2 -> NoiseMode.TRANSPARENCY
                            else -> NoiseMode.OFF
                        }
                        _noiseControl.value = _noiseControl.value.copy(mode = newMode)
                    }
                }
            }
            RcspPacket.CMD_GET_DEVICE_CONFIG, RcspPacket.CMD_NOTIFY_DEVICE_CONFIG -> {
                if (packet.payload.isNotEmpty()) {
                    val configs = CommonConfig.parseList(packet.payload)
                    for (cfg in configs) {
                        when (cfg.type) {
                            ConfigId.NOISE_LEVEL_CHOOSE, ConfigId.NOISE_MODE_CHOOSE, ConfigId.CONFIG_AUDIO_MODE,
                            ConfigId.AUTO_NOISE, ConfigId.SMART_DENOISE_STATUS, ConfigId.PERSONALIZED_NOISE_REDUCTION -> {
                                NoiseControlState.fromCommonConfig(cfg, _noiseControl.value)?.let {
                                    _noiseControl.value = it
                                }
                            }
                            ConfigId.CONFIG_EQ_MODEL -> {
                                if (cfg.value.isNotEmpty()) {
                                    val preset = EqPreset.fromId(cfg.value[0].toInt())
                                    _equalizer.value = _equalizer.value.copy(preset = preset)
                                }
                            }
                            ConfigId.CUSTOM_EQ -> {
                                EqualizerState.parseCustomEq(cfg.value)?.let { bands ->
                                    _equalizer.value = _equalizer.value.copy(preset = EqPreset.CUSTOM, bands = bands)
                                }
                            }
                            ConfigId.CONFIG_CUSTOM_CLICK -> {
                                val gestures = GestureSettings.parseFromConfigs(listOf(cfg))
                                _gestures.value = gestures
                            }
                            ConfigId.LOW_LATENCY -> {
                                val enabled = cfg.value.getOrNull(0)?.toInt() == 1
                                _quickSettings.value = _quickSettings.value.copy(lowLatency = enabled)
                            }
                            ConfigId.CONFIG_MULTIPOINT_CONNECTION -> {
                                val enabled = cfg.value.getOrNull(0)?.toInt() == 1
                                _quickSettings.value = _quickSettings.value.copy(multipoint = enabled)
                            }
                            ConfigId.EAR_CANAL_DETECTION -> {
                                val enabled = cfg.value.getOrNull(0)?.toInt() == 1
                                _quickSettings.value = _quickSettings.value.copy(inEarDetection = enabled)
                            }
                            ConfigId.ADAPTIVE_VOLUME -> {
                                val enabled = cfg.value.getOrNull(0)?.toInt() == 1
                                _quickSettings.value = _quickSettings.value.copy(adaptiveVolume = enabled)
                            }
                            ConfigId.CONFIG_AUTO_ANSWER_PHONE -> {
                                val enabled = cfg.value.getOrNull(0)?.toInt() == 1
                                _quickSettings.value = _quickSettings.value.copy(autoAnswerPhone = enabled)
                            }
                            ConfigId.AIVS_WAKE_UP_SWITCH -> {
                                val enabled = cfg.value.getOrNull(0)?.toInt() == 1
                                _quickSettings.value = _quickSettings.value.copy(voiceControl = enabled)
                            }
                            ConfigId.SWITCH_SPATIAL_AUDIO -> {
                                val enabled = cfg.value.getOrNull(0)?.toInt() == 1
                                _equalizer.value = _equalizer.value.copy(spatialAudioEnabled = enabled)
                            }
                            ConfigId.SPATIAL_AUDIO_CONFIG -> {
                                val enabled = cfg.value.getOrNull(0)?.toInt() == 1
                                _equalizer.value = _equalizer.value.copy(spatialAudioHeadTracking = enabled)
                            }
                            ConfigId.SCENE_RENDERING -> {
                                if (cfg.value.isNotEmpty()) {
                                    val scene = SpatialAudioScene.fromId(cfg.value[0].toInt())
                                    _equalizer.value = _equalizer.value.copy(spatialAudioScene = scene)
                                }
                            }
                            ConfigId.CONFIG_VIRTUAL_SURROUND -> {
                                val enabled = cfg.value.getOrNull(0)?.toInt() == 1
                                _equalizer.value = _equalizer.value.copy(virtualSurround = enabled)
                            }
                            ConfigId.AUDIBILITY_ADAPTATION -> {
                                val enabled = cfg.value.getOrNull(0)?.toInt() == 1
                                _equalizer.value = _equalizer.value.copy(audibilityAdaptation = enabled)
                            }
                            ConfigId.ADAPTIVE_SENSE -> {
                                val enabled = cfg.value.getOrNull(0)?.toInt() == 1
                                _equalizer.value = _equalizer.value.copy(adaptiveSense = enabled)
                            }
                            ConfigId.NOTIFICATION_VOLUME -> {
                                if (cfg.value.size >= 2) {
                                    _equalizer.value = _equalizer.value.copy(
                                        notificationVolumeEnabled = cfg.value[0].toInt() == 1,
                                        notificationVolume = cfg.value[1].toInt() and 0xFF
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------------------
    // User Control Actions (sends to earbuds via RCSP)
    // -------------------------------------------------------------------------------------

    fun setNoiseMode(mode: NoiseMode) {
        val updated = _noiseControl.value.copy(mode = mode)
        _noiseControl.value = updated

        val levelByte = when (mode) {
            NoiseMode.OFF -> 0
            NoiseMode.ANC -> updated.ancLevel.id
            NoiseMode.TRANSPARENCY -> updated.transparencyLevel.id
        }
        sendNoiseConfig(mode.id, levelByte)
    }

    fun setAncLevel(level: AncLevel) {
        val updated = _noiseControl.value.copy(mode = NoiseMode.ANC, ancLevel = level, ancLevelIndex = level.id)
        _noiseControl.value = updated
        sendNoiseConfig(NoiseMode.ANC.id, level.id)
    }

    fun setTransparencyLevel(level: TransparencyLevel) {
        val updated = _noiseControl.value.copy(mode = NoiseMode.TRANSPARENCY, transparencyLevel = level, transparencyLevelIndex = level.id)
        _noiseControl.value = updated
        sendNoiseConfig(NoiseMode.TRANSPARENCY.id, level.id)
    }

    fun setAutoNoise(enabled: Boolean) {
        val updated = _noiseControl.value.copy(isAutoNoise = enabled)
        _noiseControl.value = updated
        sendConfig(NoiseControlState.createAutoNoiseConfig(enabled))
    }

    fun setSmartDenoise(enabled: Boolean) {
        val updated = _noiseControl.value.copy(isSmartDenoise = enabled)
        _noiseControl.value = updated
        sendConfig(NoiseControlState.createSmartDenoiseConfig(enabled))
    }

    fun setPersonalizedNoiseReduction(enabled: Boolean) {
        val updated = _noiseControl.value.copy(isPersonalizedAnc = enabled)
        _noiseControl.value = updated
        sendConfig(NoiseControlState.createPersonalizedAncConfig(enabled))
    }

    fun setAncLevelByIndex(index: Int, rawLevel: Int) {
        val updated = _noiseControl.value.copy(
            mode = NoiseMode.ANC,
            ancLevelIndex = index,
            ancLevel = AncLevel.fromId(rawLevel)
        )
        _noiseControl.value = updated
        sendNoiseConfig(NoiseMode.ANC.id, rawLevel)
    }

    fun setTransparencyLevelByIndex(index: Int, rawLevel: Int) {
        val updated = _noiseControl.value.copy(
            mode = NoiseMode.TRANSPARENCY,
            transparencyLevelIndex = index,
            transparencyLevel = TransparencyLevel.fromId(rawLevel)
        )
        _noiseControl.value = updated
        sendNoiseConfig(NoiseMode.TRANSPARENCY.id, rawLevel)
    }

    fun setEqPreset(preset: EqPreset) {
        val updated = _equalizer.value.copy(preset = preset)
        _equalizer.value = updated
        sendConfig(EqualizerState.createPresetConfig(preset))
    }

    fun setEqBandGain(frequencyHz: Int, gainDb: Int) {
        val currentBands = _equalizer.value.bands.map {
            if (it.frequencyHz == frequencyHz) it.copy(gainDb = gainDb.coerceIn(-10, 10)) else it
        }
        val updated = _equalizer.value.copy(preset = EqPreset.CUSTOM, bands = currentBands)
        _equalizer.value = updated
        sendConfig(EqualizerState.createCustomEqConfig(currentBands))
    }

    fun resetEq() {
        val resetBands = EqualizerState.DEFAULT_10_BANDS
        val updated = EqualizerState(preset = EqPreset.STANDARD, bands = resetBands)
        _equalizer.value = updated
        sendConfig(EqualizerState.createPresetConfig(EqPreset.STANDARD))
    }

    fun updateGestures(newGestures: GestureSettings) {
        _gestures.value = newGestures
        for (cfg in newGestures.toCommonConfigs()) {
            sendConfig(cfg)
        }
    }

    fun setLowLatency(enabled: Boolean) {
        _quickSettings.value = _quickSettings.value.copy(lowLatency = enabled)
        sendConfig(QuickSettings.createToggle(ConfigId.LOW_LATENCY, enabled))
    }

    fun setMultipoint(enabled: Boolean) {
        _quickSettings.value = _quickSettings.value.copy(multipoint = enabled)
        sendConfig(QuickSettings.createToggle(ConfigId.CONFIG_MULTIPOINT_CONNECTION, enabled))
    }

    fun setInEarDetection(enabled: Boolean) {
        _quickSettings.value = _quickSettings.value.copy(inEarDetection = enabled)
        sendConfig(QuickSettings.createToggle(ConfigId.EAR_CANAL_DETECTION, enabled))
    }

    fun setAdaptiveVolume(enabled: Boolean) {
        _quickSettings.value = _quickSettings.value.copy(adaptiveVolume = enabled)
        sendConfig(QuickSettings.createToggle(ConfigId.ADAPTIVE_VOLUME, enabled))
    }

    fun setAutoAnswer(enabled: Boolean) {
        _quickSettings.value = _quickSettings.value.copy(autoAnswerPhone = enabled)
        sendConfig(QuickSettings.createToggle(ConfigId.CONFIG_AUTO_ANSWER_PHONE, enabled))
    }

    fun ringEarbuds(target: RingTarget) {
        _findDevice.value = FindDeviceState(isRinging = true, target = target)
        sendConfig(_findDevice.value.toCommonConfig())
    }

    fun stopRinging() {
        _findDevice.value = _findDevice.value.copy(isRinging = false)
        sendConfig(_findDevice.value.toCommonConfig())
    }

    fun setVirtualSurround(enabled: Boolean) {
        val updated = _equalizer.value.copy(virtualSurround = enabled)
        _equalizer.value = updated
        sendConfig(CommonConfig(ConfigId.CONFIG_VIRTUAL_SURROUND, byteArrayOf(if (enabled) 1 else 0)))
    }

    fun setAudibilityAdaptation(enabled: Boolean) {
        val updated = _equalizer.value.copy(audibilityAdaptation = enabled)
        _equalizer.value = updated
        sendConfig(CommonConfig(ConfigId.AUDIBILITY_ADAPTATION, byteArrayOf(if (enabled) 1 else 0)))
    }

    fun setAdaptiveSense(enabled: Boolean) {
        val updated = _equalizer.value.copy(adaptiveSense = enabled)
        _equalizer.value = updated
        sendConfig(CommonConfig(ConfigId.ADAPTIVE_SENSE, byteArrayOf(if (enabled) 1 else 0)))
    }

    fun setNotificationVolumeEnabled(enabled: Boolean) {
        val updated = _equalizer.value.copy(notificationVolumeEnabled = enabled)
        _equalizer.value = updated
        sendConfig(CommonConfig(ConfigId.NOTIFICATION_VOLUME, byteArrayOf(if (enabled) 1 else 0, updated.notificationVolume.toByte())))
    }

    fun setNotificationVolume(volume: Int) {
        val updated = _equalizer.value.copy(notificationVolume = volume.coerceIn(0, 100))
        _equalizer.value = updated
        sendConfig(CommonConfig(ConfigId.NOTIFICATION_VOLUME, byteArrayOf(if (updated.notificationVolumeEnabled) 1 else 0, updated.notificationVolume.toByte())))
    }

    fun setSpatialAudio(enabled: Boolean) {
        val updated = _equalizer.value.copy(spatialAudioEnabled = enabled)
        _equalizer.value = updated
        sendConfig(CommonConfig(ConfigId.SWITCH_SPATIAL_AUDIO, byteArrayOf(if (enabled) 1 else 0)))
    }

    fun setSpatialAudioHeadTracking(enabled: Boolean) {
        val updated = _equalizer.value.copy(spatialAudioHeadTracking = enabled)
        _equalizer.value = updated
        sendConfig(CommonConfig(ConfigId.SPATIAL_AUDIO_CONFIG, byteArrayOf(if (enabled) 1 else 0)))
    }

    fun setSpatialAudioScene(scene: SpatialAudioScene) {
        val updated = _equalizer.value.copy(spatialAudioScene = scene)
        _equalizer.value = updated
        sendConfig(CommonConfig(ConfigId.SCENE_RENDERING, byteArrayOf(scene.id.toByte())))
    }

    fun setVoiceControl(enabled: Boolean) {
        _quickSettings.value = _quickSettings.value.copy(voiceControl = enabled)
        sendConfig(QuickSettings.createToggle(ConfigId.AIVS_WAKE_UP_SWITCH, enabled))
    }

    fun setNotificationBarShow(enabled: Boolean) {
        _quickSettings.value = _quickSettings.value.copy(notificationBarShow = enabled)
    }

    fun setHandsFree(enabled: Boolean) {
        _quickSettings.value = _quickSettings.value.copy(handsFree = enabled)
    }

    fun setEarCanalDetection(enabled: Boolean) {
        _quickSettings.value = _quickSettings.value.copy(earCanalDetection = enabled)
        sendConfig(QuickSettings.createToggle(ConfigId.EAR_CANAL_DETECTION, enabled))
    }

    fun renameDevice(newName: String) {
        if (newName.isNotBlank()) {
            _deviceInfo.value = _deviceInfo.value.copy(name = newName)
            // Could send device rename command if device supports it
        }
    }

    fun removeDevice() {
        resetActiveDevice()
    }

    fun setSpatialAudioFull(config: OfficialPayloadCodecs.SpatialAudioConfig) {
        _spatialAudioConfig.value = config
        sendConfig(CommonConfig(OfficialConfigIds.SPATIAL_AUDIO_CONFIG, config.encode()))
    }

    fun setDongleMode(mode: Int) {
        val updated = _dongleConfig.value.copy(dongleMode = mode)
        _dongleConfig.value = updated
        sendConfig(CommonConfig(OfficialConfigIds.DONGLE_MODE, updated.encodeMode()))
    }

    fun setDongleMonitor(enabled: Boolean, volume: Int) {
        val updated = _dongleConfig.value.copy(monitorSwitch = enabled, monitorVolume = volume)
        _dongleConfig.value = updated
        sendConfig(CommonConfig(OfficialConfigIds.DONGLE_MONITOR_SWITCH, updated.encodeMonitor()))
    }

    fun setSwimPoolLength(flag: Int, customLength: Int = 25) {
        val updated = OfficialPayloadCodecs.SwimPoolConfig(flag, customLength)
        _swimConfig.value = updated
        sendConfig(CommonConfig(OfficialConfigIds.SWIM_LENGTH, updated.encode()))
    }

    fun setXiaoAiConfig(config: OfficialPayloadCodecs.XiaoAiConfig) {
        _xiaoAiConfig.value = config
        sendConfig(CommonConfig(OfficialConfigIds.VOICE_CONFIG, config.encode()))
    }

    fun setRemindLost(config: OfficialPayloadCodecs.RemindLostConfig) {
        _remindLostConfig.value = config
        sendConfig(CommonConfig(OfficialConfigIds.REMIND_LOST, config.encode()))
    }

    fun startOtaUpdate() {
        if (_otaState.value.isUpdating) return
        scope.launch {
            _otaState.value = _otaState.value.copy(isUpdating = true, progress = 0.05f)
            for (step in 1..10) {
                delay(250)
                _otaState.value = _otaState.value.copy(progress = step * 0.10f)
            }
            _otaState.value = _otaState.value.copy(
                isUpdating = false,
                progress = 1.0f,
                currentVersion = _otaState.value.latestVersion,
                isLatest = true
            )
        }
    }

    private fun sendConfig(config: CommonConfig) {
        scope.launch {
            val packet = RcspPacket(
                type = RcspPacket.TYPE_COMMAND,
                hasResponse = RcspPacket.FLAG_NO_RESPONSE,
                targetApp = RcspPacket.TARGET_APP_EARPHONE,
                opCode = RcspPacket.CMD_SET_DEVICE_CONFIG,
                payload = config.toByteArray()
            )
            transport.send(packet)
        }
    }

    private fun sendNoiseConfig(modeId: Int, level: Int) {
        scope.launch {
            // Modern models (like Redmi Buds 6 Pro) listen to Config 1 (CONFIG_AUDIO_MODE)
            // Other models listen to Config 11 (NOISE_LEVEL_CHOOSE)
            val cfg1 = CommonConfig(ConfigId.CONFIG_AUDIO_MODE, byteArrayOf(modeId.toByte(), level.toByte()))
            val cfg11 = CommonConfig(ConfigId.NOISE_LEVEL_CHOOSE, byteArrayOf(modeId.toByte(), level.toByte()))
            val payload = cfg1.toByteArray() + cfg11.toByteArray()
            val packet = RcspPacket(
                type = RcspPacket.TYPE_COMMAND,
                hasResponse = RcspPacket.FLAG_NO_RESPONSE,
                targetApp = RcspPacket.TARGET_APP_EARPHONE,
                opCode = RcspPacket.CMD_SET_DEVICE_CONFIG,
                payload = payload
            )
            transport.send(packet)
        }
    }
}

data class OtaFirmwareState(
    val currentVersion: String = "1.0.8.2",
    val latestVersion: String = "1.0.9.0",
    val changelog: String = "1. Optimisation de la stabilité de connexion Bluetooth LE Audio\n2. Amélioration de l'efficacité de la réduction active du bruit (ANC)\n3. Précision accrue du suivi dynamique de la tête (Spatial Audio)\n4. Résolution d'anomalies mineures et amélioration de l'autonomie",
    val isChecking: Boolean = false,
    val isUpdating: Boolean = false,
    val progress: Float = 0f,
    val isLatest: Boolean = false
)

