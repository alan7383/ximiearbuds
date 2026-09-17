package com.alan.ximiearbuds.core.crypto

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BluetoothAuthEngineTest {

    @Test
    fun `test SAFER+ E21 encryption with standard vector 1`() {
        val input = byteArrayOf(
            0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
            0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10
        )
        val expectedHex = "facd31a7ec313d13a4cec64d52d27e21"

        val actual = BluetoothAuthEngine.encrypt(input)
        val actualHex = actual.joinToString("") { "%02x".format(it) }

        assertEquals(expectedHex, actualHex)
        assertTrue(BluetoothAuthEngine.verifyResponse(input, actual))
    }

    @Test
    fun `test SAFER+ E21 encryption with all zeroes vector`() {
        val input = ByteArray(16)
        val expectedHex = "bca5905bc849392e7bf9fdcdc570ef77"

        val actual = BluetoothAuthEngine.encrypt(input)
        val actualHex = actual.joinToString("") { "%02x".format(it) }

        assertEquals(expectedHex, actualHex)
        assertTrue(BluetoothAuthEngine.verifyResponse(input, actual))
    }

    @Test
    fun `test random factor generation and verification`() {
        val rand = BluetoothAuthEngine.generateRandomFactor()
        assertEquals(16, rand.size)

        val encrypted = BluetoothAuthEngine.encrypt(rand)
        assertEquals(16, encrypted.size)
        assertTrue(BluetoothAuthEngine.verifyResponse(rand, encrypted))

        // Mismatched response should fail verification
        val corrupted = encrypted.clone()
        corrupted[0] = (corrupted[0] + 1).toByte()
        assertFalse(BluetoothAuthEngine.verifyResponse(rand, corrupted))
    }
}
