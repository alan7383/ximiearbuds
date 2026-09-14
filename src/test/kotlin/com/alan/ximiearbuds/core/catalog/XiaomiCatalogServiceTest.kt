package com.alan.ximiearbuds.core.catalog

import com.alan.ximiearbuds.core.device.DeviceRegistry
import org.jetbrains.skia.Image
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class XiaomiCatalogServiceTest {

    @Test
    fun testBundledCatalogLoadsSuccessfully() {
        val models = XiaomiCatalogService.loadBundledCatalog()
        assertTrue(models.size >= 70, "Expected at least 70 models, got ${models.size}")

        // Ensure brands are properly categorized
        val brands = models.map { it.brand }.toSet()
        assertTrue(brands.contains("Xiaomi"))
        assertTrue(brands.contains("Redmi"))
        assertTrue(brands.contains("POCO"))
    }

    @Test
    fun testDeviceRegistryContainsAllModels() {
        val allModels = DeviceRegistry.ALL_MODELS
        assertTrue(allModels.size >= 70, "Expected at least 70 models in registry, got ${allModels.size}")

        // Generic fallback should be present
        assertTrue(allModels.any { it.codename == "GENERIC" })

        // Check key models
        val redmi5pro = DeviceRegistry.findByVidPid(10007, 20588)
        assertEquals("Redmi Buds 5 Pro", redmi5pro.commercialName)
        assertTrue(redmi5pro.hasAnc)

        val buds4pro = DeviceRegistry.findByVidPid(10007, 20533)
        assertEquals("Xiaomi Buds 4 Pro", buds4pro.commercialName)

        val buds3tpro = DeviceRegistry.findByVidPid(10007, 20525)
        assertEquals("Xiaomi Buds 3T Pro", buds3tpro.commercialName)

        val buds5 = DeviceRegistry.findByName("Xiaomi Buds 5")
        assertNotNull(buds5)
    }

    @Test
    fun testBundledWebPIconsExistAndDecodeWithSkia() {
        val models = XiaomiCatalogService.loadBundledCatalog()
        val modelsWithIcons = models.filter { it.iconFile.isNotBlank() }
        assertTrue(modelsWithIcons.size >= 65, "Expected at least 65 models with icon files")

        var decodedCount = 0
        for (model in modelsWithIcons) {
            val resourcePath = "devices/icons/${model.iconFile}"
            val stream = javaClass.classLoader.getResourceAsStream(resourcePath)
            if (stream != null) {
                val bytes = stream.use { it.readAllBytes() }
                assertTrue(bytes.isNotEmpty(), "Icon $resourcePath should not be empty")
                val skiaImage = Image.makeFromEncoded(bytes)
                assertNotNull(skiaImage, "Skia should decode WebP icon: $resourcePath")
                assertTrue(skiaImage.width > 0 && skiaImage.height > 0)
                decodedCount++
            }
        }

        assertTrue(decodedCount >= 65, "Expected at least 65 successfully decoded icons, got $decodedCount")
    }

    @Test
    fun testUrlPathEncoding() {
        val raw = "https://cdn.awsde0-fusion.fds.api.mi-img.com/static-files/tws_icon/N74A 灰.png"
        val encoded = XiaomiCatalogService.encodeUrlPath(raw)
        assertTrue(encoded.contains("%20") || !encoded.contains(" "), "Spaces should be encoded")
    }

    @Test
    fun testColorVariantIconsExistAndDecodeWithSkia() {
        // Specifically verify Redmi Buds 6 Pro color variants (including Lavender c5)
        val o76 = DeviceRegistry.findByVidPid(10007, 20638)
        assertEquals("Redmi Buds 6 Pro", o76.commercialName)
        for (colorId in listOf("1", "2", "5")) {
            val iconPath = "devices/icons/2717_509e_c$colorId.png"
            val stream = javaClass.classLoader.getResourceAsStream(iconPath)
            assertNotNull(stream, "Bundled icon $iconPath must exist in resources")
            val bytes = stream.use { it.readAllBytes() }
            assertTrue(bytes.isNotEmpty())
            val skia = Image.makeFromEncoded(bytes)
            assertNotNull(skia, "Skia should decode $iconPath")
            assertTrue(skia.width > 0 && skia.height > 0)
        }
    }
}
