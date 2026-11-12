package com.allen.hearme.core.audio

import com.allen.hearme.core.audio.model.AudioChunk
import com.allen.hearme.core.audio.model.AudioConfig
import com.allen.hearme.core.common.result.HearMeResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AudioCaptureManager {
    val isCapturing: StateFlow<Boolean>
    val audioStream: Flow<AudioChunk>
    suspend fun startCapture(config: AudioConfig = AudioConfig()): HearMeResult<Unit>
    suspend fun stopCapture()
}
