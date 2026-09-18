package com.alan.ximiearbuds.core.account

import java.net.URI
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import java.util.TreeMap
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Reverses Xiaomi Passport cryptographic algorithms:
 * - CloudCoder (MD5 uppercase, SHA1)
 * - AESCoder (AES/CBC/PKCS5Padding with IV "0102030405060708")
 * - SignatureCoder (URL request signature generation)
 * - STS ClientSign (nonce client signature)
 *
 * Sourced from decompiled com.xiaomi.accountsdk.utils:
 * - CloudCoder.java
 * - AESCoder.java
 * - SignatureCoder.java
 * - Coder.java
 */
object XiaomiAuthCoder {

    private val DEFAULT_IV = "0102030405060708".toByteArray(StandardCharsets.UTF_8)
    private const val AES_ALGORITHM = "AES/CBC/PKCS5Padding"

    /**
     * Reverses CloudCoder.getMd5DigestUpperCase(password)
     * Used as the 'hash' parameter in serviceLoginAuth2.
     */
    fun hashPassword(password: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(password.toByteArray(StandardCharsets.UTF_8))
        return bytesToHex(digest).uppercase()
    }

    /**
     * Reverses AESCoder.encrypt(plainText)
     * Uses ssecurity decoded from Base64 as the 16-byte AES key.
     */
    fun encryptAes(plainText: String, ssecurity: String): String {
        val keyBytes = Base64.getDecoder().decode(ssecurity)
        val keySpec = SecretKeySpec(keyBytes, "AES")
        val ivSpec = IvParameterSpec(DEFAULT_IV)

        val cipher = Cipher.getInstance(AES_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        val encrypted = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
        return Base64.getEncoder().encodeToString(encrypted)
    }

    /**
     * Reverses AESCoder.decrypt(cipherText)
     */
    fun decryptAes(cipherTextBase64: String, ssecurity: String): String {
        val keyBytes = Base64.getDecoder().decode(ssecurity)
        val keySpec = SecretKeySpec(keyBytes, "AES")
        val ivSpec = IvParameterSpec(DEFAULT_IV)

        val cipher = Cipher.getInstance(AES_ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
        val decodedBytes = Base64.getDecoder().decode(cipherTextBase64)
        val decrypted = cipher.doFinal(decodedBytes)
        return String(decrypted, StandardCharsets.UTF_8)
    }

    /**
     * Reverses SignatureCoder.generateSignature(method, path, map, ssecurity)
     * Format: METHOD&PATH&key1=val1&key2=val2&ssecurity -> SHA1 -> Base64
     */
    fun generateSignature(
        method: String?,
        pathOrUrl: String?,
        params: Map<String, String>,
        ssecurity: String
    ): String {
        val parts = ArrayList<String>()
        if (!method.isNullOrEmpty()) {
            parts.add(method.uppercase())
        }
        if (!pathOrUrl.isNullOrEmpty()) {
            val encodedPath = try {
                val uri = URI(pathOrUrl)
                uri.rawPath ?: pathOrUrl
            } catch (e: Exception) {
                pathOrUrl
            }
            parts.add(encodedPath)
        }
        if (params.isNotEmpty()) {
            val sortedMap = TreeMap(params)
            for ((key, value) in sortedMap) {
                parts.add("$key=$value")
            }
        }
        parts.add(ssecurity)

        val toSign = parts.joinToString("&")
        val sha1 = MessageDigest.getInstance("SHA-1")
        val hash = sha1.digest(toSign.toByteArray(StandardCharsets.UTF_8))
        return Base64.getEncoder().encodeToString(hash)
    }

    /**
     * Reverses XMPassport.getClientSign(nonce, ssecurity)
     * Used when calling STS autoLoginUrl: nonce=<nonce>&<ssecurity> -> SHA1 -> Base64
     */
    fun generateClientSign(nonce: Long, ssecurity: String): String {
        val params = mapOf("nonce" to nonce.toString())
        return generateSignature(null, null, params, ssecurity)
    }

    /**
     * Removes the Xiaomi safe prefix "&&&START&&&" from server response bodies.
     * Reverses XMPassport.removeSafePrefixAndGetRealBody.
     */
    fun removeSafePrefix(body: String): String {
        val prefix = "&&&START&&&"
        return if (body.startsWith(prefix)) {
            body.substring(prefix.length)
        } else {
            body
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }
}
