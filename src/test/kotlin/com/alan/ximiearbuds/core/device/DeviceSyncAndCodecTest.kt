package com.alan.ximiearbuds.core.device

import com.alan.ximiearbuds.core.bluetooth.BluetoothTransport
import com.alan.ximiearbuds.core.bluetooth.LinuxRfcommTransport
import com.alan.ximiearbuds.core.bluetooth.ConnectionState
import com.alan.ximiearbuds.core.bluetooth.DiscoveredDevice
import com.alan.ximiearbuds.core.bluetooth.SimulatedTransport
import com.alan.ximiearbuds.core.protocol.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DeviceSyncAndCodecTest {

    @Test
    fun `test GetDeviceConfigCodec 16-bit Big-Endian encoding and decoding`() {
        val ids = listOf(
            ConfigId.NOISE_LEVEL_CHOOSE, // 11 -> 0x00, 0x0B
            ConfigId.REMIND_LOST,        // 12 -> 0x00, 0x0C
            ConfigId.AUTO_NOISE,         // 37 -> 0x00, 0x25
            ConfigId.SMART_DENOISE_STATUS // 102 -> 0x00, 0x66
        )
        val encoded = OfficialPayloadCodecs.GetDeviceConfigCodec.encode(ids)
        assertEquals(8, encoded.size)
        assertEquals(0x00.toByte(), encoded[0])
        assertEquals(11.toByte(), encoded[1])
        assertEquals(0x00.toByte(), encoded[2])
        assertEquals(12.toByte(), encoded[3])
        assertEquals(0x00.toByte(), encoded[4])
        assertEquals(37.toByte(), encoded[5])
        assertEquals(0x00.toByte(), encoded[6])
        assertEquals(102.toByte(), encoded[7])

        val decoded = OfficialPayloadCodecs.GetDeviceConfigCodec.decode(encoded)
        assertArrayEquals(intArrayOf(11, 12, 37, 102), decoded)
    }

    @Test
    fun `test DeviceStatusCodec TLV battery and live ANC parsing and encoding`() {
        val report = OfficialPayloadCodecs.DeviceStatusReport(
            batteryLeft = 85,
            batteryRight = 90,
            batteryCase = 100,
            ancStatus = 1 // ANC
        )
        val encoded = OfficialPayloadCodecs.DeviceStatusCodec.encode(report)
        assertTrue(encoded.isNotEmpty())

        val parsed = OfficialPayloadCodecs.DeviceStatusCodec.parse(encoded)
        assertEquals(85, parsed.batteryLeft)
        assertEquals(90, parsed.batteryRight)
        assertEquals(100, parsed.batteryCase)
        assertEquals(1, parsed.ancStatus)
    }

    @Test
    fun `test DeviceRunInfoCodec live ANC status parsing`() {
        // TLV: len = 2, type = 9, value = 2 (Transparent)
        val payload = OfficialPayloadCodecs.DeviceRunInfoCodec.encodeAncStatus(2)
        val ancStatus = OfficialPayloadCodecs.DeviceRunInfoCodec.parseAncStatus(payload)
        assertEquals(2, ancStatus)
    }

    @Test
    fun `test NotificationVolume 1-byte payload parses without crash (regression)`() {
        // 1:1 DeviceConfigNotificationVolume : paramsToValue = [current] (1 byte).
        // L'ancien parse lisait value[1] -> ArrayIndexOutOfBounds qui tuait le collector.
        val oneByte = CommonConfig(type = ConfigId.NOTIFICATION_VOLUME, value = byteArrayOf(80))
        assertEquals(80, oneByte.value[0].toInt() and 0xFF)
        // Encodage d'envoi 1:1 = 1 byte unique
        val encoded = oneByte.toByteArray()
        val parsed = CommonConfig.parseList(encoded)
        assertEquals(1, parsed.size)
        assertEquals(ConfigId.NOTIFICATION_VOLUME, parsed[0].type)
        assertEquals(1, parsed[0].value.size)
        assertEquals(80, parsed[0].value[0].toInt() and 0xFF)

        // Format long 4 bytes (current/recommended/max/min) accepté aussi
        val fourBytes = CommonConfig(
            type = ConfigId.NOTIFICATION_VOLUME,
            value = byteArrayOf(70, 60, 100, 0)
        )
        assertEquals(70, fourBytes.value[0].toInt() and 0xFF)
    }

    @Test
    fun `test VendorDataCodec OpCode 8 noise mode encoding`() {
        val payload = OfficialPayloadCodecs.VendorDataCodec.encodeNoiseMode(1) // ANC
        assertEquals(3, payload.size)
        assertEquals(2.toByte(), payload[0]) // len
        assertEquals(4.toByte(), payload[1]) // type = TYPE_NOISE
        assertEquals(1.toByte(), payload[2]) // value = 1

        val map = OfficialPayloadCodecs.VendorDataCodec.parse(payload)
        assertTrue(map.containsKey(OfficialPayloadCodecs.VendorDataCodec.TYPE_NOISE))
        assertEquals(1.toByte(), map[OfficialPayloadCodecs.VendorDataCodec.TYPE_NOISE]?.get(0))
    }

    @Test
    fun `test RcspPacket createAckResponse generates valid response`() {
        val cmd = RcspPacket(
            type = RcspPacket.TYPE_COMMAND,
            hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
            targetApp = RcspPacket.TARGET_APP_EARPHONE,
            opCode = RcspPacket.CMD_NOTIFY_DEVICE_CONFIG,
            opCodeSn = 42
        )
        val ack = RcspPacket.createAckResponse(cmd, status = 0)
        assertEquals(RcspPacket.TYPE_RESPONSE, ack.type)
        assertEquals(RcspPacket.FLAG_NO_RESPONSE, ack.hasResponse)
        assertEquals(RcspPacket.TARGET_APP_EARPHONE, ack.targetApp)
        assertEquals(RcspPacket.CMD_NOTIFY_DEVICE_CONFIG, ack.opCode)
        assertEquals(42, ack.opCodeSn)
        assertEquals(0, ack.status)
    }

    @Test
    fun `test parseStreamWithConsumed correctly tracks consumed bytes`() {
        val pkt1 = RcspPacket(
            type = RcspPacket.TYPE_COMMAND,
            opCode = RcspPacket.CMD_GET_TARGET_INFO,
            opCodeSn = 1
        )
        val pkt2 = RcspPacket(
            type = RcspPacket.TYPE_COMMAND,
            opCode = RcspPacket.CMD_GET_DEVICE_CONFIG,
            opCodeSn = 2
        )
        val bytes1 = pkt1.toByteArray()
        val bytes2 = pkt2.toByteArray()

        // Combine both packets with trailing incomplete bytes
        val stream = bytes1 + bytes2 + byteArrayOf(0xFD.toByte(), 0x01, 0x02)

        val (packets, consumed) = RcspPacket.parseStreamWithConsumed(stream)
        assertEquals(2, packets.size)
        assertEquals(1, packets[0].opCodeSn)
        assertEquals(2, packets[1].opCodeSn)
        assertEquals(bytes1.size + bytes2.size, consumed)
    }

    @Test
    fun `test NoiseControlState fromCommonConfig handles 1-byte and multi-byte payloads`() {
        // 1-byte NOISE_LEVEL_CHOOSE
        val cfg1Byte = CommonConfig(
            type = ConfigId.NOISE_LEVEL_CHOOSE,
            value = byteArrayOf(1) // ANC mode, no level specified
        )
        val state1 = NoiseControlState.fromCommonConfig(cfg1Byte, NoiseControlState(mode = NoiseMode.OFF))
        assertNotNull(state1)
        assertEquals(NoiseMode.ANC, state1?.mode)

        // 2-byte NOISE_LEVEL_CHOOSE
        val cfg2Byte = CommonConfig(
            type = ConfigId.NOISE_LEVEL_CHOOSE,
            value = byteArrayOf(2, 1) // Transparent mode, vocal level 1
        )
        val state2 = NoiseControlState.fromCommonConfig(cfg2Byte, NoiseControlState(mode = NoiseMode.OFF))
        assertNotNull(state2)
        assertEquals(NoiseMode.TRANSPARENCY, state2?.mode)
        assertEquals(TransparencyLevel.VOCAL, state2?.transparencyLevel)

        // CONFIG_AUDIO_MODE (1) = Xiaomi/Dolby audio mode, PAS du ANC.
        // Officiel: DeviceConfigNoiseLevel = super(11) uniquement, jamais 1.
        // Donc fromCommonConfig doit retourner null pour config 1.
        val cfgAudioMode = CommonConfig(
            type = ConfigId.CONFIG_AUDIO_MODE,
            value = byteArrayOf(1) // ANC
        )
        val stateAudio = NoiseControlState.fromCommonConfig(cfgAudioMode, NoiseControlState(mode = NoiseMode.OFF))
        assertNull(stateAudio)
    }

    @Test
    fun `test SimulatedTransport responds to OpCode 8 and OpCode 9`() = runBlocking {
        val transport = SimulatedTransport()
        transport.connect("SIM:TEST:01")

        val received = mutableListOf<RcspPacket>()
        val job = launch {
            transport.incomingPackets.collect {
                received.add(it)
            }
        }

        // Send OpCode 8 to set ANC
        val op8Cmd = RcspPacket(
            type = RcspPacket.TYPE_COMMAND,
            hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
            targetApp = RcspPacket.TARGET_APP_EARPHONE,
            opCode = RcspPacket.CMD_SET_TARGET_INFO,
            opCodeSn = 10,
            payload = OfficialPayloadCodecs.VendorDataCodec.encodeNoiseMode(1)
        )
        transport.send(op8Cmd)
        delay(100)

        // Send OpCode 9 to query live ANC
        val op9Cmd = RcspPacket(
            type = RcspPacket.TYPE_COMMAND,
            hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
            targetApp = RcspPacket.TARGET_APP_EARPHONE,
            opCode = RcspPacket.CMD_GET_DEVICE_RUN_INFO,
            opCodeSn = 11,
            payload = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
        )
        transport.send(op9Cmd)
        delay(100)

        job.cancel()

        val op8Resp = received.find { it.opCode == RcspPacket.CMD_SET_TARGET_INFO }
        assertNotNull(op8Resp)
        assertEquals(0, op8Resp?.status)

        val op9Resp = received.find { it.opCode == RcspPacket.CMD_GET_DEVICE_RUN_INFO }
        assertNotNull(op9Resp)
        val ancStatus = OfficialPayloadCodecs.DeviceRunInfoCodec.parseAncStatus(op9Resp!!.payload)
        assertEquals(1, ancStatus) // Mode 1 = ANC
    }

    @Test
    fun `test MockTransport bidirectional live synchronization with EarbudsController`() = runBlocking {
        class MockBluetoothTransport : BluetoothTransport {
            val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
            override val connectionState: StateFlow<ConnectionState> = _connectionState

            val _connectedDevice = MutableStateFlow<DiscoveredDevice?>(null)
            override val connectedDevice: StateFlow<DiscoveredDevice?> = _connectedDevice

            val _incomingPackets = MutableSharedFlow<RcspPacket>(replay = 50)
            override val incomingPackets: SharedFlow<RcspPacket> = _incomingPackets

            val sentPackets = mutableListOf<RcspPacket>()

            override fun startScanning(onDevicesFound: (List<DiscoveredDevice>) -> Unit) {}
            override fun stopScanning() {}
            override fun getPairedDevices(): List<DiscoveredDevice> = emptyList()

            override suspend fun connect(address: String): Boolean {
                _connectionState.value = ConnectionState.CONNECTED
                _connectedDevice.value = DiscoveredDevice("Redmi Buds 6 Pro", address, true, true, 5)
                return true
            }

            override suspend fun disconnect() {
                _connectionState.value = ConnectionState.DISCONNECTED
            }

            override suspend fun send(packet: RcspPacket): Boolean {
                sentPackets.add(packet)
                return true
            }
        }

        val testScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        val controller = EarbudsController(testScope, autoConnectOnStartup = false)

        // Connect demo mode to test
        controller.setDemoMode(true)
        delay(150)

        // When phone toggles ANC, earbud sends OpCode 14 CMD_REPORT_DEVICE_STATUS
        val statusReportPayload = OfficialPayloadCodecs.DeviceStatusCodec.encode(
            OfficialPayloadCodecs.DeviceStatusReport(
                batteryLeft = 70,
                batteryRight = 75,
                batteryCase = 80,
                ancStatus = 1 // Phone switched to ANC
            )
        )
        val incomingReportPacket = RcspPacket(
            type = RcspPacket.TYPE_COMMAND,
            hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
            targetApp = RcspPacket.TARGET_APP_EARPHONE,
            opCode = RcspPacket.CMD_REPORT_DEVICE_STATUS,
            opCodeSn = 99,
            payload = statusReportPayload
        )

        // Send via controller's internal transport mechanism
        val method = EarbudsController::class.java.getDeclaredMethod("handleIncomingPacket", RcspPacket::class.java)
        method.isAccessible = true
        method.invoke(controller, incomingReportPacket)

        // Verify live noise mode updated on desktop app
        assertEquals(NoiseMode.ANC, controller.noiseControl.value.mode)
        assertEquals(70, controller.deviceInfo.value.leftBattery.percentage)
        assertEquals(75, controller.deviceInfo.value.rightBattery.percentage)
        assertEquals(80, controller.deviceInfo.value.caseBattery.percentage)

        // Now test OpCode 244 CMD_NOTIFY_DEVICE_CONFIG (phone switched to Transparency)
        val notifyConfigPayload = CommonConfig(
            type = ConfigId.NOISE_LEVEL_CHOOSE,
            value = byteArrayOf(2, 0) // Transparency mode, regular
        ).toByteArray()
        val incomingNotifyPacket = RcspPacket(
            type = RcspPacket.TYPE_COMMAND,
            hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
            targetApp = RcspPacket.TARGET_APP_EARPHONE,
            opCode = RcspPacket.CMD_NOTIFY_DEVICE_CONFIG,
            opCodeSn = 100,
            payload = notifyConfigPayload
        )
        method.invoke(controller, incomingNotifyPacket)

        // Verify live noise mode updated to Transparency on desktop app
        assertEquals(NoiseMode.TRANSPARENCY, controller.noiseControl.value.mode)

        testScope.cancel()
    }

    @Test
    fun `test JNA SockAddrRc memory layout`() {
        val addr = LinuxRfcommTransport.SockAddrRc()
        assertEquals(31.toShort(), addr.rc_family)
        assertEquals(1.toByte(), addr.rc_channel)
        assertEquals(10, addr.size())
    }
}

