# Reverse Engineering Xiaomi AIVS Opus Codecs (`libaivsopus.so` & `libjni_byopus.so`) - 100% Complete Specification

## 1. Executive Summary & Binary Identification

The Xiaomi Earbuds companion application (`com.mi.earphone` v1.37.1i) bundles two specialized native libraries implementing the Opus interactive speech audio codec for voice streaming with the **XiaoAI Voice Assistant (AIVS - AI Voice Service)** and **Voice Translation**:

| Library | File Size | Memory Footprint | Architecture | Stripped | Dependencies | Subsystem Role |
|---|:---:|:---:|:---:|:---:|---|---|
| `libaivsopus.so` | 304 KB | 321,508 B | AArch64 (ELF 64-bit) | Yes | `liblog`, `libandroid`, `libc`, `libm`, `libdl` | Self-contained Opus codec + Xiaomi AIVS streaming protocol engine (`OpusManager`) |
| `libjni_byopus.so` | 6 KB | 18,366 B | AArch64 (ELF 64-bit) | Yes | `libopus.so`, `liblog`, `libc`, `libm`, `libdl` | Thin JNI bridge for BaNyan Opus decoding (`OpusDecoder` / `OpusDecoderManager`) |
| `libopus.so` | 385 KB | 398,000 B | AArch64 (ELF 64-bit) | Yes | `libc`, `libm`, `libdl` | Official standard IETF RFC 6716 Opus audio codec shared library |

> [!NOTE]
> `libjni_byopus.so` delegates all decoding directly to `libopus.so`. In contrast, `libaivsopus.so` statically embeds its own optimized build of the Opus codec alongside Xiaomi's proprietary Bluetooth SPP streaming protocol and frame synchronization engine.

---

## 2. Reverse Engineering `libjni_byopus.so` (BaNyan Opus Decoder)

### 2.1 Java Class Binding
* **Class**: `com.banya.opus.OpusDecoder`
* **Used by**: `com.mi.earphone.audio.codec.OpusDecoderManager` & `OpusDecoderHelper`
* **Logging Tag**: `"jni_MixSdk"`

### 2.2 Complete Native Function Exports & Decompilation

`libjni_byopus.so` exports exactly 4 JNI entry points:

#### 1. `Java_com_banya_opus_OpusDecoder_createDecoder` (Address `0x00100a68`)
```c
jint Java_com_banya_opus_OpusDecoder_createDecoder(JNIEnv *env, jobject thiz, jint sampleRate, jint channels) {
    if (DAT_00109040 != NULL) {
        __android_log_print(ANDROID_LOG_ERROR, "jni_MixSdk", "exceed max decoders count!!! %d", 1);
        return -1;
    }
    int error = 0;
    OpusDecoder *decoder = opus_decoder_create(sampleRate, channels, &error);
    if (decoder != NULL && error == 0) {
        DAT_00109040 = decoder;
        return 0; // handle index 0
    }
    __android_log_print(ANDROID_LOG_ERROR, "jni_MixSdk", "failed to create decoder: %d", error);
    return -1;
}
```
* **Behavior**: Enforces a single global decoder instance. Allocates `opus_decoder_create(16000, 1, &err)`. Returns `0` on success.

#### 2. `Java_com_banya_opus_OpusDecoder_decode` (Address `0x00100b1c`)
```c
jint Java_com_banya_opus_OpusDecoder_decode(
    JNIEnv *env, jobject thiz, jint handleIndex, 
    jbyteArray inBytes, jint inBytesLen, jint sampleCount, 
    jbyteArray outBytes, jint outBytesLen
) {
    OpusDecoder *decoder = DAT_00109040;
    __android_log_print(ANDROID_LOG_ERROR, "jni_MixSdk", 
                        "index:%d, len:%d, sampleCount:%d, outputSize:%d",
                        handleIndex, inBytesLen, sampleCount, outBytesLen);

    void *inBuf = malloc(inBytesLen);
    void *outBuf = malloc(outBytesLen);
    (*env)->GetByteArrayRegion(env, inBytes, 0, inBytesLen, inBuf);

    int decodedSamples = opus_decode(decoder, inBuf, inBytesLen, (opus_int16 *)outBuf, sampleCount, 0);
    if (decodedSamples != sampleCount) {
        __android_log_print(ANDROID_LOG_ERROR, "jni_MixSdk", "failed to decode! num=%d", decodedSamples);
        free(inBuf); free(outBuf);
        return -1;
    }

    (*env)->SetByteArrayRegion(env, outBytes, 0, outBytesLen, outBuf);
    free(inBuf); free(outBuf);
    return 0;
}
```
* **Behavior**: Reads compressed Opus packet, invokes `opus_decode(..., decode_fec = 0)`, verifies that decoded samples equal `sampleCount` (320 samples), and writes 640 bytes of 16-bit linear PCM into `outBytes`.

