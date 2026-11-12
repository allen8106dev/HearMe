package com.allen.hearme.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.allen.hearme.core.database.HearMeDatabase
import com.allen.hearme.core.database.dao.ListeningSessionDao
import com.allen.hearme.core.database.dao.ModelMetadataDao
import com.allen.hearme.core.database.dao.TrainingExampleDao
import com.allen.hearme.core.database.preferences.DataStoreUserPreferencesRepository
import com.allen.hearme.core.database.preferences.UserPreferencesRepository
import com.allen.hearme.core.database.preferences.dataStore
import com.allen.hearme.core.database.repository.DefaultListeningSessionRepository
import com.allen.hearme.core.database.repository.DefaultModelMetadataRepository
import com.allen.hearme.core.database.repository.DefaultTrainingExampleRepository
import com.allen.hearme.core.database.repository.ListeningSessionRepository
import com.allen.hearme.core.database.repository.ModelMetadataRepository
import com.allen.hearme.core.database.repository.TrainingExampleRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HearMeDatabase {
        return HearMeDatabase.buildDatabase(context)
    }

    @Provides
    fun provideListeningSessionDao(database: HearMeDatabase): ListeningSessionDao {
        return database.listeningSessionDao()
    }

    @Provides
    fun provideTrainingExampleDao(database: HearMeDatabase): TrainingExampleDao {
        return database.trainingExampleDao()
    }

    @Provides
    fun provideModelMetadataDao(database: HearMeDatabase): ModelMetadataDao {
        return database.modelMetadataDao()
    }

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(
        dataStore: DataStore<Preferences>
    ): UserPreferencesRepository {
        return DataStoreUserPreferencesRepository(dataStore)
    }

    @Provides
    @Singleton
    fun provideListeningSessionRepository(
        dao: ListeningSessionDao
    ): ListeningSessionRepository {
        return DefaultListeningSessionRepository(dao)
    }

    @Provides
    @Singleton
    fun provideTrainingExampleRepository(
        dao: TrainingExampleDao
    ): TrainingExampleRepository {
        return DefaultTrainingExampleRepository(dao)
    }

    @Provides
    @Singleton
    fun provideModelMetadataRepository(
        dao: ModelMetadataDao
    ): ModelMetadataRepository {
        return DefaultModelMetadataRepository(dao)
    }
}
