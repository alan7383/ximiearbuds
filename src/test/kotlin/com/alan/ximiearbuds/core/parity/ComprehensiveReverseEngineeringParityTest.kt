package com.alan.ximiearbuds.core.parity

import com.alan.ximiearbuds.core.bluetooth.ConnectionState
import com.alan.ximiearbuds.core.bluetooth.DiscoveredDevice
import com.alan.ximiearbuds.core.bluetooth.SimulatedTransport
import com.alan.ximiearbuds.core.device.EarbudsController
import com.alan.ximiearbuds.core.protocol.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ComprehensiveReverseEngineeringParityTest {

    @Test
    fun `test EarboxSound codec and packet serialization`() {
        // Test parsing Config 115 EARBOX_SOUND_CONFIG
        // [soundType (0, 1, 2), soundId, volumeValue, totalEarVolume]
        val openBytes = byteArrayOf(0x00, 0x01, 0x50, 0x64)   // Open: id 1, vol 80, max 100
        val closeBytes = byteArrayOf(0x01, 0x02, 0x4B, 0x64)  // Close: id 2, vol 75, max 100
        val chargeBytes = byteArrayOf(0x02, 0x03, 0x55, 0x64) // Charge: id 3, vol 85, max 100
        val payload = openBytes + closeBytes + chargeBytes

        val parsed = EarboxSoundState.parseFromPayload(payload)
        assertEquals(1, parsed.openSound.soundId)
        assertEquals(80, parsed.openSound.volume)
        assertEquals(100, parsed.openSound.totalVolume)

        assertEquals(2, parsed.closeSound.soundId)
        assertEquals(75, parsed.closeSound.volume)

        assertEquals(3, parsed.chargeSound.soundId)
        assertEquals(85, parsed.chargeSound.volume)

        // Test creating SET command Config 116 EARBOX_SOUND_SET
        // type 1 (volume), secondType 0 (open), value 90
        val volCmd = EarboxSoundState.createSetCommand(
            EarboxSoundState.SETTING_TYPE_VOLUME,
            EarboxSoundState.SOUND_TYPE_OPEN,
            90
        )
        assertEquals(ConfigId.EARBOX_SOUND_SET, volCmd.type)
        assertEquals(3, volCmd.value.size)
        assertEquals(1, volCmd.value[0].toInt())
        assertEquals(0, volCmd.value[1].toInt())
        assertEquals(90, volCmd.value[2].toInt())

        // type 0 (sound tone), secondType 2 (charge), value 2
        val toneCmd = EarboxSoundState.createSetCommand(
            EarboxSoundState.SETTING_TYPE_SOUND,
            EarboxSoundState.SOUND_TYPE_CHARGE,
            2
        )
        assertEquals(ConfigId.EARBOX_SOUND_SET, toneCmd.type)
        assertEquals(0, toneCmd.value[0].toInt())
        assertEquals(2, toneCmd.value[1].toInt())
        assertEquals(2, toneCmd.value[2].toInt())
    }

    @Test
    fun `test FitDetection codec and packet serialization`() {
        // Test Start command Config 60 EAR_CANAL_DETECTION
        val startCmd = FitDetectionState.createCommand(start = true)
        assertEquals(ConfigId.EAR_CANAL_DETECTION, startCmd.type)
        assertArrayEquals(byteArrayOf(0x01), startCmd.value)

        // Test Stop command
        val stopCmd = FitDetectionState.createCommand(start = false)
        assertEquals(ConfigId.EAR_CANAL_DETECTION, stopCmd.type)
        assertArrayEquals(byteArrayOf(0x00), stopCmd.value)

        // Test parsing incoming notification: left Good (1), right Poor (2)
        val resultPayload = byteArrayOf(0x01, 0x02)
        val fitResult = FitDetectionState.parseFromPayload(resultPayload)
        assertEquals(FitDetectionState.FIT_WELL, fitResult.leftResult)
        assertEquals(FitDetectionState.FIT_NOT_WELL, fitResult.rightResult)
        assertFalse(fitResult.isRunning)
    }

    @Test
    fun `test FindDevice codec and packet serialization`() {
        // Test Ring Left Config 9 FIND_DEVICE: [state=1, deviceType=1]
        val ringLeft = FindDeviceState.createCommand(RingTarget.LEFT, start = true)
        assertEquals(ConfigId.FIND_DEVICE, ringLeft.type)
        assertArrayEquals(byteArrayOf(0x01, 0x01), ringLeft.value)

        // Test Ring Right Config 9: [state=1, deviceType=2]
        val ringRight = FindDeviceState.createCommand(RingTarget.RIGHT, start = true)
        assertArrayEquals(byteArrayOf(0x01, 0x02), ringRight.value)

        // Test Ring Both Config 9: [state=1, deviceType=3]
        val ringBoth = FindDeviceState.createCommand(RingTarget.BOTH, start = true)
        assertArrayEquals(byteArrayOf(0x01, 0x03), ringBoth.value)

        // Test Stop Ringing Both: [state=0, deviceType=3]
        val stopRing = FindDeviceState.createCommand(RingTarget.BOTH, start = false)
        assertArrayEquals(byteArrayOf(0x00, 0x03), stopRing.value)
    }

    @Test
    fun `test EarbudsController reactive state integration for newly reversed features`() = runTest {
        val controller = EarbudsController(autoConnectOnStartup = false)

        // 1. Commuting Immerse
        controller.setCommutingImmerse(2) // subway
        assertEquals(2, controller.commutingImmerseMode.value)

        // 2. Earbox Sound volume and tone
        controller.setEarboxVolume(EarboxSoundState.SOUND_TYPE_OPEN, 88)
        assertEquals(88, controller.earboxSound.value.openSound.volume)

        controller.setEarboxSoundId(EarboxSoundState.SOUND_TYPE_CHARGE, 3)
        assertEquals(3, controller.earboxSound.value.chargeSound.soundId)

        // 3. Call Listener / Hands-Free auto pickup
        controller.setCallListener(10)
        assertEquals(10, controller.callListenerSeconds.value)

        controller.setCallListener(0)
        assertEquals(0, controller.callListenerSeconds.value)

        // 4. Voice Hotword
        controller.setVoiceHotword(true)
        assertTrue(controller.voiceHotword.value)

        // 5. Fit Detection
        controller.startFitDetection()
        assertTrue(controller.fitDetection.value.isRunning)

        controller.stopFitDetection()
        assertFalse(controller.fitDetection.value.isRunning)

        // 6. Find Device
        controller.ringEarbud(RingTarget.LEFT, true)
        assertTrue(controller.findDevice.value.isRingingLeft)
        assertFalse(controller.findDevice.value.isRingingRight)
        assertTrue(controller.findDevice.value.isRinging)

        controller.ringEarbud(RingTarget.RIGHT, true)
        assertTrue(controller.findDevice.value.isRingingLeft)
        assertTrue(controller.findDevice.value.isRingingRight)

        controller.stopRinging()
        assertFalse(controller.findDevice.value.isRingingLeft)
        assertFalse(controller.findDevice.value.isRingingRight)
        assertFalse(controller.findDevice.value.isRinging)
    }
}
