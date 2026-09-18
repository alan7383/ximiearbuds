package com.alan.ximiearbuds.core.account

import com.alan.ximiearbuds.core.device.DevicePreferences
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Base64

class XiaomiAuthEngineTest {

    @BeforeEach
    fun setUp() {
        XiaomiAccountClient.logout()
    }

    @Test
    fun testPasswordMd5UppercaseHash() {
        // Standard MD5 for "123456" is e10adc3949ba59abbe56e057f20f883e -> Uppercase
        val hash = XiaomiAuthCoder.hashPassword("123456")
        assertEquals("E10ADC3949BA59ABBE56E057F20F883E", hash)

        // Standard MD5 for empty string
        val emptyHash = XiaomiAuthCoder.hashPassword("")
        assertEquals("D41D8CD98F00B204E9800998ECF8427E", emptyHash)
    }

    @Test
    fun testAesCoderEncryptAndDecryptRoundTrip() {
        // 16-byte AES key encoded in Base64 (ssecurity)
        val keyBytes = ByteArray(16) { (it * 7).toByte() }
        val ssecurity = Base64.getEncoder().encodeToString(keyBytes)

        val sampleText = "user_account_id_9988776655"
        val encrypted = XiaomiAuthCoder.encryptAes(sampleText, ssecurity)
        assertNotNull(encrypted)
        assertNotEquals(sampleText, encrypted)

        val decrypted = XiaomiAuthCoder.decryptAes(encrypted, ssecurity)
        assertEquals(sampleText, decrypted)
    }

    @Test
    fun testSignatureCoderDeterministic() {
        val keyBytes = ByteArray(16) { 0x42 }
        val ssecurity = Base64.getEncoder().encodeToString(keyBytes)

        val params = mapOf(
            "userId" to "12345678",
            "sid" to "passportapi",
            "transId" to "abcdef123456789",
            "flags" to "3"
        )

        val sig1 = XiaomiAuthCoder.generateSignature("GET", "/pass/v2/safe/user/coreInfo", params, ssecurity)
        val sig2 = XiaomiAuthCoder.generateSignature("GET", "/pass/v2/safe/user/coreInfo", params, ssecurity)

        assertNotNull(sig1)
        assertTrue(sig1.isNotEmpty())
        assertEquals(sig1, sig2, "Signature must be deterministic for identical inputs")
    }

    @Test
    fun testClientSignDeterministic() {
        val keyBytes = ByteArray(16) { 0x11 }
        val ssecurity = Base64.getEncoder().encodeToString(keyBytes)
        val nonce = 1726665544332L

        val sign1 = XiaomiAuthCoder.generateClientSign(nonce, ssecurity)
        val sign2 = XiaomiAuthCoder.generateClientSign(nonce, ssecurity)

        assertNotNull(sign1)
        assertEquals(sign1, sign2)
    }

    @Test
    fun testRemoveSafePrefix() {
        val raw = "&&&START&&&{\"code\":0,\"desc\":\"success\"}"
        val clean = XiaomiAuthCoder.removeSafePrefix(raw)
        assertEquals("{\"code\":0,\"desc\":\"success\"}", clean)

        val withoutPrefix = "{\"code\":70016}"
        assertEquals(withoutPrefix, XiaomiAuthCoder.removeSafePrefix(withoutPrefix))
    }

    @Test
    fun testLogoutClearsPreferences() {
        DevicePreferences.saveAccountInfo(
            isLoggedIn = true,
            userId = "99887766",
            userName = "Test User",
            avatarAddress = "/tmp/avatar.jpg"
        )

        assertTrue(DevicePreferences.isLoggedIn())
        assertEquals("99887766", DevicePreferences.getUserId())
        assertEquals("Test User", DevicePreferences.getUserName())

        XiaomiAccountClient.logout()

        assertFalse(DevicePreferences.isLoggedIn())
        assertNull(DevicePreferences.getUserId())
        assertNull(DevicePreferences.getUserName())
        assertNull(DevicePreferences.getAvatarAddress())
    }

    @Test
    fun testSignatureSortOrderIndependence() {
        val ssecurity = Base64.getEncoder().encodeToString(ByteArray(16) { 0x55 })

        val map1 = linkedMapOf("zebra" to "1", "alpha" to "2", "beta" to "3")
        val map2 = linkedMapOf("alpha" to "2", "beta" to "3", "zebra" to "1")

        val sig1 = XiaomiAuthCoder.generateSignature("POST", "/test", map1, ssecurity)
        val sig2 = XiaomiAuthCoder.generateSignature("POST", "/test", map2, ssecurity)

        assertEquals(sig1, sig2, "Signature must be sorted alphabetically by keys regardless of insertion order")
    }

    @Test
    fun testLoginValidationEmptyFields() = kotlinx.coroutines.runBlocking {
        val res1 = XiaomiAccountClient.loginWithPassword("", "password123")
        assertTrue(res1 is XiaomiAccountClient.AuthResult.Error)
        assertEquals(-1, (res1 as XiaomiAccountClient.AuthResult.Error).code)

        val res2 = XiaomiAccountClient.loginWithPassword("user", "")
        assertTrue(res2 is XiaomiAccountClient.AuthResult.Error)
        assertEquals(-1, (res2 as XiaomiAccountClient.AuthResult.Error).code)

        val res3 = XiaomiAccountClient.loginWithPassword("   ", "   ")
        assertTrue(res3 is XiaomiAccountClient.AuthResult.Error)
    }
}
