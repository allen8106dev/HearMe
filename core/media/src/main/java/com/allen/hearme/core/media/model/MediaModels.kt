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
