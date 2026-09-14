package com.alan.ximiearbuds.core.protocol

import com.alan.ximiearbuds.core.bluetooth.SimulatedTransport
import com.alan.ximiearbuds.core.device.DeviceRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ProtocolTests {

    @Test
    fun testRcspPacketEncodingAndDecoding() {
        val payload = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val packet = RcspPacket(
            type = RcspPacket.TYPE_COMMAND,
            hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
            targetApp = RcspPacket.TARGET_APP_EARPHONE,
            opCode = RcspPacket.CMD_GET_TARGET_INFO,
            opCodeSn = 42,
            payload = payload
        )

        val bytes = packet.toByteArray()
        assertTrue(bytes.size > 8)
        assertEquals(RcspPacket.START_BYTE_0, bytes[0])
        assertEquals(RcspPacket.START_BYTE_1, bytes[1])
        assertEquals(RcspPacket.START_BYTE_2, bytes[2])
        assertEquals(RcspPacket.END_BYTE, bytes.last())

        val parsed = RcspPacket.parse(bytes)
        assertNotNull(parsed)
        assertEquals(packet.type, parsed.type)
        assertEquals(packet.hasResponse, parsed.hasResponse)
        assertEquals(packet.targetApp, parsed.targetApp)
        assertEquals(packet.opCode, parsed.opCode)
        assertEquals(packet.opCodeSn, parsed.opCodeSn)
        assertTrue(packet.payload.contentEquals(parsed.payload))
    }

    @Test
    fun testCommonConfigTlvSerialization() {
        val config1 = CommonConfig(ConfigId.NOISE_LEVEL_CHOOSE, byteArrayOf(1, 2))
        val config2 = CommonConfig(ConfigId.LOW_LATENCY, byteArrayOf(1))

        val encoded = CommonConfig.toByteArray(listOf(config1, config2))
        val decoded = CommonConfig.parseList(encoded)

        assertEquals(2, decoded.size)
        assertEquals(ConfigId.NOISE_LEVEL_CHOOSE, decoded[0].type)
        assertTrue(byteArrayOf(1, 2).contentEquals(decoded[0].value))

        assertEquals(ConfigId.LOW_LATENCY, decoded[1].type)
        assertTrue(byteArrayOf(1).contentEquals(decoded[1].value))
    }

    @Test
    fun testNoiseControlStateConversion() {
        val ancState = NoiseControlState(
            mode = NoiseMode.ANC,
            ancLevel = AncLevel.DEEP
        )
        val config = ancState.toCommonConfig()
        assertEquals(ConfigId.NOISE_LEVEL_CHOOSE, config.type)
        assertEquals(NoiseMode.ANC.id, config.value[0].toInt())
        assertEquals(AncLevel.DEEP.id, config.value[1].toInt())

        val parsed = NoiseControlState.fromCommonConfig(config)
        assertNotNull(parsed)
        assertEquals(NoiseMode.ANC, parsed.mode)
        assertEquals(AncLevel.DEEP, parsed.ancLevel)
    }

    @Test
    fun testTargetDeviceInfoEncodingAndParsing() {
        val original = TargetDeviceInfo(
            name = "Redmi Buds 5 Pro",
            versionName = "1.0.4",
            versionCode = 0x0104,
            vendorId = 10007,
            productId = 20588,
            leftBattery = BatteryInfo(80, false, true),
            rightBattery = BatteryInfo(80, true, true),
            caseBattery = BatteryInfo(95, false, true)
        )

        val payload = TargetDeviceInfo.encodeTargetInfoResponse(original)
        val parsed = TargetDeviceInfo.parseFromResponsePayload(payload)

        assertEquals(original.name, parsed.name)
        assertEquals(original.vendorId, parsed.vendorId)
        assertEquals(original.productId, parsed.productId)
        assertEquals(80, parsed.leftBattery.percentage)
        assertEquals(false, parsed.leftBattery.isCharging)
        assertEquals(80, parsed.rightBattery.percentage)
        assertEquals(true, parsed.rightBattery.isCharging)
        assertEquals(95, parsed.caseBattery.percentage)
    }

    @Test
    fun testCustomEqualizerEncodingAndParsing() {
        val bands = listOf(
            EqBand(31, -2),
            EqBand(62, 0),
            EqBand(125, 4),
            EqBand(250, 6)
        )
        val config = EqualizerState.createCustomEqConfig(bands)
        val parsed = EqualizerState.parseCustomEq(config.value)

        assertNotNull(parsed)
        assertEquals(bands.size, parsed.size)
        assertEquals(-2, parsed[0].gainDb)
        assertEquals(0, parsed[1].gainDb)
        assertEquals(4, parsed[2].gainDb)
        assertEquals(6, parsed[3].gainDb)
    }

    @Test
    fun testGestureSettingsSerialization() {
        val gestures = GestureSettings(
            left = EarbudGestures(
                singleTap = GestureAction.PLAY_PAUSE,
                doubleTap = GestureAction.NEXT_TRACK,
                tripleTap = GestureAction.PREV_TRACK,
                longPress = GestureAction.NOISE_CONTROL
            ),
            right = EarbudGestures(
                singleTap = GestureAction.VOICE_ASSISTANT,
                doubleTap = GestureAction.VOLUME_UP,
                tripleTap = GestureAction.VOLUME_DOWN,
                longPress = GestureAction.NOISE_CONTROL
            )
        )

        val configs = gestures.toCommonConfigs()
        assertEquals(5, configs.size)

        val parsed = GestureSettings.parseFromConfigs(configs)
        assertEquals(GestureAction.PLAY_PAUSE, parsed.left.singleTap)
        assertEquals(GestureAction.VOICE_ASSISTANT, parsed.right.singleTap)
        assertEquals(GestureAction.VOLUME_UP, parsed.right.doubleTap)
        assertEquals(GestureAction.VOLUME_DOWN, parsed.right.tripleTap)
        assertEquals(GestureAction.NOISE_CONTROL, parsed.left.longPress)
    }

    @Test
    fun testDeviceRegistryLookup() {
        val rmb5p = DeviceRegistry.findByVidPid(10007, 20588)
        assertEquals("Redmi Buds 5 Pro", rmb5p.commercialName)
        assertTrue(rmb5p.hasAnc)
        assertTrue(rmb5p.has10BandEq)

        val xmb3t = DeviceRegistry.findByVidPid(10007, 20525)
        assertEquals("Xiaomi Buds 3T Pro", xmb3t.commercialName)

        val xmb4p = DeviceRegistry.findByVidPid(10007, 20533)
        assertEquals("Xiaomi Buds 4 Pro", xmb4p.commercialName)

        val byName = DeviceRegistry.findByName("Xiaomi Buds 5")
        assertEquals("Xiaomi Buds 5", byName.commercialName)
    }

    @Test
    fun testSimulatedTransportRcspResponse() = runBlocking {
        val transport = SimulatedTransport()
        val connected = transport.connect("SIM:RD:B5:PR:01:02")
        assertTrue(connected)

        val cmd = RcspPacket(
            type = RcspPacket.TYPE_COMMAND,
            hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
            targetApp = RcspPacket.TARGET_APP_EARPHONE,
            opCode = RcspPacket.CMD_GET_TARGET_INFO
        )

        transport.send(cmd)
        val resp = transport.incomingPackets.first()
        assertEquals(RcspPacket.CMD_GET_TARGET_INFO, resp.opCode)
        assertEquals(0, resp.status)

        val info = TargetDeviceInfo.parseFromResponsePayload(resp.payload)
        assertEquals("Redmi Buds 5 Pro", info.name)
        assertTrue(info.leftBattery.percentage > 0)
    }

    @Test
    fun testColorTypeExtractionFromTag13() {
        // Test RCSP Tag 13 (ATTR_TYPE_COLOR_TYPE) parsing and round-trip encoding
        val original = TargetDeviceInfo(
            name = "Redmi Buds 6 Pro",
            versionName = "1.0.8",
            versionCode = 0x0108,
            vendorId = 10007,
            productId = 20638,
            colorType = 5, // Lavender / Purple
            leftBattery = BatteryInfo(85, false, true),
            rightBattery = BatteryInfo(85, false, true),
            caseBattery = BatteryInfo(90, false, true)
        )

        val payload = TargetDeviceInfo.encodeTargetInfoResponse(original)
        val parsed = TargetDeviceInfo.parseFromResponsePayload(payload)

        assertEquals(original.name, parsed.name)
        assertEquals(original.vendorId, parsed.vendorId)
        assertEquals(original.productId, parsed.productId)
        assertEquals(5, parsed.colorType)
        assertEquals(85, parsed.leftBattery.percentage)
        assertEquals(85, parsed.rightBattery.percentage)
        assertEquals(90, parsed.caseBattery.percentage)
    }

    @Test
    fun testRedmiBuds6ProColorVariantsInCatalog() {
        val o76 = DeviceRegistry.findByVidPid(10007, 20638)
        assertEquals("Redmi Buds 6 Pro", o76.commercialName)
        assertEquals(2, o76.defaultColor)
        assertEquals(3, o76.colorVariants.size)
        assertTrue(o76.colorVariants.containsKey("1")) // White
        assertTrue(o76.colorVariants.containsKey("2")) // Black
        assertTrue(o76.colorVariants.containsKey("5")) // Lavender / Purple
    }

    @Test
    fun testDevicePreferencesDynamicColorPersistence() {
        val testMac = "AA:BB:CC:DD:EE:FF"
        com.alan.ximiearbuds.core.device.DevicePreferences.saveDeviceColor(testMac, 5)
        assertEquals(5, com.alan.ximiearbuds.core.device.DevicePreferences.getDeviceColor(testMac))
        assertEquals(5, com.alan.ximiearbuds.core.device.DevicePreferences.getDeviceColor(testMac.lowercase()))

        val testName = "Redmi Buds 6 Pro Lavender"
        com.alan.ximiearbuds.core.device.DevicePreferences.saveDeviceColor(testName, 5)
        assertEquals(5, com.alan.ximiearbuds.core.device.DevicePreferences.getDeviceColor(testName))

        // Clean up
        com.alan.ximiearbuds.core.device.DevicePreferences.removeDevice(testMac)
        com.alan.ximiearbuds.core.device.DevicePreferences.removeDevice(testName)
    }

    @Test
    fun testResolveDeviceColorLogic() {
        // Direct hardware colorType
        assertEquals(5, DeviceRegistry.resolveDeviceColor(10007, 20638, 5))
        assertEquals(1, DeviceRegistry.resolveDeviceColor(10007, 20638, 1))

        // Legacy fallback calculation when hardware reports 0
        assertEquals(1, DeviceRegistry.resolveDeviceColor(10007, 20588, 0, majorId = 1, minorId = 5))
        assertEquals(2, DeviceRegistry.resolveDeviceColor(10007, 20588, 0, majorId = 1, minorId = 6))
        assertEquals(9, DeviceRegistry.resolveDeviceColor(10007, 20588, 0, majorId = 1, minorId = 10))
    }
}
