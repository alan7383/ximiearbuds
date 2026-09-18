package com.alan.ximiearbuds.core.crypto

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BluetoothAuthEngineTest {

    @Test
    fun `test SAFER+ E21 encryption against physical Redmi Buds 6 Pro response`() {
        val input = byteArrayOf(
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
            0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f
        )
        // Exact 16-byte response returned by physical hardware over RFCOMM
        val expectedHex = "8713913c41434e091cad794a1b5d95c4"

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
