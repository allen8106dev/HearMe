package com.allen.hearme.core.ml

import com.allen.hearme.core.audio.model.AudioSegment
import com.allen.hearme.core.common.result.HearMeResult
import com.allen.hearme.core.ml.model.ConfidenceScore
import com.allen.hearme.core.ml.model.IntentClassification
import com.allen.hearme.core.ml.model.PersonalisationProfile
import com.allen.hearme.core.ml.model.SpeechClassification
import com.allen.hearme.core.ml.model.TrainingSample
import com.allen.hearme.core.ml.model.TranscriptionResult
import kotlinx.coroutines.flow.Flow

interface SpeechRecognizer {
    suspend fun transcribe(segment: AudioSegment): HearMeResult<TranscriptionResult>
    fun isAvailable(): Boolean
}

interface SpeechClassifier {
    suspend fun classify(segment: AudioSegment): HearMeResult<SpeechClassification>
    suspend fun isDirectedSpeech(segment: AudioSegment, threshold: Float = 0.65f): Boolean
}

interface IntentClassifier {
    suspend fun classifyIntent(transcription: String): HearMeResult<IntentClassification>
}

interface ConfidenceEvaluator {
    fun evaluate(
        vadConfidence: Float,
        targetConfidence: Float,
        intentConfidence: Float,
        userThreshold: Float = 0.70f
    ): ConfidenceScore
}

interface TrainingManager {
    suspend fun recordTrainingSample(sample: TrainingSample): HearMeResult<Unit>
    suspend fun retrainPersonalModel(): HearMeResult<PersonalisationProfile>
    fun getTrainedSampleCount(): Int
}

interface FeedbackRepository {
    suspend fun saveFeedback(sampleId: String, isPositive: Boolean): HearMeResult<Unit>
    fun observeRecentFeedback(): Flow<List<Pair<String, Boolean>>>
}

interface PersonalisationEngine {
    suspend fun getProfile(): PersonalisationProfile
    suspend fun updateThreshold(newOffset: Float)
}
