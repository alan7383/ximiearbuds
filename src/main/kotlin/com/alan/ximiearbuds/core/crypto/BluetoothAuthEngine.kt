package com.alan.ximiearbuds.core.crypto

import java.security.SecureRandom

/**
 * Bit-exact implementation of the Xiaomi Bluetooth Authentication Protocol ($E_{21}$ / SAFER+).
 * Reverse-engineered directly from official `libxm_bluetooth.so` (`function_E21`, `FUN_00101630`, `FUN_0010176c`)
 * and verified against physical Redmi Buds 6 Pro hardware.
 */
object BluetoothAuthEngine {

    // Hardcoded BD_ADDR from libxm_bluetooth.so at DAT_0010a6c0: 11 22 33 33 22 11
    val DUMMY_BD_ADDR = byteArrayOf(0x11, 0x22, 0x33, 0x33, 0x22, 0x11)

    private val EXP_TABLE = intArrayOf(
        1, 45, 226, 147, 190, 69, 21, 174, 120, 3, 135, 164, 184, 56, 207, 63,
        8, 103, 9, 148, 235, 38, 168, 107, 189, 24, 52, 27, 187, 191, 114, 247,
        64, 53, 72, 156, 81, 47, 59, 85, 227, 192, 159, 216, 211, 243, 141, 177,
        255, 167, 62, 220, 134, 119, 215, 166, 17, 251, 244, 186, 146, 145, 100, 131,
        241, 51, 239, 218, 44, 181, 178, 43, 136, 209, 153, 203, 140, 132, 29, 20,
        129, 151, 113, 202, 95, 163, 139, 87, 60, 130, 196, 82, 92, 28, 232, 160,
        4, 180, 133, 74, 246, 19, 84, 182, 223, 12, 26, 142, 222, 224, 57, 252,
        32, 155, 36, 78, 169, 152, 158, 171, 242, 96, 208, 108, 234, 250, 199, 217,
        0, 212, 31, 110, 67, 188, 236, 83, 137, 254, 122, 93, 73, 201, 50, 194,
        249, 154, 248, 109, 22, 219, 89, 150, 68, 233, 205, 230, 70, 66, 143, 10,
        193, 204, 185, 101, 176, 210, 198, 172, 30, 65, 98, 41, 46, 14, 116, 80,
        2, 90, 195, 37, 123, 138, 42, 91, 240, 6, 13, 71, 111, 112, 157, 126,
        16, 206, 18, 39, 213, 76, 79, 214, 121, 48, 104, 54, 117, 125, 228, 237,
        128, 106, 144, 55, 162, 94, 118, 170, 197, 127, 61, 175, 165, 229, 25, 97,
        253, 77, 124, 183, 11, 238, 173, 75, 34, 245, 231, 115, 35, 33, 200, 5,
        225, 102, 221, 179, 88, 105, 99, 86, 15, 161, 49, 149, 23, 7, 58, 40
    )

    private val LOG_TABLE = intArrayOf(
        128, 0, 176, 9, 96, 239, 185, 253, 16, 18, 159, 228, 105, 186, 173, 248,
        192, 56, 194, 101, 79, 6, 148, 252, 25, 222, 106, 27, 93, 78, 168, 130,
        112, 237, 232, 236, 114, 179, 21, 195, 255, 171, 182, 71, 68, 1, 172, 37,
        201, 250, 142, 65, 26, 33, 203, 211, 13, 110, 254, 38, 88, 218, 50, 15,
        32, 169, 157, 132, 152, 5, 156, 187, 34, 140, 99, 231, 197, 225, 115, 198,
        175, 36, 91, 135, 102, 39, 247, 87, 244, 150, 177, 183, 92, 139, 213, 84,
        121, 223, 170, 246, 62, 163, 241, 17, 202, 245, 209, 23, 123, 147, 131, 188,
        189, 82, 30, 235, 174, 204, 214, 53, 8, 200, 138, 180, 226, 205, 191, 217,
        208, 80, 89, 63, 77, 98, 52, 10, 72, 136, 181, 86, 76, 46, 107, 158,
        210, 61, 60, 3, 19, 251, 151, 81, 117, 74, 145, 113, 35, 190, 118, 42,
        95, 249, 212, 85, 11, 220, 55, 49, 22, 116, 215, 119, 167, 230, 7, 219,
        164, 47, 70, 243, 97, 69, 103, 227, 12, 162, 59, 28, 133, 24, 4, 29,
        41, 160, 143, 178, 90, 216, 166, 126, 238, 141, 83, 75, 161, 154, 193, 14,
        122, 73, 165, 44, 129, 196, 199, 54, 43, 127, 67, 149, 51, 242, 108, 104,
        109, 240, 2, 40, 206, 221, 155, 234, 94, 153, 124, 20, 134, 207, 229, 66,
        184, 64, 120, 45, 58, 233, 100, 31, 146, 144, 125, 57, 111, 224, 137, 48
    )

