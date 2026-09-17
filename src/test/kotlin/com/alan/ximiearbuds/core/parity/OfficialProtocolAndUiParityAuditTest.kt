package com.alan.ximiearbuds.core.parity

import com.alan.ximiearbuds.core.protocol.BatteryInfo
import com.alan.ximiearbuds.core.protocol.RcspPacket
import com.alan.ximiearbuds.core.protocol.TargetDeviceInfo
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Audit test comparing protocol constants and UI layout models directly against
 * the decompiled Xiaomi Earbuds source code and XML resources.
 */
class OfficialProtocolAndUiParityAuditTest {

    private val decompiledDir = File("/home/alan/earbuds_decompiled")
    private val decompiledSources = File(decompiledDir, "sources")
    private val decompiledLayouts = File(decompiledDir, "resources/res/layout")

    @Test
    fun `test RcspPacket OpCodes match decompiled Command java constants`() {
        val commandJava = File(decompiledSources, "com/xiaomi/aivsbluetoothsdk/constant/Command.java")
        assertTrue(commandJava.exists(), "Command.java must exist in decompiled sources")

        val lines = commandJava.readLines()
        val parsedCommands = mutableMapOf<String, Int>()
        val regex = Regex("""public static final int\s+(\w+)\s*=\s*(\d+);""")
        for (line in lines) {
            val match = regex.find(line.trim())
            if (match != null) {
                parsedCommands[match.groupValues[1]] = match.groupValues[2].toInt()
            }
        }

        // Verify critical OpCodes that were previously inaccurate
        assertEquals(parsedCommands["CMD_SET_TARGET_INFO"], RcspPacket.CMD_SET_TARGET_INFO)
        assertEquals(8, RcspPacket.CMD_SET_TARGET_INFO)

        assertEquals(parsedCommands["CMD_DISCONNECT_CLASSIC_BLUETOOTH"], RcspPacket.CMD_DISCONNECT_CLASSIC_BT)
        assertEquals(6, RcspPacket.CMD_DISCONNECT_CLASSIC_BT)

        assertEquals(parsedCommands["CMD_REBOOT_DEVICE"], RcspPacket.CMD_REBOOT_DEVICE)
        assertEquals(3, RcspPacket.CMD_REBOOT_DEVICE)

        assertEquals(parsedCommands["CMD_SETTINGS_COMMUNICATION_MTU"], RcspPacket.CMD_SETTINGS_MTU)
        assertEquals(5, RcspPacket.CMD_SETTINGS_MTU)

        assertEquals(parsedCommands["CMD_GET_TARGET_INFO"], RcspPacket.CMD_GET_TARGET_INFO)
        assertEquals(2, RcspPacket.CMD_GET_TARGET_INFO)

        assertEquals(parsedCommands["CMD_GET_DEVICE_RUN_INFO"], RcspPacket.CMD_GET_DEVICE_RUN_INFO)
        assertEquals(9, RcspPacket.CMD_GET_DEVICE_RUN_INFO)

        assertEquals(parsedCommands["CMD_FIND_DEVICE_CMD"], RcspPacket.CMD_FIND_DEVICE)
        assertEquals(66, RcspPacket.CMD_FIND_DEVICE)

        assertEquals(parsedCommands["CMD_SET_DEVICE_CONFIG"], RcspPacket.CMD_SET_DEVICE_CONFIG)
        assertEquals(242, RcspPacket.CMD_SET_DEVICE_CONFIG)

        assertEquals(parsedCommands["CMD_GET_DEVICE_CONFIG"], RcspPacket.CMD_GET_DEVICE_CONFIG)
        assertEquals(243, RcspPacket.CMD_GET_DEVICE_CONFIG)

        assertEquals(parsedCommands["CMD_NOTIFY_DEVICE_CONFIG"], RcspPacket.CMD_NOTIFY_DEVICE_CONFIG)
        assertEquals(244, RcspPacket.CMD_NOTIFY_DEVICE_CONFIG)
    }

    @Test
    fun `test firmware version decoding matches 4-nibble format in decompiled f7 f java`() {
        // In f7/f.java lines 535-548, 2049:
        // byte 0: v1 = (byte0 >> 4) & 0xF, v2 = byte0 & 0xF
        // byte 1: v3 = (byte1 >> 4) & 0xF, v4 = byte1 & 0xF
        // resulting in "v1.v2.v3.v4" (e.g. 1.0.8.2)

        // Simulate TargetDeviceInfo payload:
        // Tag 1 (Version), valueLen 2: len = 3, tag = 1, byte 0 = 0x10, byte 1 = 0x82 -> "1.0.8.2"
        val payload1 = byteArrayOf(
            3, 1, 0x10.toByte(), 0x82.toByte()
        )
        val info1 = TargetDeviceInfo.parseFromResponsePayload(payload1)
        assertEquals("1.0.8.2", info1.versionName)
        assertEquals(0x1082, info1.versionCode)

        // Tag 1 (Version), valueLen 2: len = 3, tag = 1, byte 0 = 0x21, byte 1 = 0x59 -> "2.1.5.9"
        val payload2 = byteArrayOf(
            3, 1, 0x21.toByte(), 0x59.toByte()
        )
        val info2 = TargetDeviceInfo.parseFromResponsePayload(payload2)
        assertEquals("2.1.5.9", info2.versionName)
        assertEquals(0x2159, info2.versionCode)
    }

