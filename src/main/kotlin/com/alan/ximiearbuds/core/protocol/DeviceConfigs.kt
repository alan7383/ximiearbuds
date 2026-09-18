package com.alan.ximiearbuds.core.protocol

import java.nio.charset.StandardCharsets

/**
 * All Config IDs, parsed models, and byte converters for Xiaomi Earbuds.
 * Reverse-engineered from DeviceConfigIdConstantKt, BaseDeviceConfigKt,
 * DeviceConfigNoiseLevel, DeviceConfigCustomEq, DeviceConfigClickSet, and f7.f.
 */
object OfficialGroupIds {
    const val GROUP_NOISE = 1
    const val GROUP_VOICE = 2
    const val GROUP_FUNCTION_SETTING = 3
    const val GROUP_GESTURE_SETTING = 4
    const val GROUP_NORMAL = 5
    const val GROUP_LABORATORY = 6
    const val GROUP_USB = 7
    const val GROUP_SPORT = 8
}

object OfficialFunctionIds {
    // Group 1: Noise
    const val FUNC_ANC_SWITCH = 1001
    const val FUNC_ANC_NOISE_THREE = 1002
    const val FUNC_ANC_NOISE_SIX = 1003
    const val FUNC_ANC_TRANSPARENT_TWO = 1004
    const val FUNC_ANC_TRANSPARENT_THREE = 1005
    const val FUNC_ANC_NOISE_FOUR = 1006
    const val FUNC_ANC_NOISE_TWO = 1007
    const val FUNC_SMART_NOISE_REDUCTION = 1008
    const val FUNC_NOISE_IMMERSE = 1009

    // Group 2: Sound & EQ
    const val FUNC_SOUND_SETTING_VIRTUAL_SURROUND = 2001
    const val FUNC_SOUND_SETTING_CLOSE_SPATIAL_AUDIO = 2002
    const val FUNC_SOUND_SETTING_SPATIAL_AUDIO_PREFERENCE = 2003
    const val FUNC_SOUND_SETTING_SPATIAL_AUDIO_HEAD_TRACKING = 2004
    const val FUNC_SOUND_SETTING_SPATIAL_AUDIO_VIRTUAL_SURROUND = 2005
    const val FUNC_SOUND_SETTINGS_SOUND_MODE = 2006
    const val FUNC_SOUND_SETTINGS_SPATIAL_AUDIO_NOTIFY = 2007
    const val FUNC_SOUND_SETTINGS_ADAPTIVE_SENSE = 2008
    const val FUNC_SOUND_EFFECT_RENDERING = 2009
    const val FUNC_SOUND_SETTINGS_SOUND_MODE_OTA = 2016
    const val FUNC_SOUND_SETTINGS_ADAPTIVE_VOLUME = 2017
    const val FUNC_PERSONAL_SPATIAL_AUDIO = 2018
    const val FUNC_AUDIBILITY_ADAPTATION = 2019
    const val FUNC_AUDIO_MODE = 2020
    const val FUNC_NOTIFICATION_VOLUME = 2021

    // Group 3: Function Settings
    const val FUNC_SMART_FREE_PICK = 3001
    const val FUNC_WEAR_DETECTION = 3002
    const val FUNC_FIT_DETECT = 3003
    const val FUNC_DUAL_DEVICE_CONNECTION = 3004
    const val FUNC_VOICE_CONTROL = 3005
    const val FUNC_HEARING_HEALTH = 3006
    const val FUNC_NOTIFICATION = 3007
    const val FUNC_AUTO_PICK_CALL = 3008
    const val FUNC_AUTO_GAMING_MODE = 3009
    const val FUNC_CUSTOM_SKIN = 3010
    const val FUNC_LOW_LATENCY = 3011
    const val FUNC_PERSONALIZED_NOISE_REDUCTION = 3012
    const val FUNC_EAR_CANAL_DETECTION = 3013
    const val FUNC_NOTIFICATION_TTS = 3014
    const val FUNC_EARBOX_SOUND = 3015

    // Group 4: Gestures
    const val FUNC_GESTURE_DOUBLE_CLICK = 4001
    const val FUNC_GESTURE_TRIPLE_CLICK = 4002
    const val FUNC_GESTURE_LONG_PRESS = 4003
    const val FUNC_GESTURE_PRESS_TWICE = 4004
    const val FUNC_GESTURE_PRESS_TRIPLE = 4005
    const val FUNC_GESTURE_PRESS_ONCE = 4006
    const val FUNC_GESTURE_SLIDE = 4011
    const val FUNC_DOUBLE_MFB = 4012
    const val FUNC_TRIPLE_MFB = 4013
    const val FUNC_LONG_PRESS_MFB = 4014
    const val FUNC_LONG_PRESS_REDUCE = 4015
    const val FUNC_LONG_PRESS_MFB_AND_REDUCE = 4016
    const val FUNC_LONG_PRESS_ADD_AND_REDUCE = 4017
    const val FUNC_ONCE_MFB = 4018

