/*
 * xm_bluetooth.c — Reconstruction 100% de libxm_bluetooth.so
 *
 * Cœur cryptographique propriétaire Bluetooth Xiaomi (AIVS SDK)
 *
 * Contient :
 *   - SAFER+ : chiffrement par blocs 128 bits, 8 tours, S-boxes standard BT
 *   - function_E1test : authentification Bluetooth (dérivé E1, Bluetooth Core Spec §H.1)
 *   - function_E21   : dérivation de clé liaison Bluetooth (dérivé E21, §H.2)
 *   - function_xiaomi: composition E1 + E21 pour l'authentification mutuelle Xiaomi
 *   - JNI interface  : com/xiaomi/aivsbluetoothsdk/impl/BluetoothAuth (6 méthodes)
 *
 * Source originale :
 *   /home/alan/earbuds_decompiled/resources/lib/arm64-v8a/libxm_bluetooth.so
 *   BuildID: 9e0d7974e2e930a54b68488d88d6d1dc3ae644e8
 *   NDK r25b, clang 14.0.6, ARM64
 *
 * Symboles exportés : 7 (parité 100% avec l'original)
 *   function_E1test, function_E21, function_xiaomi, JNI_OnLoad,
 *   attach_current_thread, detach_current_thread, register_xm_bluetooth
 */

#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

#ifndef ANDROID_LOG_ERROR
#define ANDROID_LOG_ERROR 6
#endif

#ifdef __cplusplus
extern "C" {
#endif
int __android_log_print(int prio, const char *tag, const char *fmt, ...);
#ifdef __cplusplus
}
#endif

/* =====================================================================
 * SAFER+ S-boxes standard Bluetooth
 *
 * sbox_exp[x] = 45^x mod 257  (sbox_exp[128] = 0 par convention)
 * sbox_log[x] = log_45(x) mod 257 (sbox_log[0] = 128 par convention)
 *
 * Extraites de .rodata :
 *   DAT_00100a70 → sbox_exp (256 octets)
 *   DAT_00100b70 → sbox_log (256 octets)
 * ===================================================================== */
static const uint8_t sbox_exp[256] = {
      1,  45, 226, 147, 190,  69,  21, 174, 120,   3, 135, 164, 184,  56, 207,  63,
      8, 103,   9, 148, 235,  38, 168, 107, 189,  24,  52,  27, 187, 191, 114, 247,
     64,  53,  72, 156,  81,  47,  59,  85, 227, 192, 159, 216, 211, 243, 141, 177,
    255, 167,  62, 220, 134, 119, 215, 166,  17, 251, 244, 186, 146, 145, 100, 131,
    241,  51, 239, 218,  44, 181, 178,  43, 136, 209, 153, 203, 140, 132,  29,  20,
    129, 151, 113, 202,  95, 163, 139,  87,  60, 130, 196,  82,  92,  28, 232, 160,
      4, 180, 133,  74, 246,  19,  84, 182, 223,  12,  26, 142, 222, 224,  57, 252,
     32, 155,  36,  78, 169, 152, 158, 171, 242,  96, 208, 108, 234, 250, 199, 217,
      0, 212,  31, 110,  67, 188, 236,  83, 137, 254, 122,  93,  73, 201,  50, 194,
    249, 154, 248, 109,  22, 219,  89, 150,  68, 233, 205, 230,  70,  66, 143,  10,
    193, 204, 185, 101, 176, 210, 198, 172,  30,  65,  98,  41,  46,  14, 116,  80,
      2,  90, 195,  37, 123, 138,  42,  91, 240,   6,  13,  71, 111, 112, 157, 126,
     16, 206,  18,  39, 213,  76,  79, 214, 121,  48, 104,  54, 117, 125, 228, 237,
    128, 106, 144,  55, 162,  94, 118, 170, 197, 127,  61, 175, 165, 229,  25,  97,
    253,  77, 124, 183,  11, 238, 173,  75,  34, 245, 231, 115,  35,  33, 200,   5,
    225, 102, 221, 179,  88, 105,  99,  86,  15, 161,  49, 149,  23,   7,  58,  40
};

