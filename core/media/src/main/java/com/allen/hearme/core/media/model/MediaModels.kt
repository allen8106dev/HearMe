package com.allen.hearme.core.media.model

enum class PlaybackAction {
    PAUSE,
    RESUME,
    LOWER_VOLUME,
    RESTORE_VOLUME
}

enum class AudioFocusState {
    FOCUSED,
    LOSS_TRANSIENT_CAN_DUCK,
    LOSS_TRANSIENT,
    LOSS_PERMANENT,
    NO_FOCUS
}

data class AudioFocusRequestConfig(
    val audioUsage: Int = 1, // USAGE_MEDIA
    val contentType: Int = 2, // CONTENT_TYPE_SPEECH or MUSIC
    val acceptsDucking: Boolean = true
)

/**
 * Represents the currently-active media track detected via MediaSessionManager.
 * All fields are nullable because media apps vary in what they expose.
 */
data class MediaTrackInfo(
    val sessionId: String,
    val packageName: String,
    val title: String?,
    val artist: String?,
    val album: String?,
    val isPlaying: Boolean,
    val isPaused: Boolean,
    val playbackPositionMs: Long,
    val canPause: Boolean,
    val canPlay: Boolean
)

/** Which concrete action HearMe took during an interruption. */
enum class MediaInterruptionAction {
    PAUSED_VIA_TRANSPORT,
    DUCKED_VIA_AUDIO_FOCUS,
    NONE
}

/**
 * Records the state of a media session at the moment HearMe interrupted it,
 * so that safe restoration can be verified afterwards.
 */
data class InterruptionRecord(
    val sessionId: String,
    val packageName: String,
    val wasPlayingBeforeInterruption: Boolean,
    val positionMsAtInterruption: Long,
    val actionTaken: MediaInterruptionAction,
    val timestampMs: Long,
    /** True only if HearMe actually issued a pause or duck command. */
    val didModifyPlayback: Boolean
)
