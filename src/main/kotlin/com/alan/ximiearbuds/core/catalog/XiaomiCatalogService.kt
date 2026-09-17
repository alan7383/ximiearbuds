package com.alan.ximiearbuds.core.catalog

import com.alan.ximiearbuds.core.device.AncCapabilities
import com.alan.ximiearbuds.core.device.GestureCapabilities
import com.alan.ximiearbuds.core.device.SoundCapabilities
import com.alan.ximiearbuds.core.device.MoreSettingsCapabilities
import com.alan.ximiearbuds.core.device.EarbudsModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.io.File
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

/**
 * 1:1 Kotlin implementation of official Xiaomi Earbuds Catalog & Asset Service:
 * - Replicates DeviceService.kt, DeviceRequest.kt, and RegionUrlSwitcherImpl.kt from mobile app.
 * - Endpoints:
 *   - Region detection: https://region.tws.wear.xiaomiwear.com/twsregion/region/user_region_by_ip
 *   - Product list: https://{region}.tws.wear.xiaomiwear.com/twswear/product/get_product_list
 *   - Auth Cookie: auth_key=rwelJuWBFJxmbMKD
 * - Automatically fetches, parses, deduplicates, and caches device definitions and high-res icons.
 */
object XiaomiCatalogService {
    private const val AUTH_COOKIE = "auth_key=rwelJuWBFJxmbMKD"
    private const val APP_VERSION = "1.37.1i"
    private const val REGION_DETECTION_URL = "https://region.tws.wear.xiaomiwear.com/twsregion/region/user_region_by_ip"
    private val REGIONS = listOf("de", "cn", "sg", "ru", "us", "i2")

    private val httpClient: HttpClient by lazy {
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()
    }

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    val cacheDir: File by lazy {
        val home = System.getProperty("user.home") ?: "."
        val dir = File(home, ".ximiearbuds/cache")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    val iconsCacheDir: File by lazy {
        val dir = File(cacheDir, "icons")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    /**
     * Loads the bundled offline catalog from classpath resources.
     */
    fun loadBundledCatalog(): List<EarbudsModel> {
        return try {
            val stream = javaClass.classLoader.getResourceAsStream("devices/catalog.json")
                ?: return emptyList()
            val text = stream.bufferedReader().use { it.readText() }
            parseCatalogJson(text)
        } catch (e: Exception) {
            System.err.println("[XiaomiCatalogService] Failed to load bundled catalog: ${e.message}")
            emptyList()
        }
    }

    /**
     * Loads the cached catalog from user app directory if it exists and is newer.
     */
    fun loadCachedCatalog(): List<EarbudsModel>? {
        val file = File(cacheDir, "catalog_cache.json")
        if (!file.exists() || file.length() == 0L) return null
        return try {
            val list = parseCatalogJson(file.readText())
            // If cached catalog was saved with an older schema missing color variants, invalidate it
            if (list.isNotEmpty() && list.all { it.colorVariants.isEmpty() }) {
                file.delete()
                null
            } else {
                list
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Detects user region via official Xiaomi endpoint, e.g. "de", "cn", "sg".
     */
    suspend fun detectRegion(): String = withContext(Dispatchers.IO) {
        try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create(REGION_DETECTION_URL))
                .header("Cookie", AUTH_COOKIE)
                .timeout(Duration.ofSeconds(6))
                .GET()
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200) {
                val json = jsonParser.parseToJsonElement(response.body()).jsonObject
                val result = json["result"]?.jsonObject
                val region = result?.get("region")?.jsonPrimitive?.contentOrNull
                if (!region.isNullOrBlank()) {
                    return@withContext region.lowercase()
                }
            }
        } catch (e: Exception) {
            // ignore network failure, fallback to default region
        }
        "de"
    }

    /**
     * Fetches products from a single regional server.
     */
    suspend fun fetchRegionalProductList(region: String): List<JsonObject> = withContext(Dispatchers.IO) {
        val url = "https://$region.tws.wear.xiaomiwear.com/twswear/product/get_product_list"
        val payload = """{"app_version":"$APP_VERSION","app_platform":0,"last_modify_time":0}"""
        val formBody = "data=" + URLEncoder.encode(payload, StandardCharsets.UTF_8)

        try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Cookie", AUTH_COOKIE)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(12))
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200) {
                val json = jsonParser.parseToJsonElement(response.body()).jsonObject
                val result = json["result"]?.jsonObject ?: return@withContext emptyList()
                val list = result["tw_product_list"]?.jsonArray ?: return@withContext emptyList()
                return@withContext list.mapNotNull { it as? JsonObject }
            }
        } catch (e: Exception) {
            System.err.println("[XiaomiCatalogService] Fetch failed for region $region: ${e.message}")
        }
        emptyList()
    }

