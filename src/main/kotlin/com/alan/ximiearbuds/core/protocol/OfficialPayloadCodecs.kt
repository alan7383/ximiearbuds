package com.alan.ximiearbuds.core.protocol

import java.nio.ByteBuffer

/**
 * 1:1 Exhaustive binary TLV encoders and decoders for Xiaomi Earbuds official protocols.
 * Matches:
 * - com.mi.earphone.bluetoothsdk.setting.lab.DeviceConfigFindDevice
 * - com.mi.earphone.bluetoothsdk.setting.function.DeviceConfigCustomEq
 * - com.mi.earphone.bluetoothsdk.setting.function.DeviceConfigFit
 * - com.mi.earphone.bluetoothsdk.setting.function.DeviceConfigSpatialAudio
 * - com.mi.earphone.bluetoothsdk.setting.function.DeviceConfigNoiseLevel
 * - com.mi.earphone.bluetoothsdk.setting.gesture.DeviceConfigClickSet
 * - com.mi.earphone.bluetoothsdk.setting.lab.BaseCommonDeviceConfig
 */
object OfficialPayloadCodecs {

    // -------------------------------------------------------------
    // 1. FIND DEVICE (Config 9)
    // -------------------------------------------------------------
    data class FindDeviceConfig(
        val state: Byte,      // 0 = stop, 1 = play chime
        val deviceType: Byte  // 1 = left, 2 = right, 3 = both
    ) {
        fun encode(): ByteArray = byteArrayOf(state, deviceType)

        companion object {
            fun decode(bytes: ByteArray): FindDeviceConfig {
                if (bytes.size >= 2) {
                    return FindDeviceConfig(bytes[0], bytes[1])
                }
                return FindDeviceConfig(0, 3)
            }
        }
    }

    // -------------------------------------------------------------
    // 2. CUSTOM EQUALIZER (Config 55)
    // -------------------------------------------------------------
    data class EqBand(
        val frequency: Int,
        val gain: Int,        // -10 to +10 dB
        val gainBound: Int = 10
    )

    enum class EqOperationType(val code: Int) {
        UNKNOWN(0),
        REPORT_EQ_PARAMS(2),
        SELECT_EQ_MODE(3),
        SET_CUSTOM_EQ(4),
        RESET_CUSTOM_EQ(6),
        CUSTOM_EQ_PREVIEW(8)
    }

    data class CustomEqConfig(
        val operationType: EqOperationType = EqOperationType.SELECT_EQ_MODE,
        val soundEffectMode: Int = 0,
        val gainBound: Int = 10,
        val bands: List<EqBand> = emptyList()
    ) {
        fun encode(): ByteArray {
            return when (operationType) {
                EqOperationType.SELECT_EQ_MODE -> {
                    byteArrayOf(1.toByte(), soundEffectMode.toByte())
                }
                EqOperationType.SET_CUSTOM_EQ -> {
                    val header = byteArrayOf(1, 10, 1, 1, 1, 0, bands.size.toByte())
                    val buffer = ByteBuffer.allocate(header.size + bands.size * 3)
                    buffer.put(header)
                    for (band in bands) {
                        buffer.putShort(band.frequency.toShort())
                        val convertedGain = if (band.gain < 0) (-band.gain + 128).toByte() else band.gain.toByte()
                        buffer.put(convertedGain)
                    }
                    buffer.array()
                }
                EqOperationType.RESET_CUSTOM_EQ -> {
                    byteArrayOf(4, 1, 1, 1)
                }
                EqOperationType.CUSTOM_EQ_PREVIEW -> {
                    val header = byteArrayOf(5, 1, 1, bands.size.toByte())
                    val buffer = ByteBuffer.allocate(header.size + bands.size * 3)
                    buffer.put(header)
                    for (band in bands) {
                        buffer.putShort(band.frequency.toShort())
                        val convertedGain = if (band.gain < 0) (-band.gain + 128).toByte() else band.gain.toByte()
                        buffer.put(convertedGain)
                    }
                    buffer.array()
                }
                else -> byteArrayOf()
            }
        }

        companion object {
            fun decode(bytes: ByteArray): CustomEqConfig {
                if (bytes.size < 7) return CustomEqConfig()
                val buffer = ByteBuffer.wrap(bytes)
                val firstByte = buffer.get().toInt() and 0xFF
                if (firstByte != 1) return CustomEqConfig()

                val mode = buffer.get().toInt() and 0xFF
                var bound = buffer.get().toInt() and 0xFF
                if (bound == 0) bound = 10
                val minBound = buffer.get().toInt() and 0xFF
                val eqId = buffer.get().toInt() and 0xFF
                val eqNameLen = buffer.get().toInt() and 0xFF

                if (eqNameLen > 0 && buffer.remaining() >= eqNameLen) {
                    buffer.position(buffer.position() + eqNameLen)
                }
                if (!buffer.hasRemaining()) return CustomEqConfig(soundEffectMode = mode, gainBound = bound)

                val barCount = buffer.get().toInt() and 0xFF
                val bandList = mutableListOf<EqBand>()
                while (buffer.remaining() >= 3 && bandList.size < barCount) {
                    val freq = buffer.short.toInt() and 0xFFFF
                    val rawGain = buffer.get().toInt() and 0xFF
                    val gain = if (rawGain < 128) rawGain else 128 - rawGain
                    bandList.add(EqBand(freq, gain, bound))
                }
                return CustomEqConfig(
                    operationType = EqOperationType.REPORT_EQ_PARAMS,
                    soundEffectMode = mode,
                    gainBound = bound,
                    bands = bandList
                )
            }
        }
    }

