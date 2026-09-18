# Reverse Engineering of Official Xiaomi Native Libraries (.so)

## 1. Overview of Bundled Shared Libraries

Inspection of the official APK (`com.mi.earphone` v1.37.1i, located at `/home/alan/earbuds_decompiled/resources/lib/arm64-v8a/`) reveals 9 compiled ELF shared objects:

| Shared Object (`.so`) | File Size | Stripped | Primary Purpose / Subsystem |
|---|:---:|:---:|---|
| `libxm_bluetooth.so` | 12 KB | Yes | Xiaomi Bluetooth authentication cipher ($E_{21}$, $E_1$, SAFER+) |
| `libaudio_detect.so` | 92 KB | No (with debug info) | Acoustic ear canal sweep detection & HRTF filter coordinator |
| `libphrtf.so` | 370 KB | No (with debug info) | DSP engine for Personalized Spatial Audio & Kiss FFT convolution |
| `libjni_lc3.so` | 2.3 MB | No (with debug info) | Bluetooth SIG LC3 (Low Complexity Communication Codec) for LE Audio |
| `libjlspeex.so` | 132 KB | Yes | JieLi Speex voice stream encoder/decoder for XiaoAI voice assistant |
| `libaivsopus.so` | 304 KB | Yes | Xiaomi AIVS (AI Voice Service) Opus audio codec stream manager |
| `libjni_byopus.so` | 6 KB | Yes | JNI wrapper for BaNyan Opus audio decoding |
| `libopus.so` | 385 KB | Yes | Standard IETF Opus interactive audio codec |
| `libmmkv.so` | 575 KB | Yes | Tencent MMKV high-performance memory-mapped key-value storage |

---

## 2. In-Depth Reverse Engineering: `libxm_bluetooth.so`

`libxm_bluetooth.so` is the core proprietary security library responsible for authenticating with Xiaomi, Redmi, and POCO earbuds.

### 2.1 Dynamic Export Table
```
000000000000112c T function_E1test
00000000000014a0 T function_E21
0000000000001b98 T function_xiaomi
0000000000001bd4 T JNI_OnLoad
0000000000001d10 T register_xm_bluetooth
```

### 2.2 JNI Method Bindings (`com/xiaomi/aivsbluetoothsdk/impl/BluetoothAuth`)
During `JNI_OnLoad`, `register_xm_bluetooth` dynamically binds 6 native methods via `(*env)->RegisterNatives`:

| JNI Native Method | Java Signature | Native Implementation Offset | Behavior & Description |
|---|---|:---:|---|
| `nativeInit()` | `()Z` | `0x1d74` | Caches Java VM pointer, creates global reference to `BluetoothAuth` instance. |
| `setLinkKey(byte[] key)` | `([B)I` | `0x2034` | Validates `key.length == 16`, stores into global memory at `0xa6b0`. Returns `0` (success) or `3` (invalid length). |
| `getRandomAuthData()` | `()[B` | `0x1e20` | Invokes libc `rand()` 16 times to generate a 16-byte random challenge. |
| `getRandomAuthCheckData()` | `()[B` | `0x1f2c` | Generates a 17-byte buffer with leading byte `0x00` followed by 16 random bytes. |
| `getEncryptedAuthData(byte[] rand)` | `([B)[B` | `0x20cc` | Calls `function_xiaomi(DUMMY_BD_ADDR, rand, linkKey, out)` and prepends `0x01` byte. Returns 17 bytes. |
| `getEncryptedAuthCheckData(byte[] rand)` | `([B)[B` | `0x2200` | Calls `function_xiaomi(DUMMY_BD_ADDR, rand, linkKey, out)`. Returns 16 bytes. |

- **Default MAC Address**: Hardcoded at `.data` offset `0xa6c0` (`0x26c0` in file):
  `11 22 33 33 22 11` (6 bytes: `0x11, 0x22, 0x33, 0x33, 0x22, 0x11`).

---

