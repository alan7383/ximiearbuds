package com.alan.ximiearbuds.core.crypto

import java.security.SecureRandom

/**
 * Pure Kotlin implementation of the Xiaomi Bluetooth Authentication Protocol.
 * Reverse-engineered from official libxm_bluetooth.so and com.xiaomi.aivsbluetoothsdk.impl.BluetoothAuth.
 *
 * Implements the Bluetooth Core Specification SAFER+ ($E_{21}$) cipher customized by Xiaomi
 * for SPP / RCSP channel challenge-response authorization.
 */
object BluetoothAuthEngine {

    // Dummy BD_ADDR hardcoded in libxm_bluetooth.so at 0xa6c0: 11 22 33 33 22 11
    val DUMMY_BD_ADDR = byteArrayOf(0x11, 0x22, 0x33, 0x33, 0x22, 0x11)

    private val EXP_TABLE = IntArray(256)
    private val LOG_TABLE = IntArray(256)

    init {
        var p = 1
        for (x in 0 until 256) {
            val v = if (p % 257 == 256) 0 else (p % 257)
            EXP_TABLE[x] = v
            LOG_TABLE[v] = x
            p = (p * 45) % 257
        }
    }

    // Bias table for the 16 rounds of key expansion (256 bytes total)
    private val BIAS_TABLE = intArrayOf(
        0x46, 0x97, 0xB1, 0xBA, 0xA3, 0xB7, 0x10, 0x0A, 0xC5, 0x37, 0xB3, 0xC9, 0x5A, 0x28, 0xAC, 0x64,
        0xEC, 0xAB, 0xAA, 0xC6, 0x67, 0x95, 0x58, 0x0D, 0xF8, 0x9A, 0xF6, 0x6E, 0x66, 0xDC, 0x05, 0x3D,
        0x8A, 0xC3, 0xD8, 0x89, 0x6A, 0xE9, 0x36, 0x49, 0x43, 0xBF, 0xEB, 0xD4, 0x96, 0x9B, 0x68, 0xA0,
        0x5D, 0x57, 0x92, 0x1F, 0xD5, 0x71, 0x5C, 0xBB, 0x22, 0xC1, 0xBE, 0x7B, 0xBC, 0x99, 0x63, 0x94,
        0x2A, 0x61, 0xB8, 0x34, 0x32, 0x19, 0xFD, 0xFB, 0x17, 0x40, 0xE6, 0x51, 0x1D, 0x41, 0x44, 0x8F,
        0xDD, 0x04, 0x80, 0xDE, 0xE7, 0x31, 0xD6, 0x7F, 0x01, 0xA2, 0xF7, 0x39, 0xDA, 0x6F, 0x23, 0xCA,
        0x3A, 0xD0, 0x1C, 0xD1, 0x30, 0x3E, 0x12, 0xA1, 0xCD, 0x0F, 0xE0, 0xA8, 0xAF, 0x82, 0x59, 0x2C,
        0x7D, 0xAD, 0xB2, 0xEF, 0xC2, 0x87, 0xCE, 0x75, 0x06, 0x13, 0x02, 0x90, 0x4F, 0x2E, 0x72, 0x33,
        0xC0, 0x8D, 0xCF, 0xA9, 0x81, 0xE2, 0xC4, 0x27, 0x2F, 0x6C, 0x7A, 0x9F, 0x52, 0xE1, 0x15, 0x38,
        0xFC, 0x20, 0x42, 0xC7, 0x08, 0xE4, 0x09, 0x55, 0x5E, 0x8C, 0x14, 0x76, 0x60, 0xFF, 0xDF, 0xD7,
        0xFA, 0x0B, 0x21, 0x00, 0x1A, 0xF9, 0xA6, 0xB9, 0xE8, 0x9E, 0x62, 0x4C, 0xD9, 0x91, 0x50, 0xD2,
        0x18, 0xB4, 0x07, 0x84, 0xEA, 0x5B, 0xA4, 0xC8, 0x0E, 0xCB, 0x48, 0x69, 0x4B, 0x4E, 0x9C, 0x35,
        0x45, 0x4D, 0x54, 0xE5, 0x25, 0x3C, 0x0C, 0x4A, 0x8B, 0x3F, 0xCC, 0xA7, 0xDB, 0x6B, 0xAE, 0xF4,
        0x2D, 0xF3, 0x7C, 0x6D, 0x9D, 0xB5, 0x26, 0x74, 0xF2, 0x93, 0x53, 0xB0, 0xF0, 0x11, 0xED, 0x83,
        0xB6, 0x03, 0x16, 0x73, 0x3B, 0x1E, 0x8E, 0x70, 0xBD, 0x86, 0x1B, 0x47, 0x7E, 0x24, 0x56, 0xF1,
        0x88, 0x46, 0x97, 0xB1, 0xBA, 0xA3, 0xB7, 0x10, 0x0A, 0xC5, 0x37, 0xB3, 0xC9, 0x5A, 0x28, 0xAC
    )

