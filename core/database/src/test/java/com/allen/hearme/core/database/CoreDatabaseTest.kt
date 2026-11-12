package com.allen.hearme.core.database

import com.allen.hearme.core.database.entity.ListeningSessionEntity
import com.allen.hearme.core.database.entity.ModelMetadataEntity
import com.allen.hearme.core.database.entity.TrainingExampleEntity
import com.allen.hearme.core.database.fake.FakeListeningSessionRepository
import com.allen.hearme.core.database.fake.FakeModelMetadataRepository
import com.allen.hearme.core.database.fake.FakeTrainingExampleRepository
import com.allen.hearme.core.database.fake.FakeUserPreferencesRepository
import com.allen.hearme.core.database.preferences.DetectionSensitivity
import com.allen.hearme.core.database.preferences.MusicInterruptionBehavior
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreDatabaseTest {

    @Test
    fun listeningSessionRepositoryPerformsCrud() = runTest {
        val repo = FakeListeningSessionRepository()
        val session = ListeningSessionEntity(
            id = "sess-1",
            startTime = 1000L,
            endTime = 2000L,
            triggerSource = "BLUETOOTH_CONNECT",
            sessionOutcome = "COMPLETED_NORMAL",
            detectionCount = 3
        )

        repo.recordSession(session)
        val sessions = repo.getAllSessions().first()
        assertEquals(1, sessions.size)
        assertEquals("sess-1", sessions[0].id)

        val updated = session.copy(detectionCount = 4)
        repo.updateSession(updated)
        assertEquals(4, repo.getSessionById("sess-1")?.detectionCount)

        repo.clearAllSessions()
        assertTrue(repo.getAllSessions().first().isEmpty())
    }

    @Test
    fun trainingExampleRepositoryManagesExamples() = runTest {
        val repo = FakeTrainingExampleRepository()
        val example = TrainingExampleEntity(
            id = "ex-1",
            timestamp = 1000L,
            classification = "DIRECTED_AT_USER",
            transcription = "Excuse me",
            confidence = 0.95f,
            language = "en",
            environmentMetadata = "{\"ambientRms\": 0.4}",
            modelVersion = "1.0.0"
        )

        repo.saveExample(example)
        assertEquals(1, repo.countExamples())
        assertNotNull(repo.getExampleById("ex-1"))

        repo.deleteExample("ex-1")
        assertEquals(0, repo.countExamples())
        assertNull(repo.getExampleById("ex-1"))
    }

    @Test
    fun modelMetadataRepositoryStoresAndRetrieves() = runTest {
        val repo = FakeModelMetadataRepository()
        val meta = ModelMetadataEntity(
            modelIdentifier = "vad_silero_v4",
            version = "4.0.0",
            installationStatus = "READY",
            modelConfiguration = "{\"sampleRate\": 16000}",
            lastUpdated = 5000L
        )

        repo.updateMetadata(meta)
        val retrieved = repo.getMetadata("vad_silero_v4")
        assertNotNull(retrieved)
        assertEquals("READY", retrieved?.installationStatus)
    }

    @Test
    fun userPreferencesRepositoryUpdatesAndEmits() = runTest {
        val repo = FakeUserPreferencesRepository()
        repo.setListeningEnabled(true)
        repo.setDetectionSensitivity(DetectionSensitivity.HIGH)
        repo.setPreferredBehavior(MusicInterruptionBehavior.LOWER_VOLUME)
        repo.setOnboardingCompleted(true)

        val prefs = repo.preferencesFlow.first()
        assertTrue(prefs.listeningEnabled)
        assertEquals(DetectionSensitivity.HIGH, prefs.detectionSensitivity)
        assertEquals(MusicInterruptionBehavior.LOWER_VOLUME, prefs.preferredBehavior)
        assertTrue(prefs.onboardingCompleted)
    }
}
