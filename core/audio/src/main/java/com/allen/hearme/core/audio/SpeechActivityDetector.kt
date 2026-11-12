package com.allen.hearme.core.audio

import com.allen.hearme.core.audio.model.AudioChunk
import com.allen.hearme.core.audio.model.SpeechActivity

interface SpeechActivityDetector {
    suspend fun detectActivity(chunk: AudioChunk): SpeechActivity
    fun reset()
}
