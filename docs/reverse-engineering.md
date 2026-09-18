# Xiaomi Earbuds Reverse Engineering Methodology & Audit Guide

## 1. Overview of Decompiled Target

- **Target Package**: `com.mi.earphone`
- **Application Name**: Xiaomi Earbuds
- **Target Version**: `1.37.1i`
- **Local Decompiled Base**: `/home/alan/earbuds_decompiled`
  - `resources/`: Layouts (`res/layout/`), Drawables (`res/drawable*/`), Values (`res/values*/`), Strings (`res/values*/strings.xml`).
  - `sources/`: Java/Kotlin bytecode decompiled by JADX.

---

## 2. Methodology & Investigation Steps

### 2.1 UI and Layout Audit
1. Every screen in XimiEarbuds maps to an authentic Android layout file in `res/layout/`:
   - Empty state: `device_settings_empty_layout.xml`
   - Passport login: `passport_activity_layout_wrapper.xml`, `passport_fragment_password_login.xml`
   - Custom EQ: `device_settings_fragment_customized_eq.xml`
   - Noise reduction: `device_settings_noise_reduction_layout.xml`
   - Battery indicators: `layout_battery.xml`
2. Measurements (padding, margins, text sizes, corner radii) were extracted directly from the XML attributes or referenced dimen files (`res/values/dimens.xml`).

### 2.2 Localization & String Parity
1. Android resources contain localized strings for over 25 languages in `res/values-*/strings.xml`.
2. All keys were indexed into json files in `src/main/resources/i18n/strings_*.json`.
3. Strict parity rules:
   - Zero hardcoded user-facing strings in UI code.
   - Exact variable placeholder syntax (`%1$s`, `%2$s`).
   - Preservation of HTML anchor formatting (`<a href=...>...</a>`) transformed via `parseHtmlLinks()` into clickable Compose `AnnotatedString` spans.

### 2.3 Hardware Protocol Analysis
1. Inspected `com.mi.earphone.control` to deduce packet structures, OpCodes, and checksum algorithms.
2. Verified against live Bluetooth traces (`btmon` / `hcidump`) on Linux when communicating with physical hardware (`00:BB:43:8B:C0:F3`).
3. Confirmed JieLi (RCSP) and Bestechnic command sets and RFCOMM channel allocations (primarily Channel 24).

### 2.4 Cryptographic Verification
1. Identified authentication handshake routines in `com.mi.earphone.control.auth`.
2. Extracted the SAFER+ E21 block cipher implementation, verifying 16 rounds of byte substitutions, key additions, and linear transformations.
3. Implemented clean-room Kotlin reproduction in `BluetoothAuthEngine.kt` and validated against reference test vectors.