### 2.3 SAFER+ Block Cipher Engine (`function_E21` at `0x14a0`)
The cipher implements the 16-round SAFER+ block cipher:
1. **Key Schedule Expansion (`0x1630`)**:
   - Takes a 16-byte key and computes a 17th parity byte: `parity = key[0] ^ key[1] ^ ... ^ key[15]`.
   - Expands the 17 bytes into 17 round subkeys of 16 bytes each (272 bytes total, allocated via `malloc(0x110)`).
   - Each round rotates all 17 bytes left by 3 bits (`ROL3: (b << 3) | (b >> 5)`), and adds bias table values modulo 256.
   - For `E21`, the input key has byte 15 perturbed by XOR: `key[15] ^= 0x06`.
2. **Cipher Transformation (`0x176c`)**:
   - 8 double-rounds (16 rounds total).
   - In round 2 (`r == 2`), when mode flag `isE21 == true` (`w2 == 1`), the original input block is mixed back into the working state (`state[i] = (state[i] + orig[i]) mod 256` or XOR).
   - Non-linear layer using substitution tables `EXP_TABLE` ($45^x \pmod{257}$) and `LOG_TABLE` ($\log_{45}(x)$).
   - Linear diffusion using 2-point Pseudo-Hadamard Transform (PHT) layers (`mul2Add(a, b) = (2a + b, a + b)`).

---

### 2.4 Mutual Authentication Algorithm (`function_E1test` / `function_xiaomi` at `0x112c`)
The full two-way authentication handshake executes a multi-stage cryptographic pipeline:

```
[Challenge Nonce (16B)] -----> [Pass 1: SAFER+ (isE21 = false)]
                                            |
                                            v (Pass 1 Output)
                                            |
[BD_ADDR (6B)] ---------------> [Intermediate Mix: (pass1 ^ rand + bd_addr) mod 256]
                                            |
                                            v
[Link Key (16B)] -------------> [Key2 Diversification: Constants Vector]
                                            |
                                            v (Key 2)
                                            |
[Mixed Block (16B)] ----------> [Pass 2: SAFER+ (isE21 = true)] -----> [Final 16B Token]
```

#### Step-by-Step Mathematical Formulation:
1. **Pass 1**: The 16-byte challenge `rand` is encrypted with `linkKey` using standard SAFER+ (`isE21 = false`).
2. **Intermediate Mixing**:
   $$\text{intermediate}[i] = ((\text{pass1}[i] \oplus \text{rand}[i]) + \text{bdAddr}[i \pmod 6]) \pmod{256}$$
3. **Key Diversification**:
   A secondary 16-byte key `key2` is derived from `linkKey` using fixed arithmetic and bitwise offsets:
   $$\begin{aligned}
   \text{key2}[0] &= (\text{linkKey}[0] - \text{0x17}) \pmod{256} \\
   \text{key2}[1] &= \text{linkKey}[1] \oplus \text{0xe5} \\
   \text{key2}[2] &= (\text{linkKey}[2] - \text{0x21}) \pmod{256} \\
   \text{key2}[3] &= \text{linkKey}[3] \oplus \text{0xc1} \\
   \text{key2}[4] &= (\text{linkKey}[4] - \text{0x4d}) \pmod{256} \\
   \text{key2}[5] &= \text{linkKey}[5] \oplus \text{0xa7} \\
   \text{key2}[6] &= (\text{linkKey}[6] - \text{0x6b}) \pmod{256} \\
   \text{key2}[7] &= \text{linkKey}[7] \oplus \text{0x83} \\
   \text{key2}[8] &= \text{linkKey}[8] \oplus \text{0xe9} \\
   \text{key2}[9] &= (\text{linkKey}[9] - \text{0x1b}) \pmod{256} \\
   \text{key2}[10] &= \text{linkKey}[10] \oplus \text{0xdf} \\
   \text{key2}[11] &= (\text{linkKey}[11] - \text{0x3f}) \pmod{256} \\
   \text{key2}[12] &= \text{linkKey}[12] \oplus \text{0xb3} \\
   \text{key2}[13] &= (\text{linkKey}[13] - \text{0x59}) \pmod{256} \\
   \text{key2}[14] &= \text{linkKey}[14] \oplus \text{0x95} \\
   \text{key2}[15] &= (\text{linkKey}[15] - \text{0x7d}) \pmod{256}
   \end{aligned}$$