static const uint8_t sbox_log[256] = {
    128,   0, 176,   9,  96, 239, 185, 253,  16,  18, 159, 228, 105, 186, 173, 248,
    192,  56, 194, 101,  79,   6, 148, 252,  25, 222, 106,  27,  93,  78, 168, 130,
    112, 237, 232, 236, 114, 179,  21, 195, 255, 171, 182,  71,  68,   1, 172,  37,
    201, 250, 142,  65,  26,  33, 203, 211,  13, 110, 254,  38,  88, 218,  50,  15,
     32, 169, 157, 132, 152,   5, 156, 187,  34, 140,  99, 231, 197, 225, 115, 198,
    175,  36,  91, 135, 102,  39, 247,  87, 244, 150, 177, 183,  92, 139, 213,  84,
    121, 223, 170, 246,  62, 163, 241,  17, 202, 245, 209,  23, 123, 147, 131, 188,
    189,  82,  30, 235, 174, 204, 214,  53,   8, 200, 138, 180, 226, 205, 191, 217,
    208,  80,  89,  63,  77,  98,  52,  10,  72, 136, 181,  86,  76,  46, 107, 158,
    210,  61,  60,   3,  19, 251, 151,  81, 117,  74, 145, 113,  35, 190, 118,  42,
     95, 249, 212,  85,  11, 220,  55,  49,  22, 116, 215, 119, 167, 230,   7, 219,
    164,  47,  70, 243,  97,  69, 103, 227,  12, 162,  59,  28, 133,  24,   4,  29,
     41, 160, 143, 178,  90, 216, 166, 126, 238, 141,  83,  75, 161, 154, 193,  14,
    122,  73, 165,  44, 129, 196, 199,  54,  43, 127,  67, 149,  51, 242, 108, 104,
    109, 240,   2,  40, 206, 221, 155, 234,  94, 153, 124,  20, 134, 207, 229,  66,
    184,  64, 120,  45,  58, 233, 100,  31, 146, 144, 125,  57, 111, 224, 137,  48
};

/* =====================================================================
 * Constantes de biais SAFER+ (bias constants)
 *
 * 16 rounds × 16 octets. Extraites de .rodata à DAT_0010097f.
 * Utilisées dans le planning de clé pour XOR/addition aux sous-clés.
 * ===================================================================== */
static const uint8_t bias_constants[16][16] = {
    { 0x46,0x3d,0x05,0xdc,0x66,0x6e,0xf6,0x9a,0xf8,0x0d,0x58,0x95,0x67,0xc6,0xaa,0xab },
    { 0xec,0xa0,0x68,0x9b,0x96,0xd4,0xeb,0xbf,0x43,0x49,0x36,0xe9,0x6a,0x89,0xd8,0xc3 },
    { 0x8a,0x94,0x63,0x99,0xbc,0x7b,0xbe,0xc1,0x22,0xbb,0x5c,0x71,0xd5,0x1f,0x92,0x57 },
    { 0x5d,0x8f,0x44,0x41,0x1d,0x51,0xe6,0x40,0x17,0xfb,0xfd,0x19,0x32,0x34,0xb8,0x61 },
    { 0x2a,0xca,0x23,0x6f,0xda,0x39,0xf7,0xa2,0x01,0x7f,0xd6,0x31,0xe7,0xde,0x80,0x04 },
    { 0xdd,0x2c,0x59,0x82,0xaf,0xa8,0xe0,0x0f,0xcd,0xa1,0x12,0x3e,0x30,0xd1,0x1c,0xd0 },
    { 0x3a,0x33,0x72,0x2e,0x4f,0x90,0x02,0x13,0x06,0x75,0xce,0x87,0xc2,0xef,0xb2,0xad },
    { 0x7d,0x38,0x15,0xe1,0x52,0x9f,0x7a,0x6c,0x2f,0x27,0xc4,0xe2,0x81,0xa9,0xcf,0x8d },
    { 0xc0,0xd7,0xdf,0xff,0x60,0x76,0x14,0x8c,0x5e,0x55,0x09,0xe4,0x08,0xc7,0x42,0x20 },
    { 0xfc,0xd2,0x50,0x91,0xd9,0x4c,0x62,0x9e,0xe8,0xb9,0xa6,0xf9,0x1a,0x00,0x21,0x0b },
    { 0xfa,0x35,0x9c,0x4e,0x4b,0x69,0x48,0xcb,0x0e,0xc8,0xa4,0x5b,0xea,0x84,0x07,0xb4 },
    { 0x18,0xf4,0xae,0x6b,0xdb,0xa7,0xcc,0x3f,0x8b,0x4a,0x0c,0x3c,0x25,0xe5,0x54,0x4d },
    { 0x45,0x83,0xed,0x11,0xf0,0xb0,0x53,0x93,0xf2,0x74,0x26,0xb5,0x9d,0x6d,0x7c,0xf3 },
    { 0x2d,0xf1,0x56,0x24,0x7e,0x47,0x1b,0x86,0xbd,0x70,0x8e,0x1e,0x3b,0x73,0x16,0x03 },
    { 0xb6,0xac,0x28,0x5a,0xc9,0xb3,0x37,0xc5,0x0a,0x10,0xb7,0xa3,0xba,0xb1,0x97,0x46 },
    { 0x88,0x01,0x2d,0xe2,0x93,0xbe,0x45,0x15,0xae,0x78,0x03,0x87,0xa4,0xb8,0x38,0xcf }
};

