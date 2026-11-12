package com.allen.hearme.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.allen.hearme.core.database.dao.ListeningSessionDao
import com.allen.hearme.core.database.dao.ModelMetadataDao
import com.allen.hearme.core.database.dao.TrainingExampleDao
import com.allen.hearme.core.database.entity.ListeningSessionEntity
import com.allen.hearme.core.database.entity.ModelMetadataEntity
import com.allen.hearme.core.database.entity.TrainingExampleEntity

@Database(
    entities = [
        TrainingExampleEntity::class,
        ListeningSessionEntity::class,
        ModelMetadataEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class HearMeDatabase : RoomDatabase() {
    abstract fun trainingExampleDao(): TrainingExampleDao
    abstract fun listeningSessionDao(): ListeningSessionDao
    abstract fun modelMetadataDao(): ModelMetadataDao

    companion object {
        const val DATABASE_NAME = "hearme_database.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Prepared migration schema upgrade hook for future milestones
            }
        }

        fun buildDatabase(context: Context): HearMeDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                HearMeDatabase::class.java,
                DATABASE_NAME
            ).fallbackToDestructiveMigration()
             .addMigrations(MIGRATION_1_2)
             .build()
        }
    }
}
