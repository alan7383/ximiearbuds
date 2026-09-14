package com.alan.ximiearbuds.core.protocol

/**
 * 1:1 Exhaustive port of official constants decompiled from Xiaomi Earbuds (com.mi.earphone).
 * Sources:
 * - com.mi.earphone.bluetoothsdk.constant.CMDConstantKt
 * - com.mi.earphone.bluetoothsdk.constant.DeviceConfigIdConstantKt
 * - com.mi.earphone.device.manager.export.Function
 * - com.mi.earphone.bluetoothsdk.setting.gesture.GestureClick
 * - com.mi.earphone.bluetoothsdk.setting.gesture.GestureType
 * - com.mi.earphone.bluetoothsdk.setting.function.NoiseLevelConstants
 * - com.mi.earphone.bluetoothsdk.setting.lab.FindDeviceConstant
 */
object OfficialCommands {
    const val CMD_DATA: Int = 1
    const val CMD_GET_TARGET_INFO: Int = 2
    const val CMD_REBOOT_DEVICE: Int = 3
    const val CMD_NOTIFY_DEVICE_APP_INFO: Int = 4
    const val CMD_SETTINGS_COMMUNICATION_MTU: Int = 5
    const val CMD_DISCONNECT_CLASSIC_BLUETOOTH: Int = 6
    const val CMD_F2A_EDR_STATUS: Int = 7
    const val CMD_SET_TARGET_INFO: Int = 8
    const val CMD_GET_DEVICE_RUN_INFO: Int = 9
    const val CMD_NOTIFY_COMMUNICATION_WAY: Int = 10
    const val CMD_WAKEUP_CLASSIC_BLUETOOTH: Int = 11
    const val CMD_NOTIFY_PHONE_VIRTUAL_ADDR: Int = 12
    const val CMD_NOTIFY_F2A_BT_OP: Int = 13
    const val CMD_NOTIFY_DEVICE_CHANGE: Int = 14
    const val CMD_RESET_BT_OP: Int = 15
    const val NOTIFY_A2F_STATUS: Int = 16
    const val ONEMORE_DEVICE_INFO: Int = 1
    const val CMD_ASR_REQUEST: Int = 48
    const val CMD_ASR_RESULT: Int = 49
    const val CMD_TTS_REQUEST: Int = 50
    const val CMD_TTS_RESULT: Int = 51
    const val CMD_NLP_REQUEST: Int = 52
    const val CMD_NLP_RESULT: Int = 53
    const val CMD_ENTER_LOG_UPLOAD_MODE: Int = 193
    const val CMD_LOG_UPLOAD_PRECESS: Int = 194
    const val CMD_EXIT_LOG_UPLOAD_MODE: Int = 195
    const val CMD_LOG_UPLOAD_ROLE_SWITCH: Int = 196
    const val CMD_START_SPEECH: Int = 208
    const val CMD_STOP_SPEECH: Int = 209
    const val CMD_CANCEL_SPEECH: Int = 210
    const val CMD_LONG_HOLD_SPEECH: Int = 211
    const val CMD_OTA_GET_DEVICE_UPDATE_FILE_INFO_OFFSET: Int = 225
    const val CMD_OTA_INQUIRE_DEVICE_IF_CAN_UPDATE: Int = 226
    const val CMD_OTA_ENTER_UPDATE_MODE: Int = 227
    const val CMD_OTA_EXIT_UPDATE_MODE: Int = 228
    const val CMD_OTA_SEND_FIRMWARE_UPDATE_BLOCK: Int = 229
    const val CMD_OTA_GET_DEVICE_REFRESH_FIRMWARE_STATUS: Int = 230
    const val CMD_OTA_NOTIFY_UBOOT_UPDATE_MODE: Int = 231
}

