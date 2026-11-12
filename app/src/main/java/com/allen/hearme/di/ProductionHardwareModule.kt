package com.allen.hearme.di

import android.content.Context
import com.allen.hearme.core.audio.AudioCaptureManager
import com.allen.hearme.core.audio.AudioSegmentProcessor
import com.allen.hearme.core.audio.EnergyVadDetector
import com.allen.hearme.core.audio.RealAudioCaptureManager
import com.allen.hearme.core.audio.SpeechActivityDetector
import com.allen.hearme.core.audio.model.AudioSegment
import com.allen.hearme.core.bluetooth.AudioDeviceStateProvider
import com.allen.hearme.core.bluetooth.EarphoneConnectionMonitor
import com.allen.hearme.core.bluetooth.RealEarphoneConnectionMonitor
import com.allen.hearme.core.common.dispatcher.DispatcherProvider
import com.allen.hearme.core.common.dispatcher.StandardDispatcherProvider
import com.allen.hearme.core.common.result.HearMeResult
import com.allen.hearme.core.database.preferences.UserPreferencesRepository
import com.allen.hearme.core.media.AudioFocusManager
import com.allen.hearme.core.media.MediaPlaybackController
import com.allen.hearme.core.media.RealAudioFocusManager
import com.allen.hearme.core.ml.ConfidenceEvaluator
import com.allen.hearme.core.ml.FeedbackRepository
import com.allen.hearme.core.ml.IntentClassifier
import com.allen.hearme.core.ml.PersonalisationEngine
import com.allen.hearme.core.ml.SpeechClassifier
import com.allen.hearme.core.ml.SpeechRecognizer
import com.allen.hearme.core.ml.TrainingManager
import com.allen.hearme.core.ml.model.ConfidenceScore
import com.allen.hearme.core.ml.model.IntentClassification
import com.allen.hearme.core.ml.model.PersonalisationProfile
import com.allen.hearme.core.ml.model.SpeechClassification
import com.allen.hearme.core.ml.model.TrainingSample
import com.allen.hearme.core.ml.model.TranscriptionResult
import com.allen.hearme.media.RealMediaPlaybackController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Singleton

// ─── Standalone stubs (Milestone 2: not yet implemented) ─────────────────────

private class StandaloneAudioSegmentProcessor : AudioSegmentProcessor {
    override suspend fun processSegment(segment: AudioSegment): HearMeResult<AudioSegment> {
        return HearMeResult.Success(segment)
    }
}

private class StandaloneSpeechRecognizer : SpeechRecognizer {
    override suspend fun transcribe(segment: AudioSegment): HearMeResult<TranscriptionResult> {
        return HearMeResult.Error(UnsupportedOperationException("Speech recognition model not yet loaded."))
    }

    override fun isAvailable(): Boolean = false
}

private class StandaloneSpeechClassifier : SpeechClassifier {
    override suspend fun classify(segment: AudioSegment): HearMeResult<SpeechClassification> {
        return HearMeResult.Error(UnsupportedOperationException("Speech classification model not yet loaded."))
    }

    override suspend fun isDirectedSpeech(segment: AudioSegment, threshold: Float): Boolean = false
}

private class StandaloneIntentClassifier : IntentClassifier {
    override suspend fun classifyIntent(transcription: String): HearMeResult<IntentClassification> {
        return HearMeResult.Error(UnsupportedOperationException("Intent classifier model not yet loaded."))
    }
}

private class StandaloneConfidenceEvaluator : ConfidenceEvaluator {
    override fun evaluate(
        vadConfidence: Float,
        targetConfidence: Float,
        intentConfidence: Float,
        userThreshold: Float
    ): ConfidenceScore {
        return ConfidenceScore(
            overallConfidence = 0.0f,
            vadConfidence = vadConfidence,
            speechTargetConfidence = targetConfidence,
            intentConfidence = intentConfidence,
            meetsThreshold = false
        )
    }
}

private class StandaloneTrainingManager : TrainingManager {
    override suspend fun recordTrainingSample(sample: TrainingSample): HearMeResult<Unit> {
        return HearMeResult.Success(Unit)
    }

