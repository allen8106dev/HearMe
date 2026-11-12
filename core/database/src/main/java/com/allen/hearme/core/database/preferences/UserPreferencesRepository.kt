package com.allen.hearme.core.database.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hearme_preferences")

enum class DetectionSensitivity(val value: Float, val label: String) {
    LOW(0.80f, "Low (Fewer Triggers)"),
    MEDIUM(0.65f, "Medium (Balanced)"),
    HIGH(0.50f, "High (Sensitive)")
}

enum class MusicInterruptionBehavior(val label: String) {
    PAUSE_MUSIC("Pause Music"),
    LOWER_VOLUME("Lower Volume (Duck)"),
    DO_NOT_CONTROL("Do Not Control Media")
}

data class UserPreferences(
    val listeningEnabled: Boolean = false,
    val automaticActivation: Boolean = true,
    val detectionSensitivity: DetectionSensitivity = DetectionSensitivity.MEDIUM,
    val preferredBehavior: MusicInterruptionBehavior = MusicInterruptionBehavior.PAUSE_MUSIC,
    val trainingRemindersEnabled: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val autoRestorePlayback: Boolean = true,
    val restorationDelayMs: Long = 1500L
)

interface UserPreferencesRepository {
    val preferencesFlow: Flow<UserPreferences>
    suspend fun setListeningEnabled(enabled: Boolean)
    suspend fun setAutomaticActivation(enabled: Boolean)
    suspend fun setDetectionSensitivity(sensitivity: DetectionSensitivity)
    suspend fun setPreferredBehavior(behavior: MusicInterruptionBehavior)
    suspend fun setTrainingReminders(enabled: Boolean)
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setAutoRestorePlayback(enabled: Boolean)
    suspend fun setRestorationDelayMs(delayMs: Long)
}

class DataStoreUserPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) : UserPreferencesRepository {

    private object Keys {
        val LISTENING_ENABLED = booleanPreferencesKey("pref_listening_enabled")
        val AUTOMATIC_ACTIVATION = booleanPreferencesKey("pref_auto_activation")
        val DETECTION_SENSITIVITY = stringPreferencesKey("pref_sensitivity")
        val PREFERRED_BEHAVIOR = stringPreferencesKey("pref_behavior")
        val TRAINING_REMINDERS = booleanPreferencesKey("pref_training_reminders")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("pref_onboarding_completed")
        val AUTO_RESTORE_PLAYBACK = booleanPreferencesKey("pref_auto_restore_playback")
        val RESTORATION_DELAY_MS = stringPreferencesKey("pref_restoration_delay_ms")
    }

    override val preferencesFlow: Flow<UserPreferences> = dataStore.data.map { prefs ->
        val sensitivityStr = prefs[Keys.DETECTION_SENSITIVITY] ?: DetectionSensitivity.MEDIUM.name
        val sensitivity = runCatching { DetectionSensitivity.valueOf(sensitivityStr) }.getOrDefault(DetectionSensitivity.MEDIUM)

        val behaviorStr = prefs[Keys.PREFERRED_BEHAVIOR] ?: MusicInterruptionBehavior.PAUSE_MUSIC.name
        val behavior = runCatching { MusicInterruptionBehavior.valueOf(behaviorStr) }.getOrDefault(MusicInterruptionBehavior.PAUSE_MUSIC)

        UserPreferences(
            listeningEnabled = prefs[Keys.LISTENING_ENABLED] ?: false,
            automaticActivation = prefs[Keys.AUTOMATIC_ACTIVATION] ?: true,
            detectionSensitivity = sensitivity,
            preferredBehavior = behavior,
            trainingRemindersEnabled = prefs[Keys.TRAINING_REMINDERS] ?: true,
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            autoRestorePlayback = prefs[Keys.AUTO_RESTORE_PLAYBACK] ?: true,
            restorationDelayMs = prefs[Keys.RESTORATION_DELAY_MS]?.toLongOrNull() ?: 1500L
        )
    }

    override suspend fun setListeningEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.LISTENING_ENABLED] = enabled }
    }

    override suspend fun setAutomaticActivation(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTOMATIC_ACTIVATION] = enabled }
    }

    override suspend fun setDetectionSensitivity(sensitivity: DetectionSensitivity) {
        dataStore.edit { it[Keys.DETECTION_SENSITIVITY] = sensitivity.name }
    }

    override suspend fun setPreferredBehavior(behavior: MusicInterruptionBehavior) {
        dataStore.edit { it[Keys.PREFERRED_BEHAVIOR] = behavior.name }
    }

    override suspend fun setTrainingReminders(enabled: Boolean) {
        dataStore.edit { it[Keys.TRAINING_REMINDERS] = enabled }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    override suspend fun setAutoRestorePlayback(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTO_RESTORE_PLAYBACK] = enabled }
    }

    override suspend fun setRestorationDelayMs(delayMs: Long) {
        dataStore.edit { it[Keys.RESTORATION_DELAY_MS] = delayMs.toString() }
    }
}
