package com.allen.hearme.feature.smartlistening

import com.allen.hearme.core.audio.fake.FakeAudioCaptureManager
import com.allen.hearme.core.bluetooth.fake.FakeEarphoneConnectionMonitor
import com.allen.hearme.core.media.fake.FakeMediaPlaybackController
import com.allen.hearme.feature.smartlistening.model.DecisionContext
import com.allen.hearme.feature.smartlistening.model.ListeningState
import com.allen.hearme.feature.smartlistening.model.ListeningTrigger
import com.allen.hearme.feature.smartlistening.model.SmartAction
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartListeningTest {

    @Test
    fun decisionEngineEvaluatesInterruptionAction() {
        val engine = DefaultListeningDecisionEngine()

        // Not connected to earphones -> no action
        val noEarphones = DecisionContext(
            hasSpeech = true,
            isDirected = true,
            confidence = 0.9f,
            isEarphoneConnected = false,
            isMediaPlaying = true
        )
        assertEquals(SmartAction.NO_ACTION, engine.decide(noEarphones))

        // Directed speech with earphones -> pause
        val directedPause = DecisionContext(
            hasSpeech = true,
            isDirected = true,
            confidence = 0.85f,
            isEarphoneConnected = true,
            isMediaPlaying = true,
            preferPause = true
        )
        assertEquals(SmartAction.PAUSE_MUSIC, engine.decide(directedPause))

        // Directed speech with ducking preference -> lower volume
        val directedDuck = directedPause.copy(preferPause = false)
        assertEquals(SmartAction.LOWER_VOLUME, engine.decide(directedDuck))

        // Background speech -> no action
        val backgroundSpeech = directedPause.copy(isDirected = false)
        assertEquals(SmartAction.NO_ACTION, engine.decide(backgroundSpeech))
    }

    @Test
    fun orchestratorStartsInStandbyState() = runTest {
        val audioManager = FakeAudioCaptureManager()
        val earphoneMonitor = FakeEarphoneConnectionMonitor()
        val mediaController = FakeMediaPlaybackController()
        val decisionEngine = DefaultListeningDecisionEngine()

        val orchestrator = DefaultListeningOrchestrator(
            audioManager,
            earphoneMonitor,
            mediaController,
            decisionEngine
        )

        assertEquals(ListeningState.STANDBY, orchestrator.currentState.value)
        assertFalse(orchestrator.isListening)
    }

    @Test
    fun orchestratorTransitionsToActiveOnStart() = runTest {
        val audioManager = FakeAudioCaptureManager()
        val earphoneMonitor = FakeEarphoneConnectionMonitor()
        val mediaController = FakeMediaPlaybackController()
        val decisionEngine = DefaultListeningDecisionEngine()

        val orchestrator = DefaultListeningOrchestrator(
            audioManager,
            earphoneMonitor,
            mediaController,
            decisionEngine
        )

        orchestrator.startListening(ListeningTrigger.USER_MANUAL)
        assertEquals(ListeningState.ACTIVE_LISTENING, orchestrator.currentState.value)
        assertTrue(orchestrator.isListening)
    }

    @Test
    fun orchestratorTransitionsToStandbyOnStop() = runTest {
        val audioManager = FakeAudioCaptureManager()
        val earphoneMonitor = FakeEarphoneConnectionMonitor()
        val mediaController = FakeMediaPlaybackController()
        val decisionEngine = DefaultListeningDecisionEngine()

        val orchestrator = DefaultListeningOrchestrator(
            audioManager,
            earphoneMonitor,
            mediaController,
            decisionEngine
        )

        orchestrator.startListening(ListeningTrigger.USER_MANUAL)
        orchestrator.stopListening()
        assertEquals(ListeningState.STANDBY, orchestrator.currentState.value)
        assertFalse(orchestrator.isListening)
    }

    @Test
    fun orchestratorTransitionsToMicUnavailableOnPermissionError() = runTest {
        val audioManager = FakeAudioCaptureManager()
        audioManager.failWithSecurityException = true
        val earphoneMonitor = FakeEarphoneConnectionMonitor()
        val mediaController = FakeMediaPlaybackController()
        val decisionEngine = DefaultListeningDecisionEngine()

        val orchestrator = DefaultListeningOrchestrator(
            audioManager,
            earphoneMonitor,
            mediaController,
            decisionEngine
        )

        orchestrator.startListening(ListeningTrigger.USER_MANUAL)
        assertEquals(ListeningState.MICROPHONE_UNAVAILABLE, orchestrator.currentState.value)
        assertFalse(orchestrator.isListening)
    }

    @Test
    fun fakeOrchestratorTransitionsCorrectly() = runTest {
        val fakeOrchestrator = FakeListeningOrchestrator()
        assertEquals(ListeningState.STANDBY, fakeOrchestrator.currentState.value)

        fakeOrchestrator.startListening(ListeningTrigger.USER_MANUAL)
        assertEquals(ListeningState.ACTIVE_LISTENING, fakeOrchestrator.currentState.value)
        assertTrue(fakeOrchestrator.isListening)

        fakeOrchestrator.stopListening()
        assertEquals(ListeningState.STANDBY, fakeOrchestrator.currentState.value)
        assertFalse(fakeOrchestrator.isListening)
    }
}
