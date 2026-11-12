package com.allen.hearme.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allen.hearme.core.bluetooth.EarphoneConnectionMonitor
import com.allen.hearme.core.bluetooth.model.EarphoneConnectionState
import com.allen.hearme.core.database.entity.ListeningSessionEntity
import com.allen.hearme.core.database.preferences.DetectionSensitivity
import com.allen.hearme.core.database.preferences.MusicInterruptionBehavior
import com.allen.hearme.core.database.preferences.UserPreferences
import com.allen.hearme.core.database.preferences.UserPreferencesRepository
import com.allen.hearme.core.database.repository.ListeningSessionRepository
import com.allen.hearme.feature.smartlistening.ListeningOrchestrator
import com.allen.hearme.feature.smartlistening.model.ListeningState
import com.allen.hearme.feature.smartlistening.model.ListeningTrigger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val isSmartListeningOn: Boolean = false,
    val listeningStatusText: String = "Inactive • Tap switch to activate",
    val earphoneName: String? = null,
    val isEarphoneConnected: Boolean = false,
    val preferences: UserPreferences = UserPreferences(),
    val sessions: List<ListeningSessionEntity> = emptyList()
)

@HiltViewModel
class HearMeViewModel @Inject constructor(
    private val orchestrator: ListeningOrchestrator,
    private val earphoneMonitor: EarphoneConnectionMonitor,
    private val preferencesRepository: UserPreferencesRepository,
    private val sessionRepository: ListeningSessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        observeEarphoneConnection()
        observePreferences()
        observeListeningState()
        observeSessions()
    }

    private fun observeEarphoneConnection() {
        viewModelScope.launch {
            earphoneMonitor.connectionState.collect { connectionState ->
                when (connectionState) {
                    is EarphoneConnectionState.Connected -> {
                        _uiState.value = _uiState.value.copy(
                            isEarphoneConnected = true,
                            earphoneName = connectionState.device.name
                        )
                        if (_uiState.value.preferences.automaticActivation && !_uiState.value.isSmartListeningOn) {
                            toggleSmartListening(true)
                        }
                    }
                    is EarphoneConnectionState.Disconnected -> {
                        _uiState.value = _uiState.value.copy(
                            isEarphoneConnected = false,
                            earphoneName = null
                        )
                    }
                    is EarphoneConnectionState.Connecting -> {
                        _uiState.value = _uiState.value.copy(
                            earphoneName = "Connecting..."
                        )
                    }
                }
            }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesRepository.preferencesFlow.collect { prefs ->
                _uiState.value = _uiState.value.copy(preferences = prefs)
            }
        }
    }

    private fun observeListeningState() {
        viewModelScope.launch {
            orchestrator.currentState.collect { state ->
                val statusText = when (state) {
                    ListeningState.IDLE -> "Standby • Ready to listen"
                    ListeningState.ACTIVE_LISTENING -> "Active • Monitoring speech"
                    ListeningState.SPEECH_DETECTED -> "Speech detected • Evaluating intent"
                    ListeningState.INTERRUPTING_MUSIC -> "Speech directed to user • Music paused"
                    ListeningState.RECOVERING -> "Resuming audio playback"
                    ListeningState.ERROR -> "Error initializing microphone"
                }
                _uiState.value = _uiState.value.copy(
                    isSmartListeningOn = orchestrator.isListening,
                    listeningStatusText = statusText
                )
            }
        }
    }

    private fun observeSessions() {
        viewModelScope.launch {
            sessionRepository.getAllSessions().collect { list ->
                _uiState.value = _uiState.value.copy(sessions = list)
            }
        }
    }

    fun toggleSmartListening(enable: Boolean) {
        viewModelScope.launch {
            if (enable) {
                orchestrator.startListening(ListeningTrigger.USER_MANUAL)
                preferencesRepository.setListeningEnabled(true)
            } else {
                orchestrator.stopListening()
                preferencesRepository.setListeningEnabled(false)
            }
        }
    }

    fun setAutoActivation(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAutomaticActivation(enabled)
        }
    }

    fun setSensitivity(sensitivity: DetectionSensitivity) {
        viewModelScope.launch {
            preferencesRepository.setDetectionSensitivity(sensitivity)
        }
    }

    fun setInterruptionBehavior(behavior: MusicInterruptionBehavior) {
        viewModelScope.launch {
            preferencesRepository.setPreferredBehavior(behavior)
        }
    }

    fun setTrainingReminders(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setTrainingReminders(enabled)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            preferencesRepository.setOnboardingCompleted(true)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            sessionRepository.clearAllSessions()
        }
    }
}
