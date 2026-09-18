# Xiaomi, Redmi & POCO Earbuds Device Matrix & Capabilities

## 1. Device Catalog Architecture

XimiEarbuds replicates the official device catalog architecture from `com.mi.earphone`:
1. **Bundled Offline Catalog**: `devices/catalog.json` contains 78 officially verified models with complete hardware capability bitmaps, color variants, vendor IDs, and product IDs.
2. **Cloud Catalog Synchronization (`XiaomiCatalogService`)**:
   - Communicates with Xiaomi Wear endpoints across 6 international server regions:
     - `de` (Europe / Global)
     - `cn` (China Mainland)
     - `sg` (Singapore / Southeast Asia)
     - `ru` (Russia)
     - `us` (United States)
     - `i2` (India)
   - Auto-detects region via `https://region.tws.wear.xiaomiwear.com/twsregion/region/user_region_by_ip` using the official `auth_key=rwelJuWBFJxmbMKD` token.
   - Merges and caches device definitions and icons in `~/.ximiearbuds/cache/`.

---

## 2. Supported Models Matrix (78 Official Devices)

### 2.1 Xiaomi Flagship Series
| Model / Codename | Commercial Name | Chipset / SOC | ANC | 10-Band EQ | Spatial Audio | Multipoint | Wear Detect |
|---|---|---|:---:|:---:|:---:|:---:|:---:|
| `Air2Pro` | Mi True Wireless Earphones 2 Pro | BES | Yes | No | No | No | Yes |
| `Air2s` | Mi True Wireless Earphones 2S | BES | No | No | No | No | Yes |
| `J77S` | Mi True Wireless Earphones 2 Basic | BES | No | No | No | No | Yes |
| `Air2Pokemon` | Mi True Wireless Earphones 2 (Pikachu) | BES | No | No | No | No | Yes |
| `FlipBudsPro` | Xiaomi FlipBuds Pro | BES2500YP | Yes | No | No | Yes | Yes |
| `Buds3` | Xiaomi Buds 3 | BES | Yes | No | No | Yes | Yes |
| `Buds3Pro` | Xiaomi Buds 3 Pro | BES2600 | Yes | No | Yes | Yes | Yes |
| `Buds3TPro` | Xiaomi Buds 3T Pro | BES2600 | Yes | No | Yes | Yes | Yes |
| `Buds4` | Xiaomi Buds 4 | BES2600 | Yes | Yes | Yes | Yes | Yes |
| `Buds4Pro` | Xiaomi Buds 4 Pro | BES2600 | Yes | Yes | Yes | Yes | Yes |
| `Buds5` | Xiaomi Buds 5 | BES2700 | Yes | Yes | Yes | Yes | Yes |
| `Buds5Pro` | Xiaomi Buds 5 Pro | BES2700 | Yes | Yes | Yes | Yes | Yes |

### 2.2 Redmi Series
| Model / Codename | Commercial Name | Chipset / SOC | ANC | 10-Band EQ | Spatial Audio | Multipoint | Wear Detect |
|---|---|---|:---:|:---:|:---:|:---:|:---:|
| `L76` | Redmi Buds 4 Pro | BES2600 | Yes | Yes | Yes | Yes | Yes |
| `L77` | Redmi Buds 4 | Airoha | Yes | No | No | No | Yes |
| `M79A` | Redmi Buds 4 Active | JL7006 | No | No | No | No | No |
| `L79` | Redmi Buds 4 Lite | JL | No | No | No | No | No |
| `N78` | Redmi Buds 5 Pro | BES2600 | Yes | Yes | Yes | Yes | Yes |
| `N77` | Redmi Buds 5 | Airoha | Yes | Yes | No | Yes | Yes |
| `O79` | Redmi Buds 6 Active | JL | No | Yes | No | No | No |
| `O77` | Redmi Buds 6 Play | JL | No | Yes | No | No | No |
| `O76` | Redmi Buds 6 Pro | BES | Yes | Yes | Yes | Yes | Yes |
| `K70` | Redmi AirDots 3 Pro | BES | Yes | No | No | Yes | Yes |

### 2.3 POCO Series
| Model / Codename | Commercial Name | Chipset / SOC | ANC | 10-Band EQ | Spatial Audio | Multipoint | Wear Detect |
|---|---|---|:---:|:---:|:---:|:---:|:---:|
| `M79A` | POCO Pods | JL7006 | No | No | No | No | No |
| `N79A` | POCO Buds X1 | JL | Yes | Yes | No | No | Yes |
| `K70` | POCO Buds Pro (Genshin Impact) | BES | Yes | No | No | Yes | Yes |

---

## 3. Capabilities Schema (`core.device`)

Each device entry encapsulates four sub-capability descriptors:

```kotlin
data class AncCapabilities(
    val hasAdaptiveAnc: Boolean,
    val hasPersonalizedAnc: Boolean,
    val hasSmartDenoise: Boolean,
    val ancLevels: List<Int>,         // e.g. [1, 0, 2, 3]
    val transparencyLevels: List<Int>,// e.g. [0, 1, 3]
    val isSingleToggleOnly: Boolean
)

data class GestureCapabilities(
    val allowedActions: Map<Int, List<Int>>,
    val noiseControlActions: List<Int>,
    val hasSecondaryPage: Boolean,
    val isPinchGesture: Boolean,
    val isSlideGesture: Boolean,
    val isMfbGesture: Boolean
)

data class SoundCapabilities(
    val supportedPresets: List<Int>,
    val hasVirtualSurround: Boolean,
    val hasAdaptiveSense: Boolean,
    val hasAudibilityAdaptation: Boolean,
    val hasAdaptiveVolume: Boolean,
    val hasNotificationVolume: Boolean,
    val hasSpatialAudio: Boolean,
    val hasHeadTracking: Boolean,
    val spatialScenes: List<Int>
)

data class MoreSettingsCapabilities(
    val hasWearDetection: Boolean,
    val hasMultipoint: Boolean,
    val hasLowLatency: Boolean,
    val hasAutoPickCall: Boolean,
    val hasFitDetection: Boolean,
    val hasEarCanalDetection: Boolean,
    val hasEarboxSound: Boolean,
    val hasVoiceControl: Boolean,
    val hasCustomSkin: Boolean,
    val hasDongle: Boolean,
    val hasSport: Boolean,
    val hasFindDevice: Boolean
)
```
