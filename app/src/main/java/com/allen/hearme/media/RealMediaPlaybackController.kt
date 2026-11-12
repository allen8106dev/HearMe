package com.allen.hearme.media

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import com.allen.hearme.core.common.dispatcher.DispatcherProvider
import com.allen.hearme.core.common.result.HearMeResult
import com.allen.hearme.core.database.preferences.MusicInterruptionBehavior
import com.allen.hearme.core.database.preferences.UserPreferencesRepository
import com.allen.hearme.core.media.AudioFocusManager
import com.allen.hearme.core.media.MediaPlaybackController
import com.allen.hearme.core.media.model.AudioFocusRequestConfig
import com.allen.hearme.core.media.model.InterruptionRecord
import com.allen.hearme.core.media.model.MediaInterruptionAction
import com.allen.hearme.core.media.model.MediaTrackInfo
import com.allen.hearme.service.HearMeNotificationListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Production implementation of [MediaPlaybackController] using Android's
 * [MediaSessionManager] API.
 *
 * # Notification Listener Access
 * [MediaSessionManager.getActiveSessions] requires the calling app to hold the
 * android.permission.BIND_NOTIFICATION_LISTENER_SERVICE privilege, granted by the user
 * via Settings → Apps → Special app access → Notification access.
 *
 * Without this access a [SecurityException] is thrown.  This class degrades gracefully:
 * - [hasNotificationListenerAccess] reports the live state.
 * - When NLS access is unavailable, falls back to Audio Focus ducking.
 *
 * # Phone-Call Safety
 * Before requesting audio focus or pausing media the [AudioManager.getMode] is checked.
 * If the device is in a call, ringing, or VoIP the interruption is skipped.
 *
 * # Restoration Safety
 * [restorePlayback] only resumes media if:
 *   1. HearMe actually modified playback ([InterruptionRecord.didModifyPlayback] == true).
 *   2. The user has [autoRestorePlayback] enabled in preferences.
 *   3. The session that was paused is still alive and still paused
 *      (i.e. user did not manually resume something).
 */
