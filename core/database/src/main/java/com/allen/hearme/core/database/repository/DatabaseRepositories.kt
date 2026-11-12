package com.allen.hearme.core.database.repository

import com.allen.hearme.core.database.dao.ListeningSessionDao
import com.allen.hearme.core.database.dao.ModelMetadataDao
import com.allen.hearme.core.database.dao.TrainingExampleDao
import com.allen.hearme.core.database.entity.ListeningSessionEntity
import com.allen.hearme.core.database.entity.ModelMetadataEntity
import com.allen.hearme.core.database.entity.TrainingExampleEntity
import kotlinx.coroutines.flow.Flow

interface ListeningSessionRepository {
    fun getAllSessions(): Flow<List<ListeningSessionEntity>>
    suspend fun getSessionById(id: String): ListeningSessionEntity?
    suspend fun recordSession(session: ListeningSessionEntity)
    suspend fun updateSession(session: ListeningSessionEntity)
    suspend fun clearAllSessions()
}

class DefaultListeningSessionRepository(
    private val dao: ListeningSessionDao
) : ListeningSessionRepository {
    override fun getAllSessions(): Flow<List<ListeningSessionEntity>> = dao.getAllSessions()
    override suspend fun getSessionById(id: String): ListeningSessionEntity? = dao.getById(id)
    override suspend fun recordSession(session: ListeningSessionEntity) = dao.insert(session)
    override suspend fun updateSession(session: ListeningSessionEntity) = dao.update(session)
    override suspend fun clearAllSessions() = dao.clearAll()
}

interface TrainingExampleRepository {
    fun getAllExamples(): Flow<List<TrainingExampleEntity>>
    suspend fun getExampleById(id: String): TrainingExampleEntity?
    suspend fun saveExample(example: TrainingExampleEntity)
    suspend fun countExamples(): Int
    suspend fun deleteExample(id: String)
}

class DefaultTrainingExampleRepository(
    private val dao: TrainingExampleDao
) : TrainingExampleRepository {
    override fun getAllExamples(): Flow<List<TrainingExampleEntity>> = dao.getAllExamples()
    override suspend fun getExampleById(id: String): TrainingExampleEntity? = dao.getById(id)
    override suspend fun saveExample(example: TrainingExampleEntity) = dao.insert(example)
    override suspend fun countExamples(): Int = dao.count()
    override suspend fun deleteExample(id: String) = dao.deleteById(id)
}

interface ModelMetadataRepository {
    fun getAllMetadata(): Flow<List<ModelMetadataEntity>>
    suspend fun getMetadata(modelId: String): ModelMetadataEntity?
    suspend fun updateMetadata(metadata: ModelMetadataEntity)
}

class DefaultModelMetadataRepository(
    private val dao: ModelMetadataDao
) : ModelMetadataRepository {
    override fun getAllMetadata(): Flow<List<ModelMetadataEntity>> = dao.getAllMetadata()
    override suspend fun getMetadata(modelId: String): ModelMetadataEntity? = dao.getById(modelId)
    override suspend fun updateMetadata(metadata: ModelMetadataEntity) = dao.upsert(metadata)
}
