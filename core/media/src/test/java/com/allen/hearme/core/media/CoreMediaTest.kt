package com.allen.hearme.core.media

import com.allen.hearme.core.media.fake.FakeAudioFocusManager
import com.allen.hearme.core.media.fake.FakeMediaPlaybackController
import com.allen.hearme.core.media.model.AudioFocusState
import com.allen.hearme.core.media.model.InterruptionRecord
import com.allen.hearme.core.media.model.MediaInterruptionAction
import com.allen.hearme.core.media.model.MediaTrackInfo
import com.allen.hearme.core.media.model.PlaybackAction
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    @Test
    fun tracksMediaMetadataAndNotificationAccess() = runTest {
        val controller = FakeMediaPlaybackController(isPlaying = false)

        assertNull(controller.activeMediaTrack.value)
        assertFalse(controller.hasNotificationListenerAccess.value)

        controller.setNlsAccess(true)
        assertTrue(controller.hasNotificationListenerAccess.value)

        val track = MediaTrackInfo(
            sessionId = "session-1",
            packageName = "com.spotify.music",
            title = "Midnight City",
            artist = "M83",
            album = "Hurry Up, We're Dreaming",
            isPlaying = true,
            isPaused = false,
            playbackPositionMs = 45000L,
            canPause = true,
            canPlay = true
        )
        controller.setActiveTrack(track)

        assertEquals("Midnight City", controller.activeMediaTrack.value?.title)
        assertEquals("M83", controller.activeMediaTrack.value?.artist)
        assertEquals("com.spotify.music", controller.activeMediaTrack.value?.packageName)
        assertTrue(controller.activeMediaTrack.value?.canPause == true)
    }

    @Test
    fun restorePlaybackResumesOnlyIfPreviouslyPlaying() = runTest {
        val controller = FakeMediaPlaybackController(isPlaying = false)

        // Case 1: Interrupted while music was playing
        val recordPlaying = InterruptionRecord(
            sessionId = "session-1",
            packageName = "com.spotify.music",
            wasPlayingBeforeInterruption = true,
            positionMsAtInterruption = 12000L,
            actionTaken = MediaInterruptionAction.PAUSED_VIA_TRANSPORT,
            timestampMs = System.currentTimeMillis(),
            didModifyPlayback = true
        )
        controller.setActiveInterruption(recordPlaying)
        assertNotNull(controller.getActiveInterruption())

        val restoreResult = controller.restorePlayback()
        assertTrue(restoreResult.isSuccess)
        assertTrue(controller.isMediaPlaying())
        assertEquals(PlaybackAction.RESUME, controller.actionHistory.last())
        assertNull(controller.getActiveInterruption())

        // Case 2: Music was ALREADY paused before HearMe interrupted -> should NOT resume!
        val controller2 = FakeMediaPlaybackController(isPlaying = false)
        val recordPaused = InterruptionRecord(
            sessionId = "session-2",
            packageName = "com.apple.android.music",
            wasPlayingBeforeInterruption = false,
            positionMsAtInterruption = 0L,
            actionTaken = MediaInterruptionAction.NONE,
            timestampMs = System.currentTimeMillis(),
            didModifyPlayback = false
        )
        controller2.setActiveInterruption(recordPaused)
        controller2.restorePlayback()

        // Verify no RESUME action was issued
        assertFalse(controller2.actionHistory.contains(PlaybackAction.RESUME))
        assertFalse(controller2.isMediaPlaying())
    }

    @Test
    fun cancelInterruptionClearsActiveRecord() = runTest {
        val controller = FakeMediaPlaybackController(isPlaying = false)
        val record = InterruptionRecord(
            sessionId = "session-test",
            packageName = "com.google.android.apps.youtube.music",
            wasPlayingBeforeInterruption = true,
            positionMsAtInterruption = 5000L,
            actionTaken = MediaInterruptionAction.DUCKED_VIA_AUDIO_FOCUS,
            timestampMs = System.currentTimeMillis(),
            didModifyPlayback = true
        )
        controller.setActiveInterruption(record)
        assertNotNull(controller.getActiveInterruption())

        controller.cancelInterruption()
        assertNull(controller.getActiveInterruption())
    }
}
