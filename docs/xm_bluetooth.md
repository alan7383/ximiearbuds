# Documentation Reverse Engineering : libxm_bluetooth.so

## 1. Vue d'ensemble

`libxm_bluetooth.so` (12 Ko, ARM64, stripped, NDK r25b clang 14.0.6) est le **cœur cryptographique propriétaire Bluetooth** de Xiaomi pour l'authentification mutuelle entre l'application et les écouteurs.

Il implémente une version adaptée des primitives cryptographiques officielles Bluetooth (SAFER+, E1, E21) avec des constantes et des transformations de clé spécifiques à Xiaomi.

### Rôle :
- **SAFER+** : Chiffrement par blocs 128 bits, 8 tours, S-boxes standard Bluetooth (exp45/log45 mod 257)
- **E1** : Authentification Bluetooth (dérivé de Bluetooth Core Spec §H.1, 2 passes SAFER+)
- **E21** : Dérivation de clé de liaison (dérivé de §H.2, répétition cyclique + SAFER+)
- **function_xiaomi** : Composition E1 + E21 pour l'authentification mutuelle complète
- **Interface JNI** : `com/xiaomi/aivsbluetoothsdk/impl/BluetoothAuth` (6 méthodes natives)

---

## 2. Interface Java (com.xiaomi.aivsbluetoothsdk.impl.BluetoothAuth)

```java
package com.xiaomi.aivsbluetoothsdk.impl;

public class BluetoothAuth {
    static {
        System.loadLibrary("xm_bluetooth");
    }

    // Initialise le module et la référence à l'objet courant
    public native boolean nativeInit();

    // Génère 17 octets : [0x00, rand×16] (challenge local)
    public native byte[] getRandomAuthData();

    // Génère 16 octets aléatoires (challenge de vérification)
    public native byte[] getRandomAuthCheckData();

    // Stocke la clé de vérification reçue (16 octets)
    // Retourne 0 si OK, 3 si null ou longueur incorrecte
    public native int getEncryptedAuthCheckData(byte[] key);

    // Calcule la réponse d'authentification chiffrée (17 octets)
    public native byte[] getEncryptedAuthData(byte[] randData);

    // Calcule la réponse de liaison (16 octets)
    public native byte[] setLinkKey(byte[] keyData);
}
```

---

## 3. Structure et Organisation des Sources C

