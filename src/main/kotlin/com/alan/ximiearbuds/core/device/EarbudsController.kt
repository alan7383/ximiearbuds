package com.alan.ximiearbuds.core.device

import com.alan.ximiearbuds.core.audio.FitDetectionAudioPlayer
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

    // Trajet immersif / Commuting immerse (config 103, fonction 1009).
    // Officiel : DeviceSettingsViewModel._commutingImmerse (défaut = close = 0),
    // DeviceConfigCommutingImmerse(status) 0=close 1=flight 2=subway 3=HSR.
    private val _commutingImmerseMode = MutableStateFlow(0)
    val commutingImmerseMode: StateFlow<Int> = _commutingImmerseMode

    private val _earboxSound = MutableStateFlow(EarboxSoundState())
    val earboxSound: StateFlow<EarboxSoundState> = _earboxSound

    private val _fitDetection = MutableStateFlow(FitDetectionState())
    val fitDetection: StateFlow<FitDetectionState> = _fitDetection

    private val _callListenerSeconds = MutableStateFlow(0)
    val callListenerSeconds: StateFlow<Int> = _callListenerSeconds

    private val _voiceHotword = MutableStateFlow(false)
    val voiceHotword: StateFlow<Boolean> = _voiceHotword

    private var packetCollectorJob: Job? = null
    private var periodicPollJob: Job? = null
    private var fitDetectionTimeoutJob: Job? = null

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
    private var isAuthenticating: Boolean = false

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

    fun release() {
        periodicPollJob?.cancel()
        packetCollectorJob?.cancel()
        scanJob?.cancel()
        disconnect()
    }

    private suspend fun isConnected(): Boolean =
        transport.connectionState.value == ConnectionState.CONNECTED

    private suspend fun performAuthentication(): Boolean {
        if (isSimulated) return true

        isAuthenticating = true
        try {
            // L'officiel (BluetoothAuth.w) attend ~500ms après l'ouverture SPP avant l'auth,
            // et certains modèles (ex: M75A LE Audio, cf. BluetoothAuth.h) n'exigent pas d'auth.
            delay(350)
            if (!isConnected()) {
                println("EarbudsController: Auth skipped, already disconnected.")
                return false
            }

            println("EarbudsController: Starting Xiaomi Bluetooth Authentication Handshake (OpCode 80)...")
            val randFactor = BluetoothAuthEngine.generateRandomFactor()

            // 1. Stage 1: Phone challenges Earbuds (OpCode 80)
            // Format: [version (1 byte = 0x01), random_factor (16 bytes)]
            val authCheckPayload = byteArrayOf(0x01) + randFactor
            val authCheckCmd = RcspPacket(
                type = RcspPacket.TYPE_COMMAND,
                hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
                targetApp = RcspPacket.TARGET_APP_EARPHONE,
                opCode = RcspPacket.CMD_AUTH_CHECK,
                opCodeSn = 1,
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
                return false
            }

            if (!isConnected()) {
                println("EarbudsController: Socket closed during auth, skipping OpCode 81.")
                return verified
            }

            // 2. Send AuthSendCalcResultCmd (OpCode 81)
            val pairResult = if (verified) 0.toByte() else 1.toByte()
            val authResultCmd = RcspPacket(
                type = RcspPacket.TYPE_COMMAND,
                hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
                targetApp = RcspPacket.TARGET_APP_EARPHONE,
                opCode = RcspPacket.CMD_AUTH_SEND_CALC_RESULT,
                opCodeSn = 2,
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

            // 3. Stage 2: Wait for earbud challenge to phone (AUTH_STAGE_SELF -> AUTH_STAGE_OK)
            println("EarbudsController: Waiting for earbud-initiated challenge (Stage 2)...")
            val earbudChallenge = withTimeoutOrNull(3000) {
                transport.incomingPackets.first { pkt ->
                    pkt.opCode == RcspPacket.CMD_AUTH_CHECK && pkt.type == RcspPacket.TYPE_COMMAND
                }
            }
            if (earbudChallenge != null && earbudChallenge.payload.size >= 17) {
                val earbudRand = earbudChallenge.payload.copyOfRange(1, 17)
                val phoneEncrypted = BluetoothAuthEngine.encrypt(earbudRand)
                val phoneResp80 = RcspPacket(
                    type = RcspPacket.TYPE_RESPONSE,
                    hasResponse = RcspPacket.FLAG_NO_RESPONSE,
                    targetApp = earbudChallenge.targetApp,
                    opCode = RcspPacket.CMD_AUTH_CHECK,
                    opCodeSn = earbudChallenge.opCodeSn,
                    status = 0,
                    payload = byteArrayOf(0x01) + phoneEncrypted
                )
                transport.send(phoneResp80)
                println("EarbudsController: Sent Phone AuthCheckResponse for Stage 2!")

                // Wait for earbud's OpCode 81 command
                val earbudResultCmd = withTimeoutOrNull(2000) {
                    transport.incomingPackets.first { pkt ->
                        pkt.opCode == RcspPacket.CMD_AUTH_SEND_CALC_RESULT && pkt.type == RcspPacket.TYPE_COMMAND
                    }
                }
                if (earbudResultCmd != null) {
                    val phoneResp81 = RcspPacket(
                        type = RcspPacket.TYPE_RESPONSE,
                        hasResponse = RcspPacket.FLAG_NO_RESPONSE,
                        targetApp = earbudResultCmd.targetApp,
                        opCode = RcspPacket.CMD_AUTH_SEND_CALC_RESULT,
                        opCodeSn = earbudResultCmd.opCodeSn,
                        status = 0,
                        payload = byteArrayOf(0x01) // versionResponse = 1
                    )
                    transport.send(phoneResp81)
                    println("EarbudsController: Sent Phone AuthSendCalcResultResponse with versionResponse=1!")
                }
            }

            println("EarbudsController: Authentication handshake finished! verified=$verified (AUTH_STAGE_OK)")
            return verified
        } finally {
            isAuthenticating = false
        }
    }

    private suspend fun onConnected() {
        // Step 0: Perform Xiaomi challenge-response authentication handshake (mutual two-stage).
        performAuthentication()
        if (!isConnected()) {
            println("EarbudsController: Disconnected during/after auth, aborting initial queries.")
            return
        }
        delay(150)

        // Helper local: envoie seulement si encore connecté, sinon aborte la séquence.
        suspend fun sendOrAbort(packet: RcspPacket): Boolean {
            if (!isConnected()) {
                println("EarbudsController: Disconnected, aborting initial queries before opCode=${packet.opCode}.")
                return false
            }
            transport.send(packet)
            return true
        }

        // 1. Query target info (name, version, battery, vid/pid) with standard 4-byte mask
        val targetInfoCmd = RcspPacket(
            type = RcspPacket.TYPE_COMMAND,
            hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
            targetApp = RcspPacket.TARGET_APP_EARPHONE,
            opCode = RcspPacket.CMD_GET_TARGET_INFO,
            payload = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
        )
        if (!sendOrAbort(targetInfoCmd)) return
        delay(150)

        // 2. Query device run info (dual-connection addresses, power mode)
        val runInfoCmd = RcspPacket(
            type = RcspPacket.TYPE_COMMAND,
            hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
            targetApp = RcspPacket.TARGET_APP_EARPHONE,
            opCode = RcspPacket.CMD_GET_DEVICE_RUN_INFO,
            payload = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
        )
        if (!sendOrAbort(runInfoCmd)) return
        delay(150)

        // 3. Batched GET_DEVICE_CONFIG according to Xiaomi Official Architecture (IDeviceSettingInternal)
        // Batch 1: Noise Cancellation configs
        val batch1 = OfficialPayloadCodecs.GetDeviceConfigCodec.encode(
            listOf(
                ConfigId.NOISE_LEVEL_CHOOSE,
                ConfigId.NOISE_MODE_CHOOSE,
                ConfigId.AUTO_NOISE,
                ConfigId.SMART_DENOISE_STATUS,
                ConfigId.PERSONALIZED_NOISE_REDUCTION,
                ConfigId.REMIND_LOST,
                ConfigId.COMMUTING_IMMERSE_STATUS
            )
        )
        if (!sendOrAbort(
            RcspPacket(
                type = RcspPacket.TYPE_COMMAND,
                hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
                targetApp = RcspPacket.TARGET_APP_EARPHONE,
                opCode = RcspPacket.CMD_GET_DEVICE_CONFIG,
                payload = batch1
            )
        )) return
        delay(150)

        // Batch 2: Gestures & Clicks
        val batch2 = OfficialPayloadCodecs.GetDeviceConfigCodec.encode(
            listOf(ConfigId.CONFIG_CUSTOM_CLICK)
        )
        if (!sendOrAbort(
            RcspPacket(
                type = RcspPacket.TYPE_COMMAND,
                hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
                targetApp = RcspPacket.TARGET_APP_EARPHONE,
                opCode = RcspPacket.CMD_GET_DEVICE_CONFIG,
                payload = batch2
            )
        )) return
        delay(150)

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
        if (!sendOrAbort(
            RcspPacket(
                type = RcspPacket.TYPE_COMMAND,
                hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
                targetApp = RcspPacket.TARGET_APP_EARPHONE,
                opCode = RcspPacket.CMD_GET_DEVICE_CONFIG,
                payload = batch3
            )
        )) return
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
        if (!sendOrAbort(
            RcspPacket(
                type = RcspPacket.TYPE_COMMAND,
                hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
                targetApp = RcspPacket.TARGET_APP_EARPHONE,
                opCode = RcspPacket.CMD_GET_DEVICE_CONFIG,
                payload = batch4
            )
        )) return
        // 4. Start periodic sync polling every 8 seconds (Target info + Live ANC & Battery)
        // Strictly conforms to official keepalive: OpCode 2 (battery, firmware) & OpCode 9 (MACs, power, ANC).
        // Does NOT flood the device with unnecessary SET_DEVICE_CONFIG commands.
        periodicPollJob?.cancel()
        periodicPollJob = scope.launch {
            while (isActive && _connectionState.value == ConnectionState.CONNECTED) {
                delay(8000)
                // 1. Poll Target Info (Battery & Firmware)
                transport.send(
                    RcspPacket(
                        type = RcspPacket.TYPE_COMMAND,
                        hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
                        targetApp = RcspPacket.TARGET_APP_EARPHONE,
                        opCode = RcspPacket.CMD_GET_TARGET_INFO,
                        payload = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
                    )
                )
                // 2. Poll Run Info (Live ANC mode & run status)
                transport.send(
                    RcspPacket(
                        type = RcspPacket.TYPE_COMMAND,
                        hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
                        targetApp = RcspPacket.TARGET_APP_EARPHONE,
                        opCode = RcspPacket.CMD_GET_DEVICE_RUN_INFO,
                        payload = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
                    )
                )
            }
        }
    }

    private fun onDisconnected() {
        periodicPollJob?.cancel()
        if (_fitDetection.value.isRunning) {
            fitDetectionTimeoutJob?.cancel()
            fitDetectionTimeoutJob = null
            FitDetectionAudioPlayer.stop()
            _fitDetection.value = _fitDetection.value.copy(
                status = FitDetectionState.STATUS_NOT_START,
                errorCode = FitDetectionState.CODE_DISCONNECT
            )
        }
    }

    private fun handleIncomingPacket(packet: RcspPacket) {
        // 1. Specially handle earbud-initiated auth challenge before generic ACK
        if (packet.opCode == RcspPacket.CMD_AUTH_CHECK && packet.type == RcspPacket.TYPE_COMMAND) {
            if (!isAuthenticating && packet.payload.size >= 17) {
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

        // 2. Specially handle earbud-initiated auth result command
        if (packet.opCode == RcspPacket.CMD_AUTH_SEND_CALC_RESULT && packet.type == RcspPacket.TYPE_COMMAND) {
            if (!isAuthenticating) {
                val phoneResp81 = RcspPacket(
                    type = RcspPacket.TYPE_RESPONSE,
                    hasResponse = RcspPacket.FLAG_NO_RESPONSE,
                    targetApp = packet.targetApp,
                    opCode = RcspPacket.CMD_AUTH_SEND_CALC_RESULT,
                    opCodeSn = packet.opCodeSn,
                    status = 0,
                    payload = byteArrayOf(0x01) // versionResponse = 1
                )
                scope.launch { transport.send(phoneResp81) }
            }
            return
        }

        // 3. Specially handle ReportEdrStatusCmd (OpCode 7): mirror the status payload
        if (packet.opCode == 7 && packet.type == RcspPacket.TYPE_COMMAND) {
            if (packet.hasResponse == RcspPacket.FLAG_HAVE_RESPONSE) {
                val ack = RcspPacket.createAckResponse(packet, status = 0, payload = packet.payload)
                scope.launch { transport.send(ack) }
            }
            return
        }

        // 4. Automatically ACK other commands that expect a response (FLAG_HAVE_RESPONSE)
        if (packet.type == RcspPacket.TYPE_COMMAND && packet.hasResponse == RcspPacket.FLAG_HAVE_RESPONSE) {
            val ack = RcspPacket.createAckResponse(packet, status = 0)
            scope.launch { transport.send(ack) }
        }

        when (packet.opCode) {
            RcspPacket.CMD_SET_DEVICE_CONFIG -> {
                // Réponse à nos SET (ex: level ANC config 11). Log pour debug, pas de parsing.
                if (packet.type == RcspPacket.TYPE_RESPONSE) {
                    val hex = packet.payload.joinToString(" ") { "%02X".format(it) }
                    println("EarbudsController: SET_DEVICE_CONFIG response status=${packet.status} payload=[$hex]")
                }
            }
            RcspPacket.CMD_SET_TARGET_INFO -> {
                // Réponse à nos SET mode ANC (OpCode 8 VendorData type 4).
                if (packet.type == RcspPacket.TYPE_RESPONSE) {
                    println("EarbudsController: SET_TARGET_INFO (ANC mode) response status=${packet.status}")
                }
            }
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
                        try {
                        when (cfg.type) {
                            // NOTE: CONFIG_AUDIO_MODE (1) = Xiaomi/Dolby audio mode, PAS du ANC.
                            // Le ANC passe par OpCode 8 (mode) + Config 11 (level) uniquement.
                            ConfigId.NOISE_LEVEL_CHOOSE, ConfigId.NOISE_MODE_CHOOSE,
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
                            ConfigId.CONFIG_COMPACTNESS_LISTENER, ConfigId.EAR_CANAL_EAR_CANAL_FIT, ConfigId.EAR_CANAL_DETECTION -> {
                                if (cfg.value.isNotEmpty()) {
                                    val left = cfg.value[0].toInt() and 0xFF
                                    val right = if (cfg.value.size > 1) cfg.value[1].toInt() and 0xFF else left

                                    when {
                                        left == FitDetectionState.CODE_RESPONSE_START || right == FitDetectionState.CODE_RESPONSE_START -> {
                                            if (!FitDetectionAudioPlayer.isAudioPlaying()) {
                                                FitDetectionAudioPlayer.play()
                                            }
                                            _fitDetection.value = _fitDetection.value.copy(
                                                status = FitDetectionState.STATUS_DETECTING,
                                                errorCode = FitDetectionState.CODE_NONE
                                            )
                                        }
                                        left == FitDetectionState.CODE_EAR_OUT || right == FitDetectionState.CODE_EAR_OUT -> {
                                            fitDetectionTimeoutJob?.cancel()
                                            FitDetectionAudioPlayer.stop()
                                            _fitDetection.value = _fitDetection.value.copy(
                                                status = FitDetectionState.STATUS_NOT_START,
                                                errorCode = FitDetectionState.CODE_EAR_OUT
                                            )
                                        }
                                        left == FitDetectionState.CODE_CALLING || right == FitDetectionState.CODE_CALLING -> {
                                            fitDetectionTimeoutJob?.cancel()
                                            FitDetectionAudioPlayer.stop()
                                            _fitDetection.value = _fitDetection.value.copy(
                                                status = FitDetectionState.STATUS_NOT_START,
                                                errorCode = FitDetectionState.CODE_CALLING
                                            )
                                        }
                                        (left in 1..2 || left == FitDetectionState.FIT_ADJUST_POSITION) &&
                                        (right in 1..2 || right == FitDetectionState.FIT_ADJUST_POSITION) -> {
                                            fitDetectionTimeoutJob?.cancel()
                                            FitDetectionAudioPlayer.stop()
                                            _fitDetection.value = FitDetectionState(
                                                status = FitDetectionState.STATUS_FINISH,
                                                leftResult = left,
                                                rightResult = right,
                                                errorCode = FitDetectionState.CODE_NONE
                                            )
                                        }
                                        else -> {
                                            val fit = FitDetectionState.parseFromPayload(cfg.value)
                                            if (fit.isFinished) {
                                                fitDetectionTimeoutJob?.cancel()
                                                FitDetectionAudioPlayer.stop()
                                            }
                                            _fitDetection.value = fit
                                        }
                                    }
                                }
                            }
                            ConfigId.FIND_DEVICE -> {
                                if (cfg.value.size >= 2) {
                                    val state = cfg.value[0].toInt() and 0xFF
                                    val devType = cfg.value[1].toInt() and 0xFF
                                    val ringing = state == 1
                                    when (devType) {
                                        1 -> _findDevice.value = _findDevice.value.copy(isRingingLeft = ringing)
                                        2 -> _findDevice.value = _findDevice.value.copy(isRingingRight = ringing)
                                        3 -> _findDevice.value = FindDeviceState(isRingingLeft = ringing, isRingingRight = ringing)
                                    }
                                }
                            }
                            ConfigId.EARBOX_SOUND_CONFIG -> {
                                if (cfg.value.isNotEmpty()) {
                                    _earboxSound.value = EarboxSoundState.parseFromPayload(cfg.value)
                                }
                            }
                            ConfigId.DEVICE_CALL_LISTENER -> {
                                if (cfg.value.size >= 2) {
                                    val isOpen = (cfg.value[0].toInt() and 0xFF) == 1
                                    val secs = cfg.value[1].toInt() and 0xFF
                                    _callListenerSeconds.value = if (isOpen) secs else 0
                                }
                            }
                            ConfigId.DUAL_CONNECTION_SYNC -> {
                                if (cfg.value.size >= 2) {
                                    val subCmd = cfg.value[0].toInt() and 0xFF
                                    val subVal = cfg.value[1].toInt() and 0xFF
                                    if (subCmd == 0) {
                                        _quickSettings.value = _quickSettings.value.copy(inEarDetection = subVal == 1)
                                    } else if (subCmd == 1) {
                                        _voiceHotword.value = subVal == 1
                                    }
                                }
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
                                // 1:1 DeviceConfigNotificationVolume : paramsToValue = [current] (1 byte),
                                // valueToParams : len==4 -> current/recommended/max/min, sinon current = values[0].
                                // Jamais 2 bytes [enabled, volume] : l'ancien format crashait (Index 1 oob)
                                // dès qu'un firmware n'envoie qu'1 byte.
                                if (cfg.value.isNotEmpty()) {
                                    _equalizer.value = _equalizer.value.copy(
                                        notificationVolume = cfg.value[0].toInt() and 0xFF
                                    )
                                }
                            }
                            ConfigId.COMMUTING_IMMERSE_STATUS -> {
                                if (cfg.value.isNotEmpty()) {
                                    _commutingImmerseMode.value = (cfg.value[0].toInt() and 0xFF).coerceIn(0, 3)
                                }
                            }

                        }
                        } catch (e: Exception) {
                            // Prod : un payload inattendu (firmware plus récent/ancien) ne doit
                            // jamais tuer le collector. On log et on passe à la config suivante.
                            println("EarbudsController: Skipping malformed config type=${cfg.type} len=${cfg.value.size} ($e)")
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

        // Officiel (NoiseReductionVM.setNoiseType -> FunctionConfigImpl.updateNoise):
        // le changement de MODE (OFF/ANC/Transparency) passe par OpCode 8 (SET_TARGET_INFO)
        // avec VendorData [len=2, type=4, ancType]. JAMAIS par Config 11 ni Config 1.
        // Config 11 (OpCode 242) ne sert qu'au choix du LEVEL (profondeur).
        sendNoiseMode(mode.id)
    }

    fun setAncLevel(level: AncLevel) {
        val updated = _noiseControl.value.copy(mode = NoiseMode.ANC, ancLevel = level, ancLevelIndex = level.id)
        _noiseControl.value = updated
        sendNoiseLevel(NoiseMode.ANC.id, level.id)
    }

    fun setTransparencyLevel(level: TransparencyLevel) {
        val updated = _noiseControl.value.copy(mode = NoiseMode.TRANSPARENCY, transparencyLevel = level, transparencyLevelIndex = level.id)
        _noiseControl.value = updated
        sendNoiseLevel(NoiseMode.TRANSPARENCY.id, level.id)
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
        sendNoiseLevel(NoiseMode.ANC.id, rawLevel)
    }

    fun setTransparencyLevelByIndex(index: Int, rawLevel: Int) {
        val updated = _noiseControl.value.copy(
            mode = NoiseMode.TRANSPARENCY,
            transparencyLevelIndex = index,
            transparencyLevel = TransparencyLevel.fromId(rawLevel)
        )
        _noiseControl.value = updated
        sendNoiseLevel(NoiseMode.TRANSPARENCY.id, rawLevel)
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

    fun ringEarbud(target: RingTarget, start: Boolean) {
        val cur = _findDevice.value
        val updated = when (target) {
            RingTarget.LEFT -> cur.copy(isRingingLeft = start)
            RingTarget.RIGHT -> cur.copy(isRingingRight = start)
            RingTarget.BOTH -> FindDeviceState(isRingingLeft = start, isRingingRight = start)
        }
        _findDevice.value = updated
        sendConfig(FindDeviceState.createCommand(target, start))
    }

    fun ringEarbuds(target: RingTarget) {
        val updated = when (target) {
            RingTarget.LEFT -> FindDeviceState(isRingingLeft = true, isRingingRight = false)
            RingTarget.RIGHT -> FindDeviceState(isRingingLeft = false, isRingingRight = true)
            RingTarget.BOTH -> FindDeviceState(isRingingLeft = true, isRingingRight = true)
        }
        _findDevice.value = updated
        sendConfig(FindDeviceState.createCommand(target, true))
    }

    fun stopRinging() {
        _findDevice.value = FindDeviceState(isRingingLeft = false, isRingingRight = false)
        sendConfig(FindDeviceState.createCommand(RingTarget.BOTH, false))
    }

    fun startFitDetection() {
        fitDetectionTimeoutJob?.cancel()
        _fitDetection.value = FitDetectionState(
            status = FitDetectionState.STATUS_DETECTING,
            leftResult = FitDetectionState.FIT_UNKNOWN,
            rightResult = FitDetectionState.FIT_UNKNOWN,
            errorCode = FitDetectionState.CODE_NONE
        )
        FitDetectionAudioPlayer.play()
        for (cmd in FitDetectionState.createCommands(start = true)) {
            sendConfig(cmd)
        }

        // 10-second timeout matching official FitDetectionViewModel.java:360
        fitDetectionTimeoutJob = scope.launch {
            delay(10000L)
            if (_fitDetection.value.isRunning) {
                FitDetectionAudioPlayer.stop()
                _fitDetection.value = _fitDetection.value.copy(
                    status = FitDetectionState.STATUS_NOT_START,
                    errorCode = FitDetectionState.CODE_TIMEOUT
                )
            }
        }
    }

    fun stopFitDetection() {
        fitDetectionTimeoutJob?.cancel()
        fitDetectionTimeoutJob = null
        FitDetectionAudioPlayer.stop()
        _fitDetection.value = _fitDetection.value.copy(
            status = FitDetectionState.STATUS_NOT_START,
            errorCode = FitDetectionState.CODE_NONE
        )
        for (cmd in FitDetectionState.createCommands(start = false)) {
            sendConfig(cmd)
        }
    }

    fun setFitDetectionStatus(status: Int) {
        _fitDetection.value = _fitDetection.value.copy(status = status)
    }

    fun dismissFitDetectionError() {
        _fitDetection.value = _fitDetection.value.copy(errorCode = FitDetectionState.CODE_NONE)
    }

    fun setEarboxVolume(soundType: Int, volume: Int) {
        val cur = _earboxSound.value
        val updated = when (soundType) {
            EarboxSoundState.SOUND_TYPE_OPEN -> cur.copy(openSound = cur.openSound.copy(volume = volume))
            EarboxSoundState.SOUND_TYPE_CLOSE -> cur.copy(closeSound = cur.closeSound.copy(volume = volume))
            EarboxSoundState.SOUND_TYPE_CHARGE -> cur.copy(chargeSound = cur.chargeSound.copy(volume = volume))
            else -> cur
        }
        _earboxSound.value = updated
        sendConfig(EarboxSoundState.createSetCommand(EarboxSoundState.SETTING_TYPE_VOLUME, soundType, volume))
    }

    fun setEarboxSoundId(soundType: Int, soundId: Int) {
        val cur = _earboxSound.value
        val updated = when (soundType) {
            EarboxSoundState.SOUND_TYPE_OPEN -> cur.copy(openSound = cur.openSound.copy(soundId = soundId))
            EarboxSoundState.SOUND_TYPE_CLOSE -> cur.copy(closeSound = cur.closeSound.copy(soundId = soundId))
            EarboxSoundState.SOUND_TYPE_CHARGE -> cur.copy(chargeSound = cur.chargeSound.copy(soundId = soundId))
            else -> cur
        }
        _earboxSound.value = updated
        sendConfig(EarboxSoundState.createSetCommand(EarboxSoundState.SETTING_TYPE_SOUND, soundType, soundId))
    }

    fun setCallListener(seconds: Int) {
        _callListenerSeconds.value = seconds
        val isOpen = seconds > 0
        sendConfig(CommonConfig(ConfigId.DEVICE_CALL_LISTENER, byteArrayOf(if (isOpen) 1 else 0, seconds.toByte())))
    }

    fun setVoiceHotword(enabled: Boolean) {
        _voiceHotword.value = enabled
        val payload = byteArrayOf(0x02, 0x02, if (enabled) 1 else 0)
        scope.launch {
            val packet = RcspPacket(
                type = RcspPacket.TYPE_COMMAND,
                hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
                targetApp = RcspPacket.TARGET_APP_EARPHONE,
                opCode = RcspPacket.CMD_SET_TARGET_INFO,
                status = 0,
                payload = payload
            )
            transport.send(packet)
        }
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
        // Pas d'équivalent officiel : DeviceConfigNotificationVolume ne porte que [current].
        // On mémorise localement sans envoyer de paquet inventé.
        val updated = _equalizer.value.copy(notificationVolumeEnabled = enabled)
        _equalizer.value = updated
    }

    fun setNotificationVolume(volume: Int) {
        // 1:1 SoundEffectVM.setNotificationVolume : DeviceConfigNotificationVolume.setCurrent
        // -> CommonConfig(161, [current]) 1 byte unique.
        val updated = _equalizer.value.copy(notificationVolume = volume.coerceIn(0, 100))
        _equalizer.value = updated
        sendConfig(CommonConfig(ConfigId.NOTIFICATION_VOLUME, byteArrayOf(updated.notificationVolume.toByte())))
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

    fun setCommutingImmerse(mode: Int) {
        _commutingImmerseMode.value = mode.coerceIn(0, 3)
        sendConfig(CommonConfig(ConfigId.COMMUTING_IMMERSE_STATUS, byteArrayOf(mode.toByte())))
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
            // Officiel: SetDeviceConfigHelper.sendDeviceConfigCmd -> SetDeviceConfigCmd
            // (CommandWithParamAndResponse, type=2) => HAS_RESPONSE=1.
            // Sans réponse uniquement pour BigData (config 52), jamais pour les réglages normaux.
            val packet = RcspPacket(
                type = RcspPacket.TYPE_COMMAND,
                hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
                targetApp = RcspPacket.TARGET_APP_EARPHONE,
                opCode = RcspPacket.CMD_SET_DEVICE_CONFIG,
                payload = config.toByteArray()
            )
            transport.send(packet)
        }
    }

    /**
     * Changement de MODE ANC (OFF=0, ANC=1, Transparency=2).
     * 1:1 officiel: FunctionConfigImpl.updateNoise -> sendSetRunInfo ->
     * SetTargetInfoCmd(OpCode 8) avec VendorData [len=2, type=4, ancType], HAVE_RESPONSE.
     * Voir com.mi.earphone.settings.ui.noise.NoiseReductionVM.setNoiseType.
     */
    private fun sendNoiseMode(modeId: Int) {
        scope.launch {
            val payload = OfficialPayloadCodecs.VendorDataCodec.encodeNoiseMode(modeId.toByte())
            val packet = RcspPacket(
                type = RcspPacket.TYPE_COMMAND,
                hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
                targetApp = RcspPacket.TARGET_APP_EARPHONE,
                opCode = RcspPacket.CMD_SET_TARGET_INFO,
                payload = payload
            )
            transport.send(packet)
        }
    }

    /**
     * Changement de LEVEL (profondeur ANC / variante transparency).
     * 1:1 officiel: FunctionConfigImpl.noiseLevel -> DeviceConfigNoiseLevel(config 11)
     * [ancState, ancLevel] unique, OpCode 242 (SET_DEVICE_CONFIG), HAVE_RESPONSE.
     * Voir com.mi.earphone.bluetoothsdk.setting.function.DeviceConfigNoiseLevel(super(11)).
     * Ne JAMAIS envoyer Config 1 (CONFIG_AUDIO_MODE = Xiaomi/Dolby, pas ANC !)
     * ni concaténer 2 TLV dans le même paquet.
     */
    private fun sendNoiseLevel(modeId: Int, level: Int) {
        scope.launch {
            val cfg = CommonConfig(ConfigId.NOISE_LEVEL_CHOOSE, byteArrayOf(modeId.toByte(), level.toByte()))
            val packet = RcspPacket(
                type = RcspPacket.TYPE_COMMAND,
                hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
                targetApp = RcspPacket.TARGET_APP_EARPHONE,
                opCode = RcspPacket.CMD_SET_DEVICE_CONFIG,
                payload = cfg.toByteArray()
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

