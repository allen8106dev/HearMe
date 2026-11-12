package com.allen.hearme.core.media.fake

import com.allen.hearme.core.common.result.HearMeResult
import com.allen.hearme.core.media.AudioFocusManager
import com.allen.hearme.core.media.MediaPlaybackController
import com.allen.hearme.core.media.model.AudioFocusRequestConfig
import com.allen.hearme.core.media.model.AudioFocusState
import com.allen.hearme.core.media.model.PlaybackAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeMediaPlaybackController(
    private var isPlaying: Boolean = true
) : MediaPlaybackController {

    val actionHistory = mutableListOf<PlaybackAction>()
    var currentVolumeRatio: Float = 1.0f

    override suspend fun pauseMedia(): HearMeResult<Unit> {
        actionHistory.add(PlaybackAction.PAUSE)
        isPlaying = false
        return HearMeResult.Success(Unit)
    }

    override suspend fun resumeMedia(): HearMeResult<Unit> {
        actionHistory.add(PlaybackAction.RESUME)
        isPlaying = true
        return HearMeResult.Success(Unit)
    }

    override suspend fun lowerVolume(duckRatio: Float): HearMeResult<Unit> {
        actionHistory.add(PlaybackAction.LOWER_VOLUME)
        currentVolumeRatio = duckRatio
        return HearMeResult.Success(Unit)
    }

    override suspend fun restoreVolume(): HearMeResult<Unit> {
        actionHistory.add(PlaybackAction.RESTORE_VOLUME)
        currentVolumeRatio = 1.0f
        return HearMeResult.Success(Unit)
    }

    override fun isMediaPlaying(): Boolean = isPlaying
}

class FakeAudioFocusManager : AudioFocusManager {
    private val _currentFocusState = MutableStateFlow(AudioFocusState.NO_FOCUS)
    override val currentFocusState: StateFlow<AudioFocusState> = _currentFocusState.asStateFlow()

    override fun hasFocus(): Boolean = _currentFocusState.value == AudioFocusState.FOCUSED

    override suspend fun requestAudioFocus(config: AudioFocusRequestConfig): HearMeResult<AudioFocusState> {
        _currentFocusState.value = AudioFocusState.FOCUSED
        return HearMeResult.Success(AudioFocusState.FOCUSED)
    }

    override suspend fun abandonAudioFocus(): HearMeResult<Unit> {
        _currentFocusState.value = AudioFocusState.NO_FOCUS
        return HearMeResult.Success(Unit)
    }
}
