package com.allen.hearme.feature.smartlistening.model

enum class ListeningState {
    IDLE,
    ACTIVE_LISTENING,
    SPEECH_DETECTED,
    INTERRUPTING_MUSIC,
    RECOVERING,
    ERROR
}

enum class ListeningTrigger {
    USER_MANUAL,
    EARPHONE_AUTO_CONNECT
}

enum class SmartAction {
    NO_ACTION,
    LOWER_VOLUME,
    PAUSE_MUSIC,
    RESUME_MUSIC
}

data class DecisionContext(
    val hasSpeech: Boolean,
    val isDirected: Boolean,
    val confidence: Float,
    val isEarphoneConnected: Boolean,
    val isMediaPlaying: Boolean,
    val preferPause: Boolean = true
)
