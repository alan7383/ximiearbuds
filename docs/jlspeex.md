# Reverse Engineering: libjlspeex.so (132 Ko)

## 1. Vue d'Ensemble & Rôle dans l'Écosystème Xiaomi

`libjlspeex.so` est le codec vocal Speex propriétaire JieLi (JL / 杰理科技) intégré dans l'application Xiaomi Earbuds pour les flux vocaux **XiaoAI (AIVS - Xiaomi AI Voice Service)** sur les puces basse consommation Bluetooth (SoC JieLi AC69x / AC70x / JL701N).

| Propriété | Valeur |
|---|---|
| **Fichier original** | `/home/alan/earbuds_decompiled/resources/lib/arm64-v8a/libjlspeex.so` |
| **Taille binaire** | 145 079 octets (~132 Ko ELF AArch64) |
| **Classe Java cliente** | `com.xiaomi.aivsbluetoothsdk.voice.SpeexManager` |
| **Gestionnaire audio APK** | `g7.a` (`CodecManager`) |
| **Source C++ reversé** | [`native/jlspeex/libjlspeex.cpp`](file:///home/alan/ximiearbuds/native/jlspeex/libjlspeex.cpp) |
| **Cible de build** | `native/build/libjlspeex.so` via [`native/Makefile`](file:///home/alan/ximiearbuds/native/Makefile) |

Contrairement aux écouteurs haut de gamme équipés de processeurs Qualcomm ou BES (Bestechnic) qui privilégient le codec Opus (`libaivsopus.so`), les écouteurs économiques basés sur des microcontrôleurs JieLi utilisent ce moteur Speex Wideband pour compresser la voix captée par le micro avant transmission par paquets Bluetooth SPP / RFCOMM.

---

## 2. Table des Méthodes Natives JNI (`JNI_OnLoad`)

`libjlspeex.so` enregistre dynamiquement 5 méthodes natives via `env->RegisterNatives()` dans `JNI_OnLoad` (`0x0010996c`) pour la classe `com/xiaomi/aivsbluetoothsdk/voice/SpeexManager` :

| Méthode Java | Signature JNI | Adresse Ghidra | Description |
|---|---|---|---|
| `initNativeID` | `()Z` | `0x00109a5c` | Initialise le buffer circulaire de 10 Ko (`cbuf_t`), le sémaphore POSIX `bin_sem`, et met en cache l'instance globale `SpeexManager` et `onDecodeStreamReceive(I[B)V`. |
| `encodeAudioFile` | `(Ljava/lang/String;Ljava/lang/String;)I` | `0x00109ba8` | Encode un fichier WAV PCM 16 kHz vers le format tramé Speex JieLi (qualité 5, Wideband). |
| `decodeAudioFile` | `(Ljava/lang/String;Ljava/lang/String;)I` | `0x00109d4c` | Décode un fichier Speex tramé JieLi vers un flux PCM linéaire 16 kHz (320 échantillons = 640 octets par trame). |
| `decodeAudioStream` | `(I)V` | `0x0010a158` | Boucle de décodage temps-réel sur flux continu : extrait les trames du buffer circulaire, décode Speex Wideband et appelle `onDecodeStreamReceive(640, byte[])`. |
| `saveAudioSteam` | `([B)V` | `0x0010a568` | Réceptionne les segments audio bruts reçus du Bluetooth SPP (JieLi), les pousse dans le buffer circulaire et notifie le décodeur via `sem_post(&bin_sem)`. |

> [!NOTE]
> La faute de frappe originale `saveAudioSteam` (au lieu de `Stream`) est conservée à l'identique dans le code source C++ et la signature native pour garantir une compatibilité binaire et JNI stricte à 100%.

---

## 3. Protocole de Tramage AIVS Speex (JieLi)

Le flux binaire Speex transmis par les écouteurs JieLi utilise un protocole d'encapsulation par paquets de 5 trames :

```
+--------------------------+-----------------------+-----------------------------+
| Header Magique (4 oct.)  | Longueur Trame (2 o.) | Trame 0 Speex (L octets)    |
| 0xAA 0xEA 0xBD 0xAC      | uint16 little-endian  | (ex: 42 octets, WB Q5)      |
+--------------------------+-----------------------+-----------------------------+
| Trame 1 Speex (L octets) | Trame 2 Speex (L oct) | Trame 3 Speex | Trame 4 Spx |
+--------------------------+-----------------------+---------------+-------------+
```

1. **Octets de synchronisation (Magic)** : `0xAA, 0xEA, 0xBD, 0xAC` (en entier 32 bits little-endian : `0xacbdeaaa`).
   - Identique à la synchronisation AIVS observée dans `libaivsopus.so`.
2. **Taille de trame (`frame_len`)** : Entier non signé 16 bits little-endian (`frame_len < 401 octets`).
3. **Regroupement par 5 trames (`outframe_cnt` modulo 5)** :
   - Chaque paquet Bluetooth transmet un groupe de 5 trames de 20 ms (soit 100 ms d'audio au total).
   - L'en-tête de synchronisation (4 octets magic + 2 octets longueur) n'est envoyé qu'une seule fois au début de chaque paquet de 5 trames.
   - Les trames 1 à 4 sont lues consécutivement sans nouvel en-tête.

---

## 4. Architecture du Buffer Circulaire JieLi (`cbuf_t`)

Pour découpler la réception asynchrone des paquets Bluetooth SPP du thread de décodage audio, la bibliothèque intègre une implémentation optimisée de buffer circulaire ring-buffer (`cbuf_t`) avec support de pré-écriture spéculative et mutex POSIX :

```c
typedef struct {
    uint8_t  *begin;         // 0x00: Début du tampon alloué (cache_cbuf_ptr)
    uint8_t  *end;           // 0x08: Fin du tampon (begin + total_len)
    uint8_t  *read_ptr;      // 0x10: Curseur de lecture engagé
    uint8_t  *write_ptr;     // 0x18: Curseur d'écriture engagé
    uint8_t  *prewrite_ptr;  // 0x20: Curseur de pré-écriture spéculative
    uint32_t  prewrite_len;  // 0x28: Nombre d'octets pré-écrits
    uint32_t  data_len;      // 0x2c: Nombre d'octets disponibles à la lecture
    uint32_t  total_len;     // 0x30: Capacité totale (10 240 octets = 0x2800)
} cbuf_t;
```

### Fonctions du Buffer Circulaire :

- `cbuf_init(cbuf, buf, size)` : Initialise les pointeurs et le mutex `mutex`.
- `cbuf_clear(cbuf)` : Réinitialise les curseurs de lecture et écriture.
- `cbuf_is_write_able(cbuf, len)` : Vérifie si au moins `len` octets libres sont disponibles.
- `cbuf_write(cbuf, src, len)` : Écrit `len` octets avec repliement circulaire (*wrap-around*), met à jour `data_len` et `prewrite_len`.
- `cbuf_read(cbuf, dst, len)` : Lit `len` octets avec repliement circulaire et décrémente `data_len`.
- `cbuf_prewrite(cbuf, src, len)` : Écrit des données dans le buffer sans immédiatement les rendre disponibles à la lecture (spéculation).
- `cbuf_updata_prewrite(cbuf)` : Valide (*commit*) les données pré-écrites en avançant `write_ptr` vers `prewrite_ptr`.
- `cbuf_discard_prewrite(cbuf)` : Annule (*rollback*) les données pré-écrites.
- `cbuf_rewrite(cbuf, target, src, len)` : Écrase directement une zone sans avancer les curseurs.
- `cbuf_read_alloc_len` / `cbuf_read_alloc_len_updata` : Lecture anticipée sans copie (zero-copy peek & commit).

---

## 5. Configuration Speex & Flux de Traitement

### Paramètres du Codec :
- **Mode Speex** : Wideband (`SPEEX_MODEID_WB = 1`, fréquence d'échantillonnage 16 000 Hz).
- **Taille de trame PCM** : 320 échantillons 16 bits par trame (20 ms de voix, 640 octets PCM).
- **Qualité d'encodage** : 5 (avec VBR activé).
- **Amélioration perceptive** : `speex_decoder_ctl(dec, SPEEX_SET_ENH, &enh)` activé (`enh = 1`) pour filtrer les bruits de fond Bluetooth.

### Flux de Décodage en Streaming :
1. Les paquets Bluetooth SPP arrivent via `saveAudioSteam(byte[] bArr)`.
2. `saveAudioSteam` pousse les octets dans `cbuffer` (10 Ko) et déclenche `sem_post(&bin_sem)`.
3. Le thread de décodage `decodeAudioStream(1)` attend sur `sem_wait(&bin_sem)`.
4. Il détecte la balise `0xAA 0xEA 0xBD 0xAC`, lit `frame_len`, puis lit les 5 trames successives.
5. Chaque trame est décodée par `speex_decode_int(dec, &bits, pcm_out)` en 320 échantillons (640 octets).
6. `stream_output` attache le thread à la JVM si nécessaire (`AttachCurrentThread`), alloue un tableau Java `byte[640]` et appelle en callback :
   ```java
   SpeexManager.onDecodeStreamReceive(640, byte[] pcmData);
   ```

---

## 6. Compilation & Vérification Binaire

Le fichier source C++ complet est situé dans [`native/jlspeex/libjlspeex.cpp`](file:///home/alan/ximiearbuds/native/jlspeex/libjlspeex.cpp) et est compilé avec le Makefile natif :

```bash
make -C native
```

### Vérification des Symboles Exportés (`nm -D`) :
```
00000000000072c0 B bin_sem
00000000000072e0 B cache_cbuf_ptr
00000000000030c0 T cbuf_clear
0000000000003560 T cbuf_discard_prewrite
0000000000009ae0 B cbuffer
0000000000002f30 T cbuf_init
0000000000003110 T cbuf_is_write_able
0000000000003450 T cbuf_prewrite
0000000000003350 T cbuf_read
00000000000036b0 T cbuf_read_alloc
0000000000003740 T cbuf_read_alloc_len
00000000000037e0 T cbuf_read_alloc_len_updata
00000000000036e0 T cbuf_read_updata
00000000000035a0 T cbuf_rewrite
0000000000003520 T cbuf_updata_prewrite
0000000000003130 T cbuf_write
0000000000003620 T cbuf_write_alloc
0000000000003660 T cbuf_write_updata
0000000000002510 T check_buf
00000000000038f0 T check_stream_buf
00000000000026e0 T decodeAudioFile
0000000000003f30 T decodeAudioStream
00000000000025a0 T d_input
00000000000038e0 T d_input_stream
0000000000002400 T e_input_data
0000000000002bb0 T encodeAudioFile
0000000000007200 D ENC_OPS_G
0000000000002420 T e_output_data
0000000000007260 B fbits
0000000000007258 B fin
0000000000007250 B fout
0000000000002550 T get_lslen
0000000000004010 T get_speex_enc_obj
0000000000004020 T get_speex_ops
0000000000003990 T get_stream_lslen
0000000000002f70 T initNativeID
0000000000004030 T JNI_OnLoad
00000000000023c0 T mp_store_rev_data
0000000000007280 B mutex
000000000000724c B outframe_cnt
00000000000024f0 T output
0000000000003240 T saveAudioSteam
0000000000003840 T speex_dec_init
0000000000007180 D speex_dec_io
00000000000071e0 D speex_decoder_ops
0000000000002f20 T speex_decoder_run
0000000000007248 B speex_dec_status
00000000000023d0 T speex_encode_neebuf
00000000000023e0 T speex_enc_open
00000000000023f0 T speex_enc_run
00000000000071c0 D speex_en_io
0000000000007240 B start_decode_stream
00000000000039a0 T stream_output
```

Toutes les fonctions, buffers et tables de structures de `libjlspeex.so` sont fidèlement reconstitués et fonctionnels.