    /**
     * Performs a full sync across all regions (or user region + fallback),
     * merges all models, deduplicates by VID and PID, updates disk cache,
     * and downloads missing icons.
     */
    suspend fun syncAllRegions(forceAll: Boolean = true): List<EarbudsModel> = withContext(Dispatchers.IO) {
        val mergedRaw = LinkedHashMap<String, JsonObject>()

        val regionsToQuery = if (forceAll) REGIONS else listOf(detectRegion(), "cn", "de")

        for (region in regionsToQuery) {
            val products = fetchRegionalProductList(region)
            for (p in products) {
                val vid = p["vid"]?.jsonPrimitive?.contentOrNull ?: ""
                val pid = p["pid"]?.jsonPrimitive?.contentOrNull ?: ""
                val key = "${vid}_${pid}"
                if (key.isNotBlank() && !mergedRaw.containsKey(key)) {
                    mergedRaw[key] = p
                }
            }
        }

        if (mergedRaw.isEmpty()) {
            return@withContext loadCachedCatalog() ?: loadBundledCatalog()
        }

        val parsedModels = ArrayList<EarbudsModel>()
        for ((key, p) in mergedRaw) {
            val model = convertJsonObjectToEarbudsModel(key, p)
            if (model != null) {
                parsedModels.add(model)
            }
        }

        // Merge in legacy models if not already present
        val bundled = loadBundledCatalog()
        for (b in bundled) {
            if (parsedModels.none { it.vendorId == b.vendorId && it.productIds == b.productIds }) {
                parsedModels.add(b)
            }
        }

        parsedModels.sortBy { it.commercialName.lowercase() }

        // Save to cache
        try {
            val cacheFile = File(cacheDir, "catalog_cache.json")
            cacheFile.writeText(serializeCatalog(parsedModels))
        } catch (e: Exception) {
            System.err.println("[XiaomiCatalogService] Failed to cache catalog: ${e.message}")
        }

        // Download missing icons in background
        downloadMissingIcons(parsedModels)

        parsedModels
    }