object OfficialConfigIds {
    const val CONFIG_DATA_GET_FAIL: Int = -1
    const val CONFIG_AUDIO_MODE: Int = 1
    const val CONFIG_CUSTOM_CLICK: Int = 2
    const val CONFIG_AUTO_ANSWER_PHONE: Int = 3
    const val CONFIG_MULTIPOINT_CONNECTION: Int = 4
    const val CONFIG_OPEN_COMPACTNESS: Int = 5
    const val CONFIG_COMPACTNESS_LISTENER: Int = 6
    const val CONFIG_EQ_MODEL: Int = 7
    const val FIND_DEVICE: Int = 9
    const val NOISE_MODE_CHOOSE: Int = 10
    const val NOISE_LEVEL_CHOOSE: Int = 11
    const val REMIND_LOST: Int = 12
    const val DEVICE_CALL_LISTENER: Int = 13
    const val SWITCH_SPATIAL_AUDIO: Int = 29
    const val SPATIAL_AUDIO_CONFIG: Int = 30
    const val CONFIG_VIRTUAL_SURROUND: Int = 36
    const val AUTO_NOISE: Int = 37
    const val GET_EARPHONE_SN: Int = 39
    const val DEVICE_TIME: Int = 40
    const val ADAPTIVE_SENSE: Int = 41
    const val LOW_LATENCY: Int = 47
    const val BIG_DATA_TRANSFER: Int = 52
    const val SCENE_RENDERING: Int = 54
    const val CUSTOM_EQ: Int = 55
    const val DONGLE_STATUS: Int = 56
    const val SPATIAL_AUDIO_NOTIFY_SOUND: Int = 58
    const val PERSONALIZED_NOISE_REDUCTION: Int = 59
    const val EAR_CANAL_DETECTION: Int = 60
    const val EAR_CANAL_EAR_CANAL_FIT: Int = 61
    const val EAR_CANAL_DETECTION_EXIST: Int = 62
    const val VOICE_STATUS: Int = 68
    const val VOICE_LIST: Int = 69
    const val VOICE_CONFIG: Int = 70
    const val EARPHONE_VERSION: Int = 71
    const val ADAPTIVE_VOLUME: Int = 72
    const val BLUETOOTH_INFO: Int = 74
    const val START_RECORD: Int = 75
    const val SEND_REGION: Int = 76
    const val DONGLE_MODE: Int = 77
    const val PERSONAL_SPATIAL_AUDIO: Int = 79
    const val BONE_CONDUCTION_GESTURE: Int = 80
    const val DONGLE_MONITOR_SWITCH: Int = 81
    const val DONGLE_MONITOR_VOLUME: Int = 82
    const val DONGLE_GESTURE: Int = 83
    const val SWIM_LENGTH: Int = 84
    const val PERSONAL_SPATIAL_AUDIO_STATUS: Int = 85
    const val RECORD_CODEC_HEADER: Int = 86
    const val PERSONAL_SPATIAL_AUDIO_CONTROL: Int = 87
    const val AUDIBILITY_ADAPTATION: Int = 89
    const val RESTRICTED_CONFIG: Int = 93
    const val TTS_SPEAKER_STATUS: Int = 95
    const val VOICE_BROADCAST_SWITCH: Int = 96
    const val VOICE_BROADCAST_RATE: Int = 97
    const val VOICE_BROADCAST_INTERRUPTION: Int = 100
    const val SMART_DENOISE_STATUS: Int = 102
    const val COMMUTING_IMMERSE_STATUS: Int = 103
    const val DOLBY_AUDIO_SPATIAL_MODE: Int = 104
    const val RECORD_CONFIG: Int = 112
    const val RECORD_GESTURE: Int = 113
    const val EARBOX_SOUND_LIST_CONFIG: Int = 114
    const val EARBOX_SOUND_CONFIG: Int = 115
    const val EARBOX_SOUND_SET: Int = 116
    const val DOLBY_AUDIO_IS_SUPPORTED: Int = 118
    const val TRANSLATE_RECORD_HEADER: Int = 119
    const val TRANSLATE_SETTINGS: Int = 120
    const val AIVS_WAKE_UP_SWITCH: Int = 122
    const val AIVS_CONTINUOUS_DIALOGUE_DURATION: Int = 123
    const val AIVS_VOICE_TONE: Int = 124
    const val AIVS_SETTING: Int = 125
    const val DUAL_CONNECTION_SYNC: Int = 126
    const val AIVS_CODEC_HEADER: Int = 127
    const val NOTIFICATION_VOLUME: Int = 161
    const val LOG_MODE: Int = 61166
}

object OfficialFunctions {
    const val FUNC_ANC_NOISE_THREE: Int = 1002
    const val FUNC_ANC_NOISE_SIX: Int = 1003
    const val FUNC_ANC_TRANSPARENT_TWO: Int = 1004
    const val FUNC_ANC_TRANSPARENT_THREE: Int = 1005
    const val FUNC_ANC_NOISE_FOUR: Int = 1006
    const val FUNC_ANC_NOISE_TWO: Int = 1007
    const val FUNC_SMART_NOISE_REDUCTION: Int = 1008
    const val FUNC_NOISE_IMMERSE: Int = 1009

