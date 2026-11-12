package com.allen.hearme.feature.smartlistening

import com.allen.hearme.core.audio.AudioCaptureManager
import com.allen.hearme.core.bluetooth.EarphoneConnectionMonitor
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

    private val _currentState = MutableStateFlow(ListeningState.IDLE)
    override val currentState: StateFlow<ListeningState> = _currentState.asStateFlow()

    override val isListening: Boolean
        get() = _currentState.value != ListeningState.IDLE && _currentState.value != ListeningState.ERROR

    override suspend fun startListening(trigger: ListeningTrigger) {
        val captureResult = audioCaptureManager.startCapture()
        if (captureResult.isSuccess) {
            _currentState.value = ListeningState.ACTIVE_LISTENING
        } else {
            _currentState.value = ListeningState.ERROR
        }
    }

    override suspend fun stopListening() {
        audioCaptureManager.stopCapture()
        _currentState.value = ListeningState.IDLE
    }
}

class FakeListeningOrchestrator(
    initialState: ListeningState = ListeningState.IDLE
) : ListeningOrchestrator {
    private val _currentState = MutableStateFlow(initialState)
    override val currentState: StateFlow<ListeningState> = _currentState.asStateFlow()

    override val isListening: Boolean
        get() = _currentState.value == ListeningState.ACTIVE_LISTENING

    override suspend fun startListening(trigger: ListeningTrigger) {
        _currentState.value = ListeningState.ACTIVE_LISTENING
    }

    override suspend fun stopListening() {
        _currentState.value = ListeningState.IDLE
    }
}
