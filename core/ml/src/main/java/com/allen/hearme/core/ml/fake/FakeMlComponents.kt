package com.allen.hearme.core.ml.fake

import com.allen.hearme.core.audio.model.AudioSegment
import com.allen.hearme.core.common.result.HearMeResult
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
import com.allen.hearme.core.ml.model.SpeechTarget
import com.allen.hearme.core.ml.model.TrainingSample
import com.allen.hearme.core.ml.model.TranscriptionResult
import com.allen.hearme.core.ml.model.UserIntent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeSpeechRecognizer(
    var defaultText: String = "Excuse me, can you hear me?",
    var defaultConfidence: Float = 0.92f,
    var available: Boolean = true
) : SpeechRecognizer {
    override suspend fun transcribe(segment: AudioSegment): HearMeResult<TranscriptionResult> {
        return HearMeResult.Success(
            TranscriptionResult(text = defaultText, confidence = defaultConfidence)
        )
    }

    override fun isAvailable(): Boolean = available
}

class FakeSpeechClassifier(
    var defaultTarget: SpeechTarget = SpeechTarget.DIRECTED_AT_USER,
    var defaultConfidence: Float = 0.88f
) : SpeechClassifier {
    override suspend fun classify(segment: AudioSegment): HearMeResult<SpeechClassification> {
        return HearMeResult.Success(
            SpeechClassification(target = defaultTarget, confidence = defaultConfidence)
        )
    }

    override suspend fun isDirectedSpeech(segment: AudioSegment, threshold: Float): Boolean {
        return defaultTarget == SpeechTarget.DIRECTED_AT_USER && defaultConfidence >= threshold
    }
}

class FakeIntentClassifier(
    var defaultIntent: UserIntent = UserIntent.ATTENTION_GETTING,
    var defaultConfidence: Float = 0.85f
) : IntentClassifier {
    override suspend fun classifyIntent(transcription: String): HearMeResult<IntentClassification> {
        return HearMeResult.Success(
            IntentClassification(intent = defaultIntent, confidence = defaultConfidence)
        )
    }
}

class FakeConfidenceEvaluator : ConfidenceEvaluator {
    override fun evaluate(
        vadConfidence: Float,
        targetConfidence: Float,
        intentConfidence: Float,
        userThreshold: Float
    ): ConfidenceScore {
        val overall = (vadConfidence * 0.2f) + (targetConfidence * 0.5f) + (intentConfidence * 0.3f)
        return ConfidenceScore(
            overallConfidence = overall,
            vadConfidence = vadConfidence,
            speechTargetConfidence = targetConfidence,
            intentConfidence = intentConfidence,
            meetsThreshold = overall >= userThreshold
        )
    }
}

class FakeTrainingManager : TrainingManager {
    private val samples = mutableListOf<TrainingSample>()

    override suspend fun recordTrainingSample(sample: TrainingSample): HearMeResult<Unit> {
        samples.add(sample)
        return HearMeResult.Success(Unit)
    }

    override suspend fun retrainPersonalModel(): HearMeResult<PersonalisationProfile> {
        return HearMeResult.Success(PersonalisationProfile(trainedSampleCount = samples.size))
    }

    override fun getTrainedSampleCount(): Int = samples.size
}

class FakeFeedbackRepository : FeedbackRepository {
    private val _feedback = MutableStateFlow<List<Pair<String, Boolean>>>(emptyList())

    override suspend fun saveFeedback(sampleId: String, isPositive: Boolean): HearMeResult<Unit> {
        _feedback.value = _feedback.value + (sampleId to isPositive)
        return HearMeResult.Success(Unit)
    }

    override fun observeRecentFeedback(): Flow<List<Pair<String, Boolean>>> = _feedback.asStateFlow()
}

class FakePersonalisationEngine(
    private var profile: PersonalisationProfile = PersonalisationProfile()
) : PersonalisationEngine {
    override suspend fun getProfile(): PersonalisationProfile = profile

    override suspend fun updateThreshold(newOffset: Float) {
        profile = profile.copy(sensitivityOffset = newOffset)
    }
}
