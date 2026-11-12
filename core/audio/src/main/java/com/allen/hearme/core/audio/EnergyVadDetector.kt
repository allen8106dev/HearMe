package com.allen.hearme.core.audio

import com.allen.hearme.core.audio.model.AudioChunk
import com.allen.hearme.core.audio.model.SpeechActivity
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Lightweight, energy-based Voice Activity Detector (VAD).
 *
 * Algorithm:
 * 1. Computes the Root Mean Square (RMS) energy across 16-bit PCM samples.
 * 2. Maintains an exponentially smoothed adaptive noise floor estimation.
 * 3. Compares signal energy against the adaptive noise floor modulated by [sensitivity].
 * 4. Applies a hangover counter (hysteresis/silence timeout) and minimum speech duration window
 *    to prevent rapid oscillating state transitions between words.
 * 5. Highly CPU-efficient with zero memory allocation per evaluation.
 */
class EnergyVadDetector(
    private var sensitivity: Float = 0.5f, // 0.0 (least sensitive) to 1.0 (most sensitive)
    private val minSpeechDurationMs: Long = 120L,
    private val silenceTimeoutMs: Long = 300L
) : SpeechActivityDetector {

    // Noise floor tracking in RMS amplitude (0.0 to 1.0)
    private var adaptiveNoiseFloor: Float = 0.015f
    private val noiseAlpha: Float = 0.05f // Smoothing factor for noise updates

    // Temporal hangover tracking (-1 indicates unset / idle)
    private var speechStartTimestampMs: Long = -1L
    private var lastSpeechDetectedTimestampMs: Long = -1L
    private var isCurrentlyInSpeech: Boolean = false

    override suspend fun detectActivity(chunk: AudioChunk): SpeechActivity {
        val rms = calculateRms(chunk.data)

        // Threshold calculation:
        // High sensitivity lowers the SNR barrier above the noise floor.
        // Low sensitivity requires significantly higher energy above noise.
        // Sensitivity range maps multiplier between 1.8x and 5.0x
        val snrMultiplier = 5.0f - (sensitivity.coerceIn(0.0f, 1.0f) * 3.2f)
        val speechThreshold = max(0.012f, adaptiveNoiseFloor * snrMultiplier)

        val isEnergyAboveThreshold = rms >= speechThreshold
        val currentTimeMs = chunk.timestampMs

        if (isEnergyAboveThreshold) {
            if (speechStartTimestampMs < 0L) {
                speechStartTimestampMs = currentTimeMs
            }
            lastSpeechDetectedTimestampMs = currentTimeMs
            val durationInSpeech = currentTimeMs - speechStartTimestampMs
            if (durationInSpeech >= minSpeechDurationMs) {
                isCurrentlyInSpeech = true
            }
        } else {
            // Adaptive noise floor update (only when speech is not present and silence has passed)
            if (!isCurrentlyInSpeech && lastSpeechDetectedTimestampMs >= 0L &&
                (currentTimeMs - lastSpeechDetectedTimestampMs) > silenceTimeoutMs) {
                adaptiveNoiseFloor = (1 - noiseAlpha) * adaptiveNoiseFloor + noiseAlpha * rms
                adaptiveNoiseFloor = adaptiveNoiseFloor.coerceIn(0.005f, 0.25f)
            }

            // Silence hangover check
            if (isCurrentlyInSpeech) {
                val silenceElapsed = if (lastSpeechDetectedTimestampMs >= 0L) {
                    currentTimeMs - lastSpeechDetectedTimestampMs
                } else 0L

                if (silenceElapsed >= silenceTimeoutMs) {
                    isCurrentlyInSpeech = false
                    speechStartTimestampMs = -1L
                }
            } else {
                speechStartTimestampMs = -1L
            }
        }

        // Calculate continuous speech likelihood score between 0.0 and 1.0
        val likelihood = if (speechThreshold > 0f) {
            min(1.0f, max(0.0f, (rms - adaptiveNoiseFloor) / (speechThreshold * 1.5f)))
        } else 0.0f

        return SpeechActivity(
            hasSpeech = isCurrentlyInSpeech,
            confidence = likelihood,
            energyRms = rms,
            timestampMs = currentTimeMs
        )
    }

    override fun reset() {
        adaptiveNoiseFloor = 0.015f
        speechStartTimestampMs = -1L
        lastSpeechDetectedTimestampMs = -1L
        isCurrentlyInSpeech = false
    }

    fun updateSensitivity(newSensitivity: Float) {
        this.sensitivity = newSensitivity.coerceIn(0.0f, 1.0f)
    }

    companion object {
        /**
         * Calculates normalized Root Mean Square (RMS) amplitude from 16-bit PCM little-endian byte array.
         * Returns value in range [0.0, 1.0].
         */
        fun calculateRms(pcmBytes: ByteArray): Float {
            if (pcmBytes.isEmpty()) return 0.0f
            val sampleCount = pcmBytes.size / 2
            if (sampleCount == 0) return 0.0f

            var sumOfSquares = 0.0
            val byteBuffer = ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN)

            while (byteBuffer.remaining() >= 2) {
                val sample = byteBuffer.short.toDouble()
                val normalizedSample = sample / 32768.0 // Normalize short to [-1.0, 1.0]
                sumOfSquares += normalizedSample * normalizedSample
            }

            val meanSquare = sumOfSquares / sampleCount
            return sqrt(meanSquare).toFloat().coerceIn(0.0f, 1.0f)
        }
    }
}
