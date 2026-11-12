package com.allen.hearme.core.audio

import com.allen.hearme.core.audio.fake.FakeAudioCaptureManager
import com.allen.hearme.core.audio.fake.FakeAudioSegmentProcessor
import com.allen.hearme.core.audio.fake.FakeSpeechActivityDetector
import com.allen.hearme.core.audio.model.AudioChunk
import com.allen.hearme.core.audio.model.AudioSegment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

class CoreAudioTest {

    // ── Fake Component Tests ────────────────────────────────────────────────

    @Test
    fun audioCaptureManagerStartStopCycle() = runTest {
        val captureManager = FakeAudioCaptureManager()
        assertFalse(captureManager.isCapturing.value)

        val result = captureManager.startCapture()
        assertTrue(result.isSuccess)
        assertTrue(captureManager.isCapturing.value)

        captureManager.stopCapture()
        assertFalse(captureManager.isCapturing.value)
    }

    @Test
    fun audioCaptureStreamEmitsCorrectly() = runTest(kotlinx.coroutines.test.UnconfinedTestDispatcher()) {
        val captureManager = FakeAudioCaptureManager()
        val chunk = AudioChunk(
            timestampMs = 1000L,
            data = byteArrayOf(1, 2, 3, 4)
        )

        var receivedChunk: AudioChunk? = null
        backgroundScope.launch {
            receivedChunk = captureManager.audioStream.first()
        }

        captureManager.emitChunk(chunk)
        assertEquals(chunk, receivedChunk)
    }

    @Test
    fun speechActivityDetectorReportsActivity() = runTest {
        val detector = FakeSpeechActivityDetector(defaultSpeech = true, defaultConfidence = 0.9f)
        val chunk = AudioChunk(timestampMs = 0L, data = ByteArray(10))
        val activity = detector.detectActivity(chunk)

        assertTrue(activity.hasSpeech)
        assertEquals(0.9f, activity.confidence, 0.001f)

        detector.reset()
        assertEquals(1, detector.resetCallCount)
    }

    @Test
    fun segmentProcessorReturnsResult() = runTest {
        val processor = FakeAudioSegmentProcessor()
        val segment = AudioSegment(
            id = "seg-1",
            startTimeMs = 0L,
            endTimeMs = 500L,
            rawBytes = ByteArray(100)
        )

        val result = processor.processSegment(segment)
        assertTrue(result.isSuccess)
        assertEquals(1, processor.processedCount)
    }

    // ── Real Energy-based VAD & RMS Tests ────────────────────────────────────

    private fun generateSilencePcm(durationMs: Int, sampleRate: Int = 16000): ByteArray {
        val sampleCount = (sampleRate * durationMs) / 1000
        return ByteArray(sampleCount * 2) // 16-bit PCM = 2 bytes per sample
    }

    private fun generateSineWavePcm(
        durationMs: Int,
        frequencyHz: Double = 440.0,
        amplitude: Double = 0.8,
        sampleRate: Int = 16000
    ): ByteArray {
        val sampleCount = (sampleRate * durationMs) / 1000
        val bytes = ByteArray(sampleCount * 2)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until sampleCount) {
            val angle = 2.0 * Math.PI * frequencyHz * i / sampleRate
            val sampleVal = (sin(angle) * amplitude * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
            buffer.putShort(sampleVal)
        }
        return bytes
    }

    @Test
    fun rmsCalculationOnSilenceReturnsZero() {
        val silence = generateSilencePcm(40)
        val rms = EnergyVadDetector.calculateRms(silence)
        assertEquals(0.0f, rms, 0.0001f)
    }

    @Test
    fun rmsCalculationOnSineWaveReturnsExpectedValue() {
        val sineWave = generateSineWavePcm(durationMs = 100, amplitude = 0.7071)
        val rms = EnergyVadDetector.calculateRms(sineWave)
        // Theoretical RMS of sine wave with peak amplitude A is A / sqrt(2) ≈ 0.7071 / 1.4142 ≈ 0.50
        assertEquals(0.50f, rms, 0.05f)
    }