/* =====================================================================
 * Globales : clé de liaison et vecteur BD_ADDR
 *
 * Valeurs initiales extraites de la section .data du binaire original.
 * ===================================================================== */

/* Clé de liaison Bluetooth courante (16 octets) */
static uint8_t g_link_key[16] = {
    0x06, 0x77, 0x5f, 0x87, 0x91, 0x8d, 0xd4, 0x23,
    0x00, 0x5d, 0xf1, 0xd8, 0xcf, 0x0c, 0x14, 0x2b
};

/* Adresse BD_ADDR 6 octets : 11:22:33:33:22:11 (DAT_0010a6c0) */
static const uint8_t g_bd_addr[6] = { 0x11, 0x22, 0x33, 0x33, 0x22, 0x11 };

/* Globales JNI */
static JavaVM  *g_jvm   = NULL;   /* DAT_0010a6c8 (.bss) */
static jclass   g_class = NULL;   /* DAT_0010a6d0 (.bss) */

/* =====================================================================
 * safer_key_schedule() — Planning de clé SAFER+ (FUN_00101630)
 *
 * key_in[16]  : clé SAFER+ 128 bits
 * ks[0x110]   : buffer de planning de clé (272 octets = 17 sous-clés × 16)
 *
 * Algorithme décompilé :
 *   - Copie key_in comme 1ère sous-clé
 *   - Calcule un octet de parité (XOR de tous les octets de clé, roté)
 *   - Pour 16 tours : rotation gauche de 3 bits sur chaque octet,
 *     puis addition des bias_constants[round]
 *   - La 17e sous-clé (whitening final) est stockée à ks[0x100..0x10f]
 * ===================================================================== */
static void safer_key_schedule(const uint8_t *key_in, uint8_t *ks)
{
    memset(ks, 0, 0x110);
    memcpy(ks, key_in, 16);   /* sous-clé 0 */

    /* Octet de parité : XOR de tous les octets, rotation droite 5 (= gauche 3) */
    uint8_t parity = 0;
    for (int i = 0; i < 16; i++) parity ^= key_in[i];
    /* La boucle suit le pattern de la décompilation : rotation gauche 3 bits */

    /* k[0..15] = clé courante, k[16] = parity */
    uint8_t k[17];
    memcpy(k, key_in, 16);
    k[16] = parity;

    /* 16 rounds */
    uint8_t *sk = ks + 16;
    for (int r = 0; r < 16; r++) {
        /* Rotation gauche 3 bits de chaque octet */
        for (int i = 0; i <= 16; i++) {
            k[i] = (uint8_t)((k[i] << 3) | (k[i] >> 5));
        }
        /* Sous-clé = k[0..15] + bias_constants[r][0..15] */
        for (int i = 0; i < 16; i++) {
            sk[i] = (uint8_t)(k[i] + bias_constants[r][i]);
        }
        sk += 16;
    }
}

/* =====================================================================
 * safer_encrypt_block() — Chiffrement SAFER+ d'un bloc 16 octets
 *
 * Conforme à FUN_0010176c (Ghidra). 8 tours, puis whitening final.
 *
 * pattern XOR (mask 0x9999) : positions 0,3,4,7,8,11,12,15 → XOR
 *                              positions 1,2,5,6,9,10,13,14 → addition
 * ===================================================================== */
#define XOR_MASK 0x9999U

static inline uint8_t apply_op(uint8_t a, uint8_t b, int is_xor)
{
    return is_xor ? (a ^ b) : (uint8_t)(a + b);
}

