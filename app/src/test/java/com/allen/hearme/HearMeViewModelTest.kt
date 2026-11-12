package com.allen.hearme

import com.allen.hearme.core.bluetooth.fake.FakeEarphoneConnectionMonitor
import com.allen.hearme.core.bluetooth.model.AudioDeviceInfo
import com.allen.hearme.core.bluetooth.model.AudioDeviceType
import com.allen.hearme.core.database.fake.FakeListeningSessionRepository
import com.allen.hearme.core.database.fake.FakeUserPreferencesRepository
import com.allen.hearme.core.database.preferences.DetectionSensitivity
import com.allen.hearme.core.database.preferences.MusicInterruptionBehavior
import com.allen.hearme.core.media.fake.FakeMediaPlaybackController
import com.allen.hearme.core.media.model.MediaTrackInfo
import com.allen.hearme.core.media.model.PlaybackAction
import com.allen.hearme.feature.smartlistening.FakeListeningOrchestrator
import com.allen.hearme.service.FakeServiceController
import com.allen.hearme.ui.HearMeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HearMeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var serviceController: FakeServiceController
    private lateinit var orchestrator: FakeListeningOrchestrator
    private lateinit var earphoneMonitor: FakeEarphoneConnectionMonitor
    private lateinit var preferencesRepo: FakeUserPreferencesRepository
    private lateinit var sessionRepo: FakeListeningSessionRepository
    private lateinit var mediaController: FakeMediaPlaybackController
    private lateinit var viewModel: HearMeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        serviceController = FakeServiceController()
        orchestrator = FakeListeningOrchestrator()
        earphoneMonitor = FakeEarphoneConnectionMonitor()
        preferencesRepo = FakeUserPreferencesRepository()
        sessionRepo = FakeListeningSessionRepository()
        mediaController = FakeMediaPlaybackController()

        viewModel = HearMeViewModel(
            serviceController = serviceController,
            orchestrator = orchestrator,
            earphoneMonitor = earphoneMonitor,
            preferencesRepository = preferencesRepo,
            sessionRepository = sessionRepo,
            mediaPlaybackController = mediaController
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun togglingSmartListeningUpdatesState() = runTest {
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isSmartListeningOn)

        viewModel.toggleSmartListening(true)
        advanceUntilIdle()
        assertTrue(serviceController.isRunning)
        assertEquals(1, serviceController.startCallCount)

        viewModel.toggleSmartListening(false)
        advanceUntilIdle()
        assertFalse(serviceController.isRunning)
        assertEquals(1, serviceController.stopCallCount)
    }

    @Test
    fun earphoneConnectionUpdatesUiState() = runTest {
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isEarphoneConnected)

        val device = AudioDeviceInfo("sony-1", "Sony WH-1000XM5", AudioDeviceType.BLUETOOTH_A2DP)
        earphoneMonitor.setConnected(device)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEarphoneConnected)
        assertEquals("Sony WH-1000XM5", viewModel.uiState.value.earphoneName)
    }

    @Test
    fun preferencesUpdateChangesUiState() = runTest {
        advanceUntilIdle()
        viewModel.setSensitivity(DetectionSensitivity.HIGH)
        viewModel.setInterruptionBehavior(MusicInterruptionBehavior.LOWER_VOLUME)
        advanceUntilIdle()

        assertEquals(DetectionSensitivity.HIGH, viewModel.uiState.value.preferences.detectionSensitivity)
        assertEquals(MusicInterruptionBehavior.LOWER_VOLUME, viewModel.uiState.value.preferences.preferredBehavior)
    }

    @Test
    fun mediaStateObservationUpdatesUiState() = runTest {
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.hasNotificationListenerAccess)
        assertEquals(null, viewModel.uiState.value.activeMediaTrack)

        mediaController.setNlsAccess(true)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.hasNotificationListenerAccess)

        val track = MediaTrackInfo(
            sessionId = "session-1",
            packageName = "com.spotify.music",
            title = "Starboy",
            artist = "The Weeknd",
            album = "Starboy",
            isPlaying = true,
            isPaused = false,
            playbackPositionMs = 15000L,
            canPause = true,
            canPlay = true
        )
        mediaController.setActiveTrack(track)
        advanceUntilIdle()

        assertEquals("Starboy", viewModel.uiState.value.activeMediaTrack?.title)
        assertEquals("The Weeknd", viewModel.uiState.value.activeMediaTrack?.artist)
    }

    @Test
    fun simulateMediaInterruptionPausesThenRestores() = runTest {
        advanceUntilIdle()
        viewModel.simulateMediaInterruption()
        advanceUntilIdle()

        assertTrue(mediaController.actionHistory.contains(PlaybackAction.PAUSE))
    }
}
