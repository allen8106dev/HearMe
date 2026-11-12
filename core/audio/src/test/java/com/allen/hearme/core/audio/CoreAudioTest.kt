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

class CoreAudioTest {

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
}