static void safer_encrypt_block(uint8_t *block, const uint8_t *ks, int round2)
{
    uint8_t orig[16];
    memcpy(orig, block, 16);

    const uint8_t *sk = ks;

    for (int iter = 0; iter < 8; iter++) {

        /* round2 : au tour 2, XOR/add avec le bloc original sauvegardé */
        if (round2 && iter == 2) {
            for (int i = 0; i < 16; i++) {
                int xor = (XOR_MASK >> (i & 0x1f)) & 1;
                block[i] = apply_op(block[i], orig[i], xor);
            }
        }

        /* Étape 1 : XOR/add avec sous-clé sk (première moitié du round) */
        for (int i = 0; i < 16; i++) {
            int xor = (XOR_MASK >> (i & 0x1f)) & 1;
            block[i] = apply_op(block[i], sk[i], xor);
        }

        /* Étape 2 : S-boxes alternées (positions XOR-mask → exp, autres → log) */
        block[0]  = sbox_exp[block[0]];
        block[1]  = sbox_log[block[1]];
        block[2]  = sbox_log[block[2]];
        block[3]  = sbox_exp[block[3]];
        block[4]  = sbox_exp[block[4]];
        block[5]  = sbox_log[block[5]];
        block[6]  = sbox_log[block[6]];
        block[7]  = sbox_exp[block[7]];
        block[8]  = sbox_exp[block[8]];
        block[9]  = sbox_log[block[9]];
        block[10] = sbox_log[block[10]];
        block[11] = sbox_exp[block[11]];
        block[12] = sbox_exp[block[12]];
        block[13] = sbox_log[block[13]];
        block[14] = sbox_log[block[14]];
        block[15] = sbox_exp[block[15]];

        /* Étape 3 : XOR/add avec sous-clé sk+16 (opérateurs inversés par rapport à étape 1) */
        for (int i = 0; i < 16; i++) {
            int xor = (XOR_MASK >> (i & 0x1f)) & 1;
            /* inversé : positions XOR-mask → addition, autres → XOR */
            block[i] = xor ? (uint8_t)(block[i] + sk[16 + i]) : (block[i] ^ sk[16 + i]);
        }
        sk += 32;

        /* Étape 4 : PHT (Pseudo-Hadamard Transform) en arbre 4 niveaux
         * Reconstruction conforme à la sortie Ghidra FUN_0010176c.
         * Toutes les multiplications par 2 sont des additions signées en char.
         */
        int8_t *b = (int8_t *)block;

        /* Niveau 1 : paires (0,1), (2,3), (4,5), (6,7), (8,9), (10,11), (12,13), (14,15) */
        int8_t c17 = b[1] + b[0]*2;  int8_t c1  = b[1] + b[0];
        int8_t c8  = b[3] + b[2]*2;  int8_t c2  = b[3] + b[2];
        int8_t c10 = b[5] + b[4]*2;  int8_t c11 = b[5] + b[4];
        int8_t c3  = b[7] + b[6]*2;  int8_t c16 = b[7] + b[6];
        int8_t c5  = b[9] + b[8]*2;  int8_t c12 = b[9] + b[8];
        int8_t c9  = b[11]+ b[10]*2; int8_t c19 = b[11]+ b[10];
        int8_t c7  = b[13]+ b[12]*2; int8_t c14 = b[13]+ b[12];
        int8_t c4  = b[15]+ b[14]*2; int8_t c6  = b[15]+ b[14];

        /* Niveau 2 */
        int8_t c18 = c19 + c5*2;   int8_t q19 = c19 + c5;
        int8_t d5  = c6  + c7*2;   int8_t q6  = c6  + c7;
        int8_t d7  = c1  + c8*2;   int8_t d8  = c8  + c1;
        int8_t d13 = c11 + c3*2;   int8_t d3  = c3  + c11;
        int8_t d1  = c12 + c9*2;   int8_t d9  = c9  + c12;
        int8_t d15 = c14 + c4*2;   int8_t d4  = c4  + c14;
        int8_t d11 = c16 + c17*2;  int8_t d16 = c16 + c17;
        int8_t d17 = c2  + c10*2;  int8_t d10 = c10 + c2;

        /* Niveau 3 */
        int8_t e2  = d4  + d1*2;   int8_t e4  = d4  + d1;
        int8_t e1  = d10 + d11*2;  int8_t e11 = d11 + d10;
        int8_t e12 = d8  + d13*2;  int8_t e13 = d13 + d8;
        int8_t e14 = d9  + d15*2;  int8_t e15 = d15 + d9;
        int8_t e8  = d16 + d17*2;  int8_t e16 = d16 + d17;
        int8_t e17 = d3  + c18*2;  int8_t e18 = c18 + d3;
        int8_t e3  = q6  + d7*2;   int8_t e6  = q6  + d7;
        (void)e3;

        /* Niveau 4 / sortie — mapping exact Ghidra FUN_0010176c */
        b[0]  = e16 + e14*2;
        b[1]  = e14 + e16;
        b[2]  = e6  + e17*2;
        b[3]  = e6  + e17;
        b[4]  = e4  + e1*2;
        b[5]  = e4  + e1;
        b[6]  = d5  + e12*2;   /* cVar7 + cVar12*2 */
        b[7]  = d5  + e12;     /* cVar7 + cVar12   */
        b[8]  = e15 + e8*2;
        b[9]  = e15 + e8;
        b[10] = e18 + e3*2;    /* cVar18 + cVar3*2 → e18 + (q6+d7)*2 */
        b[11] = e3  + e18;
        b[12] = e13 + e2*2;
        b[13] = e2  + e13;
        b[14] = e11 + q19*2;
        b[15] = q19 + e11;
    }

    /* Whitening final avec la 17e sous-clé (à ks+0x100) */
    for (int i = 0; i < 16; i++) {
        int xor = (XOR_MASK >> (i & 0x1f)) & 1;
        block[i] = apply_op(block[i], ks[0x100 + i], xor);
    }
}

