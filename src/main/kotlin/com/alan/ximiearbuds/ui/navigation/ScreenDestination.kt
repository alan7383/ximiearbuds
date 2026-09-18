package com.alan.ximiearbuds.ui.navigation

/**
 * Screen destinations matching the official Xiaomi Earbuds (com.mi.earphone) Fragment hierarchy.
 */
sealed class ScreenDestination(val titleResKey: String) {
    object MainSettings : ScreenDestination("app_name")
    object MoreSettings : ScreenDestination("device_settings_more_settings")
    object CustomizedEq : ScreenDestination("device_settings_audio_equalizer")
    object GestureControl : ScreenDestination("device_settings_gesture_operation")
    object SoundEffects : ScreenDestination("device_settings_sound_spatial_audio")
    object FindDevice : ScreenDestination("device_settings_find_device")
    object FitDetection : ScreenDestination("device_settings_fit_detection")
    object EarboxSound : ScreenDestination("device_settings_earbox_sound")
    object DeviceInfo : ScreenDestination("device_settings_about_device")
    object AddDevice : ScreenDestination("device_add_title")
    object ScanDevice : ScreenDestination("device_connect_device")
    object SpatialAudio : ScreenDestination("device_settings_sound_spatial_audio")
    object FirmwareUpdate : ScreenDestination("device_settings_firmware_update")
    object DongleSettings : ScreenDestination("device_settings_usb_mode")
    object XiaoAiSettings : ScreenDestination("device_settings_voice_control")
    object Laboratory : ScreenDestination("device_settings_laboratory_function_title")
    object PersonalSkin : ScreenDestination("device_settings_skin_title")
    object SportSettings : ScreenDestination("device_settings_sport_config")
    object VoiceTranslation : ScreenDestination("device_settings_audio_title")
    object BeginnerGuide : ScreenDestination("device_settings_beginner_guide")
    object Profile : ScreenDestination("mine_label")
    object SecurityCode : ScreenDestination("mine_security_title")
}
