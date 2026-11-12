package com.allen.hearme.core.bluetooth

import com.allen.hearme.core.bluetooth.model.AudioDeviceInfo
import kotlinx.coroutines.flow.StateFlow

interface AudioDeviceStateProvider {
    val availableDevices: StateFlow<List<AudioDeviceInfo>>
    val activeOutputDevice: StateFlow<AudioDeviceInfo?>
    fun isHeadsetPluggedIn(): Boolean
}
