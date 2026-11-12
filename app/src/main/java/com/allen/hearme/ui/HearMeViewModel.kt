package com.allen.hearme.ui

import android.content.Intent
import android.provider.Settings
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
import com.allen.hearme.core.media.MediaPlaybackController
import com.allen.hearme.core.media.model.MediaTrackInfo
import com.allen.hearme.feature.smartlistening.ListeningOrchestrator
import com.allen.hearme.feature.smartlistening.model.ListeningState
import com.allen.hearme.feature.smartlistening.model.ListeningTrigger
import com.allen.hearme.service.ServiceController
import com.allen.hearme.service.SmartListeningService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val isSmartListeningOn: Boolean = false,
    val listeningStatusText: String = "Standby • Tap switch to activate",
    val listeningState: ListeningState = ListeningState.STANDBY,
    val earphoneName: String? = null,
    val isEarphoneConnected: Boolean = false,
    val rmsEnergy: Float = 0.0f,
    val isSpeechActive: Boolean = false,
    val preferences: UserPreferences = UserPreferences(),
    val sessions: List<ListeningSessionEntity> = emptyList(),
    /** Active media track detected via MediaSessionManager, null if nothing is playing. */
    val activeMediaTrack: MediaTrackInfo? = null,
    /** Whether the user has granted Notification Listener access to this app. */
    val hasNotificationListenerAccess: Boolean = false
)

@HiltViewModel
class HearMeViewModel @Inject constructor(
    private val serviceController: ServiceController,
    private val orchestrator: ListeningOrchestrator,
    private val earphoneMonitor: EarphoneConnectionMonitor,
    private val preferencesRepository: UserPreferencesRepository,
    private val sessionRepository: ListeningSessionRepository,
    private val mediaPlaybackController: MediaPlaybackController
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        observeEarphoneConnection()
        observePreferences()
        observeListeningState()
        observeSessions()
        observeServiceState()
        observeMediaState()
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
                // Only update from orchestrator if foreground service is not running
                if (!SmartListeningService.isServiceRunning.value) {
                    val statusText = when (state) {
                        ListeningState.STANDBY           -> "Standby • Ready to listen"
                        ListeningState.STARTING          -> "Starting • Initialising microphone"
                        ListeningState.ACTIVE_LISTENING  -> "Active • Monitoring speech"
                        ListeningState.SPEECH_DETECTED   -> "Speech detected • Evaluating intent"
                        ListeningState.INTERRUPTING_MUSIC -> "Speech directed to you • Music paused"
                        ListeningState.MICROPHONE_UNAVAILABLE -> "Microphone unavailable"
                        ListeningState.INTERRUPTED       -> "Interrupted • Resuming"
                        ListeningState.STOPPED           -> "Stopped"
                        ListeningState.ERROR             -> "Error initialising microphone"
                    }
                    _uiState.value = _uiState.value.copy(
                        listeningState = state,
                        isSmartListeningOn = orchestrator.isListening,
                        listeningStatusText = statusText
                    )
                }
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

    /** Observe real-time RMS energy and speech activity from the foreground service. */
    private fun observeServiceState() {
        viewModelScope.launch {
            SmartListeningService.liveRmsEnergy.collect { rms ->
                _uiState.value = _uiState.value.copy(rmsEnergy = rms)
            }
        }
        viewModelScope.launch {
            SmartListeningService.isSpeechActive.collect { active ->
                _uiState.value = _uiState.value.copy(isSpeechActive = active)
            }
        }
        viewModelScope.launch {
            SmartListeningService.liveServiceState.collect { serviceState ->
                if (SmartListeningService.isServiceRunning.value) {
                    val statusText = when (serviceState) {
                        ListeningState.ACTIVE_LISTENING  -> "Active • Monitoring speech"
                        ListeningState.SPEECH_DETECTED   -> "Speech detected"
                        ListeningState.MICROPHONE_UNAVAILABLE -> "Microphone unavailable"
                        ListeningState.ERROR             -> "Service error"
                        ListeningState.STANDBY           -> "Standby"
                        else -> _uiState.value.listeningStatusText
                    }
                    _uiState.value = _uiState.value.copy(
                        listeningState = serviceState,
                        isSmartListeningOn = serviceState == ListeningState.ACTIVE_LISTENING ||
                                serviceState == ListeningState.SPEECH_DETECTED,
                        listeningStatusText = statusText
                    )
                } else if (serviceState == ListeningState.STANDBY || serviceState == ListeningState.STOPPED) {
                    _uiState.value = _uiState.value.copy(
                        listeningState = ListeningState.STANDBY,
                        isSmartListeningOn = false,
                        listeningStatusText = "Standby • Ready to listen",
                        rmsEnergy = 0.0f,
                        isSpeechActive = false
                    )
                }
            }
        }
    }

    /** Observe media session and Notification Listener access state. */
    private fun observeMediaState() {
        viewModelScope.launch {
            mediaPlaybackController.activeMediaTrack.collect { track ->
                _uiState.value = _uiState.value.copy(activeMediaTrack = track)
            }
        }
        viewModelScope.launch {
            mediaPlaybackController.hasNotificationListenerAccess.collect { granted ->
                _uiState.value = _uiState.value.copy(hasNotificationListenerAccess = granted)
            }
        }
    }

    fun toggleSmartListening(enable: Boolean) {
        viewModelScope.launch {
            if (enable) {
                val started = serviceController.startListeningService()
                if (started) {
                    orchestrator.startListening(ListeningTrigger.USER_MANUAL)
                    preferencesRepository.setListeningEnabled(true)
                } else {
                    _uiState.value = _uiState.value.copy(
                        listeningState = ListeningState.MICROPHONE_UNAVAILABLE,
                        listeningStatusText = "Microphone permission required"
                    )
                }
            } else {
                serviceController.stopListeningService()
                orchestrator.stopListening()
                preferencesRepository.setListeningEnabled(false)
                _uiState.value = _uiState.value.copy(
                    isSmartListeningOn = false,
                    listeningState = ListeningState.STANDBY,
                    listeningStatusText = "Standby • Ready to listen",
                    rmsEnergy = 0.0f,
                    isSpeechActive = false
                )
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

    fun setAutoRestorePlayback(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAutoRestorePlayback(enabled)
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

    /**
     * Developer test action: triggers a real media interruption (pause or duck)
     * and after a short delay restores it. Use this to verify media control
     * without needing actual speech recognition.
     */
    fun simulateMediaInterruption() {
        viewModelScope.launch {
            mediaPlaybackController.pauseMedia()
            kotlinx.coroutines.delay(3000L)
            mediaPlaybackController.restorePlayback()
        }
    }
}
