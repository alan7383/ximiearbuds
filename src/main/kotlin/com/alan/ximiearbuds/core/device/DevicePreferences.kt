package com.alan.ximiearbuds.core.device

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Clean, production-ready device preferences manager for Xiaomi Earbuds.
 * Persists user device settings (e.g. detected or chosen color variant) in ~/.ximiearbuds/device_settings.json.
 * Does NOT contain any hardcoded MAC addresses or preconfigured user data.
 */
object DevicePreferences {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val jsonParser = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private val baseDir: File = File(System.getProperty("user.home"), ".ximiearbuds").apply {
        if (!exists()) mkdirs()
    }

    private val settingsFile: File = File(baseDir, "device_settings.json")

    // Key: Normalized MAC address or device name -> Value: colorType ID
    private val colorMap = ConcurrentHashMap<String, Int>()
    private var lastUsedDevice: String? = null

    init {
        loadSettings()
    }

    private fun normalizeKey(key: String): String {
        return key.trim().uppercase()
    }

    private fun loadSettings() {
        try {
            if (settingsFile.exists() && settingsFile.length() > 0L) {
                val content = settingsFile.readText()
                val root = jsonParser.parseToJsonElement(content).jsonObject
                lastUsedDevice = root["lastUsedDevice"]?.jsonPrimitive?.contentOrNull
                val colors = root["colors"]?.jsonObject
                colors?.forEach { (mac, elem) ->
                    val c = elem.jsonPrimitive.intOrNull
                    if (c != null && c > 0) {
                        colorMap[normalizeKey(mac)] = c
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore parse errors on corrupted file, start clean
        }
    }

    private fun saveSettingsAsync() {
        scope.launch {
            try {
                val json = buildJsonObject {
                    lastUsedDevice?.let { put("lastUsedDevice", it) }
                    put("colors", buildJsonObject {
                        colorMap.forEach { (mac, color) ->
                            put(mac, color)
                        }
                    })
                }
                settingsFile.writeText(jsonParser.encodeToString(JsonObject.serializer(), json))
            } catch (e: Exception) {
                // Ignore IO errors
            }
        }
    }

    /**
     * Returns the MAC address or identifier of the last used device.
     */
    fun getLastUsedDevice(): String? = lastUsedDevice

    /**
     * Saves the last active device address.
     */
    fun saveLastUsedDevice(address: String) {
        if (address.isBlank() || lastUsedDevice == address) return
        lastUsedDevice = address
        saveSettingsAsync()
    }

    /**
     * Retrieves the saved color for a device by its MAC address or name.
     */
    fun getDeviceColor(deviceIdentifier: String): Int? {
        if (deviceIdentifier.isBlank()) return null
        val key = normalizeKey(deviceIdentifier)
        colorMap[key]?.let { return it }
        // Fallback: match by contains
        for ((k, v) in colorMap) {
            if (key.contains(k) || k.contains(key)) {
                return v
            }
        }
        return null
    }

    /**
     * Saves the detected or selected color for a device.
     */
    fun saveDeviceColor(deviceIdentifier: String, colorType: Int) {
        if (deviceIdentifier.isBlank() || colorType <= 0) return
        val key = normalizeKey(deviceIdentifier)
        if (colorMap[key] != colorType) {
            colorMap[key] = colorType
            saveSettingsAsync()
        }
    }

    /**
     * Clears settings for a device when unpaired/removed.
     */
    fun removeDevice(deviceIdentifier: String) {
        if (deviceIdentifier.isBlank()) return
        val removed = colorMap.remove(normalizeKey(deviceIdentifier))
        if (lastUsedDevice.equals(deviceIdentifier, ignoreCase = true)) {
            lastUsedDevice = null
        }
        if (removed != null || lastUsedDevice == null) {
            saveSettingsAsync()
        }
    }
}
