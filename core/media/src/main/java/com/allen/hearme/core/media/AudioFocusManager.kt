package com.allen.hearme.core.media

import com.allen.hearme.core.common.result.HearMeResult
import com.allen.hearme.core.media.model.AudioFocusRequestConfig
import com.allen.hearme.core.media.model.AudioFocusState
import kotlinx.coroutines.flow.StateFlow

interface AudioFocusManager {
    val currentFocusState: StateFlow<AudioFocusState>
    fun hasFocus(): Boolean
    suspend fun requestAudioFocus(config: AudioFocusRequestConfig = AudioFocusRequestConfig()): HearMeResult<AudioFocusState>
    suspend fun abandonAudioFocus(): HearMeResult<Unit>
}
