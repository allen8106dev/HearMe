package com.allen.hearme.core.bluetooth

import com.allen.hearme.core.bluetooth.fake.FakeAudioDeviceStateProvider
import com.allen.hearme.core.bluetooth.fake.FakeEarphoneConnectionMonitor
import com.allen.hearme.core.bluetooth.model.AudioDeviceInfo
import com.allen.hearme.core.bluetooth.model.AudioDeviceType
import com.allen.hearme.core.bluetooth.model.EarphoneConnectionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreBluetoothTest {

    @Test
    fun monitorTracksEarphoneConnectionTransitions() {
        val monitor = FakeEarphoneConnectionMonitor()
        assertFalse(monitor.isEarphoneConnected())
        assertTrue(monitor.connectionState.value is EarphoneConnectionState.Disconnected)

        val device = AudioDeviceInfo("dev-1", "Sony WH-1000XM5", AudioDeviceType.BLUETOOTH_A2DP)
        monitor.setConnected(device)

        assertTrue(monitor.isEarphoneConnected())
        assertEquals(device, monitor.connectedDevice.value)

        monitor.setDisconnected()
        assertFalse(monitor.isEarphoneConnected())
        assertEquals(null, monitor.connectedDevice.value)
    }

    @Test
    fun monitorTracksWiredAndUsbHeadsets() {
        val monitor = FakeEarphoneConnectionMonitor()

        val wiredHeadphones = AudioDeviceInfo("wired-1", "3.5mm Headphones", AudioDeviceType.WIRED_HEADPHONES)
        monitor.setConnected(wiredHeadphones)
        assertTrue(monitor.isEarphoneConnected())
        assertEquals(AudioDeviceType.WIRED_HEADPHONES, monitor.connectedDevice.value?.type)

        val usbHeadset = AudioDeviceInfo("usb-1", "USB-C Audio", AudioDeviceType.USB_HEADSET)
        monitor.setConnected(usbHeadset)
        assertTrue(monitor.isEarphoneConnected())
        assertEquals(AudioDeviceType.USB_HEADSET, monitor.connectedDevice.value?.type)
    }

    @Test
    fun deviceStateProviderDetectsHeadsetPresence() {
        val speaker = AudioDeviceInfo("spk", "Speaker", AudioDeviceType.BUILTIN_SPEAKER)
        val provider = FakeAudioDeviceStateProvider(listOf(speaker))
        assertFalse(provider.isHeadsetPluggedIn())

        val headphones = AudioDeviceInfo("bt-1", "AirPods Pro", AudioDeviceType.BLUETOOTH_HEADSET)
        provider.updateDevices(listOf(speaker, headphones), headphones)
        assertTrue(provider.isHeadsetPluggedIn())
        assertEquals(headphones, provider.activeOutputDevice.value)
    }
}
