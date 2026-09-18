# HyperOS & MIUIX Design System Specification

## 1. Design System Principles

XimiEarbuds replicates the visual hierarchy, elevation, motion, and touch geometry of Xiaomi HyperOS (MIUI 14 / MIUIX) as extracted from `com.mi.earphone`.

```
+-----------------------------------------------------------------------------+
|                               Page Header                                   |
|   [< Back]               Redmi Buds 5 Pro               [Settings Gear]     |
+-----------------------------------------------------------------------------+
|                                                                             |
|   +---------------------------------------------------------------------+   |
|   |                         Earbuds Hero Card                           |   |
|   |                  [Left Earbud]     [Right Earbud]                   |   |
|   |                       85%                90%                        |   |
|   |                                Case: 75%                            |   |
|   +---------------------------------------------------------------------+   |
|                                                                             |
|   +---------------------------------------------------------------------+   |
|   |                     Noise Control (MIUIX Segmented)                 |   |
|   |       [   ANC   ]     |     [   OFF   ]     |  [ Transparency ]     |   |
|   |   ---------------------------------------------------------------   |   |
|   |       (•) Mild      ( ) Balanced      ( ) Deep      ( ) Adaptive    |   |
|   +---------------------------------------------------------------------+   |
|                                                                             |
|   +---------------------------------------------------------------------+   |
|   |                Sound Effects & 10-Band Studio Equalizer             |   |
|   |         Presets: [Balanced] [Bass Boost] [Voice] [Treble]           |   |
|   |   Custom Studio Graphic EQ: -10dB to +10dB curve (31Hz .. 16kHz)    |   |
|   +---------------------------------------------------------------------+   |
|                                                                             |
+-----------------------------------------------------------------------------+
```

---

## 2. Color Palette & Theming (`ui.theme.Color.kt`)

### 2.1 Dark Palette (Default)
- **Page Background**: `Color(0xFF000000)` (OLED Deep Black)
- **Surface / Card Background**: `Color(0xFF191919)`
- **Card Hover State**: `Color(0xFF242424)`
- **Card Border**: `Color(0x1AFFFFFF)` (10% translucent white)
- **Text Primary**: `Color(0xFFFFFFFF)` (100% white)
- **Text Secondary**: `Color(0x99FFFFFF)` (60% white)
- **Text Muted**: `Color(0x66FFFFFF)` (40% white)
- **Divider**: `Color(0x14FFFFFF)`

### 2.2 Light Palette
- **Page Background**: `Color(0xFFF4F4F6)`
- **Surface / Card Background**: `Color(0xFFFFFFFF)`
- **Card Hover State**: `Color(0xFFF0F0F2)`
- **Text Primary**: `Color(0xFF111111)`
- **Text Secondary**: `Color(0xFF666666)`
- **Text Muted**: `Color(0xFF999999)`

### 2.3 Official Brand & Hardware Accents
- **Xiaomi Electric Blue**: `Color(0xFF0D84FF)` (Scanning, primary actions, positive buttons)
- **Xiaomi Cyan**: `Color(0xFF00BDB1)` (Active Noise Control mode indicators)
- **Xiaomi Orange**: `Color(0xFFF04D18)` (Xiaomi Passport brand actions)
- **Xiaomi Green**: `Color(0xFF10B981)` (Optimal battery level)
- **Xiaomi Red**: `Color(0xFFEF4444)` (Low battery warning < 20%)

---

## 3. Geometry & Corner Radii

- **Pill Action Buttons (`BaseButton.Positive`)**: `180.dp` corner radius (perfect pill).
- **Surface Cards**: `24.dp` corner radius.
- **Input Fields & Text Boxes**: `12.dp` to `16.dp` corner radius.
- **Dialogs & Overlays**: `20.dp` corner radius.

---

## 4. Key Component Blueprint

### 4.1 Noise Reduction View (`MiuixNoiseReductionView.kt`)
Replicates `device_settings_noise_reduction_layout.xml`:
- Three-way segmented switch: `Noise Reduction (ANC)`, `Off`, `Transparency`.
- Dynamic sub-level cards:
  - When ANC is active: 4 options (`Mild`, `Balanced`, `Deep`, `Adaptive`).
  - When Transparency is active: 3 options (`Regular`, `Vocal Enhancement`, `Vocal Plus`).

### 4.2 10-Band Graphic Equalizer (`MiuixEqualizerScreen.kt`)
Replicates `device_settings_fragment_customized_eq.xml`:
- Frequency bands: `31 Hz`, `62 Hz`, `125 Hz`, `250 Hz`, `500 Hz`, `1 kHz`, `2 kHz`, `4 kHz`, `8 kHz`, `16 kHz`.
- Range: `-10 dB` to `+10 dB` with 0.1 dB precision.
- Real-time smooth cubic spline visualization across sliders.

### 4.3 Empty State View (`XiaomiEmptyStateView.kt`)
Replicates `device_settings_empty_layout.xml`:
- Vertically packed center: `ic_tv` (illustration + `device_no_available_device`) and `add` (`device_no_paired_device_tip`).
- Bottom-pinned button: `add_view` with 27dp horizontal/bottom margins, 180dp radius pill, primary blue accent, and `device_add_title` text.