    const val FUNC_SOUND_SETTING_VIRTUAL_SURROUND: Int = 2001
    const val FUNC_SOUND_SETTING_CLOSE_SPATIAL_AUDIO: Int = 2002
    const val FUNC_SOUND_SETTING_SPATIAL_AUDIO_PREFERENCE: Int = 2003
    const val FUNC_SOUND_SETTING_SPATIAL_AUDIO_HEAD_TRACKING: Int = 2004
    const val FUNC_SOUND_SETTING_SPATIAL_AUDIO_VIRTUAL_SURROUND: Int = 2005
    const val FUNC_SOUND_SETTINGS_SOUND_MODE: Int = 2006
    const val FUNC_SOUND_SETTINGS_SPATIAL_AUDIO_NOTIFY: Int = 2007
    const val FUNC_SOUND_SETTINGS_ADAPTIVE_SENSE: Int = 2008
    const val FUNC_SOUND_EFFECT_RENDERING: Int = 2009
    const val FUNC_SOUND_SETTINGS_SOUND_MODE_OTA: Int = 2016
    const val FUNC_SOUND_SETTINGS_ADAPTIVE_VOLUME: Int = 2017
    const val FUNC_PERSONAL_SPATIAL_AUDIO: Int = 2018
    const val FUNC_AUDIBILITY_ADAPTATION: Int = 2019
    const val FUNC_AUDIO_MODE: Int = 2020
    const val FUNC_NOTIFICATION_VOLUME: Int = 2021

    const val FUNC_SMART_FREE_PICK: Int = 3001
    const val FUNC_WEAR_DETECTION: Int = 3002
    const val FUNC_FIT_DETECT: Int = 3003
    const val FUNC_DUAL_DEVICE_CONNECTION: Int = 3004
    const val FUNC_VOICE_CONTROL: Int = 3005
    const val FUNC_HEARING_HEALTH: Int = 3006
    const val FUNC_NOTIFICATION: Int = 3007
    const val FUNC_AUTO_PICK_CALL: Int = 3008
    const val FUNC_AUTO_GAMING_MODE: Int = 3009
    const val FUNC_CUSTOM_SKIN: Int = 3010
    const val FUNC_LOW_LATENCY: Int = 3011
    const val FUNC_PERSONALIZED_NOISE_REDUCTION: Int = 3012
    const val FUNC_EAR_CANAL_DETECTION: Int = 3013
    const val FUNC_NOTIFICATION_TTS: Int = 3014
    const val FUNC_EARBOX_SOUND: Int = 3015

    const val FUNC_GESTURE_DOUBLE_CLICK: Int = 4001
    const val FUNC_GESTURE_TRIPLE_CLICK: Int = 4002
    const val FUNC_GESTURE_LONG_PRESS: Int = 4003
    const val FUNC_GESTURE_PRESS_TWICE: Int = 4004
    const val FUNC_GESTURE_PRESS_TRIPLE: Int = 4005
    const val FUNC_GESTURE_PRESS_ONCE: Int = 4006
    const val FUNC_GESTURE_SLIDE: Int = 4011
    const val FUNC_DOUBLE_MFB: Int = 4012
    const val FUNC_TRIPLE_MFB: Int = 4013
    const val FUNC_LONG_PRESS_MFB: Int = 4014
    const val FUNC_LONG_PRESS_REDUCE: Int = 4015
    const val FUNC_LONG_PRESS_MFB_AND_REDUCE: Int = 4016
    const val FUNC_LONG_PRESS_ADD_AND_REDUCE: Int = 4017
    const val FUNC_ONCE_MFB: Int = 4018

    const val FUNC_FIND_DEVICE: Int = 5001
    const val FUNC_DEVICE_INTRODUCE: Int = 5002
    const val FUNC_DEVICE_RECORD: Int = 5003
    const val FUNC_FIRMWARE_UPDATE: Int = 5004
    const val FUNC_BEGINNER_GUIDE: Int = 5005
    const val FUNC_EXERCISE_REPORT: Int = 5006
    const val FUNC_DEVICE_TRANSLATE: Int = 5008
    const val SUPER_AI: Int = 5009

    const val FUNC_LABORATORY_NOISE: Int = 6001
    const val FUNC_LABORATORY_WEAR_DETECTION: Int = 6002

