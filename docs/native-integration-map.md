# Cartographie d'intégration des bibliothèques natives

## Vue d'ensemble : qui utilise quoi dans l'app

Chaque `.so` est chargé par une classe Java wrapper spécifique, elle-même appelée par des managers de plus haut niveau. Voici la hiérarchie complète.

---

## 1. `libxm_bluetooth.so` — Authentification Bluetooth (SAFER+/E1/E21)

### Classe wrapper
`com.xiaomi.aivsbluetoothsdk.impl.BluetoothAuth`

```java
static { System.loadLibrary("xm_bluetooth"); }

native boolean  nativeInit();
native byte[]   getRandomAuthData();
native byte[]   getRandomAuthCheckData();
native int      getEncryptedAuthCheckData(byte[] key);  // stocke la clé
native byte[]   getEncryptedAuthData(byte[] randData);
native byte[]   setLinkKey(byte[] keyData);
```

### Utilisateurs directs
| Classe | Package | Rôle |
|--------|---------|------|
| `e7.f` (`BluetoothEngineImpl`) | `e7` | Moteur Bluetooth principal — orchestre tout le protocole d'auth |

### Protocole d'authentification (extrait de `e7.f`)

```
Phase 0 (TARGET) : Appareil envoie son défi
  → getRandomAuthCheckData()     // 16 octets aléatoires
  → getEncryptedAuthCheckData()  // réponse chiffrée SAFER+
  → envoie [check_rand || encrypted_check] à l'écouteur

Phase 1 (SELF) : Appareil répond au défi de l'écouteur
  → getEncryptedAuthData(bArr)   // bArr[0]==0, bArr[1..16] = rand de l'écouteur
  → envoie la réponse chiffrée

Phase 2 (OK) : Vérification finale
  → compare la réponse de l'écouteur avec sa propre valeur calculée
  → si OK → démarre le flux de données (BLE ou SPP)
  → sinon → auth fail (code 4100)

SPP uniquement :
  → setLinkKey() via n(byte[]) dans BluetoothAuth
```

---

## 2. `libmmkv.so` — Stockage clé-valeur haute performance

### Classe wrapper
`com.tencent.mmkv.MMKV`

```java
static { System.loadLibrary("mmkv"); }
// 40+ méthodes JNI (getString, putString, getInt, putInt, ...)
```

### Utilisateurs directs
| Classe | Package | Rôle |
|--------|---------|------|
| `PreferenceSupport` | `com.xiaomi.fitness.cache.sp` | Préférences fitness avec fallback MMKV/SharedPrefs |
| `SpExtKt` | `com.xiaomi.fitness.cache.sp` | Extensions Kotlin pour MMKV |
| `AppBootComponent` | `com.xiaomi.fitness.app` | Initialisation MMKV au démarrage de l'app |
| `MMKVContentProvider` | `com.tencent.mmkv` | Provider multi-process MMKV |

### Initialisation (dans `AppBootComponent`)
```kotlin
MMKV.initialize(context, mmkvDir)
```

### Données stockées (exemples extraits du code)
- Préférences utilisateur fitness (pas/calories/distance)
- État des écouteurs (volume, EQ, mode ANC)
- Tokens et sessions d'authentification
- Cache des paramètres de l'appareil

---

## 3. `libaivsopus.so` — Codec Opus AIVS (streaming voix XiaoAI)

### Classe wrapper
`com.xiaomi.aivsbluetoothsdk.voice.OpusManager`

```java
System.loadLibrary("aivsopus");
native boolean initNativeID();
native int     encodeAudioFile(String src, String dst);
native int     decodeAudioFile(String src, String dst);
native void    decodeAudioStream(int sessionId);
native void    saveAudioSteam(byte[] data);
```

### Utilisateurs directs
| Classe | Package | Rôle |
|--------|---------|------|
| `g7.a` (`VoiceManager`) | `g7` | Dispatcher codec audio AIVS — choix Opus/Speex selon le périphérique |

### Flux d'usage (extrait de `g7.a`)
```
VoiceManager.saveStream(byte[])
  → OpusManager.saveStream(byte[])
    → saveAudioSteam(byte[])     // accumule le stream compressé

VoiceManager.decode(String, String)
  → OpusManager.decode(src, dst)
    → decodeAudioFile(src, dst)  // décode un fichier Opus → PCM WAV

VoiceManager.decodeStream(int)
  → OpusManager.decodeStream(sessionId)
    → decodeAudioStream(sessionId) // streaming en temps réel
```

---

## 4. `libjni_byopus.so` — Codec Opus Banya (appels vocaux BT)