/* =====================================================================
 * function_E1test — E1 Bluetooth : authentification
 *
 * void function_E1test(char *rand_local,  byte *sres_in,
 *                      char *key,         byte *sres_out)
 *
 * Conforme décompilation Ghidra @0010112c :
 *   1. Sauvegarder les 6 premiers octets de rand_local
 *   2. Copier sres_in dans sres_out (16 octets)
 *   3. Planning de clé depuis key, chiffrement SAFER+ (round2=0)
 *   4. XOR/addition avec rand_local répété (6 octets × répétition sur 16)
 *   5. Dériver key2 depuis key avec constantes XOR/add fixes
 *   6. Planning depuis key2, chiffrement SAFER+ (round2=1)
 * ===================================================================== */
void function_E1test(const char *rand_local, const uint8_t *sres_in,
                     const char *key, uint8_t *sres_out)
{
    /* Sauvegarde des 6 octets de rand_local */
    int8_t r0 = (int8_t)rand_local[0], r1 = (int8_t)rand_local[1];
    int8_t r2 = (int8_t)rand_local[2], r3 = (int8_t)rand_local[3];
    int8_t r4 = (int8_t)rand_local[4], r5 = (int8_t)rand_local[5];

    /* Copier sres_in dans sres_out */
    memcpy(sres_out, sres_in, 16);

    /* Allouer le planning de clé (0x110 = 272 octets) */
    uint8_t *ks = (uint8_t *)malloc(0x110);

    /* Round 0 : planning depuis key, chiffrement, round2=0 */
    safer_key_schedule((const uint8_t *)key, ks);
    safer_encrypt_block(sres_out, ks, 0);

    /* XOR/addition croisée avec rand_local répété (6 octets sur 16) */
    sres_out[0]  = (uint8_t)(r0 + (sres_out[0]  ^ sres_in[0]));
    sres_out[1]  = (uint8_t)(r1 + (sres_out[1]  ^ sres_in[1]));
    sres_out[2]  = (uint8_t)(r2 + (sres_out[2]  ^ sres_in[2]));
    sres_out[3]  = (uint8_t)(r3 + (sres_out[3]  ^ sres_in[3]));
    sres_out[4]  = (uint8_t)(r4 + (sres_out[4]  ^ sres_in[4]));
    sres_out[5]  = (uint8_t)(r5 + (sres_out[5]  ^ sres_in[5]));
    sres_out[6]  = (uint8_t)(r0 + (sres_out[6]  ^ sres_in[6]));
    sres_out[7]  = (uint8_t)(r1 + (sres_out[7]  ^ sres_in[7]));
    sres_out[8]  = (uint8_t)(r2 + (sres_out[8]  ^ sres_in[8]));
    sres_out[9]  = (uint8_t)(r3 + (sres_out[9]  ^ sres_in[9]));
    sres_out[10] = (uint8_t)(r4 + (sres_out[10] ^ sres_in[10]));
    sres_out[11] = (uint8_t)(r5 + (sres_out[11] ^ sres_in[11]));
    sres_out[12] = (uint8_t)(r0 + (sres_out[12] ^ sres_in[12]));
    sres_out[13] = (uint8_t)(r1 + (sres_out[13] ^ sres_in[13]));
    sres_out[14] = (uint8_t)(r2 + (sres_out[14] ^ sres_in[14]));
    sres_out[15] = (uint8_t)(r3 + (sres_out[15] ^ sres_in[15]));

    /* Dériver key2 depuis key (transformations XOR/add fixes — Ghidra @0010112c) */
    uint8_t key2[16];
    key2[0]  = (uint8_t)((uint8_t)key[0]  + (uint8_t)(-0x17)); /* - 0x17 */
    key2[1]  = (uint8_t)key[1]  ^ 0xe5;
    key2[2]  = (uint8_t)((uint8_t)key[2]  + (uint8_t)(-0x21));
    key2[3]  = (uint8_t)key[3]  ^ 0xc1;
    key2[4]  = (uint8_t)((uint8_t)key[4]  + (uint8_t)(-0x4d));
    key2[5]  = (uint8_t)key[5]  ^ 0xa7;
    key2[6]  = (uint8_t)((uint8_t)key[6]  + (uint8_t)(-0x6b));
    key2[7]  = (uint8_t)key[7]  ^ 0x83;
    key2[8]  = (uint8_t)key[8]  ^ 0xe9;
    key2[9]  = (uint8_t)((uint8_t)key[9]  + (uint8_t)(-0x1b));
    key2[10] = (uint8_t)key[10] ^ 0xdf;
    key2[11] = (uint8_t)((uint8_t)key[11] + (uint8_t)(-0x3f));
    key2[12] = (uint8_t)key[12] ^ 0xb3;
    key2[13] = (uint8_t)((uint8_t)key[13] + (uint8_t)(-0x59));
    key2[14] = (uint8_t)key[14] ^ 0x95;
    key2[15] = (uint8_t)((uint8_t)key[15] + (uint8_t)(-0x7d));

    /* Round 1 : planning depuis key2, chiffrement, round2=1 */
    safer_key_schedule(key2, ks);
    safer_encrypt_block(sres_out, ks, 1);

    free(ks);
}