    override suspend fun retrainPersonalModel(): HearMeResult<PersonalisationProfile> {
        return HearMeResult.Error(UnsupportedOperationException("Training pipeline not yet active."))
    }

    override fun getTrainedSampleCount(): Int = 0
}

private class StandaloneFeedbackRepository : FeedbackRepository {
    override suspend fun saveFeedback(sampleId: String, isPositive: Boolean): HearMeResult<Unit> {
        return HearMeResult.Success(Unit)
    }

    override fun observeRecentFeedback(): Flow<List<Pair<String, Boolean>>> = emptyFlow()
}

private class StandalonePersonalisationEngine : PersonalisationEngine {
    override suspend fun getProfile(): PersonalisationProfile {
        return PersonalisationProfile(
            version = 1,
            trainedSampleCount = 0,
            sensitivityOffset = 0.0f,
            customKeywords = emptyList()
        )
    }

    override suspend fun updateThreshold(newOffset: Float) {}
}

// ─── Production DI Module ─────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
object ProductionHardwareModule {

    // ── Dispatcher ──────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider = StandardDispatcherProvider()

    // ── REAL: Audio Capture ─────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideAudioCaptureManager(
        @ApplicationContext context: Context,
        dispatchers: DispatcherProvider
    ): AudioCaptureManager = RealAudioCaptureManager(context, dispatchers)

    // ── REAL: VAD ───────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideSpeechActivityDetector(): SpeechActivityDetector = EnergyVadDetector()

    // ── REAL: Earphone & Device State (single instance for both interfaces) ─

    @Provides
    @Singleton
    fun provideRealEarphoneConnectionMonitor(
        @ApplicationContext context: Context
    ): RealEarphoneConnectionMonitor = RealEarphoneConnectionMonitor(context)

    @Provides
    @Singleton
    fun provideEarphoneConnectionMonitor(
        monitor: RealEarphoneConnectionMonitor
    ): EarphoneConnectionMonitor = monitor

    @Provides
    @Singleton
    fun provideAudioDeviceStateProvider(
        monitor: RealEarphoneConnectionMonitor
    ): AudioDeviceStateProvider = monitor

    // ── Service Controller ──────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideServiceController(
        @ApplicationContext context: Context
    ): com.allen.hearme.service.ServiceController = com.allen.hearme.service.RealServiceController(context)

    // ── Stubs (Milestone 3+) ─────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideAudioSegmentProcessor(): AudioSegmentProcessor = StandaloneAudioSegmentProcessor()

    // ── REAL: Audio Focus ─────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideAudioFocusManager(
        @ApplicationContext context: Context
    ): AudioFocusManager = RealAudioFocusManager(context)

    // ── REAL: Media Playback Controller ──────────────────────────────────────

    @Provides
    @Singleton
    fun provideMediaPlaybackController(
        @ApplicationContext context: Context,
        audioFocusManager: AudioFocusManager,
        preferencesRepository: UserPreferencesRepository,
        dispatchers: DispatcherProvider
    ): MediaPlaybackController = RealMediaPlaybackController(
        context = context,
        audioFocusManager = audioFocusManager,
        preferencesRepository = preferencesRepository,
        dispatchers = dispatchers
    )

    @Provides
    @Singleton
    fun provideSpeechRecognizer(): SpeechRecognizer = StandaloneSpeechRecognizer()

    @Provides
    @Singleton
    fun provideSpeechClassifier(): SpeechClassifier = StandaloneSpeechClassifier()

    @Provides
    @Singleton
    fun provideIntentClassifier(): IntentClassifier = StandaloneIntentClassifier()

    @Provides
    @Singleton
    fun provideConfidenceEvaluator(): ConfidenceEvaluator = StandaloneConfidenceEvaluator()

    @Provides
    @Singleton
    fun provideTrainingManager(): TrainingManager = StandaloneTrainingManager()

    @Provides
    @Singleton
    fun provideFeedbackRepository(): FeedbackRepository = StandaloneFeedbackRepository()

    @Provides
    @Singleton
    fun providePersonalisationEngine(): PersonalisationEngine = StandalonePersonalisationEngine()
}