### Classe wrapper
`com.banya.opus.OpusDecoder`

```java
static { System.loadLibrary("jni_byopus"); }
native int   createDecoder(int sampleRate, int channels);
native int   decode(int sessionId, byte[] encoded, int encodedLen,
                   int frameSize, byte[] decoded, int decodedLen);
native void  destroyDecoder(int sessionId);
```

### Utilisateurs directs
| Classe | Package | Rôle |
|--------|---------|------|
| `OpusDecoderManager` | `com.mi.earphone.audio.codec` | Gestion du décodeur Opus pour audio Bluetooth classique |
| `OpusDecoderHelper` | `com.mi.earphone.audio.codec` | Thread worker de décodage en temps réel |
| `OpusCodec` | `com.mi.earphone.audio.codec` | Singleton codec Opus exposé à l'app |

### Flux d'usage
```
OpusCodec.startDecoder(config, listener)
  → OpusDecoderHelper.startDecoder(listener)
    → OpusDecoder.createDecoder(sampleRate, channels)

OpusCodec.inputData(byteArray, isEnd, isFirstStream, callStatus)
  → OpusDecoderHelper.write(ByteData)
    → [thread] OpusDecoder.decode(...)  // frame par frame
      → listener(channelId, pcmData, isEnd, callStatus)
```

---

## 5. `libjlspeex.so` — Codec Speex JieLi (voix XiaoAI sur puces JL)

### Classe wrapper
`com.xiaomi.aivsbluetoothsdk.voice.SpeexManager`

```java
System.loadLibrary("jlspeex");
native boolean initNativeID();
native int     encodeAudioFile(String src, String dst);
native int     decodeAudioFile(String src, String dst);
native void    decodeAudioStream(int sessionId);
native void    saveAudioSteam(byte[] data);
```

### Utilisateurs directs
| Classe | Package | Rôle |
|--------|---------|------|
| `g7.a` (`VoiceManager`) | `g7` | Même dispatcher que pour Opus — sélection automatique |

### Sélection du codec (dans `g7.a`)
```java
// VoiceManager choisit Speex ou Opus selon le type de puce de l'écouteur
if (deviceIsSpeex) {
    codec = new SpeexManager(this);   // → libjlspeex.so
} else {
    codec = new OpusManager(this);    // → libaivsopus.so
}
```

---

## 6. `libjni_lc3.so` — Codec LC3 Bluetooth LE Audio

### Classes wrapper
`com.mi.audio.lc3codec.LC3Encoder` + `com.mi.audio.lc3codec.LC3Decoder`

```java
// LC3Encoder
static { System.loadLibrary("jni_lc3"); }
native int    createEncoder(int sampleRate, int frameDuration, int bitrate);
native void   destroyEncoder();
native byte[] encode(byte[] pcmFrame);
native int    sampleOfFrames(int sampleRate, int frameDuration);
native int    encodeFile(String src, String dst);

// LC3Decoder
native int    createDecoder(int frameSize, int sampleRate, int frameDuration, int channels);
native byte[] decode(int sessionId, byte[] lc3Frame);
native void   destroyDecoder(int sessionId);
native int    decodeFile(String src, String dst);
```

### Utilisateurs directs
| Classe | Package | Rôle |
|--------|---------|------|
| `Lc3EncoderHelper` | `com.mi.earphone.audio.codec` | Thread worker d'encodage LC3 en temps réel |
| `Lc3DecoderHelper` | `com.mi.earphone.audio.codec` | Thread worker de décodage LC3 multi-canal |
| `SuperAivsManager` | `com.mi.earphone.device.manager.manager` | Manager AIVS qui démarre l'encodeur LC3 |

### Flux d'usage
```
SuperAivsManager → Lc3EncoderHelper.startEncoder(listener)
                     → LC3Encoder.createEncoder(sampleRate, dur, bitrate)

Données PCM → Lc3EncoderHelper.write(ByteData)
  → [thread] LC3Encoder.encode(pcmFrame) → lc3Frame
    → listener(channelId, lc3Data, callStatus)

Données LC3 → Lc3DecoderHelper.write(ByteData)
  → [thread] LC3Decoder.decode(sessionId, lc3Frame) → pcmFrame
    → listener(channelId, pcmData, isEnd, callStatus)
```

---

## 7. `libphrtf.so` + `libaudio_detect.so` — Calibrage Audio Spatial Personnalisé

### Classes wrapper
`com.mi.audio.phrtf.AudioDetect` (charge `audio_detect`, qui lie `phrtf`)