/* =====================================================================
 * function_E21 — E21 Bluetooth : dérivation de clé de liaison
 *
 * void function_E21(uint8_t *key6,   uint8_t *rand16,  uint8_t *out16)
 *
 * Conforme décompilation Ghidra @001014a0 :
 *   1. Construire out16 depuis key6 répété cycliquement (16 octets)
 *   2. Construire key16 depuis rand16 avec rand16[15] ^ 0x06
 *   3. Planning de clé depuis key16, chiffrement SAFER+ (round2=1)
 * ===================================================================== */
void function_E21(const uint8_t *key6, const uint8_t *rand16, uint8_t *out16)
{
    /* Répétition cyclique de key6 sur 16 octets */
    out16[0]  = key6[0]; out16[1]  = key6[1]; out16[2]  = key6[2];
    out16[3]  = key6[3]; out16[4]  = key6[4]; out16[5]  = key6[5];
    out16[6]  = key6[0]; out16[7]  = key6[1]; out16[8]  = key6[2];
    out16[9]  = key6[3]; out16[10] = key6[4]; out16[11] = key6[5];
    out16[12] = key6[0]; out16[13] = key6[1]; out16[14] = key6[2];
    out16[15] = key6[3];

    /* Clé SAFER+ depuis rand16 avec XOR sur le dernier octet */
    uint8_t key16[16];
    memcpy(key16, rand16, 15);
    key16[15] = rand16[15] ^ 0x06;

    uint8_t *ks = (uint8_t *)malloc(0x110);
    safer_key_schedule(key16, ks);
    safer_encrypt_block(out16, ks, 1);
    free(ks);
}

/* =====================================================================
 * function_xiaomi — Composition E1 + E21 (authentification mutuelle Xiaomi)
 *
 * void function_xiaomi(uint8_t *bd_addr, uint8_t *rand_or_flag,
 *                      uint8_t *link_key, uint8_t *out)
 *
 * Conforme décompilation Ghidra @00101b98 :
 *   Appelle FUN_00101130 (≡ E1) puis function_E21.
 * ===================================================================== */
void function_xiaomi(const uint8_t *bd_addr, const uint8_t *rand_or_flag,
                     const uint8_t *link_key, uint8_t *out)
{
    /* Step 1: E1 — authentification */
    function_E1test((const char *)bd_addr,
                    (const uint8_t *)rand_or_flag,
                    (const char *)link_key,
                    out);

    /* Step 2: E21 — dérivation de clé sur le résultat E1 */
    function_E21(bd_addr, out, out);
}

