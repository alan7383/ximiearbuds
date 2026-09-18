package com.alan.ximiearbuds.core.parity

import com.alan.ximiearbuds.ui.navigation.ScreenDestination
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File

/**
 * 1:1 Parity and Integrity Test Suite for all Strings and Assets.
 * Validates:
 * 1. Zero hardcoded UI strings (all UI strings routed through authentic Xiaomi resource keys).
 * 2. 100% of stringRes(...) keys resolve in both French and English dictionaries.
 * 3. 100% of painterResource(...) drawable paths point to valid, non-empty assets on disk.
 * 4. All ScreenDestination targets use valid title resource keys.
 * 5. Authentic Xiaomi drawables catalog integrity.
 */
class ResourceAndStringParityTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun loadJsonKeys(relativePath: String): Set<String> {
        val file = File(relativePath)
        assertTrue(file.exists(), "Resource file must exist: $relativePath")
        val content = file.readText(Charsets.UTF_8)
        val obj = json.parseToJsonElement(content).jsonObject
        return obj.keys
    }

    @Test
    fun `test all stringRes calls map to valid keys in strings_fr and strings_en`() {
        val frKeys = loadJsonKeys("src/main/resources/strings/strings_fr.json")
        val enKeys = loadJsonKeys("src/main/resources/strings/strings_en.json")

        val stringResRegex = Regex("""stringRes\("([^"]+)"""")
        val missingFr = mutableListOf<String>()
        val missingEn = mutableListOf<String>()
        var totalCalls = 0

        val kotlinDir = File("src/main/kotlin")
        assertTrue(kotlinDir.exists())

        kotlinDir.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            val content = file.readText(Charsets.UTF_8)
            stringResRegex.findAll(content).forEach { match ->
                totalCalls++
                val key = match.groupValues[1]
                if (key !in frKeys) {
                    missingFr.add("${file.name}: $key")
                }
                if (key !in enKeys) {
                    missingEn.add("${file.name}: $key")
                }
            }
        }

        assertTrue(totalCalls > 250, "Expected at least 250 stringRes calls across the app, found $totalCalls")
        assertTrue(
            missingFr.isEmpty(),
            "Found keys called in stringRes missing from strings_fr.json: ${missingFr.joinToString("\n")}"
        )
        assertTrue(
            missingEn.isEmpty(),
            "Found keys called in stringRes missing from strings_en.json: ${missingEn.joinToString("\n")}"
        )
    }

    @Test
    fun `test zero hardcoded French UI strings remain in compose screens and components`() {
        val accentedRegex = Regex(""""([^"]*[éèàêëîïôöûüùçÉÈÀÊËÎÏÔÖÛÜÙÇ][^"]*)"""")
        val uiDir = File("src/main/kotlin/com/alan/ximiearbuds/ui")
        assertTrue(uiDir.exists())

        val violations = mutableListOf<String>()

        uiDir.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            // Strings.kt contains the enum language display names (e.g. "Français") which is expected
            if (file.name == "Strings.kt") return@forEach

            file.readLines(Charsets.UTF_8).forEachIndexed { index, line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("//") || trimmed.startsWith("*")) return@forEachIndexed

                accentedRegex.findAll(line).forEach { match ->
                    violations.add("${file.name}:${index + 1}: ${match.value}")
                }
            }
        }

        assertTrue(
            violations.isEmpty(),
            "Zero hardcoded French UI strings permitted. Found violations:\n${violations.joinToString("\n")}"
        )
    }

    @Test
    fun `test all painterResource drawable references exist on disk`() {
        val painterRegex = Regex("""painterResource\("drawable/([^"]+)"""")
        val drawableDir = File("src/main/resources/drawable")
        assertTrue(drawableDir.exists() && drawableDir.isDirectory)

        val missingAssets = mutableListOf<String>()
        var totalDrawables = 0

        File("src/main/kotlin").walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            val content = file.readText(Charsets.UTF_8)
            painterRegex.findAll(content).forEach { match ->
                totalDrawables++
                val assetName = match.groupValues[1]
                val assetFile = File(drawableDir, assetName)
                if (!assetFile.exists() || assetFile.length() == 0L) {
                    missingAssets.add("${file.name} -> $assetName")
                }
            }
        }

        assertTrue(totalDrawables > 10, "Expected at least 10 painterResource references, found $totalDrawables")
        assertTrue(
            missingAssets.isEmpty(),
            "Found missing or empty drawable assets:\n${missingAssets.joinToString("\n")}"
        )
    }

    @Test
    fun `test official Xiaomi drawable catalog completeness`() {
        val essentialXiaomiAssets = listOf(
            "device_settings_sport_settings.png",
            "device_settings_list_empty.webp",
            "device_settings_dongle_settings_icon.png",
            "device_settings_dongle_settings_icon_disable.png",
            "device_settings_charging.webp",
            "right_arrow_icon.webp",
            "ic_base_right_arrow_icon.webp",
            "ic_base_back.webp"
        )

        val drawableDir = File("src/main/resources/drawable")
        essentialXiaomiAssets.forEach { assetName ->
            val file = File(drawableDir, assetName)
            assertTrue(file.exists(), "Essential Xiaomi asset must exist: $assetName")
            assertTrue(file.length() > 0L, "Asset must not be empty: $assetName")
        }
    }

    @Test
    fun `test all screen destinations have valid title resource keys in both languages`() {
        val frKeys = loadJsonKeys("src/main/resources/strings/strings_fr.json")
        val enKeys = loadJsonKeys("src/main/resources/strings/strings_en.json")

        val destinations = listOf(
            ScreenDestination.MainSettings,
            ScreenDestination.MoreSettings,
            ScreenDestination.CustomizedEq,
            ScreenDestination.GestureControl,
            ScreenDestination.SoundEffects,
            ScreenDestination.FindDevice,
            ScreenDestination.FitDetection,
            ScreenDestination.EarboxSound,
            ScreenDestination.DeviceInfo,
            ScreenDestination.AddDevice,
            ScreenDestination.ScanDevice,
            ScreenDestination.SpatialAudio,
            ScreenDestination.FirmwareUpdate,
            ScreenDestination.DongleSettings,
            ScreenDestination.XiaoAiSettings,
            ScreenDestination.Laboratory,
            ScreenDestination.PersonalSkin,
            ScreenDestination.SportSettings,
            ScreenDestination.VoiceTranslation,
            ScreenDestination.BeginnerGuide,
            ScreenDestination.Profile,
            ScreenDestination.SecurityCode
        )

        destinations.forEach { dest ->
            val key = dest.titleResKey
            assertTrue(
                key in frKeys,
                "Destination ${dest::class.simpleName} titleResKey '$key' missing from strings_fr.json"
            )
            assertTrue(
                key in enKeys,
                "Destination ${dest::class.simpleName} titleResKey '$key' missing from strings_en.json"
            )
        }
    }
}