    @Test
    fun `test battery parsing parity matches decompiled DeviceElectricInfo java`() {
        // Tag 7 (Battery): 3 bytes [Left, Right, Case], len = 4, tag = 7
        // Bit 7 is charging indicator, Bits 0-6 are percentage
        val leftByte = (0x80 or 85).toByte() // 85%, charging
        val rightByte = 90.toByte()          // 90%, not charging
        val caseByte = (0x80 or 100).toByte()// 100%, charging

        val payload = byteArrayOf(
            4, 7, leftByte, rightByte, caseByte
        )
        val info = TargetDeviceInfo.parseFromResponsePayload(payload)

        assertTrue(info.leftBattery.isConnected)
        assertEquals(85, info.leftBattery.percentage)
        assertTrue(info.leftBattery.isCharging)

        assertTrue(info.rightBattery.isConnected)
        assertEquals(90, info.rightBattery.percentage)
        assertFalse(info.rightBattery.isCharging)

        assertTrue(info.caseBattery.isConnected)
        assertEquals(100, info.caseBattery.percentage)
        assertTrue(info.caseBattery.isCharging)
    }

    @Test
    fun `test UI layout parity with decompiled XML layouts`() {
        // 1. Action Bar header layout exists
        val headerXml = File(decompiledLayouts, "device_settings_layout_setting_header.xml")
        assertTrue(headerXml.exists())
        val headerContent = headerXml.readText()
        assertTrue(headerContent.contains("device_name_tv"))
        assertTrue(headerContent.contains("show_all_device_tv"))
        assertTrue(headerContent.contains("add_device_iv"))
        assertTrue(headerContent.contains("user_avatar_iv"))

        // 2. Battery layout strings
        val batteryXml = File(decompiledLayouts, "device_settings_layout_battery.xml")
        assertTrue(batteryXml.exists())
        val batteryContent = batteryXml.readText()
        assertTrue(batteryContent.contains("@string/device_settings_left"))
        assertTrue(batteryContent.contains("@string/device_settings_right"))
        assertTrue(batteryContent.contains("@string/device_settings_box"))

        // 3. Noise reduction modes layout
        val noiseXml = File(decompiledLayouts, "device_settings_layout_noise_redution.xml")
        assertTrue(noiseXml.exists())
        val noiseContent = noiseXml.readText()
        assertTrue(noiseContent.contains("@string/device_settings_noise_reduction_transparent"))
        assertTrue(noiseContent.contains("@string/device_settings_noise_reduction_open"))
        assertTrue(noiseContent.contains("@string/device_settings_noise_reduction_close"))

        // 4. Gesture layout elements
        val gestureXml = File(decompiledLayouts, "device_settings_fragment_gesture.xml")
        assertTrue(gestureXml.exists())
        val gestureContent = gestureXml.readText()
        assertTrue(gestureContent.contains("@string/device_settings_left_click"))
        assertTrue(gestureContent.contains("@string/device_settings_right_click"))
        assertTrue(gestureContent.contains("@string/device_settings_left_double_click"))
        assertTrue(gestureContent.contains("@string/device_settings_right_double_click"))
        assertTrue(gestureContent.contains("@string/device_settings_left_triple_click"))
        assertTrue(gestureContent.contains("@string/device_settings_right_triple_click"))
        assertTrue(gestureContent.contains("@string/device_settings_left_long_press"))
        assertTrue(gestureContent.contains("@string/device_settings_right_long_press"))
    }

