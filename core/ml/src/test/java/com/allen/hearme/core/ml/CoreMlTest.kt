package com.allen.hearme.core.ml

import com.allen.hearme.core.audio.model.AudioSegment
import com.allen.hearme.core.ml.fake.FakeConfidenceEvaluator
import com.allen.hearme.core.ml.fake.FakeFeedbackRepository
import com.allen.hearme.core.ml.fake.FakePersonalisationEngine
import com.allen.hearme.core.ml.fake.FakeSpeechClassifier
import com.allen.hearme.core.ml.fake.FakeSpeechRecognizer
import com.allen.hearme.core.ml.fake.FakeTrainingManager
import com.allen.hearme.core.ml.model.SpeechTarget
import com.allen.hearme.core.ml.model.TrainingSample
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreMlTest {

    @Test
    fun speechRecognizerReturnsTranscription() = runTest {
        val recognizer = FakeSpeechRecognizer(defaultText = "Hey Allen")
        val dummySegment = AudioSegment("1", 0L, 1000L, ByteArray(0))
        val result = recognizer.transcribe(dummySegment)

        assertTrue(result.isSuccess)
        assertEquals("Hey Allen", result.getOrNull()?.text)
    }

    @Test
    fun speechClassifierIdentifiesDirectedSpeech() = runTest {
        val classifier = FakeSpeechClassifier(defaultTarget = SpeechTarget.DIRECTED_AT_USER, defaultConfidence = 0.85f)
        val dummySegment = AudioSegment("1", 0L, 1000L, ByteArray(0))

        assertTrue(classifier.isDirectedSpeech(dummySegment, threshold = 0.80f))
        assertFalse(classifier.isDirectedSpeech(dummySegment, threshold = 0.90f))
    }

    @Test
    fun confidenceEvaluatorComputesScoreAndThreshold() {
        val evaluator = FakeConfidenceEvaluator()
        val highConfidence = evaluator.evaluate(0.9f, 0.9f, 0.9f, userThreshold = 0.7f)
        assertTrue(highConfidence.meetsThreshold)
        assertEquals(0.9f, highConfidence.overallConfidence, 0.001f)

        val lowConfidence = evaluator.evaluate(0.4f, 0.3f, 0.2f, userThreshold = 0.7f)
        assertFalse(lowConfidence.meetsThreshold)
    }

    @Test
    fun trainingAndFeedbackPipelineRecordsSamples() = runTest {
        val trainingManager = FakeTrainingManager()
        val sample = TrainingSample(
            id = "sample-101",
            audioBytes = byteArrayOf(1, 2),
            classification = SpeechTarget.DIRECTED_AT_USER
        )
        trainingManager.recordTrainingSample(sample)
        assertEquals(1, trainingManager.getTrainedSampleCount())

        val feedbackRepo = FakeFeedbackRepository()
        feedbackRepo.saveFeedback("sample-101", isPositive = true)
        val recentFeedback = feedbackRepo.observeRecentFeedback().first()
        assertEquals(1, recentFeedback.size)
        assertEquals("sample-101", recentFeedback[0].first)
        assertTrue(recentFeedback[0].second)

        val personalisationEngine = FakePersonalisationEngine()
        personalisationEngine.updateThreshold(0.05f)
        assertEquals(0.05f, personalisationEngine.getProfile().sensitivityOffset, 0.001f)
    }
}
