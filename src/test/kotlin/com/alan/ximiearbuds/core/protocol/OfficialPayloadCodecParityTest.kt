package com.alan.ximiearbuds.core.protocol

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OfficialPayloadCodecParityTest {

    @Test
    fun `test FindDevice binary encoding and decoding`() {
        // Find device: state = 1 (play), type = 2 (right earbud)
        val config = OfficialPayloadCodecs.FindDeviceConfig(state = 1, deviceType = 2)
        val bytes = config.encode()

        assertArrayEquals(byteArrayOf(1, 2), bytes)

        val decoded = OfficialPayloadCodecs.FindDeviceConfig.decode(bytes)
        assertEquals(1.toByte(), decoded.state)
        assertEquals(2.toByte(), decoded.deviceType)
    }

    @Test
    fun `test CustomEq SELECT_EQ_MODE encoding and decoding`() {
        // Select mode = 2
        val selectConfig = OfficialPayloadCodecs.CustomEqConfig(
            operationType = OfficialPayloadCodecs.EqOperationType.SELECT_EQ_MODE,
            soundEffectMode = 2
        )
        val encoded = selectConfig.encode()
        assertArrayEquals(byteArrayOf(1, 2), encoded)
    }

    @Test
    fun `test CustomEq RESET_CUSTOM_EQ encoding`() {
        val resetConfig = OfficialPayloadCodecs.CustomEqConfig(
            operationType = OfficialPayloadCodecs.EqOperationType.RESET_CUSTOM_EQ
        )
        val encoded = resetConfig.encode()
        assertArrayEquals(byteArrayOf(4, 1, 1, 1), encoded)
    }

    @Test
    fun `test CustomEq 10-band SET_CUSTOM_EQ encoding and decoding`() {
        val bands = listOf(
            OfficialPayloadCodecs.EqBand(frequency = 31, gain = 3),
            OfficialPayloadCodecs.EqBand(frequency = 63, gain = -2), // -2 converted = 128 + 2 = 130
            OfficialPayloadCodecs.EqBand(frequency = 125, gain = 0)
        )
        val config = OfficialPayloadCodecs.CustomEqConfig(
            operationType = OfficialPayloadCodecs.EqOperationType.SET_CUSTOM_EQ,
            bands = bands
        )
        val encoded = config.encode()

        // Header: [1, 10, 1, 1, 1, 0, 3]
        assertEquals(7 + 3 * 3, encoded.size)
        assertEquals(1.toByte(), encoded[0])
        assertEquals(10.toByte(), encoded[1]) // gainBound default
        assertEquals(3.toByte(), encoded[6]) // bar count

        // Band 0: 31 Hz (0x00, 0x1F), gain 3
        assertEquals(0.toByte(), encoded[7])
        assertEquals(31.toByte(), encoded[8])
        assertEquals(3.toByte(), encoded[9])

        // Band 1: 63 Hz (0x00, 0x3F), gain -2 -> 130 (-126 as signed byte)
        assertEquals(0.toByte(), encoded[10])
        assertEquals(63.toByte(), encoded[11])
        assertEquals((128 + 2).toByte(), encoded[12])
    }

    @Test
    fun `test FitDetection encoding and decoding`() {
        val fitConfig = OfficialPayloadCodecs.FitDetectionConfig(openCheck = true, leftResult = 0, rightResult = 0)
        assertArrayEquals(byteArrayOf(1), fitConfig.encode())

        // Simulated incoming result: left = 0 (good), right = 1 (poor)
        val incomingResult = byteArrayOf(0, 1)
        val decoded = OfficialPayloadCodecs.FitDetectionConfig.decode(incomingResult)
        assertEquals(0, decoded.leftResult)
        assertEquals(1, decoded.rightResult)
    }

    @Test
    fun `test SpatialAudio encoding and decoding`() {
        val spatial = OfficialPayloadCodecs.SpatialAudioConfig(
            isOpen = true,
            preference = 0,
            headTracking = true,
            virtualSurround = false
        )
        val encoded = spatial.encode()
        assertEquals(1, encoded.size)

        val decoded = OfficialPayloadCodecs.SpatialAudioConfig.decode(encoded)
        assertTrue(decoded.isOpen)
        assertEquals(0, decoded.preference)
        assertTrue(decoded.headTracking)
        assertFalse(decoded.virtualSurround)
    }

    @Test
    fun `test Gesture configuration encoding and decoding`() {
        val gestures = OfficialPayloadCodecs.GestureSettingsConfig(
            doubleTap = OfficialPayloadCodecs.GestureSlot(
                leftAction = OfficialGestureTypes.PREVIOUS.toByte(),
                rightAction = OfficialGestureTypes.NEXT.toByte()
            ),
            longPress = OfficialPayloadCodecs.GestureSlot(
                leftAction = OfficialGestureTypes.NOISE_CONTROL.toByte(),
                rightAction = OfficialGestureTypes.NOISE_CONTROL.toByte()
            )
        )
        val encoded = gestures.encode()
        // Double tap: [1, left, right], Long press: [3, left, right] -> 6 bytes
        assertEquals(6, encoded.size)

        val decoded = OfficialPayloadCodecs.GestureSettingsConfig.decode(encoded)
        assertEquals(OfficialGestureTypes.PREVIOUS.toByte(), decoded.doubleTap?.leftAction)
        assertEquals(OfficialGestureTypes.NEXT.toByte(), decoded.doubleTap?.rightAction)
        assertEquals(OfficialGestureTypes.NOISE_CONTROL.toByte(), decoded.longPress?.leftAction)
        assertEquals(OfficialGestureTypes.NOISE_CONTROL.toByte(), decoded.longPress?.rightAction)
    }

    @Test
    fun `test CommonToggle binary codec`() {
        assertArrayEquals(byteArrayOf(1), OfficialPayloadCodecs.CommonToggle.encode(true))
        assertArrayEquals(byteArrayOf(0), OfficialPayloadCodecs.CommonToggle.encode(false))

        assertTrue(OfficialPayloadCodecs.CommonToggle.decode(byteArrayOf(1)))
        assertFalse(OfficialPayloadCodecs.CommonToggle.decode(byteArrayOf(0)))
    }
}
