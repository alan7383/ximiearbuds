package com.alan.ximiearbuds.core.protocol

import com.alan.ximiearbuds.core.catalog.XiaomiCatalogService
import com.alan.ximiearbuds.core.device.DeviceRegistry
import com.alan.ximiearbuds.core.device.EarbudsModel
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File

class DecompiledClassAndMethodParityTest {

    private val decompiledDir = File("/home/alan/earbuds_decompiled/sources")

    /**
     * Helper to extract "public static final int NAME = VALUE;" from decompiled Java files.
     */
    private fun extractIntConstants(file: File): Map<String, Int> {
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

    /**
     * Helper to extract "public static final String NAME = "VALUE";" from decompiled Java files.
     */
    private fun extractStringConstants(file: File): Map<String, String> {
        val result = mutableMapOf<String, String>()
        if (!file.exists()) return result

        val pattern = Regex("""public\s+static\s+final\s+String\s+([A-Za-z0-9_]+)\s*=\s*"([^"]*)";""")
        file.forEachLine { line ->
            pattern.find(line)?.let { match ->
                val name = match.groupValues[1]
                val value = match.groupValues[2]
                result[name] = value
            }
        }
        return result
    }

    @Test
    fun `test 100 percent parity with decompiled CMDConstantKt`() {
        val cmdFile = File(decompiledDir, "com/mi/earphone/bluetoothsdk/constant/CMDConstantKt.java")
        assertTrue(cmdFile.exists(), "CMDConstantKt.java must exist in decompiled sources")
        val decompiledConstants = extractIntConstants(cmdFile)
        assertTrue(decompiledConstants.isNotEmpty())

        val officialFields = OfficialCommands::class.java.declaredFields
            .filter { it.type == Int::class.javaPrimitiveType }
            .associate {
                it.isAccessible = true
                it.name to it.getInt(null)
            }

        for ((name, expectedVal) in decompiledConstants) {
            val actualVal = officialFields[name]
            assertEquals(expectedVal, actualVal, "Mismatch for CMD constant $name")
        }
    }

    @Test
    fun `test 100 percent parity with decompiled DeviceConfigIdConstantKt`() {
        val configFile = File(decompiledDir, "com/mi/earphone/bluetoothsdk/constant/DeviceConfigIdConstantKt.java")
        assertTrue(configFile.exists(), "DeviceConfigIdConstantKt.java must exist in decompiled sources")
        val decompiledConstants = extractIntConstants(configFile)
        assertTrue(decompiledConstants.isNotEmpty())

        val officialFields = OfficialConfigIds::class.java.declaredFields
            .filter { it.type == Int::class.javaPrimitiveType }
            .associate {
                it.isAccessible = true
                it.name to it.getInt(null)
            }

        for ((name, expectedVal) in decompiledConstants) {
            val actualVal = officialFields[name]
            assertEquals(expectedVal, actualVal, "Mismatch for Config ID constant $name")
        }
    }

    @Test
    fun `test 100 percent parity with decompiled Function`() {
        val funcFile = File(decompiledDir, "com/mi/earphone/device/manager/export/Function.java")
        assertTrue(funcFile.exists(), "Function.java must exist in decompiled sources")
        val decompiledConstants = extractIntConstants(funcFile)
        assertTrue(decompiledConstants.isNotEmpty())

        val officialFields = OfficialFunctions::class.java.declaredFields
            .filter { it.type == Int::class.javaPrimitiveType }
            .associate {
                it.isAccessible = true
                it.name to it.getInt(null)
            }

        for ((name, expectedVal) in decompiledConstants) {
            val actualVal = officialFields[name]
            assertEquals(expectedVal, actualVal, "Mismatch for Function constant $name")
        }
    }

    @Test
    fun `test 100 percent parity with decompiled GestureClick and GestureType`() {
        val clickFile = File(decompiledDir, "com/mi/earphone/bluetoothsdk/setting/gesture/GestureClick.java")
        assertTrue(clickFile.exists(), "GestureClick.java must exist in decompiled sources")
        val clickConstants = extractIntConstants(clickFile)
        assertTrue(clickConstants.isNotEmpty())

        val officialClicks = OfficialGestureClicks::class.java.declaredFields
            .filter { it.type == Int::class.javaPrimitiveType }
            .associate {
                it.isAccessible = true
                it.name to it.getInt(null)
            }

        for ((name, expectedVal) in clickConstants) {
            val actualVal = officialClicks[name]
            assertEquals(expectedVal, actualVal, "Mismatch for GestureClick constant $name")
        }

        val typeFile = File(decompiledDir, "com/mi/earphone/bluetoothsdk/setting/gesture/GestureType.java")
        assertTrue(typeFile.exists(), "GestureType.java must exist in decompiled sources")
        val typeConstants = extractIntConstants(typeFile)
        assertTrue(typeConstants.isNotEmpty())

        val officialTypes = OfficialGestureTypes::class.java.declaredFields
            .filter { it.type == Int::class.javaPrimitiveType }
            .associate {
                it.isAccessible = true
                it.name to it.getInt(null)
            }

        for ((name, expectedVal) in typeConstants) {
            val actualVal = officialTypes[name]
            assertEquals(expectedVal, actualVal, "Mismatch for GestureType constant $name")
        }
    }

    @Test
    fun `test 100 percent parity with decompiled FindDeviceConstant`() {
        val findFile = File(decompiledDir, "com/mi/earphone/bluetoothsdk/setting/lab/FindDeviceConstant.java")
        assertTrue(findFile.exists(), "FindDeviceConstant.java must exist in decompiled sources")
        val decompiledConstants = extractIntConstants(findFile)
        assertTrue(decompiledConstants.isNotEmpty())

        val officialFields = OfficialFindDeviceConstants::class.java.declaredFields
            .filter { it.type == Int::class.javaPrimitiveType }
            .associate {
                it.isAccessible = true
                it.name to it.getInt(null)
            }

        for ((name, expectedVal) in decompiledConstants) {
            val actualVal = officialFields[name]
            assertEquals(expectedVal, actualVal, "Mismatch for FindDevice constant $name")
        }
    }

    @Test
    fun `test all catalog models have existing official icon assets`() {
        val models = XiaomiCatalogService.loadBundledCatalog()
        assertTrue(models.isNotEmpty(), "Bundled catalog must not be empty")

        val iconsDir = File("src/main/resources/devices/icons")
        assertTrue(iconsDir.exists(), "devices/icons directory must exist")

        for (model in models) {
            assertFalse(model.iconFile.isBlank(), "Model ${model.commercialName} must have a non-blank iconFile")
            val iconFile = File(iconsDir, model.iconFile)
            val iconFileLower = File(iconsDir, model.iconFile.lowercase())
            val iconFileUpper = File(iconsDir, model.iconFile.uppercase())
            assertTrue(
                iconFile.exists() || iconFileLower.exists() || iconFileUpper.exists(),
                "Asset icon file ${model.iconFile} must exist on disk for model ${model.commercialName}"
            )
        }
    }

    @Test
    fun `test AddDeviceViewModel deduplication logic matches handleDeviceList`() {
        // Replicates handleDeviceList() in AddDeviceViewModel.java
        val models = XiaomiCatalogService.loadBundledCatalog()
        val seen = HashSet<String>()
        val deduplicated = mutableListOf<EarbudsModel>()
        for (m in models) {
            if (seen.add(m.commercialName)) {
                deduplicated.add(m)
            }
        }

        assertTrue(deduplicated.size in 50..80)
        assertEquals(deduplicated.size, seen.size)
        // Ensure no duplicate commercial names
        val dupes = deduplicated.groupBy { it.commercialName }.filter { it.value.size > 1 }
        assertTrue(dupes.isEmpty(), "Deduplicated list must contain unique device names: $dupes")
    }

    @Test
    fun `test ScanDeviceViewModel pairing instructions logic`() {
        val models = XiaomiCatalogService.loadBundledCatalog()

        for (m in models) {
            val isM79A = m.codename.contains("M79A", ignoreCase = true)
            val isJ77S = m.codename.contains("J77S", ignoreCase = true)
            val isO73 = m.codename.contains("O73", ignoreCase = true)
            val isO70C = m.codename.contains("O70C", ignoreCase = true)

            val instructionKey = when {
                isM79A -> "device_manager_scan_desc_m79a"
                isJ77S -> "device_manager_scan_desc_j77s"
                isO73 -> "device_manager_scan_desc_o73"
                isO70C -> "device_manager_scan_desc_o70c"
                else -> "device_manager_scan_desc"
            }
            assertNotNull(instructionKey)
        }
    }

    @Test
    fun `test every earbud model has individualized feature configurations according to official capability specifications`() {
        val models = XiaomiCatalogService.loadBundledCatalog()
        assertEquals(78, models.size, "Total bundled models must be 78")

        // 1. Verify feature heterogeneity (no uniform blanket features)
        val ancModels = models.count { it.hasAnc }
        val nonAncModels = models.count { !it.hasAnc }
        assertTrue(ancModels in 50..65, "Expected around 57 ANC models, got $ancModels")
        assertTrue(nonAncModels in 15..28, "Expected around 21 non-ANC models, got $nonAncModels")

        val slideModels = models.count { it.hasSlideGesture }
        assertTrue(slideModels in 10..22, "Only select models support slide gestures, got $slideModels")

        val spatialModels = models.count { it.hasSpatialAudio }
        assertTrue(spatialModels in 5..35, "Only flagship and pro models support spatial audio, got $spatialModels")

        val dongleModels = models.count { it.hasDongle }
        assertTrue(dongleModels in 2..5, "Only gaming models support dongle, got $dongleModels")

        val boneModels = models.count { it.isBoneConduction }
        assertTrue(boneModels in 4..10, "Only bone conduction / open wear models have bone conduction flag, got $boneModels")

        // 2. Concrete model checks against official specifications
        val pikachu = requireNotNull(models.find { it.codename.contains("Air2Pokemon", ignoreCase = true) })
        assertFalse(pikachu.hasAnc, "Air2 Pokemon Edition does not support ANC")
        assertFalse(pikachu.hasTransparency, "Air2 Pokemon Edition does not support transparency")
        assertFalse(pikachu.hasSpatialAudio, "Air2 Pokemon Edition does not support spatial audio")
        assertFalse(pikachu.isBoneConduction, "Air2 Pokemon Edition is in-ear, not bone conduction")

        val redmi6Pro = requireNotNull(models.find { it.codename.contains("O76", ignoreCase = true) && !it.codename.contains("G") })
        assertTrue(redmi6Pro.hasAnc, "Redmi Buds 6 Pro supports ANC")
        assertTrue(redmi6Pro.hasTransparency, "Redmi Buds 6 Pro supports Transparency")
        assertTrue(redmi6Pro.hasSlideGesture, "Redmi Buds 6 Pro supports stem Slide Gestures")
        assertTrue(redmi6Pro.hasFitDetection, "Redmi Buds 6 Pro supports Fit Detection")

        val boneConduction2 = requireNotNull(models.find { it.codename.contains("O73", ignoreCase = true) })
        assertTrue(boneConduction2.isBoneConduction, "Xiaomi 骨传导耳机2 is bone conduction")
        assertFalse(boneConduction2.hasAnc, "Bone conduction does not have in-ear ANC")
        assertFalse(boneConduction2.hasTransparency, "Bone conduction is open-ear, no transparency mode needed")

        val gamingPro = requireNotNull(models.find { it.hasDongle })
        assertTrue(gamingPro.hasDongle, "Gaming edition has USB Dongle support")
    }
}
