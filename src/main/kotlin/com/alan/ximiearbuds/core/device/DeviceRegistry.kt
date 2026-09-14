package com.alan.ximiearbuds.core.device

import com.alan.ximiearbuds.core.catalog.XiaomiCatalogService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AncCapabilities(
    val hasAdaptiveAnc: Boolean = false,
    val hasPersonalizedAnc: Boolean = false,
    val hasSmartDenoise: Boolean = false,
    val ancLevels: List<Int> = listOf(1, 0, 2),
    val transparencyLevels: List<Int> = listOf(0, 1, 2),
    val isSingleToggleOnly: Boolean = false
)

data class EarbudsModel(
    val codename: String,
    val commercialName: String,
    val vendorId: Int = 10007,
    val productIds: List<Int>,
    val brand: String = "Xiaomi",
    val modelCode: String = "",
    val saleModel: String = "",
    val iconFile: String = "",
    val iconUrl: String = "",
    val colorVariants: Map<String, String> = emptyMap(),
    val defaultColor: Int = 1,
    val hasAnc: Boolean = true,
    val hasTransparency: Boolean = true,
    val has10BandEq: Boolean = true,
    val hasGestures: Boolean = true,
    val hasSlideGesture: Boolean = false,
    val hasLowLatency: Boolean = true,
    val hasMultipoint: Boolean = true,
    val hasInEarDetection: Boolean = true,
    val hasSpatialAudio: Boolean = false,
    val hasFitDetection: Boolean = false,
    val hasEarboxSound: Boolean = false,
    val hasFindDevice: Boolean = true,
    val hasDongle: Boolean = false,
    val isBoneConduction: Boolean = false,
    val ancCapabilities: AncCapabilities = AncCapabilities()
)

object DeviceRegistry {
    val GENERIC_MODEL = EarbudsModel(
        codename = "GENERIC",
        commercialName = "Xiaomi Earbuds",
        vendorId = 10007,
        productIds = emptyList(),
        brand = "Xiaomi",
        hasAnc = true,
        hasTransparency = true,
        has10BandEq = true,
        hasGestures = true,
        hasSlideGesture = true,
        hasLowLatency = true,
        hasMultipoint = true,
        hasInEarDetection = true,
        hasSpatialAudio = true,
        hasFitDetection = true,
        hasEarboxSound = true,
        hasFindDevice = true
    )

    private val _modelsFlow = MutableStateFlow<List<EarbudsModel>>(emptyList())
    val modelsFlow: StateFlow<List<EarbudsModel>> = _modelsFlow.asStateFlow()

    val ALL_MODELS: List<EarbudsModel>
        get() = _modelsFlow.value

    init {
        loadCatalog()
    }

    fun loadCatalog() {
        val loaded = XiaomiCatalogService.loadCachedCatalog()
            ?: XiaomiCatalogService.loadBundledCatalog()

        val models = if (loaded.isNotEmpty()) {
            val list = ArrayList(loaded)
            if (list.none { it.codename == "GENERIC" }) {
                list.add(GENERIC_MODEL)
            }
            list
        } else {
            FALLBACK_CATALOG + GENERIC_MODEL
        }
        _modelsFlow.value = models
    }

    fun updateModels(newModels: List<EarbudsModel>) {
        val list = ArrayList(newModels)
        if (list.none { it.codename == "GENERIC" }) {
            list.add(GENERIC_MODEL)
        }
        _modelsFlow.value = list
    }

    fun findByVidPid(vid: Int, pid: Int): EarbudsModel {
        return ALL_MODELS.find { model ->
            model.vendorId == vid && model.productIds.contains(pid)
        } ?: ALL_MODELS.last()
    }

    fun findByName(name: String): EarbudsModel {
        return ALL_MODELS.find { model ->
            name.contains(model.commercialName, ignoreCase = true) ||
                    (model.codename.isNotBlank() && name.contains(model.codename, ignoreCase = true)) ||
                    (model.saleModel.isNotBlank() && name.contains(model.saleModel, ignoreCase = true))
        } ?: ALL_MODELS.last()
    }

    fun resolveDeviceColor(vendorId: Int, productId: Int, colorType: Int, majorId: Int = 0, minorId: Int = 0): Int {
        if (colorType > 0) return colorType
        return when {
            majorId == 1 && (minorId == 2 || minorId == 16 || minorId == 18) -> 1
            majorId == 1 && (minorId == 3 || minorId == 17 || minorId == 19) -> 2
            majorId == 1 && (minorId == 14 || minorId == 15 || minorId == 20) -> 3
            majorId == 1 && minorId == 4 -> 4
            majorId == 1 && (minorId == 5 || minorId == 8) -> 1
            majorId == 1 && (minorId == 6 || minorId == 9) -> 2
            majorId == 1 && minorId == 10 -> 9
            majorId == 1 && (minorId == 7 || minorId == 15) -> 10
            majorId == 1 && minorId == 11 -> 2
            majorId == 1 && minorId == 25 -> 2
            majorId == 3 && minorId == 59 -> 2
            majorId == 1 && minorId == 26 -> 1
            majorId == 3 && minorId == 60 -> 1
            minorId == 0 || minorId == 21 || minorId == 27 -> 1
            minorId == 1 || minorId == 22 || minorId == 28 -> 2
            minorId == 2 || minorId == 24 || minorId == 29 -> 4
            minorId == 23 || minorId == 30 -> 3
            else -> 0
        }
    }

    private val FALLBACK_CATALOG = listOf(
        EarbudsModel(
            codename = "J77S",
            commercialName = "Mi True Wireless Earphones 2 Basic",
            vendorId = 10007,
            productIds = listOf(20505, 20534),
            brand = "Xiaomi",
            hasAnc = false,
            hasTransparency = false,
            has10BandEq = false,
            hasSpatialAudio = false
        ),
        EarbudsModel(
            codename = "K73",
            commercialName = "Redmi Buds 3 Pro",
            vendorId = 10007,
            productIds = listOf(20517, 20525),
            brand = "Redmi",
            hasAnc = true,
            hasTransparency = true,
            has10BandEq = false,
            hasSpatialAudio = false
        ),
        EarbudsModel(
            codename = "N76",
            commercialName = "Redmi Buds 5 Pro",
            vendorId = 10007,
            productIds = listOf(20588, 20589),
            brand = "Redmi",
            hasAnc = true,
            hasTransparency = true,
            has10BandEq = true,
            hasSpatialAudio = true,
            hasSlideGesture = true,
            hasFitDetection = true,
            hasEarboxSound = true
        ),
        EarbudsModel(
            codename = "O74",
            commercialName = "Xiaomi Buds 5",
            vendorId = 10007,
            productIds = listOf(20664, 20695),
            brand = "Xiaomi",
            hasAnc = true,
            hasTransparency = true,
            has10BandEq = true,
            hasSpatialAudio = true,
            hasSlideGesture = true,
            hasEarboxSound = true
        )
    )
}
