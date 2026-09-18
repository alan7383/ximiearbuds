package com.alan.ximiearbuds.core.device

import com.alan.ximiearbuds.core.storage.mmkv.MMKV
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
 * Authentic device preferences manager for Xiaomi Earbuds.
 * Backed 1:1 by Tencent MMKV binary storage (~/.ximiearbuds/mmkv/app_pref) as in the official Android app.
 * Maintains transparent backward compatibility with ~/.ximiearbuds/device_settings.json.
 */
object DevicePreferences {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val jsonParser = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private val baseDir: File = File(System.getProperty("user.home"), ".ximiearbuds").apply {
        if (!exists()) mkdirs()
    }

    private val mmkv: MMKV by lazy {
        MMKV.initialize(File(baseDir, "mmkv"))
        MMKV.mmkvWithID("app_pref", MMKV.MULTI_PROCESS_MODE)
    }

    private val settingsFile: File = File(baseDir, "device_settings.json")

    // Key: Normalized MAC address or device name -> Value: colorType ID
    private val colorMap = ConcurrentHashMap<String, Int>()
    private var lastUsedDevice: String? = null
    private var isWelcomeFinished: Boolean = false

    // Profile & Account State (1:1 with official Xiaomi Account Manager)
    private var isLoggedIn: Boolean = false
    private var userId: String? = null
    private var userName: String? = null
    private var avatarAddress: String? = null
    private var region: String = "France"
    private var userExperienceAccepted: Boolean = true
    private var deviceAssociated: Boolean = true

    init {
        loadSettings()
    }

    private fun normalizeKey(key: String): String {
        return key.trim().uppercase()
    }

    private fun loadSettings() {
        try {
            if (mmkv.count() > 0) {
                lastUsedDevice = mmkv.decodeString("lastUsedDevice")
                isWelcomeFinished = mmkv.decodeBool("welcomeFinished", false)
                isLoggedIn = mmkv.decodeBool("isLoggedIn", false)
                userId = mmkv.decodeString("userId")
                userName = mmkv.decodeString("userName")
                avatarAddress = mmkv.decodeString("avatarAddress")
                region = mmkv.decodeString("region", "France") ?: "France"
                userExperienceAccepted = mmkv.decodeBool("userExperienceAccepted", true)
                deviceAssociated = mmkv.decodeBool("deviceAssociated", true)

                val colorEntries = mmkv.decodeStringSet("device_colors")
                colorEntries?.forEach { entry ->
                    val parts = entry.split("=")
                    if (parts.size == 2) {
                        val mac = parts[0]
                        val c = parts[1].toIntOrNull()
                        if (c != null && c > 0) {
                            colorMap[normalizeKey(mac)] = c
                        }
                    }
                }
                return
            }

            // Legacy import from device_settings.json if MMKV is not yet populated
            if (settingsFile.exists() && settingsFile.length() > 0L) {
                val content = settingsFile.readText()
                val root = jsonParser.parseToJsonElement(content).jsonObject
                lastUsedDevice = root["lastUsedDevice"]?.jsonPrimitive?.contentOrNull
                isWelcomeFinished = root["welcomeFinished"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false
                
                // Load account state
                isLoggedIn = root["isLoggedIn"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false
                userId = root["userId"]?.jsonPrimitive?.contentOrNull
                userName = root["userName"]?.jsonPrimitive?.contentOrNull
                avatarAddress = root["avatarAddress"]?.jsonPrimitive?.contentOrNull
                region = root["region"]?.jsonPrimitive?.contentOrNull ?: "France"
                userExperienceAccepted = root["userExperienceAccepted"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: true
                deviceAssociated = root["deviceAssociated"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: true

                val colors = root["colors"]?.jsonObject
                colors?.forEach { (mac, elem) ->
                    val c = elem.jsonPrimitive.intOrNull
                    if (c != null && c > 0) {
                        colorMap[normalizeKey(mac)] = c
                    }
                }
                saveSettingsAsync()
            }
        } catch (e: Exception) {
            // Ignore parse errors on corrupted file, start clean
        }
    }

    private fun saveSettingsAsync() {
        scope.launch {
            try {
                // Save to 1:1 MMKV binary storage
                mmkv.encode("lastUsedDevice", lastUsedDevice)
                mmkv.encode("welcomeFinished", isWelcomeFinished)
                mmkv.encode("isLoggedIn", isLoggedIn)
                mmkv.encode("userId", userId)
                mmkv.encode("userName", userName)
                mmkv.encode("avatarAddress", avatarAddress)
                mmkv.encode("region", region)
                mmkv.encode("userExperienceAccepted", userExperienceAccepted)
                mmkv.encode("deviceAssociated", deviceAssociated)
                val colorSet = colorMap.map { "${it.key}=${it.value}" }.toSet()
                mmkv.encode("device_colors", colorSet)
                mmkv.sync()

                // Mirror to JSON for legacy compatibility
                val json = buildJsonObject {
                    lastUsedDevice?.let { put("lastUsedDevice", it) }
                    put("welcomeFinished", isWelcomeFinished)
                    put("isLoggedIn", isLoggedIn)
                    userId?.let { put("userId", it) }
                    userName?.let { put("userName", it) }
                    avatarAddress?.let { put("avatarAddress", it) }
                    put("region", region)
                    put("userExperienceAccepted", userExperienceAccepted)
                    put("deviceAssociated", deviceAssociated)
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
     * Checks whether the user has completed or skipped the welcome onboarding guide.
     * Matches official ModePreference.isWelcomeFinish() in com.mi.earphone.
     */
    fun isWelcomeFinished(): Boolean = isWelcomeFinished

    /**
     * Updates the welcome onboarding guide status.
     * Matches official ModePreference.setWelcomeFinish(true).
     */
    fun setWelcomeFinished(finished: Boolean) {
        if (isWelcomeFinished != finished) {
            isWelcomeFinished = finished
            saveSettingsAsync()
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

    // ==========================================
    // Account & Profile Management (Xiaomi Account 1:1)
    // ==========================================

    fun isLoggedIn(): Boolean = isLoggedIn
    fun getUserId(): String? = userId
    fun getUserName(): String? = userName
    fun getAvatarAddress(): String? = avatarAddress
    fun getRegion(): String = region
    fun isUserExperienceAccepted(): Boolean = userExperienceAccepted
    fun isDeviceAssociated(): Boolean = deviceAssociated

    fun login(id: String, name: String, avatar: String? = null) {
        isLoggedIn = true
        userId = id
        userName = name
        avatarAddress = avatar
        saveSettingsAsync()
    }

    fun saveAccountInfo(isLoggedIn: Boolean, userId: String, userName: String, avatarAddress: String = "") {
        this.isLoggedIn = isLoggedIn
        this.userId = userId.ifEmpty { null }
        this.userName = userName.ifEmpty { null }
        this.avatarAddress = avatarAddress.ifEmpty { null }
        saveSettingsAsync()
    }

    fun logout() {
        isLoggedIn = false
        userId = null
        userName = null
        avatarAddress = null
        saveSettingsAsync()
    }

    fun setRegion(newRegion: String) {
        if (newRegion.isNotBlank() && region != newRegion) {
            region = newRegion
            saveSettingsAsync()
        }
    }

    fun setUserExperienceAccepted(accepted: Boolean) {
        if (userExperienceAccepted != accepted) {
            userExperienceAccepted = accepted
            saveSettingsAsync()
        }
    }

    fun setDeviceAssociated(associated: Boolean) {
        if (deviceAssociated != associated) {
            deviceAssociated = associated
            saveSettingsAsync()
        }
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