    // Group 5: Device Management
    const val FUNC_FIND_DEVICE = 5001
    const val FUNC_DEVICE_INTRODUCE = 5002
    const val FUNC_DEVICE_RECORD = 5003
    const val FUNC_FIRMWARE_UPDATE = 5004
    const val FUNC_BEGINNER_GUIDE = 5005
    const val FUNC_EXERCISE_REPORT = 5006
    const val FUNC_DEVICE_TRANSLATE = 5008
    const val SUPER_AI = 5009

    // Group 6: Laboratory
    const val FUNC_LABORATORY_NOISE = 6001
    const val FUNC_LABORATORY_WEAR_DETECTION = 6002

    // Group 7: USB / Dongle
    const val FUNC_USB_MODE = 7001
    const val FUNC_USB_EAR_MONITOR = 7002
    const val FUNC_USB_CLICK = 7003
    const val FUNC_USB_DOUBLE_CLICK = 7004
    const val FUNC_USB_LONG_PRESS = 7005

    // Group 8: Sports / Bone Conduction
    const val FUNC_SWIM_LENGTH = 8001
}

object ConfigId {
    const val CONFIG_AUDIO_MODE = 1
    const val CONFIG_CUSTOM_CLICK = 2
    const val CONFIG_AUTO_ANSWER_PHONE = 3
    const val CONFIG_MULTIPOINT_CONNECTION = 4
    const val CONFIG_OPEN_COMPACTNESS = 5
    const val CONFIG_COMPACTNESS_LISTENER = 6
    const val CONFIG_EQ_MODEL = 7
    const val FIND_DEVICE = 9
    const val NOISE_MODE_CHOOSE = 10
    const val NOISE_LEVEL_CHOOSE = 11
    const val REMIND_LOST = 12
    const val DEVICE_CALL_LISTENER = 13
    const val SWITCH_SPATIAL_AUDIO = 29
    const val SPATIAL_AUDIO_CONFIG = 30
    const val CONFIG_VIRTUAL_SURROUND = 36
    const val AUTO_NOISE = 37
    const val GET_EARPHONE_SN = 39
    const val DEVICE_TIME = 40
    const val ADAPTIVE_SENSE = 41
    const val LOW_LATENCY = 47
    const val BIG_DATA_TRANSFER = 52
    const val SCENE_RENDERING = 54
    const val CUSTOM_EQ = 55
    const val DONGLE_STATUS = 56
    const val SPATIAL_AUDIO_NOTIFY_SOUND = 58
    const val PERSONALIZED_NOISE_REDUCTION = 59
    const val EAR_CANAL_DETECTION = 60
    const val EAR_CANAL_EAR_CANAL_FIT = 61
    const val EAR_CANAL_DETECTION_EXIST = 62
    const val EARPHONE_VERSION = 71
    const val ADAPTIVE_VOLUME = 72
    const val BLUETOOTH_INFO = 74
    const val START_RECORD = 75
    const val SEND_REGION = 76
    const val DONGLE_MODE = 77
    const val PERSONAL_SPATIAL_AUDIO = 79
    const val BONE_CONDUCTION_GESTURE = 80
    const val DONGLE_MONITOR_SWITCH = 81
    const val DONGLE_MONITOR_VOLUME = 82
    const val DONGLE_GESTURE = 83
    const val SWIM_LENGTH = 84
    const val PERSONAL_SPATIAL_AUDIO_STATUS = 85
    const val RECORD_CODEC_HEADER = 86
    const val PERSONAL_SPATIAL_AUDIO_CONTROL = 87
    const val AUDIBILITY_ADAPTATION = 89
    const val RESTRICTED_CONFIG = 93
    const val TTS_SPEAKER_STATUS = 95
    const val VOICE_BROADCAST_SWITCH = 96
    const val VOICE_BROADCAST_RATE = 97
    const val VOICE_BROADCAST_INTERRUPTION = 100
    const val SMART_DENOISE_STATUS = 102
    const val COMMUTING_IMMERSE_STATUS = 103
    const val DOLBY_AUDIO_SPATIAL_MODE = 104
    const val RECORD_CONFIG = 112
    const val RECORD_GESTURE = 113
    const val EARBOX_SOUND_LIST_CONFIG = 114
    const val EARBOX_SOUND_CONFIG = 115
    const val EARBOX_SOUND_SET = 116
    const val DOLBY_AUDIO_IS_SUPPORTED = 118
    const val TRANSLATE_RECORD_HEADER = 119
    const val TRANSLATE_SETTINGS = 120
    const val AIVS_WAKE_UP_SWITCH = 122
    const val AIVS_CONTINUOUS_DIALOGUE_DURATION = 123
    const val AIVS_VOICE_TONE = 124
    const val AIVS_SETTING = 125
    const val DUAL_CONNECTION_SYNC = 126
    const val AIVS_CODEC_HEADER = 127
    const val NOTIFICATION_VOLUME = 161
}

