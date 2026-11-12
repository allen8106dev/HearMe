package com.allen.hearme.core.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo as AndroidAudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.allen.hearme.core.bluetooth.model.AudioDeviceInfo
import com.allen.hearme.core.bluetooth.model.AudioDeviceType
import com.allen.hearme.core.bluetooth.model.EarphoneConnectionState
import com.allen.hearme.core.common.logging.HearMeLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Production implementation of [EarphoneConnectionMonitor] and [AudioDeviceStateProvider].
 *
 * Utilizes [AudioManager] and [AudioDeviceCallback] to track wired headsets, USB headsets,
 * and Bluetooth A2DP/SCO audio devices in real time without polling.
 */
class RealEarphoneConnectionMonitor(
    private val context: Context,
    private val logger: HearMeLogger = object : HearMeLogger {
        override fun d(tag: String, message: String) {}
        override fun i(tag: String, message: String) {}
        override fun w(tag: String, message: String, throwable: Throwable?) {}
        override fun e(tag: String, message: String, throwable: Throwable?) {}
    }
) : EarphoneConnectionMonitor, AudioDeviceStateProvider {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val _connectionState = MutableStateFlow<EarphoneConnectionState>(EarphoneConnectionState.Disconnected)
    override val connectionState: StateFlow<EarphoneConnectionState> = _connectionState.asStateFlow()

    private val _connectedDevice = MutableStateFlow<AudioDeviceInfo?>(null)
    override val connectedDevice: StateFlow<AudioDeviceInfo?> = _connectedDevice.asStateFlow()

    private val _availableDevices = MutableStateFlow<List<AudioDeviceInfo>>(emptyList())
    override val availableDevices: StateFlow<List<AudioDeviceInfo>> = _availableDevices.asStateFlow()

    private val _activeOutputDevice = MutableStateFlow<AudioDeviceInfo?>(null)
    override val activeOutputDevice: StateFlow<AudioDeviceInfo?> = _activeOutputDevice.asStateFlow()

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AndroidAudioDeviceInfo>?) {
            logger.d(TAG, "Audio devices added callback received")
            evaluateConnectedDevices()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AndroidAudioDeviceInfo>?) {
            logger.d(TAG, "Audio devices removed callback received")
            evaluateConnectedDevices()
        }
    }

    init {
        registerAudioDeviceCallback()
        evaluateConnectedDevices()
    }

    private fun registerAudioDeviceCallback() {
        try {
            audioManager?.registerAudioDeviceCallback(audioDeviceCallback, null)
            logger.i(TAG, "AudioDeviceCallback successfully registered")
        } catch (e: Exception) {
            logger.w(TAG, "Failed to register AudioDeviceCallback", e)
        }
    }

    @SuppressLint("InlinedApi")
    fun evaluateConnectedDevices() {
        if (audioManager == null) {
            _connectionState.value = EarphoneConnectionState.Disconnected
            _connectedDevice.value = null
            return
        }

        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val mappedDevices = mutableListOf<AudioDeviceInfo>()
        var primaryEarphone: AudioDeviceInfo? = null

        val hasBtConnectPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        for (device in devices) {
            val deviceType = mapDeviceType(device.type)
            val isEarphone = deviceType in listOf(
                AudioDeviceType.BLUETOOTH_A2DP,
                AudioDeviceType.BLUETOOTH_HEADSET,
                AudioDeviceType.WIRED_HEADSET,
                AudioDeviceType.WIRED_HEADPHONES,
                AudioDeviceType.USB_HEADSET
            )

            val rawName = if (deviceType in listOf(AudioDeviceType.BLUETOOTH_A2DP, AudioDeviceType.BLUETOOTH_HEADSET) && !hasBtConnectPermission) {
                "Bluetooth Audio Device"
            } else {
                val name = device.productName?.toString()
                if (name.isNullOrBlank()) defaultDeviceName(deviceType) else name
            }

            val info = AudioDeviceInfo(
                id = device.id.toString(),
                name = rawName,
                type = deviceType,
                isConnected = true
            )
            mappedDevices.add(info)

            // Prioritize connected private listening devices: Bluetooth > Wired > USB
            if (isEarphone && primaryEarphone == null) {
                primaryEarphone = info
            } else if (isEarphone && info.type == AudioDeviceType.BLUETOOTH_A2DP) {
                primaryEarphone = info // Prefer high quality A2DP if present
            }
        }

        _availableDevices.value = mappedDevices

        if (primaryEarphone != null) {
            _connectedDevice.value = primaryEarphone
            _connectionState.value = EarphoneConnectionState.Connected(primaryEarphone)
            _activeOutputDevice.value = primaryEarphone
            logger.i(TAG, "Earphones confirmed connected: ${primaryEarphone.name} (${primaryEarphone.type})")
        } else {
            _connectedDevice.value = null
            _connectionState.value = EarphoneConnectionState.Disconnected
            _activeOutputDevice.value = mappedDevices.firstOrNull { it.type == AudioDeviceType.BUILTIN_SPEAKER }
            logger.i(TAG, "No earphones connected. Audio routed to internal speaker / none.")
        }
    }

    override fun isEarphoneConnected(): Boolean {
        return _connectionState.value is EarphoneConnectionState.Connected
    }

    override fun isHeadsetPluggedIn(): Boolean {
        val current = _connectedDevice.value?.type ?: return false
        return current in listOf(
            AudioDeviceType.WIRED_HEADPHONES,
            AudioDeviceType.WIRED_HEADSET,
            AudioDeviceType.USB_HEADSET
        )
    }

    private fun mapDeviceType(platformType: Int): AudioDeviceType {
        return when (platformType) {
            AndroidAudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> AudioDeviceType.BLUETOOTH_A2DP
            AndroidAudioDeviceInfo.TYPE_BLUETOOTH_SCO -> AudioDeviceType.BLUETOOTH_HEADSET
            AndroidAudioDeviceInfo.TYPE_WIRED_HEADSET -> AudioDeviceType.WIRED_HEADSET
            AndroidAudioDeviceInfo.TYPE_WIRED_HEADPHONES -> AudioDeviceType.WIRED_HEADPHONES
            AndroidAudioDeviceInfo.TYPE_USB_HEADSET,
            AndroidAudioDeviceInfo.TYPE_USB_DEVICE -> AudioDeviceType.USB_HEADSET
            AndroidAudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> AudioDeviceType.BUILTIN_SPEAKER
            else -> AudioDeviceType.UNKNOWN
        }
    }

    private fun defaultDeviceName(type: AudioDeviceType): String {
        return when (type) {
            AudioDeviceType.BLUETOOTH_A2DP -> "Bluetooth Headphones"
            AudioDeviceType.BLUETOOTH_HEADSET -> "Bluetooth Headset"
            AudioDeviceType.WIRED_HEADSET -> "Wired Headset"
            AudioDeviceType.WIRED_HEADPHONES -> "Wired Headphones"
            AudioDeviceType.USB_HEADSET -> "USB Audio Headset"
            AudioDeviceType.BUILTIN_SPEAKER -> "Internal Speaker"
            AudioDeviceType.UNKNOWN -> "Audio Output Device"
        }
    }

    companion object {
        private const val TAG = "RealEarphoneMonitor"
    }
}
