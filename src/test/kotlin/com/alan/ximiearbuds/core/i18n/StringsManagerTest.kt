package com.alan.ximiearbuds.core.i18n

import com.alan.ximiearbuds.ui.theme.AppLanguage
import com.alan.ximiearbuds.ui.theme.StringsManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class StringsManagerTest {

    @Test
    fun testAuthenticStringsInFrench() {
        val fr = StringsManager.getStrings(AppLanguage.FR)
        assertEquals("Annulation du bruit", fr["device_settings_noise_reduction"])
        assertEquals("Désactivé", fr["device_settings_noise_reduction_close"])
        assertEquals("Transparence", fr["device_settings_noise_reduction_transparent"])
        assertEquals("G", fr["device_settings_left"])
        assertEquals("D", fr["device_settings_right"])
        assertEquals("Boîtier", fr["device_settings_box"])
        assertEquals("Égaliseur", fr["device_settings_audio_equalizer"])
        assertEquals("Gestes", fr["device_settings_gesture_operation"])
    }

    @Test
    fun testAuthenticStringsInEnglish() {
        val en = StringsManager.getStrings(AppLanguage.EN)
        assertEquals("Noise cancellation", en["device_settings_noise_reduction"])
        assertEquals("Off", en["device_settings_noise_reduction_close"])
        assertEquals("Transparency", en["device_settings_noise_reduction_transparent"])
        assertEquals("L", en["device_settings_left"])
        assertEquals("R", en["device_settings_right"])
        assertEquals("Case", en["device_settings_box"])
        assertEquals("Equalizer", en["device_settings_audio_equalizer"])
        assertEquals("Gestures", en["device_settings_gesture_operation"])
    }

    @Test
    fun testSoundPresetsAndGesturesStrings() {
        val fr = StringsManager.getStrings(AppLanguage.FR)
        val en = StringsManager.getStrings(AppLanguage.EN)

        // Presets
        assertNotEquals("device_settings_sound_balanced_listening", fr["device_settings_sound_balanced_listening"])
        assertNotEquals("device_settings_sound_vocal_enhancement", fr["device_settings_sound_vocal_enhancement"])
        assertNotEquals("device_settings_sound_bass_boost", fr["device_settings_sound_bass_boost"])
        assertNotEquals("device_settings_sound_treble_boost", fr["device_settings_sound_treble_boost"])
        assertNotEquals("device_settings_sound_spatial_audio", fr["device_settings_sound_spatial_audio"])
        assertNotEquals("device_settings_sound_virtual_surround", fr["device_settings_sound_virtual_surround"])

        // Gestures
        assertNotEquals("device_settings_left_click", fr["device_settings_left_click"])
        assertNotEquals("device_settings_left_slide", fr["device_settings_left_slide"])
        assertNotEquals("device_settings_volume_changed", fr["device_settings_volume_changed"])

        // More settings
        assertNotEquals("device_settings_wear_detection", fr["device_settings_wear_detection"])
        assertNotEquals("device_settings_fit_detection", fr["device_settings_fit_detection"])
        assertNotEquals("device_settings_earbox_sound", fr["device_settings_earbox_sound"])
        assertNotEquals("device_settings_dual_device_connect", fr["device_settings_dual_device_connect"])
        assertNotEquals("device_settings_auto_pick_call", fr["device_settings_auto_pick_call"])
        assertNotEquals("device_settings_remove_device", fr["device_settings_remove_device"])
    }

    @Test
    fun testAllOfficialModelsCovered() {
        val models = com.alan.ximiearbuds.core.device.DeviceRegistry.ALL_MODELS
        // Must have at least 39 models defined
        kotlin.test.assertTrue(models.size >= 39, "Expected at least 39 official models, got ${models.size}")

        // Check key models exist
        val codenames = models.map { it.codename }.toSet()
        val expected = listOf(
            "J77S", "K73", "K73a", "K75", "K75sStarWars", "L71", "L76", "L77", "L77s",
            "M75A", "M79A", "M79I", "N74A", "N75", "N76", "N76G", "N76GIP", "N77", "N77Ip",
            "N77S", "N79", "N79A", "N79B", "N79S", "O70C", "O71", "O71BT", "O71Wifi", "O73",
            "O74", "O76", "O76G", "O77", "O77S", "P75", "P76", "P76Bose", "P79", "Q74u"
        )
        for (code in expected) {
            kotlin.test.assertTrue(codenames.contains(code), "Model $code should be present in DeviceRegistry")
        }
    }
}
