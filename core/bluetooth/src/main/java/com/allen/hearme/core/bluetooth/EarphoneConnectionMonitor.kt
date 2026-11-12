package com.allen.hearme.core.bluetooth

import com.allen.hearme.core.bluetooth.model.AudioDeviceInfo
import com.allen.hearme.core.bluetooth.model.EarphoneConnectionState
import kotlinx.coroutines.flow.StateFlow

interface EarphoneConnectionMonitor {
    val connectionState: StateFlow<EarphoneConnectionState>
    val connectedDevice: StateFlow<AudioDeviceInfo?>
    fun isEarphoneConnected(): Boolean
}
