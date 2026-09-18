# Xiaomi Earbuds Communication Protocol Specification

## 1. Frame Structure (RCSP Framing)

All binary communication between the host and the earbuds follows the JieLi / Bestechnic RCSP packet framing extracted from `com.mi.earphone.control`:

```
+---------------+---------------+---------------+---------------+---------------+---------------+---------------+
| Magic (2B)    | Seq (1B)      | OpCode (1B)   | Length (2B)   | Payload (NB)  | Checksum (2B) | Delimiter (1B)|
| 0xBA 0xDC     | 0x00..0xFF    | 0x01..0xFF    | Big-Endian    | Data bytes    | CRC-16/Sum    | 0xED          |
+---------------+---------------+---------------+---------------+---------------+---------------+---------------+
```

- **Magic Prefix**: `0xBA 0xDC` (2 bytes) identifies the start of an authentic Xiaomi control packet.
- **Sequence Number**: `0x00` to `0xFF` (1 byte), incremented with each packet sent. Responses echo the request's sequence number.
- **OpCode**: `0x01` to `0xFF` (1 byte) defines the command or notification type.
- **Length**: 16-bit unsigned integer (big-endian) indicating the byte length of the payload.
- **Payload**: Command parameters or TLV data elements.
- **Checksum / CRC**: CRC-16-CCITT or modular checksum calculated over `[Seq + OpCode + Length + Payload]`.
- **End Delimiter**: Optional `0xED` frame terminator.

---

## 2. Comprehensive OpCode Catalog

| OpCode | Hex | Command Name | Direction | Description |
|---|---|---|---|---|
| 1 | `0x01` | `CMD_GET_DEV_INFO` | Host -> Device | Queries hardware version, model ID, firmware revision, and capability bits. |
| 2 | `0x02` | `CMD_GET_DEV_STATUS` | Bidirectional | Reads or notifies battery levels (Left, Right, Case) and charging status. |
| 3 | `0x03` | `CMD_SET_ANC_MODE` | Host -> Device | Sets Active Noise Cancellation mode and intensity level. |
| 4 | `0x04` | `CMD_SET_EQ` | Host -> Device | Sets audio EQ preset or custom 10-band studio frequency gains. |
| 5 | `0x05` | `CMD_SET_GESTURES` | Host -> Device | Configures touch/pinch gesture actions (tap, double-tap, triple-tap, press). |
| 6 | `0x06` | `CMD_FIND_EARBUDS` | Host -> Device | Triggers acoustic buzzer beeps on left, right, or both earbuds. |
| 7 | `0x07` | `CMD_LOW_LATENCY` | Host -> Device | Toggles low-latency / gaming audio mode (reduces latency to ~50ms). |
| 8 | `0x08` | `CMD_IN_EAR_DETECTION`| Host -> Device | Enables or disables capacitive smart wear detection (auto-pause). |
| 9 | `0x09` | `CMD_DUAL_CONNECT` | Host -> Device | Manages multi-point simultaneous dual device connectivity and priorities. |
| 18| `0x12` | `CMD_SPATIAL_AUDIO`| Host -> Device | Toggles spatial audio, fixed 3D soundstage, and dynamic head tracking. |
| 32| `0x20` | `CMD_OTA_UPDATE` | Host -> Device | Manages firmware upgrade packet chunks, transfers, and hash validation. |
| 128| `0x80`| `CMD_AUTH_REQUEST` | Host -> Device | Requests cryptographic challenge nonce from earbuds. |
| 129| `0x81`| `CMD_AUTH_RESPONSE`| Host -> Device | Sends SAFER+ E21 computed challenge response to authorize session. |

---

## 3. Command Payload Specifications

### 3.1 Noise Control (`0x03: CMD_SET_ANC_MODE`)
As verified from decompiled `NoiseLevelConstants.java`:

- **Mode Byte**:
  - `0x00`: **OFF** (Normal passive listening)
  - `0x01`: **ANC** (Active Noise Cancellation)
  - `0x02`: **TRANSPARENCY** (Pass-through ambient sound)

- **Sub-level Index Byte**:
  - When **Mode = ANC (`0x01`)**:
    - `0`: Mild (`device_settings_noise_reduction_light`)
    - `1`: Deep (`device_settings_noise_reduction_deep`)
    - `2`: Balanced (`device_settings_noise_reduction_balance`)
    - `3`: Adaptive / Smart (`device_settings_noise_reduction_adaptive`)
  - When **Mode = TRANSPARENCY (`0x02`)**:
    - `0`: Standard Transparent (`device_settings_noise_reduction_transparent_all`)
    - `1`: Vocal Enhancement (`device_settings_noise_reduction_transparent_person`)
    - `3`: Vocal Plus (`device_settings_noise_reduction_transparent_person_plus`)

### 3.2 10-Band Graphic Equalizer (`0x04: CMD_SET_EQ`)
Matches `CustomizedEqFragment` and `CustomizedEqView`:

- **Mode Byte**:
  - `0x00`: Preset Mode
  - `0x01`: Custom 10-Band Studio Mode

- **Custom Mode Payload**:
  - 10 signed 8-bit bytes representing gain in decibels from `-10 dB` (`-10`) to `+10 dB` (`+10`) for the center frequencies:
    `31Hz`, `62Hz`, `125Hz`, `250Hz`, `500Hz`, `1kHz`, `2kHz`, `4kHz`, `8kHz`, `16kHz`.

---

## 4. TLV (Type-Length-Value) Multi-Attribute Queries

Certain commands package multiple attributes into a TLV stream:
```
+-------------+-------------+------------------------+
| Tag (1B)    | Length (1B) | Value (Length bytes)   |
+-------------+-------------+------------------------+
```
Example attributes:
- Tag `0x01`: Left Earbud Battery (`0..100%`) + Charging flag (`0x80` mask)
- Tag `0x02`: Right Earbud Battery (`0..100%`) + Charging flag (`0x80` mask)
- Tag `0x03`: Charging Case Battery (`0..100%`) + Charging flag (`0x80` mask)
- Tag `0x04`: In-Ear Status (Bit 0: Left in-ear, Bit 1: Right in-ear)
