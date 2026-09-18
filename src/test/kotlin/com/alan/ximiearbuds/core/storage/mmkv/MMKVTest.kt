package com.alan.ximiearbuds.core.storage.mmkv

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class MMKVTest {

    @TempDir
    lateinit var tempDir: File

    @BeforeEach
    fun setup() {
        MMKV.initialize(tempDir)
    }

    @Test
    fun `test basic primitive encoding and decoding`() {
        val mmkv = MMKV.mmkvWithID("test_primitives")

        mmkv.encode("bool_true", true)
        mmkv.encode("bool_false", false)
        mmkv.encode("int_val", 4242)
        mmkv.encode("int_neg", -1337)
        mmkv.encode("long_val", 9876543210123L)
        mmkv.encode("float_val", 3.14159f)
        mmkv.encode("double_val", 2.718281828459045)
        mmkv.encode("string_val", "Xiaomi Buds 5 Pro")
        mmkv.encode("bytes_val", byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05))
        mmkv.encode("set_val", setOf("ANC", "Transparency", "Off"))

        assertTrue(mmkv.decodeBool("bool_true"))
        assertFalse(mmkv.decodeBool("bool_false"))
        assertEquals(4242, mmkv.decodeInt("int_val"))
        assertEquals(-1337, mmkv.decodeInt("int_neg"))
        assertEquals(9876543210123L, mmkv.decodeLong("long_val"))
        assertEquals(3.14159f, mmkv.decodeFloat("float_val"), 0.0001f)
        assertEquals(2.718281828459045, mmkv.decodeDouble("double_val"), 0.0000001)
        assertEquals("Xiaomi Buds 5 Pro", mmkv.decodeString("string_val"))
        assertArrayEquals(byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05), mmkv.decodeBytes("bytes_val"))
        assertEquals(setOf("ANC", "Transparency", "Off"), mmkv.decodeStringSet("set_val"))
    }

    @Test
    fun `test persistence across reload from disk`() {
        val mmkv1 = MMKV.mmkvWithID("test_persistence")
        mmkv1.encode("device_model", "Redmi Buds 6 Pro")
        mmkv1.encode("anc_level", 2)
        mmkv1.encode("spatial_audio", true)

        // Force reload from disk
        val mmkv2 = MMKV.mmkvWithID("test_persistence")
        mmkv2.clearMemoryCache()
        assertEquals("Redmi Buds 6 Pro", mmkv2.decodeString("device_model"))
        assertEquals(2, mmkv2.decodeInt("anc_level"))
        assertTrue(mmkv2.decodeBool("spatial_audio"))
    }

    @Test
    fun `test removal of keys and tombstones`() {
        val mmkv = MMKV.mmkvWithID("test_removal")
        mmkv.encode("temp_key", "to_be_deleted")
        assertTrue(mmkv.containsKey("temp_key"))

        mmkv.removeValueForKey("temp_key")
        assertFalse(mmkv.containsKey("temp_key"))
        assertNull(mmkv.decodeString("temp_key"))
    }

    @Test
    fun `test compaction and expansion under heavy log writes`() {
        val mmkv = MMKV.mmkvWithID("test_compaction")

        for (i in 0 until 500) {
            mmkv.encode("counter", i)
            mmkv.encode("device_status_$i", "status_data_payload_$i")
        }

        assertEquals(499, mmkv.decodeInt("counter"))
        assertEquals("status_data_payload_499", mmkv.decodeString("device_status_499"))
        assertTrue(mmkv.actualSize() > 0)
        assertTrue(mmkv.totalSize() >= 4096)
    }

    @Test
    fun `test AES-128 CFB encryption and reKey`() {
        val mmkv = MMKV.mmkvWithID("test_encrypted", cryptKey = "secretPass123456")
        assertTrue(mmkv.isEncryptionEnabled())
        assertEquals("secretPass123456", mmkv.cryptKey())

        mmkv.encode("secret_token", "xiaomi_auth_token_xyz")
        mmkv.encode("user_id", 10086)

        assertEquals("xiaomi_auth_token_xyz", mmkv.decodeString("secret_token"))
        assertEquals(10086, mmkv.decodeInt("user_id"))

        // ReKey to new password
        assertTrue(mmkv.reKey("newSecretPass987"))
        assertEquals("newSecretPass987", mmkv.cryptKey())
        assertEquals("xiaomi_auth_token_xyz", mmkv.decodeString("secret_token"))
        assertEquals(10086, mmkv.decodeInt("user_id"))
    }

    @Test
    fun `test auto-key expiration and TTL`() {
        val mmkv = MMKV.mmkvWithID("test_expiration")
        mmkv.enableAutoKeyExpire(1) // 1 second TTL
        assertTrue(mmkv.isExpirationEnabled())

        mmkv.encode("expiring_key", "temporary_val")
        assertEquals("temporary_val", mmkv.decodeString("expiring_key"))
        assertTrue(mmkv.containsKey("expiring_key"))

        // Wait 1.2 seconds for expiration
        Thread.sleep(1500)

        assertFalse(mmkv.containsKey("expiring_key"))
        assertNull(mmkv.decodeString("expiring_key"))
        assertEquals(0L, mmkv.count(filterExpire = true))
    }

    @Test
    fun `test compareBeforeSet optimization`() {
        val mmkv = MMKV.mmkvWithID("test_compare_before_set")
        mmkv.enableCompareBeforeSet()
        assertTrue(mmkv.isCompareBeforeSetEnabled())

        mmkv.encode("constant_key", "fixed_value")
        val sizeBefore = mmkv.actualSize()

        // Same write should not grow actualSize
        mmkv.encode("constant_key", "fixed_value")
        val sizeAfter = mmkv.actualSize()

        assertEquals(sizeBefore, sizeAfter)
    }

    @Test
    fun `test backup and restore operations`(@TempDir backupDir: File) {
        val mmkv = MMKV.mmkvWithID("test_backup")
        mmkv.encode("important_data", "do_not_lose_me")

        val backupSuccess = MMKV.backupOneToDirectory("test_backup", backupDir)
        assertTrue(backupSuccess)

        // Corrupt or clear original
        mmkv.clearAll()
        assertNull(mmkv.decodeString("important_data"))

        // Restore from backup
        val restoreSuccess = MMKV.restoreOneMMKVFromDirectory("test_backup", backupDir)
        assertTrue(restoreSuccess)

        assertEquals("do_not_lose_me", mmkv.decodeString("important_data"))
    }

    @Test
    fun `test library metadata and constants`() {
        assertEquals("v1.3.15", MMKV.version())
        assertEquals(4096, MMKV.pageSize())
        val metaMMKV = MMKV.mmkvWithID("test_meta")
        metaMMKV.encode("status", 1)
        assertTrue(MMKV.isFileValid("test_meta"))
    }
}