    private fun rol3(b: Int): Int = ((b shl 3) or (b ushr 5)) and 0xFF

    private fun generateSubkeys(key16: IntArray): Array<IntArray> {
        val subkeys = Array(17) { IntArray(16) }
        val buf = IntArray(17)
        var parity = 0
        for (i in 0 until 16) {
            buf[i] = key16[i]
            subkeys[0][i] = key16[i]
            parity = parity xor key16[i]
        }
        buf[16] = parity

        var biasIdx = 0
        for (r in 1..16) {
            for (k in 0 until 17) {
                buf[k] = rol3(buf[k])
            }
            for (j in 0 until 16) {
                val idx = (r + j) % 17
                subkeys[r][j] = (buf[idx] + BIAS_TABLE[biasIdx++]) and 0xFF
            }
        }
        return subkeys
    }

    private fun phtLayer(state: IntArray) {
        var w16 = state[0]
        var w17 = state[1]
        var w3 = state[2]
        var w4 = state[3]
        var w5 = state[4]
        var w7 = (w17 + (w16 shl 1)) and 0xFF
        var w6 = state[5]
        w16 = (w17 + w16) and 0xFF
        var w19 = state[6]
        var w20 = (w4 + (w3 shl 1)) and 0xFF
        w17 = state[7]
        w3 = (w4 + w3) and 0xFF
        var w21 = state[8]
        var w22 = (w6 + (w5 shl 1)) and 0xFF
        w4 = state[9]
        w5 = (w6 + w5) and 0xFF
        var w23 = state[10]
        var w24 = (w17 + (w19 shl 1)) and 0xFF
        w6 = state[11]
        w17 = (w17 + w19) and 0xFF
        var w25 = state[12]
        var w26 = (w4 + (w21 shl 1)) and 0xFF
        w19 = state[13]
        w4 = (w4 + w21) and 0xFF
        var w27 = state[14]
        var w28 = (w6 + (w23 shl 1)) and 0xFF
        w21 = state[15]
        w6 = (w6 + w23) and 0xFF
        w23 = (w19 + (w25 shl 1)) and 0xFF
        w19 = (w19 + w25) and 0xFF
        w25 = (w21 + (w27 shl 1)) and 0xFF
        w21 = (w21 + w27) and 0xFF
        w27 = (w6 + (w26 shl 1)) and 0xFF
        w6 = (w6 + w26) and 0xFF
        w26 = (w21 + (w23 shl 1)) and 0xFF
        w21 = (w21 + w23) and 0xFF
        w23 = (w16 + (w20 shl 1)) and 0xFF
        w16 = (w20 + w16) and 0xFF
        w20 = (w5 + (w24 shl 1)) and 0xFF
        w5 = (w24 + w5) and 0xFF
        w24 = (w4 + (w28 shl 1)) and 0xFF
        w4 = (w28 + w4) and 0xFF
        w28 = (w19 + (w25 shl 1)) and 0xFF
        w19 = (w25 + w19) and 0xFF
        w25 = (w17 + (w7 shl 1)) and 0xFF
        w17 = (w17 + w7) and 0xFF
        w7 = (w3 + (w22 shl 1)) and 0xFF
        w3 = (w22 + w3) and 0xFF
        w22 = (w19 + (w24 shl 1)) and 0xFF
        w19 = (w19 + w24) and 0xFF
        w24 = (w3 + (w25 shl 1)) and 0xFF
        w3 = (w25 + w3) and 0xFF
        w25 = (w16 + (w20 shl 1)) and 0xFF
        w16 = (w20 + w16) and 0xFF
        w20 = (w4 + (w28 shl 1)) and 0xFF
        w4 = (w28 + w4) and 0xFF
        w28 = (w17 + (w7 shl 1)) and 0xFF
        w17 = (w17 + w7) and 0xFF
        val w30 = (w20 + w17) and 0xFF
        w17 = (w17 + (w20 shl 1)) and 0xFF
        w7 = (w5 + (w27 shl 1)) and 0xFF
        w5 = (w27 + w5) and 0xFF
        w27 = (w21 + (w23 shl 1)) and 0xFF
        w21 = (w21 + w23) and 0xFF
        state[0] = w17 and 0xFF
        w17 = (w19 + w24) and 0xFF
        w23 = (w26 + w6) and 0xFF
        w19 = (w19 + (w24 shl 1)) and 0xFF
        w20 = (w21 + w7) and 0xFF
        w7 = (w21 + (w7 shl 1)) and 0xFF
        state[5] = w17 and 0xFF
        w17 = (w23 + (w25 shl 1)) and 0xFF
        state[4] = w19 and 0xFF
        w19 = (w4 + w28) and 0xFF
        w4 = (w4 + (w28 shl 1)) and 0xFF
        state[2] = w7 and 0xFF
        state[6] = w17 and 0xFF
        w17 = (w27 + w5) and 0xFF
        w7 = (w23 + w25) and 0xFF
        w5 = (w5 + (w27 shl 1)) and 0xFF
        state[8] = w4 and 0xFF
        w4 = (w22 + w16) and 0xFF
        w16 = (w16 + (w22 shl 1)) and 0xFF
        state[11] = w17 and 0xFF
        w17 = (w6 + (w26 shl 1)) and 0xFF
        state[1] = w30 and 0xFF
        state[13] = w4 and 0xFF
        w4 = (w17 + w3) and 0xFF
        state[12] = w16 and 0xFF
        w16 = (w3 + (w17 shl 1)) and 0xFF
        state[3] = w20 and 0xFF
        state[7] = w7 and 0xFF
        state[9] = w19 and 0xFF
        state[10] = w5 and 0xFF
        state[15] = w4 and 0xFF
        state[14] = w16 and 0xFF
    }

