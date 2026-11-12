package com.allen.hearme.core.bluetooth.model

enum class AudioDeviceType {
    BLUETOOTH_A2DP,
    BLUETOOTH_HEADSET,
    WIRED_HEADPHONES,
    WIRED_HEADSET,
    BUILTIN_SPEAKER,
    UNKNOWN
}

data class AudioDeviceInfo(
    val id: String,
    val name: String,
    val type: AudioDeviceType,
    val isConnected: Boolean = true
)

sealed interface EarphoneConnectionState {
    data class Connected(val device: AudioDeviceInfo) : EarphoneConnectionState
    data object Disconnected : EarphoneConnectionState
    data object Connecting : EarphoneConnectionState
}