class RealMediaPlaybackController(
    private val context: Context,
    private val audioFocusManager: AudioFocusManager,
    private val preferencesRepository: UserPreferencesRepository,
    private val dispatchers: DispatcherProvider
) : MediaPlaybackController {

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)
    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mediaSessionManager: MediaSessionManager =
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

    private val _activeMediaTrack = MutableStateFlow<MediaTrackInfo?>(null)
    override val activeMediaTrack: StateFlow<MediaTrackInfo?> = _activeMediaTrack.asStateFlow()

    private val _hasNlsAccess = MutableStateFlow(false)
    override val hasNotificationListenerAccess: StateFlow<Boolean> = _hasNlsAccess.asStateFlow()

    @Volatile
    private var activeInterruption: InterruptionRecord? = null

    private val nlsComponentName: ComponentName
        get() = ComponentName(context, HearMeNotificationListenerService::class.java)

    private val sessionListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            updateActiveTrack(controllers)
        }

    init {
        scope.launch { startObservingSessions() }
        // Also observe NLS connected state from service companion.
        scope.launch {
            HearMeNotificationListenerService.isConnected.collect { connected ->
                _hasNlsAccess.value = connected
                if (connected) {
                    startObservingSessions()
                } else {
                    _activeMediaTrack.value = null
                }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun checkNlsAccess(): Boolean {
        return try {
            mediaSessionManager.getActiveSessions(nlsComponentName)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    private fun startObservingSessions() {
        val hasAccess = checkNlsAccess()
        _hasNlsAccess.value = hasAccess
        if (!hasAccess) return

        try {
            val initial = mediaSessionManager.getActiveSessions(nlsComponentName)
            updateActiveTrack(initial)
            try {
                mediaSessionManager.addOnActiveSessionsChangedListener(
                    sessionListener,
                    nlsComponentName,
                    Handler(Looper.getMainLooper())
                )
            } catch (_: IllegalStateException) {
                // Listener already registered — ignore.
            }
        } catch (_: SecurityException) {
            _hasNlsAccess.value = false
        }
    }

    private fun updateActiveTrack(controllers: List<MediaController>?) {
        val playing = controllers
            ?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: controllers?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PAUSED }

        if (playing == null) {
            _activeMediaTrack.value = null
            return
        }

        val meta = playing.metadata
        val state = playing.playbackState
        val actions = state?.actions ?: 0L

        _activeMediaTrack.value = MediaTrackInfo(
            sessionId = playing.sessionToken.toString(),
            packageName = playing.packageName ?: "",
            title = meta?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE),
            artist = meta?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST),
            album = meta?.getString(android.media.MediaMetadata.METADATA_KEY_ALBUM),
            isPlaying = state?.state == PlaybackState.STATE_PLAYING,
            isPaused = state?.state == PlaybackState.STATE_PAUSED,
            playbackPositionMs = state?.position ?: 0L,
            canPause = (actions and PlaybackState.ACTION_PAUSE) != 0L,
            canPlay = (actions and PlaybackState.ACTION_PLAY) != 0L
        )
    }

    private fun isInCall(): Boolean {
        val mode = audioManager.mode
        return mode == AudioManager.MODE_IN_CALL ||
                mode == AudioManager.MODE_IN_COMMUNICATION ||
                mode == AudioManager.MODE_RINGTONE
    }

    private fun getPlayingController(): MediaController? {
        return try {
            mediaSessionManager.getActiveSessions(nlsComponentName)
                .firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
        } catch (_: SecurityException) {
            null
        }
    }

    // ── MediaPlaybackController ───────────────────────────────────────────────

    override suspend fun pauseMedia(): HearMeResult<Unit> = withContext(dispatchers.main) {
        if (isInCall()) {
            return@withContext HearMeResult.Error(
                IllegalStateException("Will not pause: active call detected")
            )
        }

        val prefs = preferencesRepository.preferencesFlow.first()

        if (prefs.preferredBehavior == MusicInterruptionBehavior.DO_NOT_CONTROL) {
            activeInterruption = InterruptionRecord(
                sessionId = "",
                packageName = "",
                wasPlayingBeforeInterruption = false,
                positionMsAtInterruption = 0,
                actionTaken = MediaInterruptionAction.NONE,
                timestampMs = System.currentTimeMillis(),
                didModifyPlayback = false
            )
            return@withContext HearMeResult.Success(Unit)
        }

        // Prefer direct transport-control pause when NLS is available.
        if (_hasNlsAccess.value &&
            prefs.preferredBehavior == MusicInterruptionBehavior.PAUSE_MUSIC
        ) {
            val controller = getPlayingController()
            if (controller != null) {
                val track = _activeMediaTrack.value
                activeInterruption = InterruptionRecord(
                    sessionId = controller.sessionToken.toString(),
                    packageName = controller.packageName ?: "",
                    wasPlayingBeforeInterruption = true,
                    positionMsAtInterruption = track?.playbackPositionMs ?: 0L,
                    actionTaken = MediaInterruptionAction.PAUSED_VIA_TRANSPORT,
                    timestampMs = System.currentTimeMillis(),
                    didModifyPlayback = true
                )
                controller.transportControls.pause()
                return@withContext HearMeResult.Success(Unit)
            }
        }

        // Fallback: Audio Focus ducking.
        val focusResult =
            audioFocusManager.requestAudioFocus(AudioFocusRequestConfig(acceptsDucking = true))
        activeInterruption = InterruptionRecord(
            sessionId = "",
            packageName = "",
            wasPlayingBeforeInterruption = isMediaPlaying(),
            positionMsAtInterruption = 0L,
            actionTaken = MediaInterruptionAction.DUCKED_VIA_AUDIO_FOCUS,
            timestampMs = System.currentTimeMillis(),
            didModifyPlayback = focusResult.isSuccess
        )
        return@withContext if (focusResult.isSuccess) HearMeResult.Success(Unit)
        else HearMeResult.Error(RuntimeException("Audio focus request failed"))
    }

    override suspend fun resumeMedia(): HearMeResult<Unit> = withContext(dispatchers.main) {
        // Lightweight resume — use restorePlayback() for safeguarded restoration.
        try {
            mediaSessionManager.getActiveSessions(nlsComponentName)
                .firstOrNull { it.playbackState?.state == PlaybackState.STATE_PAUSED }
                ?.transportControls?.play()
        } catch (_: SecurityException) {
            audioFocusManager.abandonAudioFocus()
        }
        HearMeResult.Success(Unit)
    }

    override suspend fun lowerVolume(duckRatio: Float): HearMeResult<Unit> {
        if (isInCall()) return HearMeResult.Error(IllegalStateException("Active call detected"))
        return audioFocusManager.requestAudioFocus(AudioFocusRequestConfig(acceptsDucking = true))
            .let { if (it.isSuccess) HearMeResult.Success(Unit) else HearMeResult.Error(RuntimeException("Focus failed")) }
    }

    override suspend fun restoreVolume(): HearMeResult<Unit> {
        audioFocusManager.abandonAudioFocus()
        return HearMeResult.Success(Unit)
    }

    override suspend fun restorePlayback(): HearMeResult<Unit> = withContext(dispatchers.main) {
        val record = activeInterruption
        activeInterruption = null

        if (record == null || !record.didModifyPlayback) {
            audioFocusManager.abandonAudioFocus()
            return@withContext HearMeResult.Success(Unit)
        }

        val prefs = preferencesRepository.preferencesFlow.first()
        if (!prefs.autoRestorePlayback) {
            audioFocusManager.abandonAudioFocus()
            return@withContext HearMeResult.Success(Unit)
        }

        delay(prefs.restorationDelayMs)

        when (record.actionTaken) {
            MediaInterruptionAction.PAUSED_VIA_TRANSPORT -> {
                val stillPaused = try {
                    mediaSessionManager.getActiveSessions(nlsComponentName).any { ctrl ->
                        ctrl.sessionToken.toString() == record.sessionId &&
                                ctrl.playbackState?.state == PlaybackState.STATE_PAUSED
                    }
                } catch (_: SecurityException) {
                    false
                }
                if (stillPaused && record.wasPlayingBeforeInterruption) {
                    try {
                        mediaSessionManager.getActiveSessions(nlsComponentName)
                            .firstOrNull { it.sessionToken.toString() == record.sessionId }
                            ?.transportControls?.play()
                    } catch (_: SecurityException) { /* NLS revoked */ }
                }
            }
            MediaInterruptionAction.DUCKED_VIA_AUDIO_FOCUS -> {
                audioFocusManager.abandonAudioFocus()
            }
            MediaInterruptionAction.NONE -> { /* Nothing to restore */ }
        }

        HearMeResult.Success(Unit)
    }

    override fun cancelInterruption() {
        activeInterruption = null
        scope.launch { audioFocusManager.abandonAudioFocus() }
    }

    override fun getActiveInterruption(): InterruptionRecord? = activeInterruption

    override fun isMediaPlaying(): Boolean = _activeMediaTrack.value?.isPlaying == true
}
