package com.alan.ximiearbuds.core.device

import com.alan.ximiearbuds.core.bluetooth.*
import com.alan.ximiearbuds.core.protocol.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class EarbudsController(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
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
        refreshPairedDevices()
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

    private suspend fun onConnected() {
        // Query target info (name, version, battery, vid/pid)
        val targetInfoCmd = RcspPacket(
            type = RcspPacket.TYPE_COMMAND,
            hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
            targetApp = RcspPacket.TARGET_APP_EARPHONE,
            opCode = RcspPacket.CMD_GET_TARGET_INFO
        )
        transport.send(targetInfoCmd)

        // Query all device configs (ANC, EQ, gestures, smart settings)
        val getConfigCmd = RcspPacket(
            type = RcspPacket.TYPE_COMMAND,
            hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
            targetApp = RcspPacket.TARGET_APP_EARPHONE,
            opCode = RcspPacket.CMD_GET_DEVICE_CONFIG
        )
        transport.send(getConfigCmd)

        // Start periodic battery polling every 10 seconds
        periodicPollJob?.cancel()
        periodicPollJob = scope.launch {
            while (isActive && _connectionState.value == ConnectionState.CONNECTED) {
                delay(10000)
                transport.send(
                    RcspPacket(
                        type = RcspPacket.TYPE_COMMAND,
                        hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
                        targetApp = RcspPacket.TARGET_APP_EARPHONE,
                        opCode = RcspPacket.CMD_GET_TARGET_INFO
                    )
                )
            }
        }
    }

    private fun onDisconnected() {
        periodicPollJob?.cancel()
    }

    private fun handleIncomingPacket(packet: RcspPacket) {
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
            RcspPacket.CMD_GET_DEVICE_CONFIG, RcspPacket.CMD_NOTIFY_DEVICE_CONFIG -> {
                if (packet.payload.isNotEmpty()) {
                    val configs = CommonConfig.parseList(packet.payload)
                    for (cfg in configs) {
                        when (cfg.type) {
                            ConfigId.NOISE_LEVEL_CHOOSE, ConfigId.NOISE_MODE_CHOOSE, ConfigId.AUTO_NOISE, ConfigId.SMART_DENOISE_STATUS, ConfigId.PERSONALIZED_NOISE_REDUCTION -> {
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
        sendConfig(updated.toCommonConfig())
    }

    fun setAncLevel(level: AncLevel) {
        val updated = _noiseControl.value.copy(mode = NoiseMode.ANC, ancLevel = level)
        _noiseControl.value = updated
        sendConfig(updated.toCommonConfig())
    }

    fun setTransparencyLevel(level: TransparencyLevel) {
        val updated = _noiseControl.value.copy(mode = NoiseMode.TRANSPARENCY, transparencyLevel = level)
        _noiseControl.value = updated
        sendConfig(updated.toCommonConfig())
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
        sendConfig(NoiseControlState.createNoiseLevelConfig(NoiseMode.ANC, rawLevel))
    }

    fun setTransparencyLevelByIndex(index: Int, rawLevel: Int) {
        val updated = _noiseControl.value.copy(
            mode = NoiseMode.TRANSPARENCY,
            transparencyLevelIndex = index,
            transparencyLevel = TransparencyLevel.fromId(rawLevel)
        )
        _noiseControl.value = updated
        sendConfig(NoiseControlState.createNoiseLevelConfig(NoiseMode.TRANSPARENCY, rawLevel))
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
                hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
                targetApp = RcspPacket.TARGET_APP_EARPHONE,
                opCode = RcspPacket.CMD_SET_DEVICE_CONFIG,
                payload = config.toByteArray()
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