/* =====================================================================
 * attach_current_thread — Obtenir JNIEnv pour le thread courant
 *
 * Conforme décompilation Ghidra @00101c78.
 * ===================================================================== */
void attach_current_thread(JavaVM *jvm)
{
    JNIEnv *env = NULL;
    jint ret = (*jvm)->GetEnv(jvm, (void **)&env, JNI_VERSION_1_6);
    if (ret < 0) {
        ret = (*jvm)->AttachCurrentThread(jvm, (void **)&env, NULL);
        if (ret < 0) env = NULL;
    }
    (void)env;
}

/* =====================================================================
 * detach_current_thread — Détacher le thread de la JVM
 *
 * Conforme décompilation Ghidra @00101d04.
 * ===================================================================== */
void detach_current_thread(JavaVM *jvm)
{
    (*jvm)->DetachCurrentThread(jvm);
}

/* =====================================================================
 * Prototypes des implémentations JNI (définitions après register_*)
 * ===================================================================== */
static jboolean   jni_nativeInit               (JNIEnv *, jobject);
static jbyteArray jni_getRandomAuthData        (JNIEnv *, jobject);
static jbyteArray jni_getRandomAuthCheckData   (JNIEnv *, jobject);
static jint       jni_getEncryptedAuthCheckData(JNIEnv *, jobject, jbyteArray);
static jbyteArray jni_getEncryptedAuthData     (JNIEnv *, jobject, jbyteArray);
static jbyteArray jni_setLinkKey               (JNIEnv *, jobject, jbyteArray);

/* Table de méthodes JNI (6 entrées, conforme register_xm_bluetooth @00101d10) */
static const JNINativeMethod g_methods[] = {
    { "nativeInit",                "()Z",    (void *)jni_nativeInit                },
    { "getRandomAuthData",         "()[B",   (void *)jni_getRandomAuthData          },
    { "getRandomAuthCheckData",    "()[B",   (void *)jni_getRandomAuthCheckData     },
    { "getEncryptedAuthCheckData", "([B)[B", (void *)jni_getEncryptedAuthCheckData  },
    { "getEncryptedAuthData",      "([B)I",  (void *)jni_getEncryptedAuthData       },
    { "setLinkKey",                "([B)[B", (void *)jni_setLinkKey                 },
};

/* =====================================================================
 * register_xm_bluetooth — Enregistrement JNI (symbol exporté)
 *
 * Conforme décompilation Ghidra @00101d10.
 * ===================================================================== */
jint register_xm_bluetooth(JNIEnv *env)
{
    jclass cls = (*env)->FindClass(env, "com/xiaomi/aivsbluetoothsdk/impl/BluetoothAuth");
    if (cls == NULL) return (jint)0xffffffff;
    (*env)->RegisterNatives(env, cls, g_methods, 6);
    return JNI_VERSION_1_6;
}

/* =====================================================================
 * JNI_OnLoad — Point d'entrée chargement bibliothèque (symbol exporté)
 *
 * Conforme décompilation Ghidra @00101bd4 :
 *   1. GetEnv → 0x10006
 *   2. register_xm_bluetooth()
 *   3. Logging en cas d'erreur
 * ===================================================================== */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved)
{
    (void)reserved;
    JNIEnv *env = NULL;
    jint ret = (*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_6);
    if (ret == 0) {
        g_jvm = vm;
        jint r = register_xm_bluetooth(env);
        if (r != 0) return JNI_VERSION_1_6;
        __android_log_print(ANDROID_LOG_ERROR, "XM_JNI", "load xm_bluetooth lib failed.");
    } else {
        __android_log_print(ANDROID_LOG_ERROR, "XM_JNI", "GetEnv failed!");
        return (jint)0xffffffff;
    }
    return JNI_VERSION_1_6;
}

/* =====================================================================
 * Implémentations JNI
 * ===================================================================== */

/*
 * nativeInit (()Z) — Ghidra @00101d74
 * Initialise la référence globale à l'objet BluetoothAuth.
 */
