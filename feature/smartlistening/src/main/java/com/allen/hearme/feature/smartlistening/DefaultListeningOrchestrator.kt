package com.allen.hearme.feature.smartlistening

import com.allen.hearme.core.audio.AudioCaptureManager
import com.allen.hearme.core.bluetooth.EarphoneConnectionMonitor
import com.allen.hearme.core.common.result.HearMeResult
import com.allen.hearme.core.media.MediaPlaybackController
import com.allen.hearme.feature.smartlistening.model.ListeningState
import com.allen.hearme.feature.smartlistening.model.ListeningTrigger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DefaultListeningOrchestrator(
    private val audioCaptureManager: AudioCaptureManager,
    private val earphoneMonitor: EarphoneConnectionMonitor,
    private val mediaPlaybackController: MediaPlaybackController,
    private val decisionEngine: ListeningDecisionEngine
) : ListeningOrchestrator {

    private val _currentState = MutableStateFlow(ListeningState.STANDBY)
    override val currentState: StateFlow<ListeningState> = _currentState.asStateFlow()

    override val isListening: Boolean
        get() = _currentState.value == ListeningState.ACTIVE_LISTENING ||
                _currentState.value == ListeningState.SPEECH_DETECTED

    override suspend fun startListening(trigger: ListeningTrigger) {
        _currentState.value = ListeningState.STARTING
        val captureResult = audioCaptureManager.startCapture()
        if (captureResult.isSuccess) {
            _currentState.value = ListeningState.ACTIVE_LISTENING
        } else {
            val error = (captureResult as? HearMeResult.Error)?.throwable
            if (error is SecurityException) {
                _currentState.value = ListeningState.MICROPHONE_UNAVAILABLE
            } else {
                _currentState.value = ListeningState.ERROR
            }
        }
    }

    override suspend fun stopListening() {
        audioCaptureManager.stopCapture()
        _currentState.value = ListeningState.STANDBY
    }
}

class FakeListeningOrchestrator(
    initialState: ListeningState = ListeningState.STANDBY
) : ListeningOrchestrator {
    private val _currentState = MutableStateFlow(initialState)
    override val currentState: StateFlow<ListeningState> = _currentState.asStateFlow()

    override val isListening: Boolean
        get() = _currentState.value == ListeningState.ACTIVE_LISTENING ||
                _currentState.value == ListeningState.SPEECH_DETECTED

    override suspend fun startListening(trigger: ListeningTrigger) {
        _currentState.value = ListeningState.ACTIVE_LISTENING
    }

    override suspend fun stopListening() {
        _currentState.value = ListeningState.STANDBY
    }
}
