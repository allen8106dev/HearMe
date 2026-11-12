package com.allen.hearme.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.allen.hearme.core.database.entity.ListeningSessionEntity
import com.allen.hearme.core.database.entity.ModelMetadataEntity
import com.allen.hearme.core.database.entity.TrainingExampleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingExampleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(example: TrainingExampleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(examples: List<TrainingExampleEntity>)

    @Query("SELECT * FROM training_examples ORDER BY timestamp DESC")
    fun getAllExamples(): Flow<List<TrainingExampleEntity>>

    @Query("SELECT * FROM training_examples WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TrainingExampleEntity?

    @Query("SELECT COUNT(*) FROM training_examples")
    suspend fun count(): Int

    @Query("DELETE FROM training_examples WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface ListeningSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: ListeningSessionEntity)

    @Update
    suspend fun update(session: ListeningSessionEntity)

    @Query("SELECT * FROM listening_sessions ORDER BY start_time DESC")
    fun getAllSessions(): Flow<List<ListeningSessionEntity>>

    @Query("SELECT * FROM listening_sessions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ListeningSessionEntity?

    @Query("DELETE FROM listening_sessions")
    suspend fun clearAll()
}

@Dao
interface ModelMetadataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(metadata: ModelMetadataEntity)

    @Query("SELECT * FROM model_metadata WHERE model_identifier = :modelId LIMIT 1")
    suspend fun getById(modelId: String): ModelMetadataEntity?

    @Query("SELECT * FROM model_metadata")
    fun getAllMetadata(): Flow<List<ModelMetadataEntity>>
}