    const val FUNC_USB_MODE: Int = 7001
    const val FUNC_USB_EAR_MONITOR: Int = 7002
    const val FUNC_USB_CLICK: Int = 7003
    const val FUNC_USB_DOUBLE_CLICK: Int = 7004
    const val FUNC_USB_LONG_PRESS: Int = 7005

    const val FUNC_SWIM_LENGTH: Int = 8001
}

object OfficialGestureClicks {
    const val DOUBLE_CLICK: Int = 1
    const val TRIPLE_CLICK: Int = 2
    const val LONG_PRESS: Int = 3
    const val ONCE_PRESS: Int = 4
    const val SLIDE: Int = 5
}

object OfficialGestureTypes {
    const val WAKEUP_XIAOAI: Int = 0
    const val PLAY_OR_PAUSE: Int = 1
    const val PREVIOUS: Int = 2
    const val NEXT: Int = 3
    const val INCREASE_VOLUME: Int = 4
    const val REDUCE_VOLUME: Int = 5
    const val NOISE_CONTROL: Int = 6
    const val GAME_MODE: Int = 7
    const val CANCEL: Int = 8
    const val TAKE_PHOTO: Int = 9
    const val RECORD: Int = 10
    const val VOLUME_CHANGE: Int = 11
    const val CONVERSATION_TRANSLATE: Int = 12
    const val PREVIOUS_NEXT: Int = 13
}

object OfficialNoiseConstants {
    const val ANC_MODE_CLOSE: Byte = 0
    const val ANC_MODE_NOISE: Byte = 1
    const val ANC_MODE_TRANSPARENT: Byte = 2

    const val ANC_LEVEL_NOISE_BALANCE: Byte = 0
    const val ANC_LEVEL_NOISE_COMFORTABLE: Byte = 1
    const val ANC_LEVEL_NOISE_DEEP: Byte = 2
    const val ANC_LEVEL_NOISE_ADAPTIVE: Byte = 3
    const val ANC_LEVEL_NOISE_ANTI_WIND: Byte = 4
    const val ANC_LEVEL_NOISE_DEEP_PLUS: Byte = 5

    const val ANC_LEVEL_TRANSPARENT_TRANSPARENT: Byte = 0
    const val ANC_LEVEL_TRANSPARENT_VOCAL_ENHANCEMENT: Byte = 1
    const val ANC_LEVEL_TRANSPARENT_ENV_ENHANCEMENT: Byte = 2
    const val ANC_LEVEL_TRANSPARENT_VOCAL_ENHANCEMENT_PLUS: Byte = 3

    const val ANC_WEAR_TYPE_NO_WEAR: Int = 1
    const val ANC_WEAR_TYPE_ONE_WEAR: Int = 2
    const val ANC_WEAR_TYPE_BOTH_WEAR: Int = 3

    const val IMMERSE_CLOSE_MODE: Int = 0
    const val IMMERSE_FLIGHT_MODE: Int = 1
    const val IMMERSE_SUBWAY_MODE: Int = 2
    const val IMMERSE_HSR_MODE: Int = 3

    const val XIAOMI_AUDIO_MODE: Int = 0
    const val DUBI_AUDIO_MODE: Int = 1
}

object OfficialFindDeviceConstants {
    const val FIND_DEVICE_CHOOSE_STATUE_NORMAL: Int = 0
    const val FIND_DEVICE_CHOOSE_STATUE_CHOOSED: Int = 1
    const val FIND_DEVICE_CHOOSE_STATUE_UNENABLE: Int = 2

    const val FIND_DEVICE_STATE_LEFT: Int = 1
    const val FIND_DEVICE_STATE_RIGHT: Int = 2
    const val FIND_DEVICE_STATE_ALL: Int = 3
    const val FIND_DEVICE_STATE_UNENABLE: Int = 4
}

object OfficialRcspAttrTypes {
    const val ATTR_TYPE_PROTOCOL_VERSION: Int = 0
    const val ATTR_TYPE_FIRMWARE_VERSION: Int = 1
    const val ATTR_TYPE_HARDWARE_VERSION: Int = 2
    const val ATTR_TYPE_VID_PID: Int = 3
    const val ATTR_TYPE_NAME: Int = 4
    const val ATTR_TYPE_MAC_ADDR: Int = 5
    const val ATTR_TYPE_BATTERY: Int = 7
    const val ATTR_TYPE_COLOR_TYPE: Int = 13
    const val ATTR_TYPE_SPATIAL_AUDIO: Int = 14
}