    // -------------------------------------------------------------
    // 3. FIT DETECTION / ACOUSTIC SEAL (Config 5 & 61)
    // -------------------------------------------------------------
    data class FitDetectionConfig(
        val openCheck: Boolean,
        val leftResult: Int,   // 0 = good seal, 1 = adjust, 2 = not worn
        val rightResult: Int
    ) {
        fun encode(): ByteArray = byteArrayOf(if (openCheck) 1 else 0)

        companion object {
            fun decode(bytes: ByteArray): FitDetectionConfig {
                if (bytes.isEmpty()) return FitDetectionConfig(false, 0, 0)
                val left = bytes[0].toInt() and 0xFF
                val right = if (bytes.size > 1) bytes[1].toInt() and 0xFF else left
                return FitDetectionConfig(openCheck = false, leftResult = left, rightResult = right)
            }
        }
    }

    // -------------------------------------------------------------
    // 4. SPATIAL AUDIO (Config 29)
    // -------------------------------------------------------------
    data class SpatialAudioConfig(
        val isOpen: Boolean,
        val preference: Int,      // 0 = High quality, 1 = Low latency
        val headTracking: Boolean,
        val virtualSurround: Boolean
    ) {
        fun encode(): ByteArray {
            val bit7 = if (isOpen) '1' else '0'
            val bit56 = when (preference) {
                0 -> "00"
                1 -> "01"
                2 -> "10"
                else -> "11"
            }
            val bit4 = if (headTracking) '1' else '0'
            val bit3 = if (virtualSurround) '1' else '0'
            // Format: [b0..b2: reserved] [b3: virtualSurround] [b4: headTracking] [b5..b6: preference] [b7: isOpen]
            val bitsStr = "000$bit3$bit4$bit56$bit7"
            var value = 0
            for (ch in bitsStr) {
                value = (value shl 1) or (if (ch == '1') 1 else 0)
            }
            return byteArrayOf(value.toByte())
        }

        companion object {
            fun decode(bytes: ByteArray): SpatialAudioConfig {
                if (bytes.isEmpty()) return SpatialAudioConfig(false, 0, false, false)
                val byteVal = bytes[0].toInt() and 0xFF
                val isOpen = (byteVal and 0x01) != 0
                val preference = (byteVal shr 1) and 0x03
                val headTracking = (byteVal and 0x08) != 0
                val virtualSurround = (byteVal and 0x10) != 0
                return SpatialAudioConfig(isOpen, preference, headTracking, virtualSurround)
            }
        }
    }