static jboolean jni_nativeInit(JNIEnv *env, jobject thiz)
{
    (*env)->MonitorEnter(env, (jobject)(void *)&g_jvm);
    if (g_class) {
        (*env)->DeleteGlobalRef(env, g_class);
        g_class = NULL;
    }
    g_class = (*env)->NewGlobalRef(env, thiz);

    jclass cls = (*env)->GetObjectClass(env, thiz);
    if (cls == NULL) {
        jclass fallback = (*env)->FindClass(env, "com/xiaomi/aivsbluetoothsdk/impl/BluetoothAuth");
        if (fallback) {
            (*env)->ThrowNew(env, fallback, "Can not find class");
        }
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

/*
 * getRandomAuthData (()[B) — Ghidra @00101e20
 * Retourne 17 octets : buf[0]=0x00, buf[1..16] = rand()&0xff × 16.
 */
static jbyteArray jni_getRandomAuthData(JNIEnv *env, jobject thiz)
{
    (void)thiz;
    uint8_t buf[17];
    buf[0] = 0x00;
    for (int i = 1; i <= 16; i++) buf[i] = (uint8_t)(rand() & 0xff);
    jbyteArray arr = (*env)->NewByteArray(env, 17);
    (*env)->SetByteArrayRegion(env, arr, 0, 17, (const jbyte *)buf);
    return arr;
}

/*
 * getRandomAuthCheckData (()[B) — Ghidra @00101f2c
 * Retourne 16 octets aléatoires.
 */
static jbyteArray jni_getRandomAuthCheckData(JNIEnv *env, jobject thiz)
{
    (void)thiz;
    uint8_t buf[16];
    for (int i = 0; i < 16; i++) buf[i] = (uint8_t)(rand() & 0xff);
    jbyteArray arr = (*env)->NewByteArray(env, 16);
    (*env)->SetByteArrayRegion(env, arr, 0, 16, (const jbyte *)buf);
    return arr;
}

/*
 * getEncryptedAuthCheckData (([B)[B→jint) — Ghidra @00102034
 * Stocke les 16 octets de la clé dans g_link_key.
 * Retourne 0 si succès, 3 si null ou longueur incorrecte.
 */
static jint jni_getEncryptedAuthCheckData(JNIEnv *env, jobject thiz, jbyteArray key_arr)
{
    (void)thiz;
    if (key_arr == NULL) return 3;
    jsize len = (*env)->GetArrayLength(env, key_arr);
    if (len != 16) return 3;
    jbyte *data = (*env)->GetByteArrayElements(env, key_arr, NULL);
    memcpy(g_link_key, data, 16);
    (*env)->ReleaseByteArrayElements(env, key_arr, data, JNI_ABORT);
    return 0;
}

/*
 * getEncryptedAuthData (([B)I) — Ghidra @001020cc
 * Calcule la réponse d'authentification chiffrée.
 * Retourne jbyteArray de 17 octets (out[0]=0x01, out[1..16] = résultat SAFER+).
 */
static jbyteArray jni_getEncryptedAuthData(JNIEnv *env, jobject thiz, jbyteArray rand_arr)
{
    (void)thiz;
    uint8_t out[17];
    out[0] = 0x01;

    if (rand_arr == NULL) {
        function_xiaomi(g_bd_addr, out, g_link_key, out);
    } else {
        jbyte *rd = (*env)->GetByteArrayElements(env, rand_arr, NULL);
        function_xiaomi(g_bd_addr, (const uint8_t *)(rd + 1), g_link_key, out);
        if (rd) (*env)->ReleaseByteArrayElements(env, rand_arr, rd, JNI_ABORT);
    }

    jbyteArray arr = (*env)->NewByteArray(env, 17);
    (*env)->SetByteArrayRegion(env, arr, 0, 17, (const jbyte *)out);
    return arr;
}

/*
 * setLinkKey (([B)[B) — Ghidra @00102200
 * Calcule la réponse de liaison chiffrée.
 * Retourne jbyteArray de 16 octets.
 */
static jbyteArray jni_setLinkKey(JNIEnv *env, jobject thiz, jbyteArray key_arr)
{
    (void)thiz;
    uint8_t out[16];
    memset(out, 0, 16);

    if (key_arr == NULL) {
        function_xiaomi(g_bd_addr, NULL, g_link_key, out);
    } else {
        jbyte *kd = (*env)->GetByteArrayElements(env, key_arr, NULL);
        function_xiaomi(g_bd_addr, (const uint8_t *)kd, g_link_key, out);
        if (kd) (*env)->ReleaseByteArrayElements(env, key_arr, kd, JNI_ABORT);
    }

    jbyteArray arr = (*env)->NewByteArray(env, 16);
    (*env)->SetByteArrayRegion(env, arr, 0, 16, (const jbyte *)out);
    return arr;
}
