package com.alan.ximiearbuds.core.parity

import com.alan.ximiearbuds.core.device.DevicePreferences
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Validates 1:1 Parity for Profile (MineFragment) & Security Code (SecurityCodeFragment)
 * against the decompiled official Xiaomi Earbuds APK (com.mi.earphone).
 */
class ProfileAndSecurityCodeParityTest {

    @Test
    fun `test account and profile state persistence in DevicePreferences`() {
        // Test login
        DevicePreferences.login("8192384729", "Alan")
        assertTrue(DevicePreferences.isLoggedIn())
        assertEquals("8192384729", DevicePreferences.getUserId())
        assertEquals("Alan", DevicePreferences.getUserName())

        // Test region change
        DevicePreferences.setRegion("France")
        assertEquals("France", DevicePreferences.getRegion())

        // Test experience program toggle
        DevicePreferences.setUserExperienceAccepted(false)
        assertFalse(DevicePreferences.isUserExperienceAccepted())
        DevicePreferences.setUserExperienceAccepted(true)
        assertTrue(DevicePreferences.isUserExperienceAccepted())

        // Test device association toggle
        DevicePreferences.setDeviceAssociated(false)
        assertFalse(DevicePreferences.isDeviceAssociated())
        DevicePreferences.setDeviceAssociated(true)
        assertTrue(DevicePreferences.isDeviceAssociated())

        // Test logout
        DevicePreferences.logout()
        assertFalse(DevicePreferences.isLoggedIn())
        assertNull(DevicePreferences.getUserId())
        assertNull(DevicePreferences.getUserName())
    }

    @Test
    fun `test official 20-digit security code validation algorithm from SecurityCodeFragment`() {
        // Replicates lines 80-93 of decompiled SecurityCodeFragment.java:
        // Strips all whitespace characters, then checks if exact length is 20 digits
        fun validateCode(input: String): Boolean {
            val sb = StringBuilder()
            for (c in input) {
                if (!c.isWhitespace()) {
                    sb.append(c)
                }
            }
            val cleaned = sb.toString()
            return cleaned.length == 20 && cleaned.all { it.isDigit() }
        }

        assertTrue(validateCode("1234 5678 9012 3456 7890"))
        assertTrue(validateCode("12345678901234567890"))
        assertTrue(validateCode("  1234 5678   9012 3456 7890  "))

        assertFalse(validateCode("1234 5678")) // Too short
        assertFalse(validateCode("123456789012345678901")) // 21 digits
        assertFalse(validateCode("1234 5678 9012 3456 ABCD")) // Non-digits
        assertFalse(validateCode("")) // Empty
    }

    @Test
    fun `test presence of all official Mine and Security string resources`() {
        val stringsFrFile = File("src/main/resources/strings/strings_fr.json")
        assertTrue(stringsFrFile.exists(), "strings_fr.json must exist")

        val jsonParser = Json { ignoreUnknownKeys = true }
        val rootFr = jsonParser.parseToJsonElement(stringsFrFile.readText()).jsonObject

        val expectedKeys = listOf(
            "mine_label",
            "mine_click_login",
            "mine_id_empty",
            "mine_user_id",
            "mine_area",
            "mine_system_permission_management",
            "mine_permission_granted",
            "mine_device_auth_title",
            "mine_device_auth_status_yes",
            "mine_device_auth_status_no",
            "mine_feedback",
            "mine_feedback_submit",
            "mine_app_current_version",
            "mine_user_agreement",
            "mine_user_privacy_policy",
            "mine_revoke_privacy_granted",
            "mine_revoke_privacy_granted_des",
            "mine_revoke_privacy_granted_des2",
            "mine_verify_revoke",
            "mine_user_experience_improvement",
            "mine_user_experience_improvement_engage",
            "mine_user_experience_improvement_engage_detail",
            "mine_security_title",
            "mine_security_tip_text",
            "mine_security_notify_text",
            "mine_security_title_immediately",
            "mine_security_verify_success_text",
            "mine_security_verify_times",
            "mine_security_notify_query_much_text",
            "mine_security_notifyto_verify",
            "mine_logout",
            "mine_logout_exit",
            "mine_logout_confirm",
            "cancel",
            "mine_case_number"
        )

        for (key in expectedKeys) {
            assertTrue(rootFr.containsKey(key), "strings_fr.json must contain key '$key'")
            val value = rootFr[key]?.toString()?.trim('"')
            assertNotNull(value, "Value for key '$key' should not be null")
            assertTrue(value!!.isNotEmpty(), "Value for key '$key' should not be empty")
        }
    }
}
