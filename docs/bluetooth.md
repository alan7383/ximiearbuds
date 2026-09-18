# Xiaomi Earbuds Bluetooth Transport & Connectivity Specification

## 1. Physical & Link-Layer Mechanics

Analysis of the official Android package (`com.mi.earphone`) reveals that Xiaomi, Redmi, and POCO earbuds utilize a dual-mode communication pipeline:

```
+-----------------------------------------------------------------------------+
|                             Operating System                                |
|                                                                             |
|   +--------------------------+          +-------------------------------+   |
|   |   Audio Subsystem        |          |  XimiEarbuds Control Channel  |   |
|   |   (PipeWire / PulseAudio)|          |  (BlueZ RFCOMM / Winsock2)    |   |
|   +--------------------------+          +-------------------------------+   |
|                 |                                       |                   |
|          A2DP / HFP Profile                      RFCOMM (SPP) Channel 24    |
|                 \                                       /                   |
|                  \                                     /                    |
|                   +-----------------------------------+                     |
|                   |   Physical Bluetooth Controller   |                     |
|                   |          (BR/EDR Classic)         |                     |
|                   +-----------------------------------+                     |
+-----------------------------------------------------------------------------+
                                      |
                               Over-The-Air
                                      v
+-----------------------------------------------------------------------------+
|                            Xiaomi Earbuds Firmware                          |
|   (Bestechnic BES2600 / JieLi JL7006 / Airoha AB1565 Dual-Core SOC)         |
+-----------------------------------------------------------------------------+
```

### 1.1 Audio vs Control Demultiplexing
- **Audio Profile (A2DP / HFP)**: High-resolution media streaming (AAC, LHDC 5.0, LC3, SBC) is managed directly by the host operating system's audio stack (PipeWire or PulseAudio on Linux; Windows Core Audio on Windows).
- **Control Channel (RFCOMM / SPP)**: Commands (ANC switching, 10-band EQ adjustments, touch gestures, firmware telemetry, battery queries) are communicated over a dedicated Classic Bluetooth RFCOMM stream.

---

## 2. Linux Implementation (`LinuxRfcommTransport`)

On Linux, raw RFCOMM connections interface with the kernel BlueZ subsystem via POSIX socket calls using JNA (`net.java.dev.jna`):

```kotlin
val fd = socket(AF_BLUETOOTH, SOCK_STREAM, BTPROTO_RFCOMM)
```

### 2.1 Security Requirements (`BT_SECURITY_MEDIUM`)
Earbuds firmware running recent firmware refuses unauthenticated RFCOMM channels. The socket option `SOL_BLUETOOTH = 274` with level `BT_SECURITY = 4` must be explicitly configured prior to connection:

```c
struct bt_security {
    uint8_t level; // BT_SECURITY_MEDIUM (2) or BT_SECURITY_HIGH (3)
    uint8_t key_size;
};
setsockopt(fd, SOL_BLUETOOTH, BT_SECURITY, &opt, sizeof(opt));
```
Without `BT_SECURITY_MEDIUM`, the kernel BlueZ socket will receive `ECONNREFUSED (111)` or `EACCES (13)`.

### 2.2 Channel Selection & Discovery
- **Default RCSP Channel**: Most JieLi and BES based Xiaomi models (such as Redmi Buds 5 Pro, Buds 4 Pro, Xiaomi Buds 4) listen on RFCOMM channel `24`.
- **SDP Query**: If channel 24 does not respond, a dynamic SDP probe iterates through standard SerialPort UUIDs (`00001101-0000-1000-8000-00805f9b34fb`) to detect the assigned RFCOMM server channel.

### 2.3 Kernel Socket Recycling & DLC Timeout
When an RFCOMM connection is closed or interrupted, the Linux kernel keeps the underlying Data Link Connection (DLC) in a teardown state for up to 3000ms. Immediate reconnections will fail with `EBUSY (16)`.
`LinuxRfcommTransport` enforces a minimum 3500ms cooldown window before re-establishing sockets to ensure clean kernel recycling.

---

## 3. Windows Implementation (`WindowsRfcommTransport`)

On Windows, the transport utilizes Winsock2 Bluetooth sockets:
- Address family: `AF_BTH (32)`.
- Protocol: `BTHPROTO_RFCOMM (3)`.
- Socket type: `SOCK_STREAM (1)`.
- Target addressing: Uses `SOCKADDR_BTH` structure with the 48-bit Bluetooth MAC address converted from hexadecimal.

---

## 4. Bluetooth Low Energy (BLE) Fallback

For devices that utilize BLE for commands or pairing discovery:
- **Service UUID**: `0000fe95-0000-1000-8000-00805f9b34fb` (Official Xiaomi Inc. SIG-assigned UUID).
- **Control Characteristic**: `00000001-0000-1000-8000-00805f9b34fb` (Write / Write Without Response).
- **Status Notification Characteristic**: `00000002-0000-1000-8000-00805f9b34fb` (Notify / Indicate).
