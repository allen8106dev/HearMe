package com.allen.hearme

import com.allen.hearme.core.bluetooth.fake.FakeEarphoneConnectionMonitor
import com.allen.hearme.core.bluetooth.model.AudioDeviceInfo
import com.allen.hearme.core.bluetooth.model.AudioDeviceType
import com.allen.hearme.core.database.fake.FakeListeningSessionRepository
import com.allen.hearme.core.database.fake.FakeUserPreferencesRepository
import com.allen.hearme.core.database.preferences.DetectionSensitivity
import com.allen.hearme.core.database.preferences.MusicInterruptionBehavior
import com.allen.hearme.feature.smartlistening.FakeListeningOrchestrator
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
    private lateinit var orchestrator: FakeListeningOrchestrator
    private lateinit var earphoneMonitor: FakeEarphoneConnectionMonitor
    private lateinit var preferencesRepo: FakeUserPreferencesRepository
    private lateinit var sessionRepo: FakeListeningSessionRepository
    private lateinit var viewModel: HearMeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        orchestrator = FakeListeningOrchestrator()
        earphoneMonitor = FakeEarphoneConnectionMonitor()
        preferencesRepo = FakeUserPreferencesRepository()
        sessionRepo = FakeListeningSessionRepository()

        viewModel = HearMeViewModel(
            orchestrator = orchestrator,
            earphoneMonitor = earphoneMonitor,
            preferencesRepository = preferencesRepo,
            sessionRepository = sessionRepo
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
        assertTrue(viewModel.uiState.value.isSmartListeningOn)

        viewModel.toggleSmartListening(false)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isSmartListeningOn)
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
}
