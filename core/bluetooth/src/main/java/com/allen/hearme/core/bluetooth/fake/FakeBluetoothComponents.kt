package com.allen.hearme.core.bluetooth.fake

import com.allen.hearme.core.bluetooth.AudioDeviceStateProvider
import com.allen.hearme.core.bluetooth.EarphoneConnectionMonitor
import com.allen.hearme.core.bluetooth.model.AudioDeviceInfo
import com.allen.hearme.core.bluetooth.model.AudioDeviceType
import com.allen.hearme.core.bluetooth.model.EarphoneConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeEarphoneConnectionMonitor(
    initialState: EarphoneConnectionState = EarphoneConnectionState.Disconnected
) : EarphoneConnectionMonitor {

    private val _connectionState = MutableStateFlow(initialState)
    override val connectionState: StateFlow<EarphoneConnectionState> = _connectionState.asStateFlow()

    private val _connectedDevice = MutableStateFlow<AudioDeviceInfo?>(
        (initialState as? EarphoneConnectionState.Connected)?.device
    )
    override val connectedDevice: StateFlow<AudioDeviceInfo?> = _connectedDevice.asStateFlow()

    fun setConnected(device: AudioDeviceInfo) {
        _connectionState.value = EarphoneConnectionState.Connected(device)
        _connectedDevice.value = device
    }

    fun setDisconnected() {
        _connectionState.value = EarphoneConnectionState.Disconnected
        _connectedDevice.value = null
    }

    override fun isEarphoneConnected(): Boolean {
        return _connectionState.value is EarphoneConnectionState.Connected
    }
}

class FakeAudioDeviceStateProvider(
    initialDevices: List<AudioDeviceInfo> = listOf(
        AudioDeviceInfo("builtin-speaker", "Internal Speaker", AudioDeviceType.BUILTIN_SPEAKER)
    )
) : AudioDeviceStateProvider {

    private val _availableDevices = MutableStateFlow(initialDevices)
    override val availableDevices: StateFlow<List<AudioDeviceInfo>> = _availableDevices.asStateFlow()

    private val _activeOutputDevice = MutableStateFlow<AudioDeviceInfo?>(initialDevices.firstOrNull())
    override val activeOutputDevice: StateFlow<AudioDeviceInfo?> = _activeOutputDevice.asStateFlow()

    fun updateDevices(devices: List<AudioDeviceInfo>, active: AudioDeviceInfo?) {
        _availableDevices.value = devices
        _activeOutputDevice.value = active
    }

    override fun isHeadsetPluggedIn(): Boolean {
        return _availableDevices.value.any {
            it.type == AudioDeviceType.WIRED_HEADPHONES ||
            it.type == AudioDeviceType.WIRED_HEADSET ||
            it.type == AudioDeviceType.BLUETOOTH_A2DP ||
            it.type == AudioDeviceType.BLUETOOTH_HEADSET
        }
    }
}