Reconstruit à 100% en C dans [native/xm_bluetooth/xm_bluetooth.c](file:///home/alan/ximiearbuds/native/xm_bluetooth/xm_bluetooth.c).

### Variables globales :

| Variable | Type | Valeur initiale | Description |
| :--- | :--- | :--- | :--- |
| `g_link_key` | `uint8_t[16]` | `06 77 5f 87 91 8d d4 23 00 5d f1 d8 cf 0c 14 2b` | Clé de liaison BT courante (DAT_0010a6b0) |
| `g_bd_addr` | `const uint8_t[6]` | `11:22:33:33:22:11` | Adresse BD fixe (DAT_0010a6c0) |
| `g_jvm` | `JavaVM *` | `NULL` | Référence à la JVM (DAT_0010a6c8, .bss) |
| `g_class` | `jclass` | `NULL` | Référence à la classe BluetoothAuth (DAT_0010a6d0, .bss) |

---

## 4. Table Exhaustive des Symboles Exportés (Parité 100%)

La bibliothèque recompilée `build/libxm_bluetooth.so` exporte **exactement les 7 mêmes symboles** que le fichier `.so` original :

| Symbole | Taille originale | Description |
| :--- | :--- | :--- |
| `function_E1test` | 4 octets (wrapper) | Authentification Bluetooth E1 (2 passes SAFER+) |
| `function_E21` | 400 octets | Dérivation de clé de liaison E21 (SAFER+) |
| `function_xiaomi` | 60 octets | Composition E1 + E21 (authentification mutuelle) |
| `JNI_OnLoad` | 164 octets | Chargement JNI et enregistrement des méthodes |
| `attach_current_thread` | 140 octets | Attacher un thread à la JVM |
| `detach_current_thread` | 12 octets | Détacher un thread de la JVM |
| `register_xm_bluetooth` | 100 octets | Enregistrer les 6 méthodes JNI natives |

---

## 5. Cryptographie SAFER+

### 5.1 S-boxes standard Bluetooth

Deux tables de 256 octets extraites de `.rodata` :

- **sbox_exp** (DAT_00100a70) : `45^x mod 257` avec `sbox_exp[128] = 0`
- **sbox_log** (DAT_00100b70) : `log_45(x) mod 257` avec `sbox_log[0] = 128`

Vérification :
```python
# sbox_exp[0] = 1  (45^0 = 1)
# sbox_exp[1] = 45 (45^1 = 45)
# sbox_log[45] = 1 (log_45(45) = 1) ✓
# sbox_log[1]  = 0 (log_45(1) = 0)  ✓
```

### 5.2 Constantes de biais (bias_constants)

16 rounds × 16 octets extraites de `.rodata` à DAT_0010097f. Ces constantes sont additionnées aux sous-clés lors du planning de clé pour empêcher les attaques par clé faible.

### 5.3 Pattern XOR/Addition (mask 0x9999)

Le masque `0x9999 = 0b1001100110011001` détermine l'opération appliquée à chaque position du bloc :

| Position | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12 | 13 | 14 | 15 |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| Opération | XOR | ADD | ADD | XOR | XOR | ADD | ADD | XOR | XOR | ADD | ADD | XOR | XOR | ADD | ADD | XOR |
| S-box | exp | log | log | exp | exp | log | log | exp | exp | log | log | exp | exp | log | log | exp |

---

## 6. Fonctions Cryptographiques

### 6.1 safer_key_schedule() — Planning de clé SAFER+

**Conforme à FUN_00101630 (Ghidra)**

```
Entrée : key[16] (128 bits)
Sortie  : ks[272] = 17 sous-clés × 16 octets

1. ks[0..15] = key (sous-clé 0)
2. Parité = XOR(key[0..15])
3. k[0..15] = key, k[16] = parity
4. Pour round = 0..15 :
     Pour i = 0..16 : k[i] = (k[i] << 3) | (k[i] >> 5)  [rot. gauche 3 bits]
     ks[16*(round+1)+i] = k[i] + bias_constants[round][i]
```

### 6.2 safer_encrypt_block() — Chiffrement SAFER+

**Conforme à FUN_0010176c (Ghidra) — 8 tours**

Pour chaque tour r = 0..7 :
1. **[si round2 && r==2]** : XOR/ADD avec le bloc original sauvegardé
2. **XOR/ADD** avec sous-clé `sk` (selon le masque)
3. **S-boxes** : exp aux positions XOR-mask, log aux autres
4. **XOR/ADD inversé** avec sous-clé `sk+16`
5. **PHT** (Pseudo-Hadamard Transform) en arbre 4 niveaux sur 16 octets

PHT formule : `(a, b) → (2a + b, a + b) mod 256`

Puis **whitening final** avec la 17e sous-clé (ks+0x100).

### 6.3 function_E1test() — Authentification E1

**Conforme à Ghidra @0010112c (function_E1test)**

```
Entrée : rand_local[6], sres_in[16], key[16]
Sortie  : sres_out[16]

1. sres_out = sres_in
2. ks = safer_key_schedule(key)
3. sres_out = safer_encrypt_block(sres_out, ks, round2=0)
4. Pour i=0..15 : sres_out[i] += sres_out[i] ^ sres_in[i] + rand_local[i%6]
5. key2 = transformations XOR/add fixes sur key :
     key2[0] = key[0] - 0x17    key2[1] = key[1] ^ 0xE5
     key2[2] = key[2] - 0x21    key2[3] = key[3] ^ 0xC1
     key2[4] = key[4] - 0x4D    key2[5] = key[5] ^ 0xA7
     key2[6] = key[6] - 0x6B    key2[7] = key[7] ^ 0x83
     key2[8] = key[8] ^ 0xE9    key2[9] = key[9] - 0x1B
     key2[10]= key[10]^ 0xDF    key2[11]= key[11]- 0x3F
     key2[12]= key[12]^ 0xB3    key2[13]= key[13]- 0x59
     key2[14]= key[14]^ 0x95    key2[15]= key[15]- 0x7D
6. ks = safer_key_schedule(key2)
7. sres_out = safer_encrypt_block(sres_out, ks, round2=1)
```

### 6.4 function_E21() — Dérivation de clé E21

**Conforme à Ghidra @001014a0**

```
Entrée : key6[6], rand16[16]
Sortie  : out16[16]

1. out16 = répétition cyclique de key6 sur 16 octets :
   [k0,k1,k2,k3,k4,k5, k0,k1,k2,k3,k4,k5, k0,k1,k2,k3]
2. key16 = rand16[0..14] || (rand16[15] ^ 0x06)
3. ks = safer_key_schedule(key16)
4. out16 = safer_encrypt_block(out16, ks, round2=1)
```

### 6.5 function_xiaomi() — Authentification Mutuelle Xiaomi

**Conforme à Ghidra @00101b98**

```
function_E1test(bd_addr, rand_or_flag, link_key, out)
function_E21(bd_addr, out, out)
```

---

## 7. Protocole d'Authentification Mutuelle

```
Smartphone                              Écouteurs
    │                                       │
    │  getRandomAuthData()                  │
    │  → challenge_A [0x00, 16 octets rand] │
    │                                       │
    │──── challenge_A ─────────────────────►│
    │                                       │  calcule réponse avec link_key
    │◄─── response_B ──────────────────────┤
    │                                       │
    │  getEncryptedAuthCheckData(response_B)│
    │  (stocke dans g_link_key)             │
    │                                       │
    │  getRandomAuthCheckData()             │
    │  → check_rand [16 octets]             │
    │──── check_rand ───────────────────────►│
    │                                       │
    │  getEncryptedAuthData(challenge_A)    │
    │  → E1(bd_addr, challenge_A, key) +    │
    │    E21(bd_addr, résultat, résultat)   │
    │──── auth_response ────────────────────►│ vérifie
    │                                       │
    │  setLinkKey(check_rand)               │
    │  (dérive et vérifie la clé finale)    │
```

---

## 8. Données Initiales de la Section .data

Extraites directement du binaire original :

| Adresse | Contenu | Signification |
| :--- | :--- | :--- |
| `0xa6b0` | `06 77 5f 87 91 8d d4 23 00 5d f1 d8 cf 0c 14 2b` | Clé de liaison initiale (`g_link_key`) |
| `0xa6c0` | `11 22 33 33 22 11` | Adresse BD fixe (`g_bd_addr`) |
| `0xa6c8` | `.bss` (0x00 × 8) | `g_jvm` (JavaVM*) |
| `0xa6d0` | `.bss` (0x00 × 8) | `g_class` (jclass) |

---

## 9. Compilation et Validation

```bash
# Compilation (via Makefile)
make -C native
# Produit : native/build/libxm_bluetooth.so

# Vérification parité des symboles (diff doit être vide)
diff \
  <(nm -D earbuds_decompiled/resources/lib/arm64-v8a/libxm_bluetooth.so \
    | grep -v " U " | awk '{print $3}' | sort) \
  <(nm -D native/build/libxm_bluetooth.so \
    | grep -v " U " | awk '{print $3}' | sort)
```

---

## 10. Résumé de Reconstruction

| Critère | Résultat |
| :--- | :--- |
| Parité des symboles dynamiques | **7/7 (100%)** |
| Tables SAFER+ (sbox_exp, sbox_log) | Extraites et vérifiées |
| Constantes de biais (16×16) | Extraites du .rodata original |
| Algorithme E1 (2 passes SAFER+) | Décompilé intégralement |
| Algorithme E21 (dérivation clé) | Décompilé intégralement |
| Masque XOR/ADD (0x9999) | Identifié et appliqué |
| PHT 4 niveaux | Reconstructed depuis Ghidra |
| Interface JNI (6 méthodes) | Complète avec signatures exactes |
| Données initiales (.data) | Extraites octets par octets |
| Compilation propre | ✅ Zéro warning, zéro erreur |