```java
static { System.loadLibrary("audio_detect"); }
native void      nativeInit();
native int       nativeDetect(byte[] l1, byte[] l2, byte[] r1, byte[] r2, byte[] c1, byte[] c2);
native int[][]   getHrtfData44k();
native int[][]   getHrtfData48k();
native int       getInputShift44k();
native int       getInputShift48k();
```

### Utilisateurs directs
| Classe | Package | Rôle |
|--------|---------|------|
| `PersonalAudioVM` | `com.mi.earphone.settings.ui.spatialaudio` | ViewModel de la page "Audio Spatial Personnalisé" dans les paramètres |

### Flux d'usage complet (HRTF calibration)

```
UI : Paramètres → Audio Spatial → Commencer l'adaptation

PersonalAudioVM.startAdaptation()
  → Joue les balayages audio via MediaPlayer (depuis les 3 angles : L, C, R)
  → Reçoit les enregistrements PCM via BigDataTransfer (protocole BT)
  → Organise les 6 canaux : l1, l2, r1, r2, c1, c2

AudioDetect.nativeDetect(l1, l2, r1, r2, c1, c2)
  → libphrtf.so : hrtfWavCheck() → code 111 si succès
  → libphrtf.so : phrtf_process() → indexStamp 0..16 (profil HRTF optimal)
  → libphrtf.so : allocateAndFill2DArray(72, 25, indexStamp, 44100/48000)

PersonalAudioVM récupère :
  → AudioDetect.getHrtfData44k()   // int[72][25] — filtres biquad 44.1 kHz
  → AudioDetect.getHrtfData48k()   // int[72][25] — filtres biquad 48.0 kHz
  → AudioDetect.getInputShift44k() // facteur d'échelle Q-format
  → AudioDetect.getInputShift48k()

→ Encode les matrices et les envoie à l'écouteur via BigDataTransfer
  (data44k, data48k sous forme byte[] avec isSend44K selon la fréquence)
→ L'écouteur applique les filtres HRTF pour personnaliser le rendu 3D
```

---

## 8. Diagramme d'architecture global

```
App Android (Xiaomi Earbuds)
│
├── Stockage ──────────────── MMKV (libmmkv.so)
│   └── PreferenceSupport, SpExtKt, AppBootComponent
│
├── Authentification BT ───── SAFER+/E1/E21 (libxm_bluetooth.so)
│   └── BluetoothAuth ← BluetoothEngineImpl (e7.f)
│       ├── Phase 0 : défi/réponse cible
│       ├── Phase 1 : réponse au défi de l'écouteur
│       └── Phase 2 : vérification + démarrage flux données
│
├── Voix XiaoAI (AIVS) ───── Speex (libjlspeex.so)
│   └── SpeexManager ← VoiceManager (g7.a)   ← [puces JieLi]
│
├── Voix XiaoAI (AIVS) ───── Opus AIVS (libaivsopus.so)
│   └── OpusManager ← VoiceManager (g7.a)    ← [autres puces]
│
├── Audio Bluetooth ────────── Opus Banya (libjni_byopus.so)
│   └── OpusDecoder ← OpusDecoderHelper ← OpusCodec
│
├── LE Audio (BT 5.2) ──────── LC3 (libjni_lc3.so)
│   └── LC3Encoder/Decoder ← Lc3EncoderHelper/Lc3DecoderHelper ← SuperAivsManager
│
└── Audio Spatial HRTF ──────── libphrtf.so + libaudio_detect.so
    └── AudioDetect ← PersonalAudioVM (Settings UI)
        ├── Joue balayages audio 3D
        ├── Analyse microphones écouteurs
        └── Envoie profil HRTF personnalisé à l'écouteur
```

---

## 9. Points d'intégration dans ton projet reverse

Pour que ton projet `ximiearbuds` soit complet et fonctionnel, voici ce qu'il reste à intégrer au niveau **Java/Kotlin** :

| Priorité | Fichier à reconstruire | Pourquoi |
|----------|----------------------|----------|
| 🔴 Critique | `e7.f` (`BluetoothEngineImpl`) | Orchestre tout le protocole auth + flux données |
| 🔴 Critique | `g7.a` (`VoiceManager`) | Dispatche Opus/Speex pour XiaoAI |
| 🟠 Important | `PersonalAudioVM` | Interface complète du calibrage HRTF |
| 🟡 Utile | `Lc3EncoderHelper/Lc3DecoderHelper` | Pipeline LC3 complet |
| 🟡 Utile | `OpusDecoderHelper` / `OpusCodec` | Pipeline Opus complet |
| 🟢 Déjà clair | `BluetoothAuth`, `AudioDetect`, `MMKV` | Wrappers JNI simples, déjà docs |
