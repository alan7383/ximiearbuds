package com.alan.ximiearbuds.core.bluetooth

import com.alan.ximiearbuds.core.protocol.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Interactive virtual earbud simulator for instant testing on any platform.
 * Responds to official RCSP packets with accurate status and payloads.
 */
class SimulatedTransport(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : BluetoothTransport {

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _incomingPackets = MutableSharedFlow<RcspPacket>(extraBufferCapacity = 64)
    override val incomingPackets: SharedFlow<RcspPacket> = _incomingPackets

    private val _connectedDevice = MutableStateFlow<DiscoveredDevice?>(null)
    override val connectedDevice: StateFlow<DiscoveredDevice?> = _connectedDevice

    // Virtual device state
    var simulatedDeviceInfo = TargetDeviceInfo(
        name = "Redmi Buds 5 Pro",
        versionName = "1.0.4.8",
        versionCode = 0x0104,
        vendorId = 10007,
        productId = 20588,
        leftBattery = BatteryInfo(percentage = 85, isCharging = false, isConnected = true),
        rightBattery = BatteryInfo(percentage = 90, isCharging = true, isConnected = true),
        caseBattery = BatteryInfo(percentage = 100, isCharging = false, isConnected = true)
    )

    var simulatedNoise = NoiseControlState(
        mode = NoiseMode.ANC,
        ancLevel = AncLevel.DEEP
    )

    var simulatedEq = EqualizerState(
        preset = EqPreset.STANDARD,
        bands = EqualizerState.DEFAULT_10_BANDS
    )

    var simulatedGestures = GestureSettings()

    var simulatedQuick = QuickSettings(
        lowLatency = false,
        multipoint = true,
        inEarDetection = true,
        adaptiveVolume = false
    )

    private val demoDevices = listOf(
        DiscoveredDevice(name = "Redmi Buds 5 Pro (Simulé)", address = "SIM:RD:B5:PR:01:02", isConnected = false, isXiaomiEarbuds = true),
        DiscoveredDevice(name = "Xiaomi Buds 4 Pro (Simulé)", address = "SIM:XM:B4:PR:03:04", isConnected = false, isXiaomiEarbuds = true),
        DiscoveredDevice(name = "Redmi Buds 6 Play (Simulé)", address = "SIM:RD:B6:PL:05:06", isConnected = false, isXiaomiEarbuds = true)
    )

    override fun getPairedDevices(): List<DiscoveredDevice> {
        return demoDevices
    }

    override suspend fun connect(address: String): Boolean {
        _connectionState.value = ConnectionState.CONNECTING
        delay(300) // Realistic connection delay
        val dev = demoDevices.find { it.address == address } ?: DiscoveredDevice(name = "Xiaomi Earbuds (Simulé)", address = address, isConnected = true)
        _connectedDevice.value = dev
        _connectionState.value = ConnectionState.CONNECTED

        // Auto-send target info on connect
        scope.launch {
            delay(100)
            sendTargetInfoResponse(1)
        }

        return true
    }

    override suspend fun disconnect() {
        _connectionState.value = ConnectionState.DISCONNECTED
        _connectedDevice.value = null
    }

    override suspend fun send(packet: RcspPacket): Boolean {
        if (_connectionState.value != ConnectionState.CONNECTED) return false

        scope.launch {
            delay(30) // Simulate processing time
            when (packet.opCode) {
                RcspPacket.CMD_AUTH_CHECK -> {
                    val randFactor = if (packet.payload.size >= 17) packet.payload.copyOfRange(1, 17) else ByteArray(16)
                    val encrypted = com.alan.ximiearbuds.core.crypto.BluetoothAuthEngine.encrypt(randFactor)
                    _incomingPackets.emit(
                        RcspPacket(
                            type = RcspPacket.TYPE_RESPONSE,
                            hasResponse = RcspPacket.FLAG_NO_RESPONSE,
                            targetApp = packet.targetApp,
                            opCode = RcspPacket.CMD_AUTH_CHECK,
                            opCodeSn = packet.opCodeSn,
                            status = 0,
                            payload = byteArrayOf(0x01) + encrypted
                        )
                    )
                }
                RcspPacket.CMD_AUTH_SEND_CALC_RESULT -> {
                    _incomingPackets.emit(
                        RcspPacket(
                            type = RcspPacket.TYPE_RESPONSE,
                            hasResponse = RcspPacket.FLAG_NO_RESPONSE,
                            targetApp = packet.targetApp,
                            opCode = RcspPacket.CMD_AUTH_SEND_CALC_RESULT,
                            opCodeSn = packet.opCodeSn,
                            status = 0,
                            payload = byteArrayOf(0x01)
                        )
                    )
                }
                RcspPacket.CMD_GET_TARGET_INFO -> {
                    sendTargetInfoResponse(packet.opCodeSn)
                }
                RcspPacket.CMD_GET_DEVICE_CONFIG -> {
                    sendAllConfigsResponse(packet.opCodeSn)
                }
                RcspPacket.CMD_SET_DEVICE_CONFIG -> {
                    val configs = CommonConfig.parseList(packet.payload)
                    for (cfg in configs) {
                        when (cfg.type) {
                            ConfigId.NOISE_LEVEL_CHOOSE -> {
                                NoiseControlState.fromCommonConfig(cfg)?.let { simulatedNoise = it }
                            }
                            ConfigId.NOISE_MODE_CHOOSE -> {
                                NoiseControlState.fromCommonConfig(cfg)?.let { simulatedNoise = it }
                            }
                            ConfigId.CONFIG_EQ_MODEL -> {
                                if (cfg.value.isNotEmpty()) {
                                    simulatedEq = simulatedEq.copy(preset = EqPreset.fromId(cfg.value[0].toInt()))
                                }
                            }
                            ConfigId.CUSTOM_EQ -> {
                                EqualizerState.parseCustomEq(cfg.value)?.let {
                                    simulatedEq = simulatedEq.copy(preset = EqPreset.CUSTOM, bands = it)
                                }
                            }
                            ConfigId.CONFIG_CUSTOM_CLICK -> {
                                simulatedGestures = GestureSettings.parseFromConfigs(listOf(cfg))
                            }
                            ConfigId.LOW_LATENCY -> {
                                simulatedQuick = simulatedQuick.copy(lowLatency = cfg.value.getOrNull(0)?.toInt() == 1)
                            }
                            ConfigId.CONFIG_MULTIPOINT_CONNECTION -> {
                                simulatedQuick = simulatedQuick.copy(multipoint = cfg.value.getOrNull(0)?.toInt() == 1)
                            }
                            ConfigId.EAR_CANAL_DETECTION -> {
                                val isStart = cfg.value.getOrNull(0)?.toInt() == 1
                                if (isStart) {
                                    scope.launch {
                                        delay(1500)
                                        // Emit fit detection result: Left Well (1), Right Well (1)
                                        val fitResp = CommonConfig(ConfigId.EAR_CANAL_DETECTION, byteArrayOf(1, 1))
                                        val packetResp = RcspPacket(
                                            type = RcspPacket.TYPE_COMMAND,
                                            hasResponse = RcspPacket.FLAG_NO_RESPONSE,
                                            targetApp = RcspPacket.TARGET_APP_EARPHONE,
                                            opCode = RcspPacket.CMD_NOTIFY_DEVICE_CONFIG,
                                            opCodeSn = 0,
                                            status = 0,
                                            payload = CommonConfig.toByteArray(listOf(fitResp))
                                        )
                                        _incomingPackets.emit(packetResp)
                                    }
                                }
                            }
                            ConfigId.FIND_DEVICE -> {
                                if (cfg.value.size >= 2) {
                                    val notifyCfg = CommonConfig(ConfigId.FIND_DEVICE, cfg.value)
                                    val packetResp = RcspPacket(
                                        type = RcspPacket.TYPE_COMMAND,
                                        hasResponse = RcspPacket.FLAG_NO_RESPONSE,
                                        targetApp = RcspPacket.TARGET_APP_EARPHONE,
                                        opCode = RcspPacket.CMD_NOTIFY_DEVICE_CONFIG,
                                        opCodeSn = 0,
                                        status = 0,
                                        payload = CommonConfig.toByteArray(listOf(notifyCfg))
                                    )
                                    scope.launch { _incomingPackets.emit(packetResp) }
                                }
                            }
                            ConfigId.EARBOX_SOUND_SET -> {
                                if (cfg.value.size >= 3) {
                                    val setType = cfg.value[0].toInt() and 0xFF
                                    val soundType = cfg.value[1].toInt() and 0xFF
                                    val value = cfg.value[2].toInt() and 0xFF
                                    // Generate earbox sound config notification
                                    val open = byteArrayOf(0, 1, 75, 100)
                                    val close = byteArrayOf(1, 1, 75, 100)
                                    val charge = byteArrayOf(2, 1, 75, 100)
                                    val notifyCfg = CommonConfig(ConfigId.EARBOX_SOUND_CONFIG, open + close + charge)
                                    val packetResp = RcspPacket(
                                        type = RcspPacket.TYPE_COMMAND,
                                        hasResponse = RcspPacket.FLAG_NO_RESPONSE,
                                        targetApp = RcspPacket.TARGET_APP_EARPHONE,
                                        opCode = RcspPacket.CMD_NOTIFY_DEVICE_CONFIG,
                                        opCodeSn = 0,
                                        status = 0,
                                        payload = CommonConfig.toByteArray(listOf(notifyCfg))
                                    )
                                    scope.launch { _incomingPackets.emit(packetResp) }
                                }
                            }
                            ConfigId.ADAPTIVE_VOLUME -> {
                                simulatedQuick = simulatedQuick.copy(adaptiveVolume = cfg.value.getOrNull(0)?.toInt() == 1)
                            }

                        }
                    }

                    // Send success response
                    val resp = RcspPacket(
                        type = RcspPacket.TYPE_RESPONSE,
                        hasResponse = RcspPacket.FLAG_NO_RESPONSE,
                        targetApp = RcspPacket.TARGET_APP_EARPHONE,
                        opCode = RcspPacket.CMD_SET_DEVICE_CONFIG,
                        opCodeSn = packet.opCodeSn,
                        status = 0,
                        payload = ByteArray(0)
                    )
                    _incomingPackets.emit(resp)
                }
                RcspPacket.CMD_SET_TARGET_INFO -> {
                    val vendorMap = OfficialPayloadCodecs.VendorDataCodec.parse(packet.payload)
                    vendorMap[OfficialPayloadCodecs.VendorDataCodec.TYPE_NOISE]?.let { modeBytes ->
                        if (modeBytes.isNotEmpty()) {
                            simulatedNoise = simulatedNoise.copy(mode = NoiseMode.fromId(modeBytes[0].toInt() and 0xFF))
                        }
                    }
                    val resp = RcspPacket(
                        type = RcspPacket.TYPE_RESPONSE,
                        hasResponse = RcspPacket.FLAG_NO_RESPONSE,
                        targetApp = RcspPacket.TARGET_APP_EARPHONE,
                        opCode = RcspPacket.CMD_SET_TARGET_INFO,
                        opCodeSn = packet.opCodeSn,
                        status = 0,
                        payload = ByteArray(0)
                    )
                    _incomingPackets.emit(resp)
                }
                RcspPacket.CMD_GET_DEVICE_RUN_INFO -> {
                    val resp = RcspPacket(
                        type = RcspPacket.TYPE_RESPONSE,
                        hasResponse = RcspPacket.FLAG_NO_RESPONSE,
                        targetApp = RcspPacket.TARGET_APP_EARPHONE,
                        opCode = RcspPacket.CMD_GET_DEVICE_RUN_INFO,
                        opCodeSn = packet.opCodeSn,
                        status = 0,
                        payload = OfficialPayloadCodecs.DeviceRunInfoCodec.encodeAncStatus(simulatedNoise.mode.id)
                    )
                    _incomingPackets.emit(resp)
                }
                RcspPacket.CMD_FIND_DEVICE -> {
                    // Send success response
                    val resp = RcspPacket(
                        type = RcspPacket.TYPE_RESPONSE,
                        hasResponse = RcspPacket.FLAG_NO_RESPONSE,
                        targetApp = RcspPacket.TARGET_APP_EARPHONE,
                        opCode = RcspPacket.CMD_FIND_DEVICE,
                        opCodeSn = packet.opCodeSn,
                        status = 0,
                        payload = ByteArray(0)
                    )
                    _incomingPackets.emit(resp)
                }
            }
        }
        return true
    }

    private suspend fun sendTargetInfoResponse(sn: Int) {
        val payload = TargetDeviceInfo.encodeTargetInfoResponse(simulatedDeviceInfo)
        val resp = RcspPacket(
            type = RcspPacket.TYPE_RESPONSE,
            hasResponse = RcspPacket.FLAG_NO_RESPONSE,
            targetApp = RcspPacket.TARGET_APP_EARPHONE,
            opCode = RcspPacket.CMD_GET_TARGET_INFO,
            opCodeSn = sn,
            status = 0,
            payload = payload
        )
        _incomingPackets.emit(resp)
    }

    private suspend fun sendAllConfigsResponse(sn: Int) {
        val configs = mutableListOf<CommonConfig>()
        configs.add(simulatedNoise.toCommonConfig())
        configs.add(EqualizerState.createPresetConfig(simulatedEq.preset))
        if (simulatedEq.preset == EqPreset.CUSTOM) {
            configs.add(EqualizerState.createCustomEqConfig(simulatedEq.bands))
        }
        configs.addAll(simulatedGestures.toCommonConfigs())
        configs.add(QuickSettings.createToggle(ConfigId.LOW_LATENCY, simulatedQuick.lowLatency))
        configs.add(QuickSettings.createToggle(ConfigId.CONFIG_MULTIPOINT_CONNECTION, simulatedQuick.multipoint))
        configs.add(QuickSettings.createToggle(ConfigId.EAR_CANAL_DETECTION, simulatedQuick.inEarDetection))
        configs.add(QuickSettings.createToggle(ConfigId.ADAPTIVE_VOLUME, simulatedQuick.adaptiveVolume))

        val resp = RcspPacket(
            type = RcspPacket.TYPE_RESPONSE,
            hasResponse = RcspPacket.FLAG_NO_RESPONSE,
            targetApp = RcspPacket.TARGET_APP_EARPHONE,
            opCode = RcspPacket.CMD_GET_DEVICE_CONFIG,
            opCodeSn = sn,
            status = 0,
            payload = CommonConfig.toByteArray(configs)
        )
        _incomingPackets.emit(resp)
    }
}