    @Test
    fun vadSilenceBelowThresholdDoesNotTriggerSpeech() = runTest {
        val detector = EnergyVadDetector()
        val silenceChunk = AudioChunk(timestampMs = 100L, data = generateSilencePcm(40))

        val activity = detector.detectActivity(silenceChunk)
        assertFalse("Silence should not trigger speech", activity.hasSpeech)
        assertEquals(0.0f, activity.confidence, 0.01f)
    }

    @Test
    fun vadSpeechAboveThresholdTriggersSpeechAfterMinDuration() = runTest {
        val detector = EnergyVadDetector(
            sensitivity = 0.7f,
            minSpeechDurationMs = 80L,
            silenceTimeoutMs = 200L
        )

        val loudPcm = generateSineWavePcm(durationMs = 40, amplitude = 0.6)

        // Frame 1 at 0ms: starts speech tracking, duration = 0ms
        val act1 = detector.detectActivity(AudioChunk(timestampMs = 0L, data = loudPcm))
        assertFalse("Should not be speech before minDuration elapsed", act1.hasSpeech)

        // Frame 2 at 40ms: duration = 40ms, still below 80ms
        val act2 = detector.detectActivity(AudioChunk(timestampMs = 40L, data = loudPcm))
        assertFalse("Should not be speech before minDuration elapsed", act2.hasSpeech)

        // Frame 3 at 80ms: duration = 80ms, reaches min speech duration
        val act3 = detector.detectActivity(AudioChunk(timestampMs = 80L, data = loudPcm))
        assertTrue("Should be speech once minDuration reached", act3.hasSpeech)
        assertTrue("Confidence should be > 0", act3.confidence > 0f)

        // Frame 4 at 120ms: continues speech
        val act4 = detector.detectActivity(AudioChunk(timestampMs = 120L, data = loudPcm))
        assertTrue("Speech should remain active", act4.hasSpeech)
    }

    @Test
    fun vadSilenceHangoverPreservesSpeechStateTemporarily() = runTest {
        val detector = EnergyVadDetector(
            sensitivity = 0.7f,
            minSpeechDurationMs = 40L,
            silenceTimeoutMs = 200L
        )

        val loudPcm = generateSineWavePcm(durationMs = 40, amplitude = 0.6)
        val silencePcm = generateSilencePcm(40)

        // Frame 1 at 0ms (starts speech tracking)
        detector.detectActivity(AudioChunk(timestampMs = 0L, data = loudPcm))
        // Frame 2 at 40ms (duration = 40ms >= minSpeechDurationMs, speech active)
        val actSpeech = detector.detectActivity(AudioChunk(timestampMs = 40L, data = loudPcm))
        assertTrue(actSpeech.hasSpeech)

        // 100ms silence at 140ms (silenceElapsed = 100ms < 200ms timeout) -> hangover keeps speech true
        val actHangover = detector.detectActivity(AudioChunk(timestampMs = 140L, data = silencePcm))
        assertTrue("Hangover should maintain speech state during short silence", actHangover.hasSpeech)

        // 250ms silence at 290ms (silenceElapsed = 250ms > 200ms timeout) -> speech ends
        val actEnded = detector.detectActivity(AudioChunk(timestampMs = 290L, data = silencePcm))
        assertFalse("Speech should clear once silence timeout exceeded", actEnded.hasSpeech)
    }

    @Test
    fun vadResetCleansInternalState() = runTest {
        val detector = EnergyVadDetector(minSpeechDurationMs = 40L)
        val loudPcm = generateSineWavePcm(durationMs = 40, amplitude = 0.6)

        detector.detectActivity(AudioChunk(timestampMs = 40L, data = loudPcm))
        detector.detectActivity(AudioChunk(timestampMs = 80L, data = loudPcm))

        detector.reset()

        // After reset, single loud frame should start from scratch (not immediately in speech)
        val actAfterReset = detector.detectActivity(AudioChunk(timestampMs = 120L, data = loudPcm))
        assertFalse("After reset, min duration window starts over", actAfterReset.hasSpeech)
    }
}
