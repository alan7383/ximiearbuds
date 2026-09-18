# XimiEarbuds System Architecture & Reverse Engineering Blueprint

## 1. Architectural Overview

XimiEarbuds is a high-fidelity desktop reproduction of the official Xiaomi Earbuds mobile application (`com.mi.earphone`, target version 1.37.1i). The application provides complete hardware control, telemetry, configuration, and cloud account synchronization for Xiaomi, Redmi, and POCO earbuds across desktop environments (Linux and Windows).

```
+-----------------------------------------------------------------------------+
|                               UI Layer                                      |
|  (Compose Multiplatform: HyperOS / MIUIX 1:1 Design Language)               |
|  - MainWindow (Navigation Stack, Dynamic Title, System Tray)                |
|  - MiuixNoiseReductionView (Segmented ANC & Transparency Controls)          |
|  - MiuixEqualizerScreen (10-Band Studio Graphic EQ + Xiaomi Presets)        |
|  - MiuixBatteryBar (Left / Right / Case Telemetry with Charging Animations) |
|  - XiaomiEmptyStateView (device_settings_empty_layout.xml)                  |
|  - MiuixLoginScreen & Dialog (Xiaomi Passport OAuth + QR Login)             |
+-----------------------------------------------------------------------------+
                                      |
                                      v
+-----------------------------------------------------------------------------+
|                          Device & State Layer                               |
|  - EarbudsController (Reactive StateFlow orchestrator)                      |
|  - DeviceConfigs (NoiseControlState, GestureConfig, DeviceProfile)          |
|  - XiaomiAccountClient (SID "miotstore", Cloud Devices Sync)                |
+-----------------------------------------------------------------------------+
                                      |
                                      v
+-----------------------------------------------------------------------------+
|                      Protocol & Cryptography Engine                         |
|  - RcspPacket (0xBA 0xDC Framing, OpCodes, Sequence Numbers, CRC)           |
|  - BluetoothAuthEngine (SAFER+ E21 Block Cipher, Challenge-Response)        |
|  - TLV Stream Deserializer & Serializer                                     |
+-----------------------------------------------------------------------------+
                                      |
                                      v
+-----------------------------------------------------------------------------+
|                     Transport & Hardware Abstraction                        |
|  - BluetoothTransport interface (connect, disconnect, send, receiveFlow)    |
|  - LinuxRfcommTransport (BlueZ POSIX RFCOMM Sockets, BT_SECURITY_MEDIUM)    |
|  - WindowsRfcommTransport (Winsock2 Bluetooth Sockets)                      |
|  - BleGattTransport (Bluetooth Low Energy Fallback)                         |
+-----------------------------------------------------------------------------+
```

---

## 2. Core Architectural Layers

### 2.1 Transport Layer (`core.bluetooth`)
Official Xiaomi earbuds utilize Classic Bluetooth RFCOMM (Serial Port Profile - SPP) as their primary high-speed bidirectional command stream, reserving Bluetooth Low Energy (BLE) for proximity discovery and Fast Pair notifications.
- **Linux Implementation**: `LinuxRfcommTransport` binds directly to Linux BlueZ kernel sockets (`AF_BLUETOOTH`, `SOCK_STREAM`, `BTPROTO_RFCOMM`) via JNA. It explicitly configures `BT_SECURITY_MEDIUM` to satisfy hardware encryption requirements.
- **Windows Implementation**: `WindowsRfcommTransport` interfaces with Microsoft Winsock2 Bluetooth APIs (`AF_BTH`, `BTHPROTO_RFCOMM`).
- **Resilience**: Automatic retry logic, kernel DLC socket drain periods, and non-blocking coroutine flow adapters.

### 2.2 Protocol Engine (`core.protocol`)
All messages exchanged with the earbuds follow the proprietary protocol specifications discovered from the decompiled APK (`com.mi.earphone.control`):
- **JieLi / JL RCSP Protocol**: Encapsulated with `0xBA 0xDC` magic prefixes, sequence tracking, and payload checksums.
- **Bestechnic (BES) Protocol**: Command packet framing with opcode routing.
- **TLV (Type-Length-Value)**: Configuration parameters are queried and updated in batched TLV payloads.

### 2.3 Cryptography & Authentication (`core.crypto`)
Earbuds firmware requires two-way authentication before accepting sensitive parameter modifications:
- **Cipher**: 16-round SAFER+ E21 block cipher.
- **Handshake Flow**:
  1. Desktop requests authentication challenge (`CMD_AUTH_REQUEST = 0x80`).
  2. Earbuds supply an 8-byte or 16-byte random nonce.
  3. Desktop computes SAFER+ key diversification and replies (`CMD_AUTH_RESPONSE = 0x81`).
  4. Mutual session validation is established.

### 2.4 Device Controller & State Management (`core.device`)
- `EarbudsController` exposes immutable Kotlin `StateFlow` primitives (`connectionState`, `batteryState`, `noiseControlState`, `equalizerState`).
- Bidirectional synchronization ensures changes in the UI emit packet updates, while incoming asynchronous status broadcasts from the earbuds update the UI instantly.

### 2.5 Cloud & Passport Layer (`core.account`)
- Authenticates against Xiaomi Passport services using service ID `miotstore`.
- Supports OAuth web login, username/password API authentication, and QR code token exchange.
- Synchronizes user paired devices list from Xiaomi Cloud API.

### 2.6 User Interface (`ui`)
- Implemented entirely in Jetpack Compose Desktop / Compose Multiplatform.
- Strictly adheres to the MIUI / HyperOS design language:
  - Exact spacing (8dp, 12dp, 16dp, 20dp, 24dp, 27dp).
  - Exact corner radii (180dp pills for buttons, 24dp for surface cards, 12dp/16dp for items).
  - Official Xiaomi font sizes (28sp titles, 16sp buttons/cards, 14sp body/hints, 12sp captions).
  - Zero hardcoded user-facing strings; all texts are localized through official resource bundles (`strings_*.json`).

---

## 3. Automated Parity Test Suite

To guarantee that the implementation never diverges from the official Android APK:
- `DecompiledUiAndFunctionCoverageTest`: Asserts 100% coverage of official UI fragments, preferences, and 68 hardware features.
- `ProtocolTests`: Verifies packet framing, opcode serialization, checksum calculation, and state conversions.
- `ResourceAndStringParityTest`: Guarantees zero hardcoded strings and 1:1 parity with the 25 official Android localized resource bundles.
- `DeviceProfilesParityTest`: Validates the complete catalogue of 78 Xiaomi, Redmi, and POCO earbuds profiles against APK definitions.
