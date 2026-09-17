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
        val isOpen: Boolean = false,
        val preference: Int = 1,      // 0 = High quality, 1 = Low latency
        val headTracking: Boolean = false,
        val virtualSurround: Boolean = false,
        val sceneRenderingMode: Int = 0 // 0 = Default, 1 = Cinema, 2 = Game, 3 = Music
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
                val sceneMode = if (bytes.size > 1) bytes[1].toInt() and 0xFF else 0
                return SpatialAudioConfig(isOpen, preference, headTracking, virtualSurround, sceneMode)
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

    // -------------------------------------------------------------
    // 8. DONGLE 2.4GHz GAMING CONFIGS (Configs 77, 81, 82)
    // -------------------------------------------------------------
    data class DongleConfig(
        val dongleMode: Int = 1, // 0 = Lossless, 1 = Low Latency, 2 = Wireless Mic
        val monitorSwitch: Boolean = false,
        val monitorVolume: Int = 50 // 0-100
    ) {
        fun encodeMode(): ByteArray = byteArrayOf(dongleMode.toByte())
        fun encodeMonitor(): ByteArray = byteArrayOf(if (monitorSwitch) 1 else 0, monitorVolume.coerceIn(0, 100).toByte())

        companion object {
            fun decode(bytes: ByteArray): DongleConfig {
                if (bytes.isEmpty()) return DongleConfig()
                val mode = bytes[0].toInt() and 0xFF
                val monSwitch = if (bytes.size > 1) (bytes[1].toInt() and 0xFF) == 1 else false
                val monVol = if (bytes.size > 2) bytes[2].toInt() and 0xFF else 50
                return DongleConfig(mode, monSwitch, monVol)
            }
        }
    }

    // -------------------------------------------------------------
    // 10. SPORTS & SWIM POOL (Config 84)
    // -------------------------------------------------------------
    data class SwimPoolConfig(
        val currentLengthFlag: Int = 1, // 1 = 25m, 2 = 50m, 255 = custom
        val customLength: Int = 25
    ) {
        fun encode(): ByteArray {
            return if (currentLengthFlag != 255) {
                byteArrayOf((currentLengthFlag and 0xFF).toByte())
            } else {
                byteArrayOf(
                    0xFF.toByte(),
                    ((customLength shr 8) and 0xFF).toByte(),
                    (customLength and 0xFF).toByte(),
                    0, 0, 0, 0
                )
            }
        }

        companion object {
            fun decode(bytes: ByteArray): SwimPoolConfig {
                if (bytes.isEmpty()) return SwimPoolConfig()
                val flag = bytes[0].toInt() and 0xFF
                val custom = if (flag == 255 && bytes.size >= 3) {
                    ((bytes[1].toInt() and 0xFF) shl 8) or (bytes[2].toInt() and 0xFF)
                } else if (flag == 2) 50 else 25
                return SwimPoolConfig(flag, custom)
            }
        }
    }

    // -------------------------------------------------------------
    // 11. REMIND LOST / LAB ANTI-LOST (Config 12)
    // -------------------------------------------------------------
    data class RemindLostConfig(
        val isEnabled: Boolean = true,
        val leftInBox: Boolean = false,
        val leftWear: Boolean = true,
        val rightInBox: Boolean = false,
        val rightWear: Boolean = true
    ) {
        fun encode(): ByteArray {
            var b = 0
            if (isEnabled) b = b or 0x01
            if (leftInBox) b = b or 0x02
            if (leftWear) b = b or 0x04
            if (rightInBox) b = b or 0x08
            if (rightWear) b = b or 0x10
            return byteArrayOf(b.toByte())
        }

        companion object {
            fun decode(bytes: ByteArray): RemindLostConfig {
                if (bytes.isEmpty()) return RemindLostConfig()
                val b = bytes[0].toInt() and 0xFF
                return RemindLostConfig(
                    isEnabled = (b and 0x01) != 0,
                    leftInBox = (b and 0x02) != 0,
                    leftWear = (b and 0x04) != 0,
                    rightInBox = (b and 0x08) != 0,
                    rightWear = (b and 0x10) != 0
                )
            }
        }
    }

    // -------------------------------------------------------------
    // 12. XIAOAI & VOICE ASSISTANT (Configs 68, 69, 70)
    // -------------------------------------------------------------
    data class XiaoAiConfig(
        val wakeUpWordOpen: Boolean = true,
        val continuousDialogueOpen: Boolean = false,
        val timbre: Int = 1 // 1 = Classic, 2 = Gentle, 3 = Energetic
    ) {
        fun encode(): ByteArray {
            return byteArrayOf(
                if (wakeUpWordOpen) 1 else 0,
                if (continuousDialogueOpen) 1 else 0,
                timbre.toByte()
            )
        }

        companion object {
            fun decode(bytes: ByteArray): XiaoAiConfig {
                if (bytes.isEmpty()) return XiaoAiConfig()
                val wake = bytes[0].toInt() == 1
                val cont = if (bytes.size > 1) bytes[1].toInt() == 1 else false
                val tim = if (bytes.size > 2) bytes[2].toInt() and 0xFF else 1
                return XiaoAiConfig(wake, cont, tim)
            }
        }
    }

    // -------------------------------------------------------------
    // 13. GET DEVICE CONFIG PARAMS (OpCode 243 / 0xF3)
    // -------------------------------------------------------------
    object GetDeviceConfigCodec {
        fun encode(vararg configIds: Int): ByteArray {
            val bytes = ByteArray(configIds.size * 2)
            var i = 0
            for (id in configIds) {
                bytes[i++] = ((id shr 8) and 0xFF).toByte()
                bytes[i++] = (id and 0xFF).toByte()
            }
            return bytes
        }

        fun encode(configIds: Collection<Int>): ByteArray = encode(*configIds.toIntArray())

        fun decode(bytes: ByteArray): IntArray {
            val count = bytes.size / 2
            val ids = IntArray(count)
            for (i in 0 until count) {
                val msb = bytes[i * 2].toInt() and 0xFF
                val lsb = bytes[i * 2 + 1].toInt() and 0xFF
                ids[i] = (msb shl 8) or lsb
            }
            return ids
        }
    }

    // -------------------------------------------------------------
    // 14. REPORT DEVICE STATUS (OpCode 14 / 0x0E)
    // -------------------------------------------------------------
    data class DeviceStatusReport(
        val batteryLeft: Int? = null,
        val batteryRight: Int? = null,
        val batteryCase: Int? = null,
        val ancStatus: Int? = null, // 0 = Off, 1 = ANC, 2 = Transparent
        val twsStatus: Int? = null
    )

    object DeviceStatusCodec {
        fun parse(paramData: ByteArray): DeviceStatusReport {
            var batteryLeft: Int? = null
            var batteryRight: Int? = null
            var batteryCase: Int? = null
            var ancStatus: Int? = null
            var twsStatus: Int? = null

            val length = paramData.size
            var offset = 0
            while (offset + 1 < length) {
                val len = paramData[offset].toInt() and 0xFF
                if (len <= 0 || offset + 1 + len > length) break
                val type = paramData[offset + 1].toInt() and 0xFF
                val valueLen = len - 1
                val valStart = offset + 2

                when (type) {
                    0 -> { // mulQuantity: battery percentages
                        if (valueLen >= 1) batteryLeft = paramData[valStart].toInt() and 0xFF
                        if (valueLen >= 2) batteryRight = paramData[valStart + 1].toInt() and 0xFF
                        if (valueLen >= 3) batteryCase = paramData[valStart + 2].toInt() and 0xFF
                    }
                    1 -> { // twsStatus
                        if (valueLen >= 1) twsStatus = paramData[valStart].toInt() and 0xFF
                    }
                    4 -> { // ancStatus: 0 = Off, 1 = ANC, 2 = Transparent
                        if (valueLen >= 1) ancStatus = paramData[valStart].toInt() and 0xFF
                    }
                }
                offset += len + 1
            }

            return DeviceStatusReport(
                batteryLeft = batteryLeft,
                batteryRight = batteryRight,
                batteryCase = batteryCase,
                ancStatus = ancStatus,
                twsStatus = twsStatus
            )
        }

        fun encode(report: DeviceStatusReport): ByteArray {
            val buffer = ByteBuffer.allocate(64)
            if (report.batteryLeft != null || report.batteryRight != null || report.batteryCase != null) {
                val left = (report.batteryLeft ?: 0).toByte()
                val right = (report.batteryRight ?: 0).toByte()
                val case = (report.batteryCase ?: 0).toByte()
                buffer.put(4.toByte()) // len = 4 (type + 3 bytes)
                buffer.put(0.toByte()) // type = 0
                buffer.put(left)
                buffer.put(right)
                buffer.put(case)
            }
            if (report.ancStatus != null) {
                buffer.put(2.toByte()) // len = 2 (type + 1 byte)
                buffer.put(4.toByte()) // type = 4
                buffer.put(report.ancStatus.toByte())
            }
            if (report.twsStatus != null) {
                buffer.put(2.toByte())
                buffer.put(1.toByte())
                buffer.put(report.twsStatus.toByte())
            }
            buffer.flip()
            val result = ByteArray(buffer.remaining())
            buffer.get(result)
            return result
        }
    }

    // -------------------------------------------------------------
    // 15. DEVICE RUN INFO (OpCode 9 / 0x09)
    // -------------------------------------------------------------
    object DeviceRunInfoCodec {
        fun encodeAncStatus(ancStatus: Int): ByteArray {
            return byteArrayOf(2, 9, ancStatus.toByte())
        }

        fun parseAncStatus(paramData: ByteArray): Int? {
            val length = paramData.size
            var offset = 0
            while (offset + 1 < length) {
                val len = paramData[offset].toInt() and 0xFF
                if (len <= 0 || offset + 1 + len > length) break
                val type = paramData[offset + 1].toInt() and 0xFF
                val valueLen = len - 1
                val valStart = offset + 2

                when (type) {
                    9 -> { // ancStatus
                        if (valueLen >= 1) return paramData[valStart].toInt() and 0xFF
                    }
                    5 -> { // vendorData
                        val vendorMap = VendorDataCodec.parse(paramData.copyOfRange(valStart, valStart + valueLen))
                        val noiseData = vendorMap[VendorDataCodec.TYPE_NOISE]
                        if (noiseData != null && noiseData.isNotEmpty()) {
                            return noiseData[0].toInt() and 0xFF
                        }
                    }
                }
                offset += len + 1
            }
            return null
        }
    }

    // -------------------------------------------------------------
    // 16. VENDOR DATA (OpCode 8 / 0x08 - CMD_SET_TARGET_INFO)
    // -------------------------------------------------------------
    object VendorDataCodec {
        const val TYPE_NOISE: Byte = 4
        const val TYPE_WEAR: Byte = 6

        fun encodeNoiseMode(ancType: Byte): ByteArray {
            // Format: [len = 2, type = 4, ancType]
            return byteArrayOf(2, TYPE_NOISE, ancType)
        }

        fun parse(data: ByteArray): Map<Byte, ByteArray> {
            val map = mutableMapOf<Byte, ByteArray>()
            var offset = 0
            while (offset + 1 < data.size) {
                val len = data[offset].toInt() and 0xFF
                if (len <= 0 || offset + 1 + len > data.size) break
                val type = data[offset + 1]
                val valLen = len - 1
                val value = if (valLen > 0) data.copyOfRange(offset + 2, offset + 2 + valLen) else ByteArray(0)
                map[type] = value
                offset += len + 1
            }
            return map
        }
    }
}