    /**
     * Downloads any missing icons from Xiaomi CDN to the local user cache directory.
     */
    suspend fun downloadMissingIcons(models: List<EarbudsModel>) = withContext(Dispatchers.IO) {
        for (m in models) {
            if (m.iconUrl.isBlank() || m.iconFile.isBlank()) continue
            val targetFile = File(iconsCacheDir, m.iconFile)
            if (targetFile.exists() && targetFile.length() > 0L) continue

            try {
                // Encode the path segment because Xiaomi CDN URLs might contain unescaped Chinese or spaces
                val cleanUri = encodeUrlPath(m.iconUrl)
                val request = HttpRequest.newBuilder()
                    .uri(URI.create(cleanUri))
                    .header("User-Agent", "Mozilla/5.0")
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build()

                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray())
                if (response.statusCode() == 200 && response.body().isNotEmpty()) {
                    targetFile.writeBytes(response.body())
                }
            } catch (e: Exception) {
                // Skip individual failed image download
            }
        }
    }

    /**
     * Resolves an icon for the given model:
     * 1. Check local cache directory (~/.ximiearbuds/cache/icons/{iconFile})
     * 2. Check bundled classpath resource (/devices/icons/{iconFile})
     * 3. Fallback to null (caller uses default vector/drawable)
     */
    fun getLocalIconFile(model: EarbudsModel): File? {
        if (model.iconFile.isBlank()) return null
        val cached = File(iconsCacheDir, model.iconFile)
        if (cached.exists() && cached.length() > 0L) return cached
        return null
    }

    /**
     * Fixes and properly encodes URL path segments that may contain spaces or UTF-8 characters.
     */
    fun encodeUrlPath(rawUrl: String): String {
        return try {
            val schemeEnd = rawUrl.indexOf("://")
            if (schemeEnd == -1) return rawUrl
            val pathStart = rawUrl.indexOf('/', schemeEnd + 3)
            if (pathStart == -1) return rawUrl
            val hostPart = rawUrl.substring(0, pathStart)
            val pathAndQuery = rawUrl.substring(pathStart)
            val queryStart = pathAndQuery.indexOf('?')
            val path = if (queryStart == -1) pathAndQuery else pathAndQuery.substring(0, queryStart)
            val query = if (queryStart == -1) "" else pathAndQuery.substring(queryStart)

            val encodedPath = path.split("/").joinToString("/") { part ->
                URLEncoder.encode(part, StandardCharsets.UTF_8).replace("+", "%20")
            }
            "$hostPart$encodedPath$query"
        } catch (e: Exception) {
            rawUrl
        }
    }

    private fun convertJsonObjectToEarbudsModel(key: String, p: JsonObject): EarbudsModel? {
        val name = p["name"]?.jsonPrimitive?.contentOrNull?.trim() ?: return null
        val model = p["model"]?.jsonPrimitive?.contentOrNull?.trim() ?: ""
        val pattern = p["pattern"]?.jsonPrimitive?.contentOrNull?.trim() ?: ""
        val vidStr = p["vid"]?.jsonPrimitive?.contentOrNull ?: "2717"
        val pidStr = p["pid"]?.jsonPrimitive?.contentOrNull ?: "0"
        val extra = p["extra_info"]?.jsonObject

        val vid = try {
            if (vidStr.startsWith("0x", true)) Integer.parseInt(vidStr.substring(2), 16)
            else Integer.parseInt(vidStr, 16)
        } catch (e: Exception) {
            10007
        }

        val pid = try {
            if (pidStr.startsWith("0x", true)) Integer.parseInt(pidStr.substring(2), 16)
            else Integer.parseInt(pidStr, 16)
        } catch (e: Exception) {
            0
        }

        val saleModel = extra?.get("sale_model")?.jsonPrimitive?.contentOrNull ?: ""
        val defaultColor = extra?.get("default_color")?.jsonPrimitive?.intOrNull ?: 1

        val funcList = p["func_list"]?.jsonArray ?: JsonArray(emptyList())
        val funcIds = HashSet<Int>()
        val groupIds = HashSet<Int>()
        val extraDataMap = HashMap<Int, String>()

        for (f in funcList) {
            val fObj = f as? JsonObject ?: continue
            val fid = fObj["func_id"]?.jsonPrimitive?.intOrNull ?: continue
            val gid = fObj["group_id"]?.jsonPrimitive?.intOrNull ?: 0
            val extraEl = fObj["extra_data"]
            val extraStr = when (extraEl) {
                is JsonObject -> extraEl.toString()
                is JsonPrimitive -> extraEl.content
                else -> ""
            }
            funcIds.add(fid)
            if (gid > 0) groupIds.add(gid)
            extraDataMap[fid] = extraStr
        }

        for (fid in funcIds) {
            when (fid) {
                in 1001..1009 -> groupIds.add(1)
                in 2001..2021 -> groupIds.add(2)
                in 3001..3015 -> groupIds.add(3)
                in 4001..4018 -> groupIds.add(4)
                in 5001..5009 -> groupIds.add(5)
                in 6001..6002 -> groupIds.add(6)
                in 7001..7005 -> groupIds.add(7)
                8001 -> groupIds.add(8)
            }
        }

        val hasAnc = funcIds.any { it in listOf(1001, 1002, 1003, 1006, 1007, 1008, 1009) }
        val hasTransparency = funcIds.any { it in listOf(1004, 1005) }
        val hasEq = funcIds.any { it in listOf(2006, 2008, 2009, 2016) }
        val hasGestures = funcIds.any { it in 4001..4018 }
        val hasSlideGesture = 4011 in funcIds
        val hasLowLatency = (3011 in funcIds) || (3009 in funcIds) || (3008 in funcIds)
        val hasMultipoint = 3004 in funcIds
        val hasInEar = 3002 in funcIds
        val hasSpatial = funcIds.any { it in listOf(2001, 2002, 2004, 2005, 2018) }
        val hasFitDetection = 3003 in funcIds
        val hasEarboxSound = 3015 in funcIds
        val hasDongle = (7001 in funcIds) || name.contains("电竞") || name.contains("Gaming", true) || model.contains("gcn", true)
        val isBone = (extra?.get("headphone_type")?.jsonPrimitive?.intOrNull == 1) || (8001 in funcIds) ||
                name.contains("骨传导") || name.contains("Bone", true) || name.contains("OpenWear", true)

        val brand = when {
            name.contains("redmi", ignoreCase = true) -> "Redmi"
            name.contains("poco", ignoreCase = true) -> "POCO"
            else -> "Xiaomi"
        }

        val rawCodename = if (pattern.isNotBlank()) pattern else model.removePrefix("miwear.headphone.")
        val codename = resolveCanonicalCodename(vid, pid, rawCodename)

        val icons = p["icon_static"]?.jsonArray ?: JsonArray(emptyList())
        var primaryIconUrl = ""
        val colorVariants = HashMap<String, String>()
        for (ic in icons) {
            val icObj = ic as? JsonObject ?: continue
            val col = icObj["color"]?.jsonPrimitive?.intOrNull?.toString() ?: continue
            val u = icObj["icon_url"]?.jsonPrimitive?.contentOrNull ?: continue
            colorVariants[col] = u
            if (icObj["color"]?.jsonPrimitive?.intOrNull == defaultColor) {
                primaryIconUrl = u
            }
        }
        if (primaryIconUrl.isBlank() && icons.isNotEmpty()) {
            val first = icons[0] as? JsonObject
            primaryIconUrl = first?.get("icon_url")?.jsonPrimitive?.contentOrNull ?: ""
        }

        val hasSmartDenoise = 1008 in funcIds
        val hasPersonalizedAnc = 3012 in funcIds
        val isSingleToggle = isBone || !hasTransparency
        var ancLevels = listOf(1, 0, 2)
        var transLevels = if (hasTransparency) listOf(0, 1, 2) else emptyList()
        var hasAdaptiveAnc = false

        for (fid in listOf(1002, 1003, 1006, 1007)) {
            val exStr = extraDataMap[fid]
            if (!exStr.isNullOrBlank()) {
                try {
                    val exObj = jsonParser.parseToJsonElement(exStr).jsonObject
                    exObj["tws_gear"]?.jsonArray?.mapNotNull { it.jsonPrimitive.intOrNull }?.let {
                        if (it.isNotEmpty()) ancLevels = it
                    }
                    if (exObj["auto_fit"]?.jsonPrimitive?.intOrNull == 1) {
                        hasAdaptiveAnc = true
                    }
                } catch (_: Exception) {}
            }
        }
        if (!hasAdaptiveAnc && hasAnc && codename in listOf("O76", "N76", "P76", "M75", "N75", "L71", "L76", "L77", "K73")) {
            hasAdaptiveAnc = true
        }

        for (fid in listOf(1004, 1005)) {
            val exStr = extraDataMap[fid]
            if (!exStr.isNullOrBlank()) {
                try {
                    val exObj = jsonParser.parseToJsonElement(exStr).jsonObject
                    exObj["tws_gear"]?.jsonArray?.mapNotNull { it.jsonPrimitive.intOrNull }?.let {
                        if (it.isNotEmpty()) transLevels = it
                    }
                } catch (_: Exception) {}
            }
        }

        val ancCaps = AncCapabilities(
            hasAdaptiveAnc = hasAdaptiveAnc,
            hasPersonalizedAnc = hasPersonalizedAnc,
            hasSmartDenoise = hasSmartDenoise,
            ancLevels = ancLevels,
            transparencyLevels = transLevels,
            isSingleToggleOnly = isSingleToggle
        )

        // Parse gestures
        val allowedActions = HashMap<Int, List<Int>>()
        var noiseControlActions = listOf(1, 2, 3)
        var hasSecondary = false
        for (fid in 4001..4018) {
            val exStr = extraDataMap[fid]
            if (!exStr.isNullOrBlank()) {
                try {
                    val exObj = jsonParser.parseToJsonElement(exStr).jsonObject
                    exObj["click_action"]?.jsonArray?.mapNotNull { it.jsonPrimitive.intOrNull }?.let {
                        allowedActions[fid] = it
                    }
                    exObj["noise_ctrl"]?.jsonArray?.mapNotNull { it.jsonPrimitive.intOrNull }?.let {
                        noiseControlActions = it
                    }
                    exObj["hasSecondaryPage"]?.jsonPrimitive?.booleanOrNull?.let {
                        hasSecondary = it
                    }
                } catch (_: Exception) {}
            }
        }
        val isPinch = funcIds.any { it in listOf(4004, 4005, 4006, 4012, 4013, 4014) }
        val isMfb = 4018 in funcIds || isBone
        val gestureCaps = GestureCapabilities(
            allowedActions = allowedActions,
            noiseControlActions = noiseControlActions,
            hasSecondaryPage = hasSecondary,
            isPinchGesture = isPinch,
            isSlideGesture = hasSlideGesture,
            isMfbGesture = isMfb
        )

        // Parse Sound
        var supportedPresets = emptyList<Int>()
        for (fid in listOf(2006, 2016)) {
            val exStr = extraDataMap[fid]
            if (!exStr.isNullOrBlank()) {
                try {
                    val exObj = jsonParser.parseToJsonElement(exStr).jsonObject
                    exObj["sound_effect"]?.jsonObject?.get("effects")?.jsonArray?.mapNotNull { it.jsonPrimitive.intOrNull }?.let {
                        if (it.isNotEmpty()) supportedPresets = it
                    }
                    if (supportedPresets.isEmpty()) {
                        exObj["sound_list"]?.jsonArray?.mapNotNull { it.jsonPrimitive.intOrNull }?.let {
                            if (it.isNotEmpty()) supportedPresets = it
                        }
                    }
                } catch (_: Exception) {}
            }
        }
        var spatialScenes = emptyList<Int>()
        extraDataMap[2009]?.let { exStr ->
            if (exStr.isNotBlank()) {
                try {
                    val exObj = jsonParser.parseToJsonElement(exStr).jsonObject
                    exObj["sound_render"]?.jsonObject?.get("scenes")?.jsonArray?.mapNotNull { it.jsonPrimitive.intOrNull }?.let {
                        spatialScenes = it
                    }
                } catch (_: Exception) {}
            }
        }
        val soundCaps = SoundCapabilities(
            supportedPresets = supportedPresets,
            hasVirtualSurround = 2001 in funcIds,
            hasAdaptiveSense = 2008 in funcIds,
            hasAudibilityAdaptation = 2019 in funcIds,
            hasAdaptiveVolume = 2017 in funcIds,
            hasNotificationVolume = 2021 in funcIds,
            hasSpatialAudio = hasSpatial,
            hasHeadTracking = 2004 in funcIds,
            spatialScenes = spatialScenes
        )

        val moreSettingsCaps = MoreSettingsCapabilities(
            hasWearDetection = hasInEar,
            hasMultipoint = hasMultipoint,
            hasLowLatency = hasLowLatency,
            hasAutoPickCall = 3008 in funcIds,
            hasFitDetection = hasFitDetection,
            hasEarCanalDetection = 3013 in funcIds,
            hasEarboxSound = hasEarboxSound,
            hasVoiceControl = 3005 in funcIds,
            hasCustomSkin = 3010 in funcIds,
            hasDongle = hasDongle,
            hasSport = isBone || (8 in groupIds) || (8001 in funcIds),
            hasFindDevice = 5001 in funcIds || true
        )

        return EarbudsModel(
            codename = codename,
            commercialName = name,
            vendorId = vid,
            productIds = listOf(pid),
            brand = brand,
            modelCode = model,
            saleModel = saleModel,
            iconFile = "$key.webp",
            iconUrl = primaryIconUrl,
            colorVariants = colorVariants,
            defaultColor = defaultColor,
            supportedFunctionIds = funcIds,
            functionGroups = groupIds,
            functionExtraData = extraDataMap,
            ancCapabilities = ancCaps,
            gestureCapabilities = gestureCaps,
            soundCapabilities = soundCaps,
            moreSettingsCapabilities = moreSettingsCaps,
            hasAnc = hasAnc,
            hasTransparency = hasTransparency,
            has10BandEq = hasEq,
            hasGestures = hasGestures,
            hasSlideGesture = hasSlideGesture,
            hasLowLatency = hasLowLatency,
            hasMultipoint = hasMultipoint,
            hasInEarDetection = hasInEar,
            hasSpatialAudio = hasSpatial,
            hasFitDetection = hasFitDetection,
            hasEarboxSound = hasEarboxSound,
            hasFindDevice = true,
            hasDongle = hasDongle,
            isBoneConduction = isBone
        )
    }

    private fun parseCatalogJson(jsonText: String): List<EarbudsModel> {
        val array = jsonParser.parseToJsonElement(jsonText).jsonArray
        val list = ArrayList<EarbudsModel>(array.size)

        for (el in array) {
            val obj = el as? JsonObject ?: continue
            val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: continue
            val codename = obj["codename"]?.jsonPrimitive?.contentOrNull ?: ""
            val brand = obj["brand"]?.jsonPrimitive?.contentOrNull ?: "Xiaomi"
            val model = obj["model"]?.jsonPrimitive?.contentOrNull ?: ""
            val saleModel = obj["saleModel"]?.jsonPrimitive?.contentOrNull ?: ""
            val vid = obj["vendorId"]?.jsonPrimitive?.intOrNull ?: 10007
            val pids = obj["productIds"]?.jsonArray?.mapNotNull { it.jsonPrimitive.intOrNull } ?: emptyList()
            val iconFile = obj["iconFile"]?.jsonPrimitive?.contentOrNull ?: ""
            val iconUrl = obj["iconUrl"]?.jsonPrimitive?.contentOrNull ?: ""
            val colorVariants = obj["colorVariants"]?.jsonObject?.mapNotNull { (k, v) ->
                val u = v.jsonPrimitive.contentOrNull
                if (u != null) k to u else null
            }?.toMap() ?: emptyMap()
            val defaultColor = obj["defaultColor"]?.jsonPrimitive?.intOrNull ?: 1
            val hasAnc = obj["hasAnc"]?.jsonPrimitive?.booleanOrNull ?: true
            val hasTransparency = obj["hasTransparency"]?.jsonPrimitive?.booleanOrNull ?: true
            val isBone = obj["isBoneConduction"]?.jsonPrimitive?.booleanOrNull ?: false

            // Parse func_list
            val funcList = obj["func_list"]?.jsonArray ?: JsonArray(emptyList())
            val funcIds = HashSet<Int>()
            val groupIds = HashSet<Int>()
            val extraDataMap = HashMap<Int, String>()

            for (f in funcList) {
                val fObj = f as? JsonObject ?: continue
                val fid = fObj["func_id"]?.jsonPrimitive?.intOrNull ?: continue
                val gid = fObj["group_id"]?.jsonPrimitive?.intOrNull ?: 0
                val ex = fObj["extra_data"]
                val exStr = when (ex) {
                    is JsonObject -> ex.toString()
                    is JsonPrimitive -> ex.content
                    else -> ""
                }
                funcIds.add(fid)
                if (gid > 0) groupIds.add(gid)
                extraDataMap[fid] = exStr
            }

            for (fid in funcIds) {
                when (fid) {
                    in 1001..1009 -> groupIds.add(1)
                    in 2001..2021 -> groupIds.add(2)
                    in 3001..3015 -> groupIds.add(3)
                    in 4001..4018 -> groupIds.add(4)
                    in 5001..5009 -> groupIds.add(5)
                    in 6001..6002 -> groupIds.add(6)
                    in 7001..7005 -> groupIds.add(7)
                    8001 -> groupIds.add(8)
                }
            }

            // Fallback groups if func_list was empty
            if (groupIds.isEmpty()) {
                if (hasAnc || hasTransparency) groupIds.add(1)
                groupIds.add(2)
                groupIds.add(3)
                if (obj["hasGestures"]?.jsonPrimitive?.booleanOrNull != false) groupIds.add(4)
                groupIds.add(5)
            }

            // ANC Capabilities
            val ancCapsObj = obj["ancCapabilities"]?.jsonObject
            val ancCaps = if (ancCapsObj != null) {
                AncCapabilities(
                    hasAdaptiveAnc = ancCapsObj["hasAdaptiveAnc"]?.jsonPrimitive?.booleanOrNull ?: false,
                    hasPersonalizedAnc = ancCapsObj["hasPersonalizedAnc"]?.jsonPrimitive?.booleanOrNull ?: false,
                    hasSmartDenoise = ancCapsObj["hasSmartDenoise"]?.jsonPrimitive?.booleanOrNull ?: false,
                    ancLevels = ancCapsObj["ancLevels"]?.jsonArray?.mapNotNull { it.jsonPrimitive.intOrNull } ?: listOf(1, 0, 2),
                    transparencyLevels = ancCapsObj["transparencyLevels"]?.jsonArray?.mapNotNull { it.jsonPrimitive.intOrNull } ?: listOf(0, 1, 2),
                    isSingleToggleOnly = ancCapsObj["isSingleToggleOnly"]?.jsonPrimitive?.booleanOrNull ?: false
                )
            } else {
                AncCapabilities(
                    hasAdaptiveAnc = hasAnc && codename in listOf("O76", "N76", "P76", "M75", "N75", "L71", "L76", "L77", "K73"),
                    hasPersonalizedAnc = hasAnc && codename in listOf("O76", "N76", "P76", "M75"),
                    hasSmartDenoise = hasAnc && codename in listOf("P76", "P79"),
                    ancLevels = if (codename in listOf("K75", "K73")) listOf(1, 0, 2, 4) else listOf(1, 0, 2),
                    transparencyLevels = if (!hasTransparency) emptyList() else if (codename in listOf("K75", "K73")) listOf(0, 1) else listOf(0, 1, 2),
                    isSingleToggleOnly = isBone || !hasTransparency
                )
            }

            // Gesture Capabilities
            val gestureCapsObj = obj["gestureCapabilities"]?.jsonObject
            val gestureCaps = if (gestureCapsObj != null) {
                val allowed = HashMap<Int, List<Int>>()
                gestureCapsObj["allowedActions"]?.jsonObject?.forEach { (k, v) ->
                    k.toIntOrNull()?.let { fid ->
                        allowed[fid] = v.jsonArray.mapNotNull { it.jsonPrimitive.intOrNull }
                    }
                }
                GestureCapabilities(
                    allowedActions = allowed,
                    noiseControlActions = gestureCapsObj["noiseControlActions"]?.jsonArray?.mapNotNull { it.jsonPrimitive.intOrNull } ?: listOf(1, 2, 3),
                    hasSecondaryPage = gestureCapsObj["hasSecondaryPage"]?.jsonPrimitive?.booleanOrNull ?: false,
                    isPinchGesture = gestureCapsObj["isPinchGesture"]?.jsonPrimitive?.booleanOrNull ?: false,
                    isSlideGesture = gestureCapsObj["isSlideGesture"]?.jsonPrimitive?.booleanOrNull ?: (obj["hasSlideGesture"]?.jsonPrimitive?.booleanOrNull ?: false),
                    isMfbGesture = gestureCapsObj["isMfbGesture"]?.jsonPrimitive?.booleanOrNull ?: isBone
                )
            } else {
                GestureCapabilities(
                    isSlideGesture = obj["hasSlideGesture"]?.jsonPrimitive?.booleanOrNull ?: false,
                    isMfbGesture = isBone
                )
            }

            // Sound Capabilities
            val soundCapsObj = obj["soundCapabilities"]?.jsonObject
            val soundCaps = if (soundCapsObj != null) {
                SoundCapabilities(
                    supportedPresets = soundCapsObj["supportedPresets"]?.jsonArray?.mapNotNull { it.jsonPrimitive.intOrNull } ?: emptyList(),
                    hasVirtualSurround = soundCapsObj["hasVirtualSurround"]?.jsonPrimitive?.booleanOrNull ?: false,
                    hasAdaptiveSense = soundCapsObj["hasAdaptiveSense"]?.jsonPrimitive?.booleanOrNull ?: false,
                    hasAudibilityAdaptation = soundCapsObj["hasAudibilityAdaptation"]?.jsonPrimitive?.booleanOrNull ?: false,
                    hasAdaptiveVolume = soundCapsObj["hasAdaptiveVolume"]?.jsonPrimitive?.booleanOrNull ?: false,
                    hasNotificationVolume = soundCapsObj["hasNotificationVolume"]?.jsonPrimitive?.booleanOrNull ?: false,
                    hasSpatialAudio = soundCapsObj["hasSpatialAudio"]?.jsonPrimitive?.booleanOrNull ?: (obj["hasSpatialAudio"]?.jsonPrimitive?.booleanOrNull ?: false),
                    hasHeadTracking = soundCapsObj["hasHeadTracking"]?.jsonPrimitive?.booleanOrNull ?: false,
                    spatialScenes = soundCapsObj["spatialScenes"]?.jsonArray?.mapNotNull { it.jsonPrimitive.intOrNull } ?: emptyList()
                )
            } else {
                SoundCapabilities(
                    hasSpatialAudio = obj["hasSpatialAudio"]?.jsonPrimitive?.booleanOrNull ?: false
                )
            }

            // More Settings Capabilities
            val moreCapsObj = obj["moreSettingsCapabilities"]?.jsonObject
            val moreCaps = if (moreCapsObj != null) {
                MoreSettingsCapabilities(
                    hasWearDetection = moreCapsObj["hasWearDetection"]?.jsonPrimitive?.booleanOrNull ?: (obj["hasInEarDetection"]?.jsonPrimitive?.booleanOrNull ?: true),
                    hasMultipoint = moreCapsObj["hasMultipoint"]?.jsonPrimitive?.booleanOrNull ?: (obj["hasMultipoint"]?.jsonPrimitive?.booleanOrNull ?: true),
                    hasLowLatency = moreCapsObj["hasLowLatency"]?.jsonPrimitive?.booleanOrNull ?: (obj["hasLowLatency"]?.jsonPrimitive?.booleanOrNull ?: true),
                    hasAutoPickCall = moreCapsObj["hasAutoPickCall"]?.jsonPrimitive?.booleanOrNull ?: false,
                    hasFitDetection = moreCapsObj["hasFitDetection"]?.jsonPrimitive?.booleanOrNull ?: (obj["hasFitDetection"]?.jsonPrimitive?.booleanOrNull ?: false),
                    hasEarCanalDetection = moreCapsObj["hasEarCanalDetection"]?.jsonPrimitive?.booleanOrNull ?: false,
                    hasEarboxSound = moreCapsObj["hasEarboxSound"]?.jsonPrimitive?.booleanOrNull ?: (obj["hasEarboxSound"]?.jsonPrimitive?.booleanOrNull ?: false),
                    hasVoiceControl = moreCapsObj["hasVoiceControl"]?.jsonPrimitive?.booleanOrNull ?: false,
                    hasCustomSkin = moreCapsObj["hasCustomSkin"]?.jsonPrimitive?.booleanOrNull ?: false,
                    hasDongle = moreCapsObj["hasDongle"]?.jsonPrimitive?.booleanOrNull ?: (obj["hasDongle"]?.jsonPrimitive?.booleanOrNull ?: false),
                    hasSport = moreCapsObj["hasSport"]?.jsonPrimitive?.booleanOrNull ?: isBone,
                    hasFindDevice = moreCapsObj["hasFindDevice"]?.jsonPrimitive?.booleanOrNull ?: true
                )
            } else {
                MoreSettingsCapabilities(
                    hasWearDetection = obj["hasInEarDetection"]?.jsonPrimitive?.booleanOrNull ?: true,
                    hasMultipoint = obj["hasMultipoint"]?.jsonPrimitive?.booleanOrNull ?: true,
                    hasLowLatency = obj["hasLowLatency"]?.jsonPrimitive?.booleanOrNull ?: true,
                    hasFitDetection = obj["hasFitDetection"]?.jsonPrimitive?.booleanOrNull ?: false,
                    hasEarboxSound = obj["hasEarboxSound"]?.jsonPrimitive?.booleanOrNull ?: false,
                    hasDongle = obj["hasDongle"]?.jsonPrimitive?.booleanOrNull ?: false,
                    hasSport = isBone,
                    hasFindDevice = true
                )
            }

            list.add(
                EarbudsModel(
                    codename = codename,
                    commercialName = name,
                    vendorId = vid,
                    productIds = pids,
                    brand = brand,
                    modelCode = model,
                    saleModel = saleModel,
                    iconFile = iconFile,
                    iconUrl = iconUrl,
                    colorVariants = colorVariants,
                    defaultColor = defaultColor,
                    supportedFunctionIds = funcIds,
                    functionGroups = groupIds,
                    functionExtraData = extraDataMap,
                    ancCapabilities = ancCaps,
                    gestureCapabilities = gestureCaps,
                    soundCapabilities = soundCaps,
                    moreSettingsCapabilities = moreCaps,
                    hasAnc = hasAnc,
                    hasTransparency = hasTransparency,
                    has10BandEq = obj["has10BandEq"]?.jsonPrimitive?.booleanOrNull ?: true,
                    hasGestures = obj["hasGestures"]?.jsonPrimitive?.booleanOrNull ?: true,
                    hasSlideGesture = obj["hasSlideGesture"]?.jsonPrimitive?.booleanOrNull ?: false,
                    hasLowLatency = obj["hasLowLatency"]?.jsonPrimitive?.booleanOrNull ?: true,
                    hasMultipoint = obj["hasMultipoint"]?.jsonPrimitive?.booleanOrNull ?: true,
                    hasInEarDetection = obj["hasInEarDetection"]?.jsonPrimitive?.booleanOrNull ?: true,
                    hasSpatialAudio = obj["hasSpatialAudio"]?.jsonPrimitive?.booleanOrNull ?: false,
                    hasFitDetection = obj["hasFitDetection"]?.jsonPrimitive?.booleanOrNull ?: false,
                    hasEarboxSound = obj["hasEarboxSound"]?.jsonPrimitive?.booleanOrNull ?: false,
                    hasFindDevice = obj["hasFindDevice"]?.jsonPrimitive?.booleanOrNull ?: true,
                    hasDongle = obj["hasDongle"]?.jsonPrimitive?.booleanOrNull ?: false,
                    isBoneConduction = isBone
                )
            )
        }

        return list
    }

    private fun serializeCatalog(models: List<EarbudsModel>): String {
        val array = buildJsonArray {
            for (m in models) {
                add(buildJsonObject {
                    put("name", m.commercialName)
                    put("codename", m.codename)
                    put("brand", m.brand)
                    put("model", m.modelCode)
                    put("saleModel", m.saleModel)
                    put("vendorId", m.vendorId)
                    put("productIds", buildJsonArray { m.productIds.forEach { add(it) } })
                    put("iconFile", m.iconFile)
                    put("iconUrl", m.iconUrl)
                    put("colorVariants", buildJsonObject {
                        m.colorVariants.forEach { (k, v) -> put(k, v) }
                    })
                    put("defaultColor", m.defaultColor)
                    put("func_list", buildJsonArray {
                        for (fid in m.supportedFunctionIds) {
                            add(buildJsonObject {
                                put("func_id", fid)
                                val gid = when (fid) {
                                    in 1001..1009 -> 1
                                    in 2001..2021 -> 2
                                    in 3001..3015 -> 3
                                    in 4001..4018 -> 4
                                    in 5001..5009 -> 5
                                    in 6001..6002 -> 6
                                    in 7001..7005 -> 7
                                    8001 -> 8
                                    else -> 0
                                }
                                put("group_id", gid)
                                put("extra_data", m.getExtraData(fid) ?: "")
                            })
                        }
                    })
                    put("hasAnc", m.hasAnc)
                    put("hasTransparency", m.hasTransparency)
                    put("has10BandEq", m.has10BandEq)
                    put("hasGestures", m.hasGestures)
                    put("hasSlideGesture", m.hasSlideGesture)
                    put("hasLowLatency", m.hasLowLatency)
                    put("hasMultipoint", m.hasMultipoint)
                    put("hasInEarDetection", m.hasInEarDetection)
                    put("hasSpatialAudio", m.hasSpatialAudio)
                    put("hasFitDetection", m.hasFitDetection)
                    put("hasEarboxSound", m.hasEarboxSound)
                    put("hasFindDevice", m.hasFindDevice)
                    put("hasDongle", m.hasDongle)
                    put("isBoneConduction", m.isBoneConduction)
                    put("ancCapabilities", buildJsonObject {
                        put("hasAdaptiveAnc", m.ancCapabilities.hasAdaptiveAnc)
                        put("hasPersonalizedAnc", m.ancCapabilities.hasPersonalizedAnc)
                        put("hasSmartDenoise", m.ancCapabilities.hasSmartDenoise)
                        put("ancLevels", buildJsonArray { m.ancCapabilities.ancLevels.forEach { add(it) } })
                        put("transparencyLevels", buildJsonArray { m.ancCapabilities.transparencyLevels.forEach { add(it) } })
                        put("isSingleToggleOnly", m.ancCapabilities.isSingleToggleOnly)
                    })
                    put("gestureCapabilities", buildJsonObject {
                        put("allowedActions", buildJsonObject {
                            m.gestureCapabilities.allowedActions.forEach { (fid, acts) ->
                                put(fid.toString(), buildJsonArray { acts.forEach { add(it) } })
                            }
                        })
                        put("noiseControlActions", buildJsonArray { m.gestureCapabilities.noiseControlActions.forEach { add(it) } })
                        put("hasSecondaryPage", m.gestureCapabilities.hasSecondaryPage)
                        put("isPinchGesture", m.gestureCapabilities.isPinchGesture)
                        put("isSlideGesture", m.gestureCapabilities.isSlideGesture)
                        put("isMfbGesture", m.gestureCapabilities.isMfbGesture)
                    })
                    put("soundCapabilities", buildJsonObject {
                        put("supportedPresets", buildJsonArray { m.soundCapabilities.supportedPresets.forEach { add(it) } })
                        put("hasVirtualSurround", m.soundCapabilities.hasVirtualSurround)
                        put("hasAdaptiveSense", m.soundCapabilities.hasAdaptiveSense)
                        put("hasAudibilityAdaptation", m.soundCapabilities.hasAudibilityAdaptation)
                        put("hasAdaptiveVolume", m.soundCapabilities.hasAdaptiveVolume)
                        put("hasNotificationVolume", m.soundCapabilities.hasNotificationVolume)
                        put("hasSpatialAudio", m.soundCapabilities.hasSpatialAudio)
                        put("hasHeadTracking", m.soundCapabilities.hasHeadTracking)
                        put("spatialScenes", buildJsonArray { m.soundCapabilities.spatialScenes.forEach { add(it) } })
                    })
                    put("moreSettingsCapabilities", buildJsonObject {
                        put("hasWearDetection", m.moreSettingsCapabilities.hasWearDetection)
                        put("hasMultipoint", m.moreSettingsCapabilities.hasMultipoint)
                        put("hasLowLatency", m.moreSettingsCapabilities.hasLowLatency)
                        put("hasAutoPickCall", m.moreSettingsCapabilities.hasAutoPickCall)
                        put("hasFitDetection", m.moreSettingsCapabilities.hasFitDetection)
                        put("hasEarCanalDetection", m.moreSettingsCapabilities.hasEarCanalDetection)
                        put("hasEarboxSound", m.moreSettingsCapabilities.hasEarboxSound)
                        put("hasVoiceControl", m.moreSettingsCapabilities.hasVoiceControl)
                        put("hasCustomSkin", m.moreSettingsCapabilities.hasCustomSkin)
                        put("hasDongle", m.moreSettingsCapabilities.hasDongle)
                        put("hasSport", m.moreSettingsCapabilities.hasSport)
                        put("hasFindDevice", m.moreSettingsCapabilities.hasFindDevice)
                    })
                })
            }
        }
        return jsonParser.encodeToString(JsonArray.serializer(), array)
    }

    fun resolveCanonicalCodename(vid: Int, pid: Int, fallback: String): String {
        return when {
            vid == 10007 && (pid == 20505 || pid == 20534) -> "J77S"
            vid == 10007 && pid == 20525 -> "K73a"
            vid == 10007 && pid == 20517 -> "K73"
            vid == 10007 && (pid == 20570 || pid == 20574) -> "K75sStarWars"
            vid == 10007 && (pid == 20518 || pid == 20523) -> "K75"
            vid == 10007 && (pid == 20533 || pid == 20539) -> "L71"
            vid == 23117 && (pid in 59918..59920) -> "L76"
            vid == 10007 && pid == 20573 -> "L77s"
            vid == 10007 && (pid == 20532 || pid == 20535 || pid == 20536) -> "L77"
            vid == 10007 && (pid == 20548 || pid == 20549) -> "M75A"
            vid == 10007 && pid == 20622 -> "M79I"
            vid == 10007 && (pid in listOf(20575, 20576, 20577, 20578, 20585)) -> "M79A"
            vid == 10007 && (pid == 20607 || pid == 20608) -> "N74A"
            vid == 10007 && (pid == 20609 || pid == 20610) -> "N75"
            vid == 10007 && pid == 20590 -> "N76GIP"
            vid == 10007 && pid == 20591 -> "N76G"
            vid == 10007 && (pid == 20588 || pid == 20589) -> "N76"
            vid == 10007 && pid == 20587 -> "N77Ip"
            vid == 10007 && pid == 20629 -> "N77S"
            vid == 10007 && (pid in listOf(20586, 20597, 20615)) -> "N77"
            vid == 10007 && pid == 20636 -> "N79S"
            vid == 10007 && (pid == 20634 || pid == 20635) -> "N79B"
            vid == 10007 && (pid in 20618..20621) -> "N79A"
            vid == 10007 && (pid == 20616 || pid == 20617) -> "N79"
            vid == 10007 && (pid == 20720 || pid == 20731) -> "O70C"
            vid == 10007 && (pid == 20651 || pid == 20652) -> "O71Wifi"
            vid == 10007 && pid == 20653 -> "O71BT"
            vid == 10007 && pid == 20660 -> "O71"
            vid == 10007 && pid == 20661 -> "O73"
            vid == 10007 && (pid == 20664 || pid == 20695) -> "O74"
            vid == 10007 && pid == 20655 -> "O76G"
            vid == 10007 && (pid == 20637 || pid == 20638) -> "O76"
            vid == 10007 && pid == 20665 -> "O77S"
            vid == 10007 && (pid == 20639 || pid == 20640 || pid == 20647) -> "O77"
            vid == 10007 && (pid == 20714 || pid == 20715) -> "P75"
            vid == 10007 && pid == 20724 -> "P76Bose"
            vid == 10007 && (pid == 20707 || pid == 20709) -> "P76"
            vid == 10007 && (pid in listOf(20705, 20706, 20718, 20719)) -> "P79"
            vid == 10007 && (pid == 20699 || pid == 20700) -> "Q74u"
            else -> fallback
        }
    }
}
