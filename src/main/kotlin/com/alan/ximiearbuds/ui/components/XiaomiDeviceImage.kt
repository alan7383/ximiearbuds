package com.alan.ximiearbuds.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.res.painterResource
import com.alan.ximiearbuds.core.catalog.XiaomiCatalogService
import com.alan.ximiearbuds.core.device.EarbudsModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

object DeviceImageCache {
    private val memoryCache = ConcurrentHashMap<String, ImageBitmap>()

    fun get(key: String): ImageBitmap? = memoryCache[key]
    fun put(key: String, bitmap: ImageBitmap) {
        memoryCache[key] = bitmap
    }
}

/**
 * High-performance, offline-first device image renderer for Compose Desktop.
 * Resolution hierarchy:
 * 1. In-memory cache
 * 2. User disk cache (~/.ximiearbuds/cache/icons/{iconFile})
 * 3. Classpath bundled resource (devices/icons/{iconFile})
 * 4. Background network download from Xiaomi CDN
 * 5. Fallback placeholder (drawable/device_default_icon.png)
 */
@Composable
fun XiaomiDeviceImage(
    model: EarbudsModel?,
    modifier: Modifier = Modifier,
    colorType: Int? = null,
    contentDescription: String? = model?.commercialName
) {
    if (model == null || model.iconFile.isBlank()) {
        Image(
            painter = painterResource("drawable/device_default_icon.png"),
            contentDescription = contentDescription,
            modifier = modifier
        )
        return
    }

    val effectiveColor = if (colorType != null && colorType > 0) colorType else model.defaultColor
    val iconBase = model.iconFile.substringBeforeLast(".")
    val colorIconNamePng = "${iconBase}_c$effectiveColor.png"
    val colorIconNameWebp = "${iconBase}_c$effectiveColor.webp"
    val iconKey = "${iconBase}_c$effectiveColor"

    var bitmap by remember(iconKey) { mutableStateOf(DeviceImageCache.get(iconKey)) }

    LaunchedEffect(iconKey) {
        val cached = DeviceImageCache.get(iconKey)
        if (cached != null) {
            bitmap = cached
            return@LaunchedEffect
        }

        val loadedBitmap = withContext(Dispatchers.IO) {
            val cl = DeviceImageCache::class.java.classLoader

            // 1. Check bundled resource for this specific color variant
            try {
                val stream = cl.getResourceAsStream("devices/icons/$colorIconNamePng")
                    ?: cl.getResourceAsStream("devices/icons/$colorIconNameWebp")
                if (stream != null) {
                    val bytes = stream.use { it.readAllBytes() }
                    val bm = SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
                    DeviceImageCache.put(iconKey, bm)
                    return@withContext bm
                }
            } catch (_: Exception) {}

            // 2. Check disk cache for this specific color variant
            try {
                val colorCachePng = File(XiaomiCatalogService.iconsCacheDir, colorIconNamePng)
                val colorCacheWebp = File(XiaomiCatalogService.iconsCacheDir, colorIconNameWebp)
                val candidate = when {
                    colorCachePng.exists() && colorCachePng.length() > 0L -> colorCachePng
                    colorCacheWebp.exists() && colorCacheWebp.length() > 0L -> colorCacheWebp
                    else -> null
                }
                if (candidate != null) {
                    val bytes = candidate.readBytes()
                    val bm = SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
                    DeviceImageCache.put(iconKey, bm)
                    return@withContext bm
                }
            } catch (_: Exception) {}

            // 3. Download variant-specific URL from Xiaomi CDN if available
            val variantUrl = model.colorVariants[effectiveColor.toString()]?.ifBlank { null }
            if (!variantUrl.isNullOrBlank()) {
                try {
                    val cleanUrl = XiaomiCatalogService.encodeUrlPath(variantUrl)
                    val client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(6))
                        .build()
                    val req = HttpRequest.newBuilder()
                        .uri(URI.create(cleanUrl))
                        .header("User-Agent", "Mozilla/5.0")
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build()
                    val res = client.send(req, HttpResponse.BodyHandlers.ofByteArray())
                    if (res.statusCode() == 200 && res.body().isNotEmpty()) {
                        val bytes = res.body()
                        val target = File(XiaomiCatalogService.iconsCacheDir, colorIconNamePng)
                        target.writeBytes(bytes)
                        val bm = SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
                        DeviceImageCache.put(iconKey, bm)
                        return@withContext bm
                    }
                } catch (_: Exception) {}
            }

            // 4. Fallback to default model icon
            try {
                val defaultStream = cl.getResourceAsStream("devices/icons/${model.iconFile}")
                if (defaultStream != null) {
                    val bytes = defaultStream.use { it.readAllBytes() }
                    return@withContext SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
                }
                val defaultCache = XiaomiCatalogService.getLocalIconFile(model)
                if (defaultCache != null && defaultCache.exists() && defaultCache.length() > 0L) {
                    val bytes = defaultCache.readBytes()
                    return@withContext SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
                }
            } catch (_: Exception) {}

            null
        }

        if (loadedBitmap != null) {
            bitmap = loadedBitmap
        }
    }

    val currentBitmap = bitmap
    if (currentBitmap != null) {
        Image(
            bitmap = currentBitmap,
            contentDescription = contentDescription,
            modifier = modifier,
            filterQuality = androidx.compose.ui.graphics.FilterQuality.High,
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
    } else {
        Image(
            painter = painterResource("drawable/device_default_icon.png"),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
    }
}

