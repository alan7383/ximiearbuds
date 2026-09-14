package com.alan.ximiearbuds.core.protocol

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class OfficialConstantsParityTest {

    private val decompiledDir = File("/home/alan/earbuds_decompiled/sources")

    /**
     * Helper to extract "public static final int NAME = VALUE;" from decompiled Java files.
     */
    private fun extractConstants(file: File): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        if (!file.exists()) return result

        val pattern = Regex("""public\s+static\s+final\s+int\s+([A-Za-z0-9_]+)\s*=\s*(-?[0-9]+);""")
        file.forEachLine { line ->
            pattern.find(line)?.let { match ->
                val name = match.groupValues[1]
                val value = match.groupValues[2].toInt()
                result[name] = value
            }
        }
        return result
    }

    @Test
    fun `test all 48 CMDConstantKt commands match decompiled app`() {
        val cmdFile = File(decompiledDir, "com/mi/earphone/bluetoothsdk/constant/CMDConstantKt.java")
        assertTrue(cmdFile.exists(), "CMDConstantKt.java must exist in decompiled source")
        val decompiledConstants = extractConstants(cmdFile)

        // Verify specific key commands
        assertEquals(1, OfficialCommands.CMD_DATA)
        assertEquals(2, OfficialCommands.CMD_GET_TARGET_INFO)
        assertEquals(3, OfficialCommands.CMD_REBOOT_DEVICE)
        assertEquals(8, OfficialCommands.CMD_SET_TARGET_INFO)
        assertEquals(208, OfficialCommands.CMD_START_SPEECH)
        assertEquals(227, OfficialCommands.CMD_OTA_ENTER_UPDATE_MODE)

        // Verify that every single decompiled constant matches OfficialCommands reflection/values
        val officialFields = OfficialCommands::class.java.declaredFields
            .filter { it.type == Int::class.javaPrimitiveType }
            .associate {
                it.isAccessible = true
                it.name to (it.getInt(null))
            }

        for ((name, expectedVal) in decompiledConstants) {
            val actualVal = officialFields[name]
            assertEquals(expectedVal, actualVal, "Mismatch for Command constant $name")
        }
    }

    @Test
    fun `test all 75 DeviceConfigIdConstantKt configs match decompiled app`() {
        val configFile = File(decompiledDir, "com/mi/earphone/bluetoothsdk/constant/DeviceConfigIdConstantKt.java")
        assertTrue(configFile.exists(), "DeviceConfigIdConstantKt.java must exist in decompiled source")
        val decompiledConstants = extractConstants(configFile)

        // Verify specific key configs
        assertEquals(1, OfficialConfigIds.CONFIG_AUDIO_MODE)
        assertEquals(2, OfficialConfigIds.CONFIG_CUSTOM_CLICK)
        assertEquals(3, OfficialConfigIds.CONFIG_AUTO_ANSWER_PHONE)
        assertEquals(4, OfficialConfigIds.CONFIG_MULTIPOINT_CONNECTION)
        assertEquals(9, OfficialConfigIds.FIND_DEVICE)
        assertEquals(10, OfficialConfigIds.NOISE_MODE_CHOOSE)
        assertEquals(11, OfficialConfigIds.NOISE_LEVEL_CHOOSE)
        assertEquals(29, OfficialConfigIds.SWITCH_SPATIAL_AUDIO)
        assertEquals(55, OfficialConfigIds.CUSTOM_EQ)
        assertEquals(61, OfficialConfigIds.EAR_CANAL_EAR_CANAL_FIT)

        val officialFields = OfficialConfigIds::class.java.declaredFields
            .filter { it.type == Int::class.javaPrimitiveType }
            .associate {
                it.isAccessible = true
                it.name to (it.getInt(null))
            }

        for ((name, expectedVal) in decompiledConstants) {
            val actualVal = officialFields[name]
            assertEquals(expectedVal, actualVal, "Mismatch for Config ID constant $name")
        }
    }

    @Test
    fun `test all 74 Function capability flags match decompiled app`() {
        val funcFile = File(decompiledDir, "com/mi/earphone/device/manager/export/Function.java")
        assertTrue(funcFile.exists(), "Function.java must exist in decompiled source")
        val decompiledConstants = extractConstants(funcFile)

        assertEquals(1002, OfficialFunctions.FUNC_ANC_NOISE_THREE)
        assertEquals(1005, OfficialFunctions.FUNC_ANC_TRANSPARENT_THREE)
        assertEquals(1008, OfficialFunctions.FUNC_SMART_NOISE_REDUCTION)
        assertEquals(3003, OfficialFunctions.FUNC_FIT_DETECT)
        assertEquals(3004, OfficialFunctions.FUNC_DUAL_DEVICE_CONNECTION)
        assertEquals(3015, OfficialFunctions.FUNC_EARBOX_SOUND)
        assertEquals(4001, OfficialFunctions.FUNC_GESTURE_DOUBLE_CLICK)
        assertEquals(5001, OfficialFunctions.FUNC_FIND_DEVICE)

        val officialFields = OfficialFunctions::class.java.declaredFields
            .filter { it.type == Int::class.javaPrimitiveType }
            .associate {
                it.isAccessible = true
                it.name to (it.getInt(null))
            }

        for ((name, expectedVal) in decompiledConstants) {
            val actualVal = officialFields[name]
            assertEquals(expectedVal, actualVal, "Mismatch for Function constant $name")
        }
    }

    @Test
    fun `test gesture and noise constants parity`() {
        assertEquals(1, OfficialGestureClicks.DOUBLE_CLICK)
        assertEquals(2, OfficialGestureClicks.TRIPLE_CLICK)
        assertEquals(3, OfficialGestureClicks.LONG_PRESS)
        assertEquals(4, OfficialGestureClicks.ONCE_PRESS)
        assertEquals(5, OfficialGestureClicks.SLIDE)

        assertEquals(0, OfficialGestureTypes.WAKEUP_XIAOAI)
        assertEquals(1, OfficialGestureTypes.PLAY_OR_PAUSE)
        assertEquals(2, OfficialGestureTypes.PREVIOUS)
        assertEquals(3, OfficialGestureTypes.NEXT)
        assertEquals(6, OfficialGestureTypes.NOISE_CONTROL)

        assertEquals(0.toByte(), OfficialNoiseConstants.ANC_MODE_CLOSE)
        assertEquals(1.toByte(), OfficialNoiseConstants.ANC_MODE_NOISE)
        assertEquals(2.toByte(), OfficialNoiseConstants.ANC_MODE_TRANSPARENT)

        assertEquals(0.toByte(), OfficialNoiseConstants.ANC_LEVEL_NOISE_BALANCE)
        assertEquals(1.toByte(), OfficialNoiseConstants.ANC_LEVEL_NOISE_COMFORTABLE)
        assertEquals(2.toByte(), OfficialNoiseConstants.ANC_LEVEL_NOISE_DEEP)
        assertEquals(3.toByte(), OfficialNoiseConstants.ANC_LEVEL_NOISE_ADAPTIVE)
    }

    @Test
    fun `test RCSP ATTR_TYPE_COLOR_TYPE parity with decompiled SDK`() {
        val rcspFile = File(decompiledDir, "com/xiaomi/aivsbluetoothsdk/constant/RCSP.java")
        assertTrue(rcspFile.exists(), "RCSP.java must exist in decompiled SDK")
        val constants = extractConstants(rcspFile)
        assertEquals(13, constants["ATTR_TYPE_COLOR_TYPE"])
        assertEquals(constants["ATTR_TYPE_COLOR_TYPE"], OfficialRcspAttrTypes.ATTR_TYPE_COLOR_TYPE)
    }
}
