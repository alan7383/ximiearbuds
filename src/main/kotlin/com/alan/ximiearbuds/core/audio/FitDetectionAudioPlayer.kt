package com.alan.ximiearbuds.core.audio

import java.io.BufferedInputStream
import java.util.concurrent.atomic.AtomicBoolean
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.DataLine

/**
 * 1:1 official acoustic measurement tone player for Fit Detection.
 * Replicates `FitDetectionViewModel.java` ExoPlayer playback of `R.raw.fitness_detect`.
 * Plays the continuous calibration audio wave through the system output (Bluetooth earbuds).
 */
object FitDetectionAudioPlayer {

    private var activeClip: Clip? = null
    private val isPlaying = AtomicBoolean(false)
    private val lock = Any()

    fun play() {
        synchronized(lock) {
            if (isPlaying.get()) return

            try {
                val resStream = javaClass.classLoader.getResourceAsStream("raw/fitness_detect.wav")
                    ?: javaClass.getResourceAsStream("/raw/fitness_detect.wav")

                if (resStream == null) {
                    System.err.println("[FitDetectionAudioPlayer] Warning: raw/fitness_detect.wav not found")
                    return
                }

                val bufferedStream = BufferedInputStream(resStream)
                val audioIn = AudioSystem.getAudioInputStream(bufferedStream)
                val info = DataLine.Info(Clip::class.java, audioIn.format)
                val clip = AudioSystem.getLine(info) as Clip

                clip.open(audioIn)
                clip.loop(Clip.LOOP_CONTINUOUSLY)
                clip.start()

                activeClip = clip
                isPlaying.set(true)
                println("[FitDetectionAudioPlayer] Playing fitness_detect.wav...")
            } catch (e: Exception) {
                System.err.println("[FitDetectionAudioPlayer] Could not start audio playback: ${e.message}")
            }
        }
    }

    fun stop() {
        synchronized(lock) {
            try {
                activeClip?.let { clip ->
                    if (clip.isRunning) {
                        clip.stop()
                    }
                    clip.close()
                }
            } catch (e: Exception) {
                System.err.println("[FitDetectionAudioPlayer] Error stopping audio: ${e.message}")
            } finally {
                activeClip = null
                isPlaying.set(false)
                println("[FitDetectionAudioPlayer] Stopped fitness_detect.wav")
            }
        }
    }

    fun isAudioPlaying(): Boolean = isPlaying.get()
}