    private val RAW_BIAS_TABLE = intArrayOf(
        100, 172, 40, 90, 201, 179, 55, 197, 10, 16, 183, 163, 186, 177, 151, 70,
        61, 5, 220, 102, 110, 246, 154, 248, 13, 88, 149, 103, 198, 170, 171, 236,
        160, 104, 155, 150, 212, 235, 191, 67, 73, 54, 233, 106, 137, 216, 195, 138,
        148, 99, 153, 188, 123, 190, 193, 34, 187, 92, 113, 213, 31, 146, 87, 93,
        143, 68, 65, 29, 81, 230, 64, 23, 251, 253, 25, 50, 52, 184, 97, 42,
        202, 35, 111, 218, 57, 247, 162, 1, 127, 214, 49, 231, 222, 128, 4, 221,
        44, 89, 130, 175, 168, 224, 15, 205, 161, 18, 62, 48, 209, 28, 208, 58,
        51, 114, 46, 79, 144, 2, 19, 6, 117, 206, 135, 194, 239, 178, 173, 125,
        56, 21, 225, 82, 159, 122, 108, 47, 39, 196, 226, 129, 169, 207, 141, 192,
        215, 223, 255, 96, 118, 20, 140, 94, 85, 9, 228, 8, 199, 66, 32, 252,
        210, 80, 145, 217, 76, 98, 158, 232, 185, 166, 249, 26, 0, 33, 11, 250,
        53, 156, 78, 75, 105, 72, 203, 14, 200, 164, 91, 234, 132, 7, 180, 24,
        244, 174, 107, 219, 167, 204, 63, 139, 74, 12, 60, 37, 229, 84, 77, 69,
        131, 237, 17, 240, 176, 83, 147, 242, 116, 38, 181, 157, 109, 124, 243, 45,
        241, 86, 36, 126, 71, 27, 134, 189, 112, 142, 30, 59, 115, 22, 3, 182,
        172, 40, 90, 201, 179, 55, 197, 10, 16, 183, 163, 186, 177, 151, 70, 136
    )

    private fun rol3(b: Int): Int = ((b shl 3) or (b ushr 5)) and 0xFF

    private fun generateSubkeys(key16: IntArray): Array<IntArray> {
        val subkeys = Array(17) { IntArray(16) }
        for (i in 0 until 16) {
            subkeys[0][i] = key16[i]
        }

        var parity = 0
        val buf = IntArray(17)
        for (i in 0 until 16) {
            buf[i] = key16[i]
            parity = parity xor key16[i]
        }
        buf[16] = parity

        var biasPtr = 15
        for (r in 1..16) {
            for (i in 0 until 17) {
                buf[i] = rol3(buf[i])
            }
            var currBias = biasPtr
            for (j in 0 until 16) {
                val cVar2 = buf[(r + j) % 17]
                val bVal = RAW_BIAS_TABLE[currBias]
                subkeys[r][j] = (bVal + cVar2) and 0xFF
                currBias--
            }
            biasPtr += 16
        }
        return subkeys
    }

    private inline fun isMaskBit(i: Int): Boolean =
        ((1 shl i) and 0x9999) != 0

    private fun mul2Add(a: Int, b: Int): Pair<Int, Int> =
        Pair((2 * a + b) and 0xFF, (a + b) and 0xFF)