    @Test
    fun `test model capability architecture parity against decompiled Function and Group IDs`() {
        val functionJava = File(decompiledSources, "com/mi/earphone/device/manager/export/Function.java")
        assertTrue(functionJava.exists(), "Function.java must exist in decompiled sources")
        val funcLines = functionJava.readLines()

        // Verify Function and Group constants match
        for (line in funcLines) {
            val match = Regex("""public static final int\s+(\w+)\s*=\s*(\d+);""").find(line.trim())
            if (match != null) {
                val name = match.groupValues[1]
                val id = match.groupValues[2].toInt()
                when (name) {
                    "GROUP_NOISE" -> assertEquals(com.alan.ximiearbuds.core.protocol.OfficialGroupIds.GROUP_NOISE, id)
                    "GROUP_SOUND_SETTING" -> assertEquals(com.alan.ximiearbuds.core.protocol.OfficialGroupIds.GROUP_VOICE, id)
                    "GROUP_FUNCTION_SETTING" -> assertEquals(com.alan.ximiearbuds.core.protocol.OfficialGroupIds.GROUP_FUNCTION_SETTING, id)
                    "GROUP_GESTURE_SETTING" -> assertEquals(com.alan.ximiearbuds.core.protocol.OfficialGroupIds.GROUP_GESTURE_SETTING, id)
                    "GROUP_DEVICE_MANAGE" -> assertEquals(com.alan.ximiearbuds.core.protocol.OfficialGroupIds.GROUP_NORMAL, id)
                    "GROUP_LABORATORY" -> assertEquals(com.alan.ximiearbuds.core.protocol.OfficialGroupIds.GROUP_LABORATORY, id)
                    "GROUP_USB" -> assertEquals(com.alan.ximiearbuds.core.protocol.OfficialGroupIds.GROUP_USB, id)
                    "GROUP_SPORT" -> assertEquals(com.alan.ximiearbuds.core.protocol.OfficialGroupIds.GROUP_SPORT, id)
                    "FUNC_SOUND_SETTING_VIRTUAL_SURROUND" -> assertEquals(com.alan.ximiearbuds.core.protocol.OfficialFunctionIds.FUNC_SOUND_SETTING_VIRTUAL_SURROUND, id)
                    "FUNC_SOUND_SETTING_CLOSE_SPATIAL_AUDIO" -> assertEquals(com.alan.ximiearbuds.core.protocol.OfficialFunctionIds.FUNC_SOUND_SETTING_CLOSE_SPATIAL_AUDIO, id)
                    "FUNC_SOUND_SETTING_SPATIAL_AUDIO_HEAD_TRACKING" -> assertEquals(com.alan.ximiearbuds.core.protocol.OfficialFunctionIds.FUNC_SOUND_SETTING_SPATIAL_AUDIO_HEAD_TRACKING, id)
                    "FUNC_SOUND_SETTINGS_SOUND_MODE" -> assertEquals(com.alan.ximiearbuds.core.protocol.OfficialFunctionIds.FUNC_SOUND_SETTINGS_SOUND_MODE, id)
                    "FUNC_SOUND_SETTINGS_ADAPTIVE_SENSE" -> assertEquals(com.alan.ximiearbuds.core.protocol.OfficialFunctionIds.FUNC_SOUND_SETTINGS_ADAPTIVE_SENSE, id)
                    "FUNC_SOUND_EFFECT_RENDERING" -> assertEquals(com.alan.ximiearbuds.core.protocol.OfficialFunctionIds.FUNC_SOUND_EFFECT_RENDERING, id)
                    "FUNC_SOUND_SETTINGS_ADAPTIVE_VOLUME" -> assertEquals(com.alan.ximiearbuds.core.protocol.OfficialFunctionIds.FUNC_SOUND_SETTINGS_ADAPTIVE_VOLUME, id)
                    "FUNC_AUDIBILITY_ADAPTATION" -> assertEquals(com.alan.ximiearbuds.core.protocol.OfficialFunctionIds.FUNC_AUDIBILITY_ADAPTATION, id)
                    "FUNC_NOTIFICATION_VOLUME" -> assertEquals(com.alan.ximiearbuds.core.protocol.OfficialFunctionIds.FUNC_NOTIFICATION_VOLUME, id)
                }
            }
        }

        // Verify loaded models have authentic capabilities
        val models = com.alan.ximiearbuds.core.device.DeviceRegistry.ALL_MODELS
        assertTrue(models.size >= 78, "Must load at least 78 models")

        // Flagship device check: Xiaomi Buds 4 Pro (L71)
        val l71 = models.firstOrNull { it.codename == "L71" }
        assertNotNull(l71)
        assertTrue(l71!!.hasFunction(com.alan.ximiearbuds.core.protocol.OfficialFunctionIds.FUNC_SOUND_SETTING_CLOSE_SPATIAL_AUDIO))
        assertTrue(l71.hasFunction(com.alan.ximiearbuds.core.protocol.OfficialFunctionIds.FUNC_SOUND_SETTING_SPATIAL_AUDIO_HEAD_TRACKING))
        assertTrue(l71.hasGroup(com.alan.ximiearbuds.core.protocol.OfficialGroupIds.GROUP_NOISE))
        assertTrue(l71.hasGroup(com.alan.ximiearbuds.core.protocol.OfficialGroupIds.GROUP_VOICE))
        assertTrue(l71.gestureCapabilities.isPinchGesture)
    }
}

