package com.allen.hearme.core.database.fake

import com.allen.hearme.core.database.entity.ListeningSessionEntity
import com.allen.hearme.core.database.entity.ModelMetadataEntity
import com.allen.hearme.core.database.entity.TrainingExampleEntity
import com.allen.hearme.core.database.preferences.DetectionSensitivity
import com.allen.hearme.core.database.preferences.MusicInterruptionBehavior
import com.allen.hearme.core.database.preferences.UserPreferences
import com.allen.hearme.core.database.preferences.UserPreferencesRepository
import com.allen.hearme.core.database.repository.ListeningSessionRepository
import com.allen.hearme.core.database.repository.ModelMetadataRepository
import com.allen.hearme.core.database.repository.TrainingExampleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeListeningSessionRepository : ListeningSessionRepository {
    private val sessions = MutableStateFlow<List<ListeningSessionEntity>>(emptyList())

    override fun getAllSessions(): Flow<List<ListeningSessionEntity>> = sessions.asStateFlow()

    override suspend fun getSessionById(id: String): ListeningSessionEntity? =
        sessions.value.find { it.id == id }

    override suspend fun recordSession(session: ListeningSessionEntity) {
        sessions.value = listOf(session) + sessions.value
    }

    override suspend fun updateSession(session: ListeningSessionEntity) {
        sessions.value = sessions.value.map { if (it.id == session.id) session else it }
    }

    override suspend fun clearAllSessions() {
        sessions.value = emptyList()
    }
}

class FakeTrainingExampleRepository : TrainingExampleRepository {
    private val examples = MutableStateFlow<List<TrainingExampleEntity>>(emptyList())

    override fun getAllExamples(): Flow<List<TrainingExampleEntity>> = examples.asStateFlow()

    override suspend fun getExampleById(id: String): TrainingExampleEntity? =
        examples.value.find { it.id == id }

    override suspend fun saveExample(example: TrainingExampleEntity) {
        examples.value = listOf(example) + examples.value
    }

    override suspend fun countExamples(): Int = examples.value.size

    override suspend fun deleteExample(id: String) {
        examples.value = examples.value.filterNot { it.id == id }
    }
}

class FakeModelMetadataRepository : ModelMetadataRepository {
    private val metadata = MutableStateFlow<List<ModelMetadataEntity>>(emptyList())

    override fun getAllMetadata(): Flow<List<ModelMetadataEntity>> = metadata.asStateFlow()

    override suspend fun getMetadata(modelId: String): ModelMetadataEntity? =
        metadata.value.find { it.modelIdentifier == modelId }

    override suspend fun updateMetadata(metadata: ModelMetadataEntity) {
        this.metadata.value = this.metadata.value.filterNot { it.modelIdentifier == metadata.modelIdentifier } + metadata
    }
}

class FakeUserPreferencesRepository(
    initial: UserPreferences = UserPreferences()
) : UserPreferencesRepository {
    private val _prefs = MutableStateFlow(initial)
    override val preferencesFlow: Flow<UserPreferences> = _prefs.asStateFlow()

    override suspend fun setListeningEnabled(enabled: Boolean) {
        _prefs.value = _prefs.value.copy(listeningEnabled = enabled)
    }

    override suspend fun setAutomaticActivation(enabled: Boolean) {
        _prefs.value = _prefs.value.copy(automaticActivation = enabled)
    }

    override suspend fun setDetectionSensitivity(sensitivity: DetectionSensitivity) {
        _prefs.value = _prefs.value.copy(detectionSensitivity = sensitivity)
    }

    override suspend fun setPreferredBehavior(behavior: MusicInterruptionBehavior) {
        _prefs.value = _prefs.value.copy(preferredBehavior = behavior)
    }

    override suspend fun setTrainingReminders(enabled: Boolean) {
        _prefs.value = _prefs.value.copy(trainingRemindersEnabled = enabled)
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        _prefs.value = _prefs.value.copy(onboardingCompleted = completed)
    }
}
