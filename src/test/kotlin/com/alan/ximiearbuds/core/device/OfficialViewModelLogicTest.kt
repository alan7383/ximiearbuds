package com.alan.ximiearbuds.core.device

import com.alan.ximiearbuds.core.protocol.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OfficialViewModelLogicTest {

    private lateinit var controller: EarbudsController

    @BeforeEach
    fun setUp() {
        controller = EarbudsController()
    }

    @Test
    fun `test NoiseReductionVM state transitions and levels`() {
        // Test setting ANC mode
        controller.setNoiseMode(NoiseMode.ANC)
        assertEquals(NoiseMode.ANC, controller.noiseControl.value.mode)

        // Test setting ANC levels (Balance, Comfortable, Deep, Adaptive)
        controller.setAncLevel(AncLevel.DEEP)
        assertEquals(AncLevel.DEEP, controller.noiseControl.value.ancLevel)

        controller.setAncLevel(AncLevel.ADAPTIVE)
        assertEquals(AncLevel.ADAPTIVE, controller.noiseControl.value.ancLevel)

        // Test Transparency mode & levels
        controller.setNoiseMode(NoiseMode.TRANSPARENCY)
        assertEquals(NoiseMode.TRANSPARENCY, controller.noiseControl.value.mode)

        controller.setTransparencyLevel(TransparencyLevel.VOCAL)
        assertEquals(TransparencyLevel.VOCAL, controller.noiseControl.value.transparencyLevel)

        // Test Close mode
        controller.setNoiseMode(NoiseMode.OFF)
        assertEquals(NoiseMode.OFF, controller.noiseControl.value.mode)

        // Test Auto Noise (Adaptive ANC toggle, Config 37)
        controller.setAutoNoise(true)
        assertTrue(controller.noiseControl.value.isAutoNoise)
        controller.setAutoNoise(false)
        assertFalse(controller.noiseControl.value.isAutoNoise)

        // Test Smart Denoise (Anti-vent toggle, Config 102)
        controller.setSmartDenoise(true)
        assertTrue(controller.noiseControl.value.isSmartDenoise)
        controller.setSmartDenoise(false)
        assertFalse(controller.noiseControl.value.isSmartDenoise)

        // Test Personalized Noise Reduction (Config 59)
        controller.setPersonalizedNoiseReduction(true)
        assertTrue(controller.noiseControl.value.isPersonalizedAnc)
        controller.setPersonalizedNoiseReduction(false)
        assertFalse(controller.noiseControl.value.isPersonalizedAnc)

        // Test Indexed ANC and Transparency level selection
        controller.setAncLevelByIndex(2, 2)
        assertEquals(NoiseMode.ANC, controller.noiseControl.value.mode)
        assertEquals(2, controller.noiseControl.value.ancLevelIndex)
        assertEquals(AncLevel.DEEP, controller.noiseControl.value.ancLevel)

        // Test Continuous 20-gear ANC raw level (e.g. level 15 for Redmi Buds 6 Pro)
        controller.setAncRawLevel(15)
        assertEquals(NoiseMode.ANC, controller.noiseControl.value.mode)
        assertEquals(15, controller.noiseControl.value.ancLevelIndex)
        val config11 = controller.noiseControl.value.toCommonConfig()
        assertEquals(ConfigId.NOISE_LEVEL_CHOOSE, config11.type)
        assertEquals(1, config11.value[0].toInt()) // Mode ANC = 1
        assertEquals(15, config11.value[1].toInt()) // Level = 15

        // Test fromCommonConfig decoding Config 11
        val parsedState = NoiseControlState.fromCommonConfig(
            CommonConfig(ConfigId.NOISE_LEVEL_CHOOSE, byteArrayOf(1, 18)),
            controller.noiseControl.value
        )
        assertNotNull(parsedState)
        assertEquals(18, parsedState!!.ancLevelIndex)

        controller.setTransparencyLevelByIndex(1, 1)
        assertEquals(NoiseMode.TRANSPARENCY, controller.noiseControl.value.mode)
        assertEquals(1, controller.noiseControl.value.transparencyLevelIndex)
        assertEquals(TransparencyLevel.VOCAL, controller.noiseControl.value.transparencyLevel)
    }

    @Test
    fun `test model-specific AncCapabilities resolution`() {
        val buds6pro = DeviceRegistry.findByVidPid(10007, 20638)
        assertEquals("O76", buds6pro.codename)
        assertTrue(buds6pro.ancCapabilities.hasAdaptiveAnc, "Redmi Buds 6 Pro must have Adaptive ANC")
        assertTrue(buds6pro.ancCapabilities.hasPersonalizedAnc, "Redmi Buds 6 Pro must have Personalized ANC")
        assertFalse(buds6pro.ancCapabilities.hasSmartDenoise, "Redmi Buds 6 Pro does not have Smart Wind Denoise")
        assertEquals((0..19).toList(), buds6pro.ancCapabilities.ancLevels)
        assertEquals(listOf(0, 1, 2), buds6pro.ancCapabilities.transparencyLevels)
        assertFalse(buds6pro.ancCapabilities.isSingleToggleOnly)

        // REDMI Buds 8 Pro (P76)
        val buds8pro = DeviceRegistry.findByVidPid(10007, 20707)
        assertEquals("P76", buds8pro.codename)
        assertTrue(buds8pro.ancCapabilities.hasSmartDenoise, "P76 must have Smart Denoise")

        // Xiaomi Buds 3 (K75)
        val buds3 = DeviceRegistry.findByVidPid(10007, 20518)
        assertEquals(listOf(1, 0, 2, 4), buds3.ancCapabilities.ancLevels, "K75 must have 4-level ANC including wind resistance")
        assertEquals(listOf(0, 1), buds3.ancCapabilities.transparencyLevels, "K75 must have 2-level Transparency")
    }

    @Test
    fun `test SoundEffectVM 10-band EQ presets and curve adjustments`() {
        // Initial state: BALANCED
        assertEquals(EqPreset.BALANCED, controller.equalizer.value.preset)
        assertEquals(10, controller.equalizer.value.bands.size)

        // Select BASS preset
        controller.setEqPreset(EqPreset.BASS)
        assertEquals(EqPreset.BASS, controller.equalizer.value.preset)

        // Adjust a specific frequency band (e.g., 1000 Hz to +6 dB)
        controller.setEqBandGain(1000, 6)
        assertEquals(EqPreset.CUSTOM, controller.equalizer.value.preset)
        val band1k = controller.equalizer.value.bands.find { it.frequencyHz == 1000 }
        assertNotNull(band1k)
        assertEquals(6, band1k?.gainDb)

        // Test clamping (max +10 dB, min -10 dB)
        controller.setEqBandGain(1000, 15)
        val clampedHigh = controller.equalizer.value.bands.find { it.frequencyHz == 1000 }
        assertEquals(10, clampedHigh?.gainDb)

        controller.setEqBandGain(1000, -20)
        val clampedLow = controller.equalizer.value.bands.find { it.frequencyHz == 1000 }
        assertEquals(-10, clampedLow?.gainDb)

        // Test Reset EQ
        controller.resetEq()
        assertEquals(EqPreset.STANDARD, controller.equalizer.value.preset)
        assertTrue(controller.equalizer.value.bands.all { it.gainDb == 0 })
    }

    @Test
    fun `test FindDeviceViewModel chime control`() {
        assertFalse(controller.findDevice.value.isRinging)

        // Ring left earbud
        controller.ringEarbuds(RingTarget.LEFT)
        assertTrue(controller.findDevice.value.isRinging)
        assertEquals(RingTarget.LEFT, controller.findDevice.value.target)

        // Ring right earbud
        controller.ringEarbuds(RingTarget.RIGHT)
        assertTrue(controller.findDevice.value.isRinging)
        assertEquals(RingTarget.RIGHT, controller.findDevice.value.target)

        // Stop ringing
        controller.stopRinging()
        assertFalse(controller.findDevice.value.isRinging)
    }

    @Test
    fun `test SetMoreVM quick settings toggles`() {
        // Test Low Latency toggle
        controller.setLowLatency(true)
        assertTrue(controller.quickSettings.value.lowLatency)
        controller.setLowLatency(false)
        assertFalse(controller.quickSettings.value.lowLatency)

        // Test Dual Connection / Multipoint toggle
        controller.setMultipoint(true)
        assertTrue(controller.quickSettings.value.multipoint)
        controller.setMultipoint(false)
        assertFalse(controller.quickSettings.value.multipoint)

        // Test In-Ear Detection
        controller.setInEarDetection(false)
        assertFalse(controller.quickSettings.value.inEarDetection)
        controller.setInEarDetection(true)
        assertTrue(controller.quickSettings.value.inEarDetection)

        // Test Auto Answer
        controller.setAutoAnswer(true)
        assertTrue(controller.quickSettings.value.autoAnswerPhone)

        // Test Voice Control
        controller.setVoiceControl(true)
        assertTrue(controller.quickSettings.value.voiceControl)
    }

    @Test
    fun `test Spatial Audio and Head Tracking state transitions`() {
        assertFalse(controller.equalizer.value.spatialAudioEnabled)

        controller.setSpatialAudio(true)
        assertTrue(controller.equalizer.value.spatialAudioEnabled)

        controller.setSpatialAudioHeadTracking(true)
        assertTrue(controller.equalizer.value.spatialAudioHeadTracking)

        controller.setSpatialAudioScene(SpatialAudioScene.VIDEO)
        assertEquals(SpatialAudioScene.VIDEO, controller.equalizer.value.spatialAudioScene)
    }
}
