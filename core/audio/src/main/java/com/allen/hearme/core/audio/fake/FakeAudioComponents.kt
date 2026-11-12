package com.allen.hearme.core.audio.fake

import com.allen.hearme.core.audio.AudioCaptureManager
import com.allen.hearme.core.audio.AudioSegmentProcessor
import com.allen.hearme.core.audio.SpeechActivityDetector
import com.allen.hearme.core.audio.model.AudioChunk
import com.allen.hearme.core.audio.model.AudioConfig
import com.allen.hearme.core.audio.model.AudioSegment
import com.allen.hearme.core.audio.model.SpeechActivity
import com.allen.hearme.core.common.result.HearMeResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAudioCaptureManager : AudioCaptureManager {
    private val _isCapturing = MutableStateFlow(false)
    override val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private val _audioStream = MutableSharedFlow<AudioChunk>(replay = 1, extraBufferCapacity = 64)
    override val audioStream: Flow<AudioChunk> = _audioStream.asSharedFlow()

    var shouldFail: Boolean = false

    override suspend fun startCapture(config: AudioConfig): HearMeResult<Unit> {
        if (shouldFail) {
            return HearMeResult.Error(IllegalStateException("Failed to initialize audio capture in fake"))
        }
        _isCapturing.value = true
        return HearMeResult.Success(Unit)
    }

    override suspend fun stopCapture() {
        _isCapturing.value = false
    }

    suspend fun emitChunk(chunk: AudioChunk) {
        _audioStream.emit(chunk)
    }
}

class FakeSpeechActivityDetector(
    var defaultSpeech: Boolean = false,
    var defaultConfidence: Float = 0.85f,
    var defaultEnergy: Float = 0.5f
) : SpeechActivityDetector {
    var resetCallCount: Int = 0
        private set

    override suspend fun detectActivity(chunk: AudioChunk): SpeechActivity {
        return SpeechActivity(
            hasSpeech = defaultSpeech,
            confidence = defaultConfidence,
            energyRms = defaultEnergy
        )
    }

    override fun reset() {
        resetCallCount++
    }
}

class FakeAudioSegmentProcessor : AudioSegmentProcessor {
    var processedCount: Int = 0
        private set

    override suspend fun processSegment(segment: AudioSegment): HearMeResult<AudioSegment> {
        processedCount++
        return HearMeResult.Success(segment)
    }
}
