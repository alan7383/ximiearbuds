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

    @Test
    fun `test functionE1 mutual authentication and JNI methods parity`() {
        val rand = ByteArray(16) { it.toByte() }
        val linkKey = ByteArray(16) { (15 - it).toByte() }

        val e1Result = BluetoothAuthEngine.functionE1(rand, linkKey)
        assertEquals(16, e1Result.size)

        // getEncryptedAuthData returns 17 bytes (0x01 prefix + 16-byte E1 output)
        val authData = BluetoothAuthEngine.getEncryptedAuthData(rand, linkKey)
        assertEquals(17, authData.size)
        assertEquals(0x01.toByte(), authData[0])
        assertArrayEquals(e1Result, authData.sliceArray(1..16))

        // getEncryptedAuthCheckData returns 16 bytes
        val authCheckData = BluetoothAuthEngine.getEncryptedAuthCheckData(rand, linkKey)
        assertEquals(16, authCheckData.size)
        assertArrayEquals(e1Result, authCheckData)

        // getRandomAuthCheckData returns 17 bytes with 0x00 prefix
        val randCheck = BluetoothAuthEngine.getRandomAuthCheckData()
        assertEquals(17, randCheck.size)
        assertEquals(0x00.toByte(), randCheck[0])
    }
}
