package com.allen.hearme.core.media

import com.allen.hearme.core.media.fake.FakeAudioFocusManager
import com.allen.hearme.core.media.fake.FakeMediaPlaybackController
import com.allen.hearme.core.media.model.AudioFocusState
import com.allen.hearme.core.media.model.PlaybackAction
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreMediaTest {

    @Test
    fun playbackControllerRecordsActionsAndVolume() = runTest {
        val controller = FakeMediaPlaybackController(isPlaying = true)
        assertTrue(controller.isMediaPlaying())

        controller.lowerVolume(0.3f)
        assertEquals(0.3f, controller.currentVolumeRatio, 0.001f)
        assertEquals(PlaybackAction.LOWER_VOLUME, controller.actionHistory.last())

        controller.pauseMedia()
        assertFalse(controller.isMediaPlaying())
        assertEquals(PlaybackAction.PAUSE, controller.actionHistory.last())

        controller.resumeMedia()
        assertTrue(controller.isMediaPlaying())
        assertEquals(PlaybackAction.RESUME, controller.actionHistory.last())

        controller.restoreVolume()
        assertEquals(1.0f, controller.currentVolumeRatio, 0.001f)
    }

    @Test
    fun audioFocusManagerRequestsAndAbandonsFocus() = runTest {
        val focusManager = FakeAudioFocusManager()
        assertFalse(focusManager.hasFocus())

        val requestResult = focusManager.requestAudioFocus()
        assertTrue(requestResult.isSuccess)
        assertTrue(focusManager.hasFocus())
        assertEquals(AudioFocusState.FOCUSED, focusManager.currentFocusState.value)

        val abandonResult = focusManager.abandonAudioFocus()
        assertTrue(abandonResult.isSuccess)
        assertFalse(focusManager.hasFocus())
        assertEquals(AudioFocusState.NO_FOCUS, focusManager.currentFocusState.value)
    }
}