// -----------------------------------------------------------------------------------------
// 1. Target Info (OpCode 2) & Battery
// -----------------------------------------------------------------------------------------

data class BatteryInfo(
    val percentage: Int, // 0..100, or -1 if disconnected
    val isCharging: Boolean,
    val isConnected: Boolean
)

data class TargetDeviceInfo(
    val name: String = "Xiaomi Earbuds",
    val versionName: String = "",
    val versionCode: Int = 0,
    val vendorId: Int = 10007,
    val productId: Int = 0,
    val colorType: Int = 0,
    val address: String = "",
    val serialNumber: String = "",
    val leftBattery: BatteryInfo = BatteryInfo(85, false, true),
    val rightBattery: BatteryInfo = BatteryInfo(85, false, true),
    val caseBattery: BatteryInfo = BatteryInfo(90, false, true)
) {
    companion object {
        fun parseFromResponsePayload(data: ByteArray): TargetDeviceInfo {
            var name = "Xiaomi Earbuds"
            var versionName = ""
            var versionCode = 0
            var vendorId = 10007
            var productId = 0
            var colorType = 0
            var left = BatteryInfo(0, false, false)
            var right = BatteryInfo(0, false, false)
            var case = BatteryInfo(0, false, false)

            var offset = 0
            while (offset + 2 <= data.size) {
                val len = data[offset].toInt() and 0xFF
                if (len <= 0 || offset + len + 1 > data.size) break
                val tag = data[offset + 1].toInt() and 0xFF
                val valueLen = len - 1
                val valOffset = offset + 2

                when (tag) {
                    0 -> { // Name
                        if (valueLen > 0) {
                            name = String(data, valOffset, valueLen, StandardCharsets.UTF_8).trim()
                        }
                    }
                    1 -> { // Version
                        if (valueLen >= 2) {
                            versionCode = ((data[valOffset].toInt() and 0xFF) shl 8) or (data[valOffset + 1].toInt() and 0xFF)
                            val v1 = (data[valOffset].toInt() and 0xF0) ushr 4
                            val v2 = data[valOffset].toInt() and 0x0F
                            val v3 = (data[valOffset + 1].toInt() and 0xF0) ushr 4
                            val v4 = data[valOffset + 1].toInt() and 0x0F
                            versionName = "$v1.$v2.$v3.$v4"
                        }
                    }
                    3 -> { // VID & PID
                        if (valueLen >= 4) {
                            vendorId = ((data[valOffset].toInt() and 0xFF) shl 8) or (data[valOffset + 1].toInt() and 0xFF)
                            productId = ((data[valOffset + 2].toInt() and 0xFF) shl 8) or (data[valOffset + 3].toInt() and 0xFF)
                        }
                    }
                    7 -> { // mulQuantity (Battery for Left, Right, Case)
                        if (valueLen >= 2) {
                            left = parseBatteryByte(data[valOffset].toInt() and 0xFF)
                            right = parseBatteryByte(data[valOffset + 1].toInt() and 0xFF)
                            if (valueLen >= 3) {
                                case = parseBatteryByte(data[valOffset + 2].toInt() and 0xFF)
                            }
                        }
                    }
                    13 -> { // Color Type (f7/f.java case 13: ATTR_TYPE_COLOR_TYPE)
                        if (valueLen >= 1) {
                            colorType = data[valOffset].toInt() and 0xFF
                        }
                    }
                }
                offset += len + 1
            }

            return TargetDeviceInfo(
                name = name,
                versionName = versionName,
                versionCode = versionCode,
                vendorId = vendorId,
                productId = productId,
                colorType = colorType,
                leftBattery = left,
                rightBattery = right,
                caseBattery = case
            )
        }

        private fun parseBatteryByte(byteVal: Int): BatteryInfo {
            if (byteVal == 255 || byteVal == 0xFF) {
                return BatteryInfo(-1, false, false)
            }
            val pct = byteVal and 0x7F
            val charging = (byteVal and 0x80) != 0
            return BatteryInfo(percentage = pct.coerceIn(0, 100), isCharging = charging, isConnected = true)
        }

        fun encodeTargetInfoResponse(info: TargetDeviceInfo): ByteArray {
            val nameBytes = info.name.toByteArray(StandardCharsets.UTF_8)
            val nameLen = nameBytes.size + 1

            fun encodeBattery(b: BatteryInfo): Byte {
                if (!b.isConnected || b.percentage < 0) return 0xFF.toByte()
                var v = b.percentage and 0x7F
                if (b.isCharging) v = v or 0x80
                return v.toByte()
            }

            val mulQty = byteArrayOf(
                encodeBattery(info.leftBattery),
                encodeBattery(info.rightBattery),
                encodeBattery(info.caseBattery)
            )

            val colorTagSize = if (info.colorType > 0) 3 else 0
            val totalSize = (1 + nameLen) + (1 + 4) + (1 + 5) + (1 + 4) + colorTagSize
            val result = ByteArray(totalSize)
            var idx = 0

            // Tag 0: Name
            result[idx++] = nameLen.toByte()
            result[idx++] = 0.toByte()
            System.arraycopy(nameBytes, 0, result, idx, nameBytes.size)
            idx += nameBytes.size

            // Tag 1: Version
            result[idx++] = 3.toByte()
            result[idx++] = 1.toByte()
            result[idx++] = ((info.versionCode shr 8) and 0xFF).toByte()
            result[idx++] = (info.versionCode and 0xFF).toByte()

            // Tag 3: VID / PID
            result[idx++] = 5.toByte()
            result[idx++] = 3.toByte()
            result[idx++] = ((info.vendorId shr 8) and 0xFF).toByte()
            result[idx++] = (info.vendorId and 0xFF).toByte()
            result[idx++] = ((info.productId shr 8) and 0xFF).toByte()
            result[idx++] = (info.productId and 0xFF).toByte()

            // Tag 7: Battery
            result[idx++] = 4.toByte()
            result[idx++] = 7.toByte()
            System.arraycopy(mulQty, 0, result, idx, 3)
            idx += 3

            // Tag 13: Color Type
            if (info.colorType > 0) {
                result[idx++] = 2.toByte() // len = 2 (tag byte + 1 byte val)
                result[idx++] = 13.toByte() // tag = 13
                result[idx++] = (info.colorType and 0xFF).toByte()
            }

            return result
        }
    }
}