#### 3. `Java_com_banya_opus_OpusDecoder_destroyDecoder` (Address `0x00100c98`)
```c
void Java_com_banya_opus_OpusDecoder_destroyDecoder(JNIEnv *env, jobject thiz, jint handleIndex) {
    if (DAT_00109040 != NULL) {
        opus_decoder_destroy(DAT_00109040);
        DAT_00109040 = NULL;
    }
}
```

#### 4. `JNI_OnLoad` (Address `0x00100cc8`)
* Returns `JNI_VERSION_1_6` (`0x10006`).

### 2.3 Xiaomi Operating Parameters in `OpusDecoderManager`
Decompilation of `com.mi.earphone.audio.codec.OpusDecoderManager` reveals the exact audio profile:
* `sampleRate`: **16,000 Hz** (16 kHz Wideband speech)
* `channels`: **1** (Mono)
* `pcmFrameSize`: **640 bytes** (320 samples $\times$ 2 bytes = 20 ms audio)
* `opusPacketSize`: **40 bytes** (CBR compressed packet size at 16 kbps)
* **Expansion Factor**: $640 / 40 = \mathbf{16\times}$ decompression ratio.

---

## 3. Reverse Engineering `libaivsopus.so` (Xiaomi AIVS Opus Engine)

### 3.1 JNI Registration Table
During `JNI_OnLoad` (address `0x0010aaf8`), `libaivsopus.so` dynamically registers **5 native methods** on class `com/xiaomi/aivsbluetoothsdk/voice/OpusManager` from the table at `0x00153640`:

| # | JNI Method Name | Signature | Native Address | Role & Operation |
|:---:|---|---|:---:|---|
| 1 | `initNativeID` | `()Z` | `0x0010ac04` | Allocates 10 KB circular ring buffer, initializes POSIX semaphore, caches Java callback ID. |
| 2 | `saveAudioSteam` | `([B)V` | `0x0010b7b0` | Receives Bluetooth SPP RFCOMM voice packets from earbuds, pushes to ring buffer, signals semaphore. |
| 3 | `decodeAudioStream` | `(I)V` | `0x0010b430` | Streaming decoding loop: searches sync header, extracts frames, decodes to PCM, invokes Java callback. |
| 4 | `encodeAudioFile` | `(Ljava/lang/String;Ljava/lang/String;)I` | `0x0010ad14` | Compresses a 16 kHz 16-bit WAV audio file into an AIVS-framed Opus file. |
| 5 | `decodeAudioFile` | `(Ljava/lang/String;Ljava/lang/String;)I` | `0x0010afc0` | Decompresses an AIVS-framed Opus file back into a standard 16-bit PCM WAV file. |

---

### 3.2 AIVS Streaming Protocol & Packet Layout

Inspection of `decodeAudioStream` (`0x0010b430`) and `decodeAudioFile` (`0x0010afc0`) exposes the **AIVS framing protocol**:

```
+-----------------------------------+--------------------+-------------------------------------+
| Sync Magic Header (4 Bytes)       | Frame Length (2B)  | Opus Compressed Payload (N Bytes)   |
| 0xAA  0xEA  0xBD  0xAC            | uint16_t (LE)      | Raw Opus Voice Frame (1..400 Bytes) |
+-----------------------------------+--------------------+-------------------------------------+
```

1. **4-Byte Magic Header**: `0xAA, 0xEA, 0xBD, 0xAC` (in assembly: `cmp w24, #0xaa; cmp w22, #0xea; cmp w21, #0xbd; cmp w20, #0xac`).
2. **2-Byte Frame Length**: 16-bit unsigned integer in little-endian.
3. **Validation Bound**: `frame_len < 401` (`0x191`). Any larger value triggers `"frame_len over limit"` and initiates resynchronization.
4. **Opus Payload**:
   - For real-time streaming voice: typically **40 bytes** (20 ms at 16 kbps) or up to 64 bytes (25 kbps).

---

### 3.3 Decompiled Implementations

