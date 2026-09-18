# Documentation Reverse Engineering : libaudio_detect.so

## 1. Vue d'ensemble

`libaudio_detect.so` (92 Ko dans les binaires ARM64 originaux de l'application Xiaomi Earbuds, avec symboles de débogage DWARF) est le **pont JNI** entre la couche Android Java et le moteur DSP de personnalisation HRTF (`libphrtf.so`).

Son rôle précis est d'orchestrer la **détection acoustique des balayages sonores intra-auriculaires** (*in-ear sweep detection*) utilisés lors du calibrage de l'audio spatial personnalisé (*Personalized Spatial Audio Calibration*) de Xiaomi.

### Flux d'exécution haut niveau :
```
Android Java (AudioDetect.java)
    │  [6 jbyteArray PCM 16 bits : L1, L2, R1, R2, C1, C2]
    ▼
nativeDetect()  [libaudio_detect.so — JNI bridge]
    │  byteArray2FloatArray() × 6
    ▼
phrtf_init()    [libphrtf.so — allocation DSP]
    │
    ▼
hrtfWavCheck()  [libphrtf.so — validation géométrie]
    │  == 111 ? (succès : gauche + centre + droite OK)
    ▼
phrtf_process() [libphrtf.so — UPOLS + Welch + meloctsmooth + paraMatch]
    │  → indexStamp (0..16 : indice du profil HRTF optimal)
    ▼
allocateAndFill2DArray() × 2  [libphrtf.so — remplissage des SOS]
    │  → hrtfData44k[72][25], hrtfData48k[72][25]
    ▼
getHrtfData44k() / getHrtfData48k()  [retour JNI en jobjectArray]
getInputShift44k() / getInputShift48k()  [retour valeur de décalage]
```

---

## 2. Interface Java (com.mi.audio.phrtf.AudioDetect)

Source originale : `earbuds_decompiled/sources/com/mi/audio/phrtf/AudioDetect.java`

```java
package com.mi.audio.phrtf;

public class AudioDetect {
    static {
        System.loadLibrary("audio_detect");
    }

    // Réinitialise les données HRTF en mémoire native
    public native void nativeInit();

    // Lance la détection : retourne 111 si succès, code d'erreur sinon
    // (bit 100 = angle +45° gauche OK, bit 10 = angle 0° centre OK, bit 1 = angle -45° droite OK)
    public native int nativeDetect(byte[] l1, byte[] l2,
                                   byte[] r1, byte[] r2,
                                   byte[] c1, byte[] c2);

    // Retourne la matrice de coefficients biquad IIR à 44.1 kHz (int[72][25])
    public native int[][] getHrtfData44k();

    // Retourne la matrice de coefficients biquad IIR à 48.0 kHz (int[72][25])
    public native int[][] getHrtfData48k();

    // Retourne le décalage d'entrée pour 44.1 kHz
    public native int getInputShift44k();

    // Retourne le décalage d'entrée pour 48.0 kHz
    public native int getInputShift48k();
}
```

---

## 3. Structure et Organisation des Sources C++

Le code a été reconstruit à 100% en C++ dans `native/audio_detect/audiodetect.cpp`.

### Variables globales :

| Variable | Type | Valeur initiale | Description |
| :--- | :--- | :--- | :--- |
| `rows` | `int` | `72` | Nombre de sections biquad par profil |
| `cols` | `int` | `25` | Nombre de coefficients par section |
| `hrtfData44k` | `int32_t **` | `nullptr` | Tableau 2D dynamique SOS à 44.1 kHz |
| `hrtfData48k` | `int32_t **` | `nullptr` | Tableau 2D dynamique SOS à 48.0 kHz |
| `inputShift44k` | `int` | `0` | Exposant de décalage pour normalisation à 44.1 kHz |
| `inputShift48k` | `int` | `0` | Exposant de décalage pour normalisation à 48.0 kHz |

---

## 4. Table Exhaustive des Symboles Exportés (Parité 100%)

La bibliothèque recompilée `build/libaudio_detect.so` exporte **exactement les 13 mêmes symboles** que le fichier `.so` original, sans aucun manquant ni superflu :

| Symbole dynamique | Type | Description |
| :--- | :--- | :--- |
| `Java_com_mi_audio_phrtf_AudioDetect_nativeInit` | `T` (Code) | Libère les buffers 44k/48k et remet les décalages à zéro. |
| `Java_com_mi_audio_phrtf_AudioDetect_nativeDetect` | `T` (Code) | Orchestre la détection complète, retourne le code de validation. |
| `Java_com_mi_audio_phrtf_AudioDetect_getHrtfData44k` | `T` (Code) | Construit et retourne un `int[72][25]` JNI à 44.1 kHz. |
| `Java_com_mi_audio_phrtf_AudioDetect_getHrtfData48k` | `T` (Code) | Construit et retourne un `int[72][25]` JNI à 48.0 kHz. |
| `Java_com_mi_audio_phrtf_AudioDetect_getInputShift44k` | `T` (Code) | Retourne `inputShift44k`. |
| `Java_com_mi_audio_phrtf_AudioDetect_getInputShift48k` | `T` (Code) | Retourne `inputShift48k`. |
| `_Z20byteArray2FloatArrayP7_JNIEnvP11_jbyteArrayi` | `T` (Code) | Fonction interne C++ de décodage PCM → float (symbole exact via `__asm__`). |
| `rows` | `D` (Données) | Entier global : nombre de lignes de la matrice SOS (`72`). |
| `cols` | `D` (Données) | Entier global : nombre de colonnes de la matrice SOS (`25`). |
| `hrtfData44k` | `D` (Données) | Pointeur global vers la matrice SOS 44.1 kHz. |
| `hrtfData48k` | `D` (Données) | Pointeur global vers la matrice SOS 48.0 kHz. |
| `inputShift44k` | `D` (Données) | Décalage entier pour le profil 44.1 kHz. |
| `inputShift48k` | `D` (Données) | Décalage entier pour le profil 48.0 kHz. |

---

## 5. Fonctions Détaillées

### 5.1 `byteArray2FloatArray` — Décodage PCM → float

**Signature C++ :**
```cpp
float* byteArray2FloatArray(JNIEnv *env, jbyteArray byteArr, int bitDepth);
```

Convertit un tableau de bytes JNI représentant de l'audio PCM en tableau de flottants normalisés [-1.0, +1.0].

**Formules de conversion par profondeur :**

| Profondeur | Calcul | Facteur de normalisation |
| :--- | :--- | :--- |
| 8 bits | `sample = b[i] - 128` | `× 1/128.0` |
| 16 bits | `sample = int16_t(b[2i] | b[2i+1]<<8)` | `× 1/32768.0` |
| 24 bits | `sample = int32_t(b[3i] | b[3i+1]<<8 | b[3i+2]<<16)` sign-extended | `× 1/8388608.0` |
| 32 bits | `sample = int32_t(b[4i] | ... | b[4i+3]<<24)` | `× 1/2147483648.0` |

Le tableau de bytes est libéré avec `JNI_ABORT` (sans re-écriture) après lecture.

---

### 5.2 `nativeInit` — Réinitialisation

```
1. Si hrtfData44k != nullptr → freeAndFill2DArray(hrtfData44k, rows), set nullptr
2. Si hrtfData48k != nullptr → freeAndFill2DArray(hrtfData48k, rows), set nullptr
3. inputShift44k = 0, inputShift48k = 0
```

---

### 5.3 `nativeDetect` — Détection principale

**Séquence d'exécution :**
```
1. Log: "phrtf start."
2. Décodage PCM 16 bits → float × 6 canaux (L1, L2, C1, C2, R1, R2)
3. phrtfPara = phrtf_init()
4. checkhrtf = hrtfWavCheck(phrtfPara, L1, L2, C1, C2, R1, R2)
5. Log: "checkhrtf %d"
6. Si checkhrtf == 111 :
     indexStamp = phrtf_process(phrtfPara, C1, L1, R1)
7. Libération des anciens buffers hrtfData44k et hrtfData48k
8. hrtfData44k = allocateAndFill2DArray(72, 25, indexStamp, 44100, &inputShift44k)
   hrtfData48k = allocateAndFill2DArray(72, 25, indexStamp, 48000, &inputShift48k)
9. Log: "inputshift 44k %d 48k %d"
10. phrtf_free(phrtfPara)
11. Libération des 6 buffers float
12. return (jint)checkhrtf
```

**Valeurs de retour :**

| Valeur | Signification |
| :--- | :--- |
| `111` | Succès complet — les 3 angles validés |
| `110` | Angle -45° (droite) invalide |
| `101` | Angle 0° (centre) invalide |
| `100` | Seulement +45° gauche valide |
| `011` | Angle +45° (gauche) invalide |
| `010` | Seulement 0° centre valide |
| `001` | Seulement -45° droite valide |
| `000` | Aucun angle valide |

---

### 5.4 `getHrtfData44k` / `getHrtfData48k`

**Algorithme JNI :**
```cpp
jclass intArrayClass = env->FindClass("[I");             // Classe int[]
jobjectArray result = env->NewObjectArray(72, intArrayClass, nullptr);

for (int i = 0; i < 72; i++) {
    jintArray intArray = env->NewIntArray(25);
    if (hrtfDataXXk && hrtfDataXXk[i])
        env->SetIntArrayRegion(intArray, 0, 25, (jint*)hrtfDataXXk[i]);
    env->SetObjectArrayElement(result, i, intArray);
    env->DeleteLocalRef(intArray);   // Évite le débordement de la table des refs locales JNI
}
return result;
```

---

## 6. Dépendances et Liens

| Bibliothèque | Rôle |
| :--- | :--- |
| `libphrtf.so` | Moteur DSP HRTF : FFT, UPOLS, Welch, filtres biquad |
| `liblog.so` | `__android_log_print` (stub local pour build hôte) |
| `libc` | `malloc`, `free`, `memset` |

**Symboles importés depuis `libphrtf.so` :**

| Symbole | Description |
| :--- | :--- |
| `phrtf_init` | Allocation du contexte DSP |
| `phrtf_process` | Traitement DSP complet → `indexStamp` |
| `phrtf_free` | Libération du contexte DSP |
| `hrtfWavCheck` | Validation acoustique des 6 canaux |
| `allocateAndFill2DArray` | Remplissage des matrices SOS |
| `freeAndFill2DArray` | Libération des matrices SOS |

---

## 7. Format Q de la Matrice SOS

```
rows = 72   → Nombre de sections biquad (Second-Order Sections)
cols = 25   → Coefficients entiers Q-format propriétaire Xiaomi

valeur_réelle = coefficient_entier × 2^(-inputShift)
```

La matrice complète (1800 coefficients entiers 32 bits) est extraite de `sos44` ou `sos48` dans `libphrtf.so` selon le profil `indexStamp` sélectionné par `paraMatch`.

---

## 8. Chemins Sources Originaux (DWARF)

Les symboles de débogage embarqués révèlent le workspace Jenkins de l'ingénieur Xiaomi :

```
/home/work/ssd1/newJenkins/workspace/
    mi-wear-headset-android-app/
        function/phrtf/src/main/cpp/
            audiodetect.cpp   ← source principale de libaudio_detect.so
```

---

## 9. Compilation et Validation

```bash
# Compilation
make -C native
# Produit : native/build/libaudio_detect.so

# Vérification parité des symboles
diff <(nm -D earbuds_decompiled/resources/lib/arm64-v8a/libaudio_detect.so \
         | grep -v " U " | awk '{print $3}' | sort) \
     <(nm -D native/build/libaudio_detect.so \
         | grep -v " U " | awk '{print $3}' | sort)
# → aucune différence

# Test Python
python3 -c "
import ctypes
libphrtf = ctypes.CDLL('native/build/libphrtf.so')
liblog   = ctypes.CDLL('native/build/liblog.so')
libad    = ctypes.CDLL('native/build/libaudio_detect.so')
rows = ctypes.c_int.in_dll(libad, 'rows').value
cols = ctypes.c_int.in_dll(libad, 'cols').value
print(f'rows={rows}, cols={cols}')  # → rows=72, cols=25
libad.Java_com_mi_audio_phrtf_AudioDetect_nativeInit(None, None)
libad.Java_com_mi_audio_phrtf_AudioDetect_getInputShift44k.restype = ctypes.c_int
print('shift44k:', libad.Java_com_mi_audio_phrtf_AudioDetect_getInputShift44k(None, None))  # → 0
"
```

---

## 10. Résumé de Reconstruction

| Critère | Résultat |
| :--- | :--- |
| Parité des symboles dynamiques | **13/13 (100%)** |
| Logique de décodage PCM | 4 profondeurs (8/16/24/32 bits) |
| Orchestration DSP | Complète (init → check → process → fill → free) |
| Format de retour JNI | `int[72][25]` via `jobjectArray` |
| Gestion mémoire | Zéro fuite (malloc/free, DeleteLocalRef) |
| Symboles de debug DWARF | Origine confirmée (`audiodetect.cpp`) |
| Mangling C++ exact | Via directive `__asm__` |