// -----------------------------------------------------------------------------------------
// 2. Noise Control (ANC & Transparency)
// -----------------------------------------------------------------------------------------

enum class NoiseMode(val id: Int) {
    OFF(0),
    ANC(1),
    TRANSPARENCY(2);

    companion object {
        fun fromId(id: Int): NoiseMode = entries.find { it.id == id } ?: OFF
    }
}

enum class AncLevel(val id: Int) {
    BALANCED(0),
    LIGHT(1),
    DEEP(2),
    ADAPTIVE(3),
    ANTIWIND(4),
    DEEP_PLUS(5);

    companion object {
        fun fromId(id: Int): AncLevel = entries.find { it.id == id } ?: BALANCED
    }
}

enum class TransparencyLevel(val id: Int) {
    REGULAR(0),
    VOCAL(1),
    AMBIENT(2);

    companion object {
        fun fromId(id: Int): TransparencyLevel = entries.find { it.id == id } ?: REGULAR
    }
}

data class NoiseControlState(
    val mode: NoiseMode = NoiseMode.OFF,
    val ancLevel: AncLevel = AncLevel.BALANCED,
    val transparencyLevel: TransparencyLevel = TransparencyLevel.REGULAR,
    val isAutoNoise: Boolean = false,
    val isSmartDenoise: Boolean = false,
    val isPersonalizedAnc: Boolean = false,
    val ancLevelIndex: Int = 1,
    val transparencyLevelIndex: Int = 0
) {
    fun toCommonConfig(): CommonConfig {
        val levelByte = when (mode) {
            NoiseMode.OFF -> 0
            NoiseMode.ANC -> ancLevel.id
            NoiseMode.TRANSPARENCY -> transparencyLevel.id
        }
        return CommonConfig(
            type = ConfigId.NOISE_LEVEL_CHOOSE,
            value = byteArrayOf(mode.id.toByte(), levelByte.toByte())
        )
    }

    companion object {
        fun createNoiseLevelConfig(mode: NoiseMode, rawLevel: Int): CommonConfig {
            return CommonConfig(
                type = ConfigId.NOISE_LEVEL_CHOOSE,
                value = byteArrayOf(mode.id.toByte(), rawLevel.toByte())
            )
        }

        fun createAutoNoiseConfig(enabled: Boolean): CommonConfig {
            return CommonConfig(
                type = ConfigId.AUTO_NOISE,
                value = byteArrayOf(if (enabled) 1 else 0)
            )
        }

        fun createSmartDenoiseConfig(enabled: Boolean): CommonConfig {
            return CommonConfig(
                type = ConfigId.SMART_DENOISE_STATUS,
                value = byteArrayOf(if (enabled) 1 else 0)
            )
        }

        fun createPersonalizedAncConfig(enabled: Boolean): CommonConfig {
            return CommonConfig(
                type = ConfigId.PERSONALIZED_NOISE_REDUCTION,
                value = byteArrayOf(if (enabled) 1 else 0)
            )
        }

        fun fromCommonConfig(config: CommonConfig, currentState: NoiseControlState = NoiseControlState()): NoiseControlState? {
            return when (config.type) {
                // Config 11 uniquement. Config 1 = audio mode Xiaomi/Dolby, pas ANC (cf. FunctionConfigImpl).
                ConfigId.NOISE_LEVEL_CHOOSE -> {
                    if (config.value.isNotEmpty()) {
                        val mode = NoiseMode.fromId(config.value[0].toInt() and 0xFF)
                        if (config.value.size >= 2) {
                            val level = config.value[1].toInt() and 0xFF
                            when (mode) {
                                NoiseMode.ANC -> currentState.copy(mode = mode, ancLevel = AncLevel.fromId(level), ancLevelIndex = level)
                                NoiseMode.TRANSPARENCY -> currentState.copy(mode = mode, transparencyLevel = TransparencyLevel.fromId(level), transparencyLevelIndex = level)
                                NoiseMode.OFF -> currentState.copy(mode = NoiseMode.OFF)
                            }
                        } else {
                            currentState.copy(mode = mode)
                        }
                    } else null
                }
                ConfigId.NOISE_MODE_CHOOSE -> {
                    if (config.value.isNotEmpty()) {
                        val mode = NoiseMode.fromId(config.value[0].toInt() and 0xFF)
                        currentState.copy(mode = mode)
                    } else null
                }
                ConfigId.AUTO_NOISE -> {
                    if (config.value.isNotEmpty()) {
                        currentState.copy(isAutoNoise = (config.value[0].toInt() and 0xFF) == 1)
                    } else null
                }
                ConfigId.SMART_DENOISE_STATUS -> {
                    if (config.value.isNotEmpty()) {
                        currentState.copy(isSmartDenoise = (config.value[0].toInt() and 0xFF) != 0)
                    } else null
                }
                ConfigId.PERSONALIZED_NOISE_REDUCTION -> {
                    if (config.value.isNotEmpty()) {
                        currentState.copy(isPersonalizedAnc = (config.value[0].toInt() and 0xFF) != 0)
                    } else null
                }
                else -> null
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// -----------------------------------------------------------------------------------------
// 3. Equalizer & Sound Effects (14 Official Presets, 10-Band Custom, Spatial, Virtual Surround)
// -----------------------------------------------------------------------------------------

enum class EqPreset(val id: Int, val stringKey: String) {
    BALANCED(0, "device_settings_sound_balanced_listening"),
    VOICE(1, "device_settings_sound_vocal_enhancement"),
    BASS(5, "device_settings_sound_bass_boost"),
    TREBLE(6, "device_settings_sound_treble_boost"),
    VOLUME_BOOST(7, "device_settings_sound_volume_boost"),
    CUSTOM(10, "device_settings_type_sound_personalized"),
    CLASSIC(11, "device_settings_sound_classic_hearing_sense"),
    LEGENDARY(12, "device_settings_sound_legendary_hearing_sense"),
    SOOTHING_BASS(13, "device_settings_sound_soothing_boost"),
    HARMAN(14, "device_settings_sound_harman"),
    HARMAN_MASTER(15, "device_settings_sound_harman_master"),
    STANDARD(16, "device_settings_sound_standard"),
    OUTDOOR(17, "device_settings_sound_outdoor"),
    UNDERWATER(18, "device_settings_sound_underwater");

    companion object {
        fun fromId(id: Int): EqPreset = entries.find { it.id == id } ?: BALANCED
    }
}

enum class SpatialAudioScene(val id: Int, val stringKey: String) {
    STANDARD(0, "device_settings_sound_spatial_audio_standard"),
    MUSIC(1, "device_settings_sound_spatial_audio_music"),
    VIDEO(2, "device_settings_sound_spatial_audio_video"),
    GAME(3, "device_settings_sound_spatial_audio_game"),
    AUDIOBOOK(4, "device_settings_sound_spatial_audio_audiobook");

    companion object {
        fun fromId(id: Int): SpatialAudioScene = entries.find { it.id == id } ?: STANDARD
    }
}

data class EqBand(
    val frequencyHz: Int,
    val gainDb: Int // -10 .. +10
)

data class EqualizerState(
    val preset: EqPreset = EqPreset.BALANCED,
    val bands: List<EqBand> = DEFAULT_10_BANDS,
    val virtualSurround: Boolean = false,
    val audibilityAdaptation: Boolean = false,
    val adaptiveSense: Boolean = false,
    val adaptiveVolume: Boolean = false,
    val notificationVolumeEnabled: Boolean = true,
    val notificationVolume: Int = 80, // 0..100
    val spatialAudioEnabled: Boolean = false,
    val spatialAudioHeadTracking: Boolean = false,
    val spatialAudioScene: SpatialAudioScene = SpatialAudioScene.STANDARD
) {
    companion object {
        val DEFAULT_FREQUENCIES = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
        val DEFAULT_10_BANDS = DEFAULT_FREQUENCIES.map { EqBand(it, 0) }

        fun createPresetConfig(preset: EqPreset): CommonConfig {
            return CommonConfig(ConfigId.CONFIG_EQ_MODEL, byteArrayOf(preset.id.toByte()))
        }

        fun createCustomEqConfig(bands: List<EqBand>): CommonConfig {
            val header = byteArrayOf(1, 10, 1, 1, 1, 0, bands.size.toByte())
            val buffer = java.nio.ByteBuffer.allocate(header.size + bands.size * 3)
            buffer.put(header)
            for (b in bands) {
                buffer.putShort(b.frequencyHz.toShort())
                val gainByte = if (b.gainDb < 0) (-b.gainDb + 128) else b.gainDb
                buffer.put(gainByte.toByte())
            }
            return CommonConfig(ConfigId.CUSTOM_EQ, buffer.array())
        }

        fun parseCustomEq(data: ByteArray): List<EqBand>? {
            if (data.size < 7) return null
            val numBands = data[6].toInt() and 0xFF
            if (data.size < 7 + numBands * 3) return null

            val result = mutableListOf<EqBand>()
            var idx = 7
            for (i in 0 until numBands) {
                val freq = ((data[idx].toInt() and 0xFF) shl 8) or (data[idx + 1].toInt() and 0xFF)
                val rawGain = data[idx + 2].toInt() and 0xFF
                val gain = if (rawGain > 128) -(rawGain - 128) else rawGain
                result.add(EqBand(freq, gain.coerceIn(-10, 10)))
                idx += 3
            }
            return result
        }
    }
}

// -----------------------------------------------------------------------------------------
// 4. Gestures Control (Single, Double, Triple, Long Press, Slide)
// -----------------------------------------------------------------------------------------

enum class GestureAction(val id: Int, val stringKey: String) {
    VOICE_ASSISTANT(0, "device_settings_awake_voice_assistant"),
    PLAY_PAUSE(1, "device_settings_play_or_pause"),
    PREV_TRACK(2, "device_settings_last_song"),
    NEXT_TRACK(3, "device_settings_next_song"),
    VOLUME_UP(4, "device_settings_volume_up"),
    VOLUME_DOWN(5, "device_settings_volume_down"),
    NOISE_CONTROL(6, "device_settings_noise_control"),
    NONE(8, "device_settings_click_cancle"),
    QUICK_PHOTO(9, "device_settings_gesture_quick_photo"),
    LONG_PRESS_RECORD(10, "device_settings_gesture_long_press_record"),
    VOLUME_ADJUST(11, "device_settings_volume_changed"),
    LONG_PRESS_TRANSLATE(12, "device_settings_gesture_long_press_translate"),
    PREVIOUS_NEXT(13, "device_settings_gesture_previous_next");

    companion object {
        fun fromId(id: Int): GestureAction = entries.find { it.id == id } ?: NONE
    }
}

data class EarbudGestures(
    val singleTap: GestureAction = GestureAction.PLAY_PAUSE,
    val doubleTap: GestureAction = GestureAction.NEXT_TRACK,
    val tripleTap: GestureAction = GestureAction.PREV_TRACK,
    val longPress: GestureAction = GestureAction.NOISE_CONTROL,
    val slide: GestureAction = GestureAction.VOLUME_ADJUST
)

data class GestureSettings(
    val left: EarbudGestures = EarbudGestures(),
    val right: EarbudGestures = EarbudGestures(
        singleTap = GestureAction.PLAY_PAUSE,
        doubleTap = GestureAction.NEXT_TRACK,
        tripleTap = GestureAction.PREV_TRACK,
        longPress = GestureAction.NOISE_CONTROL,
        slide = GestureAction.VOLUME_ADJUST
    )
) {
    fun toCommonConfigs(): List<CommonConfig> {
        return listOf(
            CommonConfig(ConfigId.CONFIG_CUSTOM_CLICK, byteArrayOf(1, left.singleTap.id.toByte(), right.singleTap.id.toByte())),
            CommonConfig(ConfigId.CONFIG_CUSTOM_CLICK, byteArrayOf(2, left.doubleTap.id.toByte(), right.doubleTap.id.toByte())),
            CommonConfig(ConfigId.CONFIG_CUSTOM_CLICK, byteArrayOf(3, left.tripleTap.id.toByte(), right.tripleTap.id.toByte())),
            CommonConfig(ConfigId.CONFIG_CUSTOM_CLICK, byteArrayOf(4, left.longPress.id.toByte(), right.longPress.id.toByte())),
            CommonConfig(ConfigId.CONFIG_CUSTOM_CLICK, byteArrayOf(5, left.slide.id.toByte(), right.slide.id.toByte()))
        )
    }

    companion object {
        fun parseFromConfigs(configs: List<CommonConfig>): GestureSettings {
            var left = EarbudGestures()
            var right = EarbudGestures()

            for (cfg in configs) {
                if (cfg.type == ConfigId.CONFIG_CUSTOM_CLICK && cfg.value.size >= 3) {
                    val type = cfg.value[0].toInt() and 0xFF
                    val leftAct = GestureAction.fromId(cfg.value[1].toInt() and 0xFF)
                    val rightAct = GestureAction.fromId(cfg.value[2].toInt() and 0xFF)
                    when (type) {
                        1 -> {
                            left = left.copy(singleTap = leftAct)
                            right = right.copy(singleTap = rightAct)
                        }
                        2 -> {
                            left = left.copy(doubleTap = leftAct)
                            right = right.copy(doubleTap = rightAct)
                        }
                        3 -> {
                            left = left.copy(tripleTap = leftAct)
                            right = right.copy(tripleTap = rightAct)
                        }
                        4 -> {
                            left = left.copy(longPress = leftAct)
                            right = right.copy(longPress = rightAct)
                        }
                        5 -> {
                            left = left.copy(slide = leftAct)
                            right = right.copy(slide = rightAct)
                        }
                    }
                }
            }
            return GestureSettings(left, right)
        }
    }
}

// -----------------------------------------------------------------------------------------
// 5. Quick Smart Features (BaseCommonDeviceConfig 1-byte)
// -----------------------------------------------------------------------------------------

data class QuickSettings(
    val lowLatency: Boolean = false,
    val multipoint: Boolean = true,
    val inEarDetection: Boolean = true,
    val autoAnswerPhone: Boolean = false,
    val remindLost: Boolean = false,
    val adaptiveVolume: Boolean = false,
    val voiceControl: Boolean = false,
    val notificationBarShow: Boolean = true,
    val handsFree: Boolean = false,
    val earCanalDetection: Boolean = false
) {
    companion object {
        fun createToggle(configId: Int, enabled: Boolean): CommonConfig {
            return CommonConfig(configId, byteArrayOf(if (enabled) 1 else 0))
        }
    }
}

// -----------------------------------------------------------------------------------------
// 6. Find Device / Ring Earbuds (1:1 FindDeviceConstant & DeviceConfigFindDevice)
// -----------------------------------------------------------------------------------------

enum class RingTarget(val id: Int) {
    LEFT(1),
    RIGHT(2),
    BOTH(3);

    companion object {
        fun fromId(id: Int): RingTarget = entries.find { it.id == id } ?: BOTH
    }
}

data class FindDeviceState(
    val isRingingLeft: Boolean = false,
    val isRingingRight: Boolean = false
) {
    val isRinging: Boolean get() = isRingingLeft || isRingingRight

    val target: RingTarget?
        get() = when {
            isRingingLeft && isRingingRight -> RingTarget.BOTH
            isRingingLeft -> RingTarget.LEFT
            isRingingRight -> RingTarget.RIGHT
            else -> null
        }

    companion object {
        fun createCommand(target: RingTarget, start: Boolean): CommonConfig {
            return CommonConfig(
                type = ConfigId.FIND_DEVICE,
                value = byteArrayOf(if (start) 1 else 0, target.id.toByte())
            )
        }
    }
}

// -----------------------------------------------------------------------------------------
// 7. Ear Canal Fit Detection (1:1 DeviceConfigFit / Config 5 & 6, DeviceConfigEarCanalFit / Config 60 & 61)
// -----------------------------------------------------------------------------------------

data class FitDetectionState(
    val status: Int = STATUS_TIP,
    val leftResult: Int = FIT_UNKNOWN,
    val rightResult: Int = FIT_UNKNOWN,
    val errorCode: Int = CODE_NONE
) {
    val isRunning: Boolean
        get() = status == STATUS_DETECTING

    val isFinished: Boolean
        get() = status == STATUS_FINISH

    val isSuccess: Boolean
        get() = leftResult == FIT_WELL && rightResult == FIT_WELL

    companion object {
        // Official Detection Status (FitDetectionModel.kt)
        const val STATUS_NOT_START = 0
        const val STATUS_DETECTING = 1
        const val STATUS_FINISH = 2
        const val STATUS_TIP = 3

        // Official Ear Results (FitDetectionModel.kt)
        const val FIT_UNKNOWN = 0
        const val FIT_WELL = 1
        const val FIT_NOT_GOOD = 2
        const val FIT_NOT_WELL = 2
        const val FIT_ADJUST_POSITION = 4

        // Official Error / Event Codes (FitDetectionViewModel.kt / FitDetectionFragment.kt)
        const val CODE_NONE = 0
        const val CODE_RESPONSE_START = 3
        const val CODE_EAR_OUT = 9
        const val CODE_CALLING = 10
        const val CODE_TIMEOUT = 277
        const val CODE_DISCONNECT = 1001

        fun createCommands(start: Boolean): List<CommonConfig> {
            val v = byteArrayOf(if (start) 1 else 0)
            return listOf(
                CommonConfig(type = ConfigId.CONFIG_OPEN_COMPACTNESS, value = v),
                CommonConfig(type = ConfigId.EAR_CANAL_DETECTION, value = v)
            )
        }

        fun createCommand(start: Boolean): CommonConfig {
            return CommonConfig(
                type = ConfigId.EAR_CANAL_DETECTION,
                value = byteArrayOf(if (start) 1 else 0)
            )
        }

        fun parseFromPayload(data: ByteArray): FitDetectionState {
            val left = if (data.isNotEmpty()) data[0].toInt() and 0xFF else FIT_UNKNOWN
            val right = if (data.size > 1) data[1].toInt() and 0xFF else FIT_UNKNOWN
            val finished = (left in 1..2 || left == FIT_ADJUST_POSITION) && (right in 1..2 || right == FIT_ADJUST_POSITION)
            return FitDetectionState(
                status = if (finished) STATUS_FINISH else STATUS_DETECTING,
                leftResult = left,
                rightResult = right
            )
        }
    }
}

// -----------------------------------------------------------------------------------------
// 8. Earbox Sound (1:1 DeviceConfigEarBoxSound & DeviceConfigEarBoxSoundSetting / Config 115 & 116)
// -----------------------------------------------------------------------------------------

data class EarboxSoundHeaderState(
    val soundId: Int = 0,
    val volume: Int = 75,
    val totalVolume: Int = 100
)

data class EarboxSoundState(
    val openSound: EarboxSoundHeaderState = EarboxSoundHeaderState(soundId = 0, volume = 75),
    val closeSound: EarboxSoundHeaderState = EarboxSoundHeaderState(soundId = 0, volume = 75),
    val chargeSound: EarboxSoundHeaderState = EarboxSoundHeaderState(soundId = 0, volume = 75)
) {
    companion object {
        const val SOUND_TYPE_OPEN = 0
        const val SOUND_TYPE_CLOSE = 1
        const val SOUND_TYPE_CHARGE = 2

        const val SETTING_TYPE_SOUND = 0
        const val SETTING_TYPE_VOLUME = 1

        fun parseFromPayload(data: ByteArray): EarboxSoundState {
            var open = EarboxSoundHeaderState()
            var close = EarboxSoundHeaderState()
            var charge = EarboxSoundHeaderState()

            val buffer = java.nio.ByteBuffer.wrap(data)
            while (buffer.remaining() >= 4) {
                try {
                    val soundType = buffer.get().toInt() and 0xFF
                    val soundId = buffer.get().toInt() and 0xFF
                    val volume = buffer.get().toInt() and 0xFF
                    val totalVolume = buffer.get().toInt() and 0xFF
                    val header = EarboxSoundHeaderState(soundId, volume, totalVolume)
                    when (soundType) {
                        SOUND_TYPE_OPEN -> open = header
                        SOUND_TYPE_CLOSE -> close = header
                        SOUND_TYPE_CHARGE -> charge = header
                    }
                } catch (e: Exception) {
                    break
                }
            }
            return EarboxSoundState(open, close, charge)
        }

        fun createSetCommand(settingType: Int, soundType: Int, value: Int): CommonConfig {
            return CommonConfig(
                type = ConfigId.EARBOX_SOUND_SET,
                value = byteArrayOf(settingType.toByte(), soundType.toByte(), value.coerceIn(0, 100).toByte())
            )
        }
    }
}

