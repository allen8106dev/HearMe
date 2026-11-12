package com.allen.hearme.core.media

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import com.allen.hearme.core.common.result.HearMeResult
import com.allen.hearme.core.media.model.AudioFocusRequestConfig
import com.allen.hearme.core.media.model.AudioFocusState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Real implementation of [AudioFocusManager] backed by Android [AudioManager].
 *
 * - Requests AUDIOFOCUS_GAIN_TRANSIENT when [config.acceptsDucking] is false.
 * - Requests AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK when [config.acceptsDucking] is true,
 *   which signals compliant apps (Spotify, YouTube Music, etc.) to lower their volume
 *   automatically.  HearMe never modifies the master system volume.
 * - Checks [AudioManager.getMode] before requesting; calls during phone calls
 *   ([MODE_IN_CALL], [MODE_IN_COMMUNICATION], [MODE_RINGTONE]) are silently rejected.
 */
class RealAudioFocusManager(
    private val context: Context
) : AudioFocusManager {

    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _currentFocusState = MutableStateFlow(AudioFocusState.NO_FOCUS)
    override val currentFocusState: StateFlow<AudioFocusState> = _currentFocusState.asStateFlow()

    private var currentFocusRequest: AudioFocusRequest? = null

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        _currentFocusState.value = when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK -> AudioFocusState.FOCUSED
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK  -> AudioFocusState.LOSS_TRANSIENT_CAN_DUCK
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT            -> AudioFocusState.LOSS_TRANSIENT
            AudioManager.AUDIOFOCUS_LOSS                      -> AudioFocusState.LOSS_PERMANENT
            else -> AudioFocusState.NO_FOCUS
        }
    }

    override fun hasFocus(): Boolean = _currentFocusState.value == AudioFocusState.FOCUSED

    override suspend fun requestAudioFocus(
        config: AudioFocusRequestConfig
    ): HearMeResult<AudioFocusState> {
        // Do not interfere with phone calls, VoIP or ringer.
        val mode = audioManager.mode
        if (mode == AudioManager.MODE_IN_CALL ||
            mode == AudioManager.MODE_IN_COMMUNICATION ||
            mode == AudioManager.MODE_RINGTONE
        ) {
            return HearMeResult.Error(
                IllegalStateException("Audio focus not requested: active call or ringer detected (mode=$mode)")
            )
        }

        val focusGain = if (config.acceptsDucking) {
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
        } else {
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
        }

        val attributes = AudioAttributes.Builder()
            .setUsage(config.audioUsage)
            .setContentType(config.contentType)
            .build()

        val request = AudioFocusRequest.Builder(focusGain)
            .setAudioAttributes(attributes)
            .setAcceptsDelayedFocusGain(false)
            .setOnAudioFocusChangeListener(focusChangeListener)
            .build()

        currentFocusRequest = request

        return when (audioManager.requestAudioFocus(request)) {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                _currentFocusState.value = AudioFocusState.FOCUSED
                HearMeResult.Success(AudioFocusState.FOCUSED)
            }
            AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
                HearMeResult.Success(AudioFocusState.NO_FOCUS)
            }
            else -> {
                HearMeResult.Error(RuntimeException("Audio focus request failed"))
            }
        }
    }

    override suspend fun abandonAudioFocus(): HearMeResult<Unit> {
        val request = currentFocusRequest
        return if (request != null) {
            audioManager.abandonAudioFocusRequest(request)
            currentFocusRequest = null
            _currentFocusState.value = AudioFocusState.NO_FOCUS
            HearMeResult.Success(Unit)
        } else {
            _currentFocusState.value = AudioFocusState.NO_FOCUS
            HearMeResult.Success(Unit)
        }
    }
}