#### 1. `initNativeID()` (`0x0010ac04`)
- Caches global reference to `OpusManager` instance in `DAT_00155f60`.
- Obtains method ID for callback: `onDecodeStreamReceive(I[B)V` (`DAT_00155f68`).
- Allocates circular ring buffer of **10,240 bytes** (`0x2800`) at `DAT_00153700` / `DAT_00153758`.
- Initializes POSIX semaphore: `sem_init(&DAT_00153738, 0, 0)`.

#### 2. `saveAudioSteam(byte[] bArr)` (`0x0010b7b0`)
- Validates non-null array and length.
- Writes incoming bytes into the 10 KB circular buffer.
- Calls `sem_post(&DAT_00153738)` to wake the decoder thread.
- Logs: `[jni_opus] sem notify......`

#### 3. `decodeAudioStream(int status)` (`0x0010b430`)
- When `status == 1`:
  - Creates Opus decoder: `opus_decoder_create(16000, 1, &err)`.
  - Enters streaming extraction loop:
    1. Reads 4 bytes from circular buffer, checks against `0xAA, 0xEA, 0xBD, 0xAC`.
    2. If mismatch, discards 1 byte and continues scanning (`"seeking_header"`).
    3. Reads 2-byte length `frame_len`.
    4. Blocks on `sem_wait(&DAT_00153738)` if circular buffer has fewer than `frame_len` bytes.
    5. Reads `frame_len` bytes into stack buffer.
    6. Calls `opus_decode(decoder, opusData, frame_len, pcmOut, 1920, 0)`.
    7. On success, calls `FUN_0010a994`:
       - Allocates Java byte array via `NewByteArray(env, pcmBytes)`.
       - Copies PCM samples via `SetByteArrayRegion`.
       - Calls Java callback `OpusManager.onDecodeStreamReceive(pcmBytes, pcmArray)`.
- When `status == 0`:
  - Terminates loop, calls `sem_destroy(&DAT_00153738)` and `opus_decoder_destroy(decoder)`.

#### 4. `encodeAudioFile(String wavPath, String opusPath)` (`0x0010ad14`)
- Opens input WAV and output Opus files via `fopen()`.
- Creates Opus encoder:
  `opus_encoder_create(16000, 1, OPUS_APPLICATION_RESTRICTED_LOWDELAY (2051), &err)`
- Applies optimal speech encoder parameters:
  - `OPUS_SET_COMPLEXITY(0)`: minimal CPU overhead for mobile/embedded.
  - `OPUS_SET_BITRATE(25000)`: 25 kbps speech encoding.
  - `OPUS_SET_SIGNAL(OPUS_SIGNAL_VOICE)` (`3001`): voice-optimized tuning.
  - `OPUS_SET_VBR(0)`: constant bitrate (CBR) streaming.
- Reads input PCM in chunks of **640 bytes** (320 samples = 20 ms).
- Packs each Opus packet with `0xAA 0xEA 0xBD 0xAC` sync header and 2-byte length.
- Writes framed stream to disk via `fwrite()`.

#### 5. `decodeAudioFile(String opusPath, String wavPath)` (`0x0010afc0`)
- Scans Opus file for `0xAA 0xEA 0xBD 0xAC` sync markers.
- Reads 2-byte length, decodes 20 ms Opus packets via `opus_decode(..., 1920, 0)`.
- Writes standard 44-byte RIFF/WAVE header followed by decoded 16-bit linear PCM audio.

---

## 4. Reconstructed Native C++ Sources (`native/opus/`)

In adherence to the original architecture of the official application, these libraries are native C++ shared objects and are maintained purely in C++:

* **`libjni_byopus.so` C++ Source**: [libjni_byopus.cpp](file:///home/alan/ximiearbuds/native/opus/libjni_byopus.cpp)
* **`libaivsopus.so` C++ Source**: [libaivsopus.cpp](file:///home/alan/ximiearbuds/native/opus/libaivsopus.cpp)
* **Native Build System**: [Makefile](file:///home/alan/ximiearbuds/native/Makefile)

### Native Architecture:
1. `RingBuffer`: Exact 10 KB circular ring buffer (`0x2800` bytes) with mutex protection matching `libaivsopus.so`.
2. AIVS Protocol Parser: Synchronization on `0xAA 0xEA 0xBD 0xAC` 4-byte magic headers and 16-bit little-endian frame lengths with automatic stream resynchronization.
3. Thread Synchronization: POSIX semaphore (`sem_t`) signaling when audio chunks arrive from Bluetooth SPP.
4. JNI Bindings:
   - Dynamic registration of 5 methods via `RegisterNatives` during `JNI_OnLoad` for `OpusManager`.
   - Exported JNI methods for `OpusDecoder`.