    /**
     * Executes SAFER+ encryption ($E_{21}$) matching `FUN_0010176c` in `libxm_bluetooth.so`.
     */
    private fun cipherCore(block: IntArray, subkeys: Array<IntArray>, isE21: Boolean = true): ByteArray {
        val state = block.clone()
        val orig = block.clone()

        for (r in 0 until 8) {
            if (isE21 && r == 2) {
                for (i in 0 until 16) {
                    state[i] = if (isMaskBit(i)) {
                        state[i] xor orig[i]
                    } else {
                        (state[i] + orig[i]) and 0xFF
                    }
                }
            }

            val skA = subkeys[2 * r]
            for (i in 0 until 16) {
                state[i] = if (isMaskBit(i)) {
                    state[i] xor skA[i]
                } else {
                    (state[i] + skA[i]) and 0xFF
                }
            }

            for (i in 0 until 16) {
                state[i] = if (isMaskBit(i)) EXP_TABLE[state[i]] else LOG_TABLE[state[i]]
            }

            val skB = subkeys[2 * r + 1]
            for (i in 0 until 16) {
                state[i] = if (isMaskBit(i)) {
                    (state[i] + skB[i]) and 0xFF
                } else {
                    state[i] xor skB[i]
                }
            }

            // Pseudo-Hadamard Transform (PHT) Layer
            val (cVar17, cVar1) = mul2Add(state[0], state[1])
            val (cVar8, cVar2) = mul2Add(state[2], state[3])
            val (cVar10, cVar11) = mul2Add(state[4], state[5])
            val (cVar3, cVar16) = mul2Add(state[6], state[7])
            val (cVar5, cVar12) = mul2Add(state[8], state[9])
            val (cVar9, cVar19) = mul2Add(state[10], state[11])
            val (cVar7, cVar14) = mul2Add(state[12], state[13])
            val (cVar4, cVar6) = mul2Add(state[14], state[15])

            val (cVar18, cVar19_2) = mul2Add(cVar5, cVar19)
            val (cVar5_2, cVar6_2) = mul2Add(cVar7, cVar6)
            val (cVar7_2, cVar8_2) = mul2Add(cVar8, cVar1)
            val (cVar13, cVar3_2) = mul2Add(cVar3, cVar11)
            val (cVar1_2, cVar9_2) = mul2Add(cVar9, cVar12)
            val (cVar15, cVar4_2) = mul2Add(cVar4, cVar14)
            val (cVar11_2, cVar16_2) = mul2Add(cVar17, cVar16)
            val (cVar17_2, cVar10_2) = mul2Add(cVar10, cVar2)

            val (cVar2_2, cVar4_3) = mul2Add(cVar1_2, cVar4_2)
            val (cVar1_3, cVar11_3) = mul2Add(cVar11_2, cVar10_2)
            val (cVar12_2, cVar13_2) = mul2Add(cVar13, cVar8_2)
            val (cVar14_2, cVar15_2) = mul2Add(cVar15, cVar9_2)
            val (cVar8_3, cVar16_3) = mul2Add(cVar17_2, cVar16_2)
            val (cVar17_3, cVar18_2) = mul2Add(cVar18, cVar3_2)
            val (cVar3_3, cVar6_3) = mul2Add(cVar7_2, cVar6_2)

            state[0] = (2 * cVar14_2 + cVar16_3) and 0xFF
            state[1] = (cVar14_2 + cVar16_3) and 0xFF
            state[2] = (2 * cVar17_3 + cVar6_3) and 0xFF
            state[3] = (cVar17_3 + cVar6_3) and 0xFF
            state[4] = (2 * cVar1_3 + cVar4_3) and 0xFF
            state[5] = (cVar1_3 + cVar4_3) and 0xFF
            state[6] = (2 * cVar12_2 + ((cVar5_2 + cVar19_2) and 0xFF)) and 0xFF
            state[7] = (cVar12_2 + ((cVar5_2 + cVar19_2) and 0xFF)) and 0xFF
            state[8] = (2 * cVar8_3 + cVar15_2) and 0xFF
            state[9] = (cVar8_3 + cVar15_2) and 0xFF
            state[10] = (2 * cVar3_3 + cVar18_2) and 0xFF
            state[11] = (cVar3_3 + cVar18_2) and 0xFF
            state[12] = (2 * cVar2_2 + cVar13_2) and 0xFF
            state[13] = (cVar2_2 + cVar13_2) and 0xFF
            val cVar19Final = (2 * cVar5_2 + cVar19_2) and 0xFF
            state[14] = (2 * cVar19Final + cVar11_3) and 0xFF
            state[15] = (cVar19Final + cVar11_3) and 0xFF
        }

        val skOut = subkeys[16]
        val result = ByteArray(16)
        for (i in 0 until 16) {
            val v = if (isMaskBit(i)) {
                state[i] xor skOut[i]
            } else {
                (state[i] + skOut[i]) and 0xFF
            }
            result[i] = v.toByte()
        }
        return result
    }

    /**
     * Computes the expected 16-byte response from the 16-byte random challenge factor.
     * Corresponds to `function_E21(bd_addr, rand_factor, output)` in `libxm_bluetooth.so`.
     */
    fun encrypt(randFactor: ByteArray, bdAddr: ByteArray = DUMMY_BD_ADDR): ByteArray {
        require(randFactor.size == 16) { "randFactor must be exactly 16 bytes" }
        require(bdAddr.size >= 6) { "bdAddr must have at least 6 bytes" }

        val block = IntArray(16) { bdAddr[it % 6].toInt() and 0xFF }
        val key = IntArray(16) { randFactor[it].toInt() and 0xFF }
        key[15] = key[15] xor 0x06

        val subkeys = generateSubkeys(key)
        return cipherCore(block, subkeys, isE21 = true)
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
    fun verifyResponse(randFactor: ByteArray, earbudResponse: ByteArray, bdAddr: ByteArray = DUMMY_BD_ADDR): Boolean {
        if (randFactor.size != 16 || earbudResponse.size != 16) return false
        val expected = encrypt(randFactor, bdAddr)
        return expected.contentEquals(earbudResponse)
    }
}