    // -------------------------------------------------------------
    // 5. NOISE CONTROL (Config 10 & 11)
    // -------------------------------------------------------------
    data class NoiseControlConfig(
        val ancState: Byte, // 0 = Off, 1 = Noise, 2 = Transparent
        val ancLevel: Byte  // Levels matching NoiseLevelConstants
    ) {
        fun encode(): ByteArray = byteArrayOf(ancState, ancLevel)

        companion object {
            fun decode(bytes: ByteArray): NoiseControlConfig {
                if (bytes.size >= 2) {
                    return NoiseControlConfig(bytes[0], bytes[1])
                } else if (bytes.isNotEmpty()) {
                    return NoiseControlConfig(bytes[0], 0)
                }
                return NoiseControlConfig(0, 0)
            }
        }
    }

    // -------------------------------------------------------------
    // 6. GESTURE CONFIGURATION (Config 2)
    // -------------------------------------------------------------
    data class GestureSlot(val leftAction: Byte, val rightAction: Byte)

    data class GestureSettingsConfig(
        val doubleTap: GestureSlot? = null,
        val tripleTap: GestureSlot? = null,
        val longPress: GestureSlot? = null,
        val singlePress: GestureSlot? = null,
        val slide: GestureSlot? = null
    ) {
        fun encode(): ByteArray {
            val list = mutableListOf<Byte>()
            singlePress?.let {
                list.add(OfficialGestureClicks.ONCE_PRESS.toByte())
                list.add(it.leftAction)
                list.add(it.rightAction)
            }
            doubleTap?.let {
                list.add(OfficialGestureClicks.DOUBLE_CLICK.toByte())
                list.add(it.leftAction)
                list.add(it.rightAction)
            }
            tripleTap?.let {
                list.add(OfficialGestureClicks.TRIPLE_CLICK.toByte())
                list.add(it.leftAction)
                list.add(it.rightAction)
            }
            longPress?.let {
                list.add(OfficialGestureClicks.LONG_PRESS.toByte())
                list.add(it.leftAction)
                list.add(it.rightAction)
            }
            slide?.let {
                list.add(OfficialGestureClicks.SLIDE.toByte())
                list.add(it.leftAction)
                list.add(it.rightAction)
            }
            return list.toByteArray()
        }

        companion object {
            fun decode(bytes: ByteArray): GestureSettingsConfig {
                var dTap: GestureSlot? = null
                var tTap: GestureSlot? = null
                var lPress: GestureSlot? = null
                var sPress: GestureSlot? = null
                var sSlide: GestureSlot? = null

                var i = 0
                while (i < bytes.size - 2) {
                    val clickType = bytes[i].toInt() and 0xFF
                    val left = bytes[i + 1]
                    val right = bytes[i + 2]
                    when (clickType) {
                        OfficialGestureClicks.ONCE_PRESS -> sPress = GestureSlot(left, right)
                        OfficialGestureClicks.DOUBLE_CLICK -> dTap = GestureSlot(left, right)
                        OfficialGestureClicks.TRIPLE_CLICK -> tTap = GestureSlot(left, right)
                        OfficialGestureClicks.LONG_PRESS -> lPress = GestureSlot(left, right)
                        OfficialGestureClicks.SLIDE -> sSlide = GestureSlot(left, right)
                    }
                    i += 3
                }
                return GestureSettingsConfig(dTap, tTap, lPress, sPress, sSlide)
            }
        }
    }

    // -------------------------------------------------------------
    // 7. COMMON 1-BYTE TOGGLE CONFIGS (Configs 3, 4, 47, 60, 116...)
    // -------------------------------------------------------------
    object CommonToggle {
        fun encode(enabled: Boolean): ByteArray = byteArrayOf(if (enabled) 1 else 0)
        fun decode(bytes: ByteArray): Boolean = bytes.isNotEmpty() && bytes[0].toInt() == 1
    }
}