4. **Pass 2**: The intermediate block is encrypted with `key2` using SAFER+ in $E_{21}$ mode (`isE21 = true`).
5. **Output**: The resulting 16-byte buffer is returned to authorize the connection.

---

## 3. Reverse Engineering: `libaudio_detect.so` & `libphrtf.so`

These libraries manage Xiaomi's **Personalized Spatial Audio (HRTF) and Ear Canal Fit Test**.

### 3.1 `libaudio_detect.so` JNI Interface
- `Java_com_mi_audio_phrtf_AudioDetect_nativeInit`
- `Java_com_mi_audio_phrtf_AudioDetect_nativeDetect(pcm1, pcm2, pcm3, pcm4, pcm5, pcm6)`:
  - Takes 6 PCM audio streams captured during ear canal acoustic sweep chirps.
  - Returns `111` when all 3 calibration coordinates (left ear, right ear, center reference) succeed.
  - Returns failure error codes indicating which earbud needs repositioning.
- `Java_com_mi_audio_phrtf_AudioDetect_getHrtfData44k()`: Returns $M \times N$ integer matrix of FIR filter coefficients for 44.1 kHz audio.
- `Java_com_mi_audio_phrtf_AudioDetect_getHrtfData48k()`: Returns $M \times N$ integer matrix of FIR filter coefficients for 48 kHz audio.
- `Java_com_mi_audio_phrtf_AudioDetect_getInputShift44k()` / `getInputShift48k()`: Bit shift scaling factors.

### 3.2 `libphrtf.so` DSP Engine
Contains the mathematical signal processing pipeline:
- **Chirp Generation**: Inverted linear sweep (`invlinearSweep`) and logarithmic sweep (`invlogSweep`).
- **Convolution**: Fast Uniform-Partitioned Overlap-Save convolution (`fast_conv_upols`).
- **Spectral Analysis**: Kiss FFT real and complex transforms (`kiss_fftr`, `kiss_fftri`) with Hann windowing (`hannWindow`).
- **Smoothing**: Mel-scale octave spectral smoothing (`meloctsmooth`) and Welch power spectral density estimation (`mypwelch`).
- **Filtering**: Biquad Direct Form II Transposed IIR digital filters (`biquad_df2t`).

---

## 4. Reverse Engineering: `libjni_lc3.so`

Bluetooth SIG standard Low Complexity Communication Codec (LC3) implementation used for Bluetooth LE Audio:
- Frame durations supported: `7.5 ms` and `10.0 ms`.
- Sampling rates supported: `8 kHz`, `16 kHz`, `24 kHz`, `32 kHz`, `44.1 kHz`, `48 kHz`.
- JNI Entry Points:
  - `Java_com_mi_audio_lc3codec_LC3Encoder_createEncoder`
  - `Java_com_mi_audio_lc3codec_LC3Encoder_encode`
  - `Java_com_mi_audio_lc3codec_LC3Decoder_createDecoder`
  - `Java_com_mi_audio_lc3codec_LC3Decoder_decode`

---

## 5. Reverse Engineering: `libjlspeex.so` & `libaivsopus.so`

These libraries handle voice streaming for Xiaomi's AI assistant (XiaoAI / Voice Translation):
- **`libjlspeex.so`**: JieLi proprietary Speex voice codec. Used on low-power SOCs (e.g. JL7006 on Redmi Buds 4 Active) where Bluetooth bandwidth is limited. Operates at narrow-band (8 kHz) and wide-band (16 kHz).
- **`libaivsopus.so`**: Xiaomi AIVS streaming Opus codec. Encapsulates audio frames for upstream transmission to Xiaomi cloud speech recognition servers.