    private fun isMaskBit(k: Int): Boolean {
        // Mask 0x9999 has bits 0, 3, 4, 7, 8, 11, 12, 15 set
        return (k == 0 || k == 3 || k == 4 || k == 7 || k == 8 || k == 11 || k == 12 || k == 15)
    }

    /**
     * Computes the expected 16-byte response from the 16-byte random challenge factor.
     * Uses dummy BD_ADDR (11:22:33:33:22:11) as specified by Xiaomi's protocol.
     */
    fun encrypt(randFactor: ByteArray): ByteArray {
        require(randFactor.size == 16) { "randFactor must be exactly 16 bytes" }

        val block = IntArray(16)
        val orig = IntArray(16)
        val key = IntArray(16)

        // Fill 16-byte block with repeated dummy BD_ADDR (11 22 33 33 22 11)
        val bd = DUMMY_BD_ADDR
        for (i in 0 until 16) {
            val v = bd[i % 6].toInt() and 0xFF
            block[i] = v
            orig[i] = v
        }

        // Key derivation: key[0..14] = randFactor[0..14], key[15] = randFactor[15] ^ 6
        for (i in 0 until 15) {
            key[i] = randFactor[i].toInt() and 0xFF
        }
        key[15] = (randFactor[15].toInt() and 0xFF) xor 0x06

        val subkeys = generateSubkeys(key)
        val state = block.clone()

        for (r in 0 until 8) {
            if (r == 2) {
                // E21 input block feedback addition after round 2
                for (k in 0 until 16) {
                    if (isMaskBit(k)) {
                        state[k] = state[k] xor orig[k]
                    } else {
                        state[k] = (state[k] + orig[k]) and 0xFF
                    }
                }
            }

            // 1. Key mixing with K[2*r]
            val skA = subkeys[2 * r]
            for (k in 0 until 16) {
                if (isMaskBit(k)) {
                    state[k] = state[k] xor skA[k]
                } else {
                    state[k] = (state[k] + skA[k]) and 0xFF
                }
            }

            // 2. S-box substitution
            for (k in 0 until 16) {
                state[k] = if (isMaskBit(k)) EXP_TABLE[state[k]] else LOG_TABLE[state[k]]
            }

            // 3. Key mixing with K[2*r + 1]
            val skB = subkeys[2 * r + 1]
            for (k in 0 until 16) {
                if (isMaskBit(k)) {
                    state[k] = (state[k] + skB[k]) and 0xFF
                } else {
                    state[k] = state[k] xor skB[k]
                }
            }

            // 4. PHT linear transformation layer
            phtLayer(state)
        }

        // Output key mixing with K[16]
        val skOut = subkeys[16]
        val result = ByteArray(16)
        for (k in 0 until 16) {
            val v = if (isMaskBit(k)) {
                state[k] xor skOut[k]
            } else {
                (state[k] + skOut[k]) and 0xFF
            }
            result[k] = v.toByte()
        }

        return result
    }

    /**
     * Generates a 16-byte cryptographically secure random challenge.
     */
    fun generateRandomFactor(): ByteArray {
        val random = SecureRandom()
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes
    }

    /**
     * Verifies that the response from the earbud matches the expected calculation.
     */
    fun verifyResponse(randFactor: ByteArray, earbudResponse: ByteArray): Boolean {
        if (randFactor.size != 16 || earbudResponse.size != 16) return false
        val expected = encrypt(randFactor)
        return expected.contentEquals(earbudResponse)
    }
}
