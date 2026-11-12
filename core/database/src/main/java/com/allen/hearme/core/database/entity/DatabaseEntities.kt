package com.allen.hearme.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "training_examples")
data class TrainingExampleEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "classification")
    val classification: String,

    @ColumnInfo(name = "transcription")
    val transcription: String?,

    @ColumnInfo(name = "confidence")
    val confidence: Float,

    @ColumnInfo(name = "language")
    val language: String,

    @ColumnInfo(name = "environment_metadata")
    val environmentMetadata: String,

    @ColumnInfo(name = "model_version")
    val modelVersion: String
)

@Entity(tableName = "listening_sessions")
data class ListeningSessionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "start_time")
    val startTime: Long,

    @ColumnInfo(name = "end_time")
    val endTime: Long?,

    @ColumnInfo(name = "trigger_source")
    val triggerSource: String,

    @ColumnInfo(name = "session_outcome")
    val sessionOutcome: String,

    @ColumnInfo(name = "detection_count")
    val detectionCount: Int = 0
)

@Entity(tableName = "model_metadata")
data class ModelMetadataEntity(
    @PrimaryKey
    @ColumnInfo(name = "model_identifier")
    val modelIdentifier: String,

    @ColumnInfo(name = "version")
    val version: String,

    @ColumnInfo(name = "installation_status")
    val installationStatus: String,

    @ColumnInfo(name = "model_configuration")
    val modelConfiguration: String,

    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long
)
