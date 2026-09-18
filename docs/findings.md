# Reverse Engineering Findings, Technical Quirks & Discovered Edge Cases

## 1. Protocol & Noise Control State Discrepancies

### 1.1 Non-Contiguous Transparency Level Indices
During analysis of `com.mi.earphone.control.constants.NoiseLevelConstants`:
- ANC levels are indexed:
  - `0`: Mild (`device_settings_noise_reduction_light`)
  - `1`: Deep (`device_settings_noise_reduction_deep`)
  - `2`: Balanced (`device_settings_noise_reduction_balance`)
  - `3`: Adaptive (`device_settings_noise_reduction_adaptive`)
- Transparency levels are NOT contiguous:
  - `0`: Regular transparency (`device_settings_noise_reduction_transparent_all`)
  - `1`: Vocal enhancement (`device_settings_noise_reduction_transparent_person`)
  - `2`: Reserved / Unused in firmware
  - `3`: Vocal Plus (`device_settings_noise_reduction_transparent_person_plus`)
- **Fix Applied**: `DeviceConfigs.kt` was updated to declare `TransparencyLevel.VOCAL_PLUS(3)` and map it to `device_settings_noise_reduction_transparent_person_plus` in `MiuixNoiseReductionView.kt`.

### 1.2 State Conversion Bug in `NoiseControlState`
- Previously, `NoiseControlState.toCommonConfig()` used `ancLevelIndex` (which defaulted to 1) instead of the active `ancLevel.id`.
- This caused conversions between high-level ANC states and low-level packet configs to revert to deep ANC whenever `ancLevel` was set to mild or balanced.
- **Fix Applied**: Default initializers now compute `ancLevelIndex = ancLevel.id` and `transparencyLevelIndex = transparencyLevel.id`, ensuring bidirectional fidelity.

---

## 2. Cryptographic Handshake (SAFER+ E21)

- Earbuds verify the authenticity of the client software before granting write access to critical device parameters (such as gesture reassignment or dual-connection switching).
- The authentication handshake uses the SAFER+ E21 block cipher with 16 rounds of PHT (Pseudo-Hadamard Transform), modular byte addition (`(x + y) mod 256`), and bitwise XOR operations.
- The 16-byte session key is derived by combining a hardcoded vendor salt with the hardware MAC address of the earbuds and the random challenge nonce received during `0x80 CMD_AUTH_REQUEST`.

---

## 3. Bluetooth Link-Layer Behavior on Linux

### 3.1 Kernel DLC Socket Recycling
- When an RFCOMM socket is closed, the Linux BlueZ kernel stack transitions through a disconnect timeout state (`RFCOMM_DISC_TIMEOUT`).
- Reconnecting within 3500ms results in immediate `EBUSY (Device or resource busy)` errors.
- `LinuxRfcommTransport` enforces a mandatory 3500ms recycling delay before subsequent connection attempts.

### 3.2 Security Level Escalation (`BT_SECURITY_MEDIUM`)
- Modern Xiaomi earbuds reject unencrypted Classic Bluetooth RFCOMM connections.
- By configuring socket option `SOL_BLUETOOTH = 274` with level `BT_SECURITY_MEDIUM = 2`, the kernel BlueZ stack prompts the Bluetooth controller to establish an encrypted ACL link before completing the RFCOMM handshake.

---

## 4. UI Resource Parity & Localization

### 4.1 HTML Anchor Tags in Resource Strings
- Official string resources (such as `passport_user_agreement_hint_default`) contain embedded HTML tags:
  `"J'ai lu et accepté l'<a href=%1$s>Accord Utilisateur</a> et <a href=%2$s>Politique de Confidentialité</a> du Compte Xiaomi."`
- A dedicated HTML link parser (`parseHtmlLinks`) was developed in `Strings.kt` using regex to convert `<a href="...">text</a>` into Compose `AnnotatedString` spans with URL click actions, avoiding hardcoded translated strings.

### 4.2 Empty State Hierarchy (`device_settings_empty_layout.xml`)
- Initial desktop versions rendered an arbitrary centered button with an `Icons.Default.Add` plus icon.
- Inspection of `device_settings_empty_layout.xml` revealed:
  1. The center illustration and text are vertically packed.
  2. The button (`@id/add_view`) is pinned to the bottom of the container with 27dp horizontal and bottom margins.
  3. The button uses style `BaseButton.Positive`: a 180dp radius pill without any plus icon, filled with the primary accent color.
