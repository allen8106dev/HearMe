package com.allen.hearme.core.ml.model

data class TranscriptionResult(
    val text: String,
    val confidence: Float,
    val language: String = "en",
    val isFinal: Boolean = true
)

enum class SpeechTarget {
    DIRECTED_AT_USER,
    BACKGROUND_CONVERSATION,
    AMBIENT_NOISE,
    UNKNOWN
}

data class SpeechClassification(
    val target: SpeechTarget,
    val confidence: Float,
    val speakerScore: Float = 0.0f
)

enum class UserIntent {
    CALLING_NAME,
    ATTENTION_GETTING,
    CASUAL_REMARK,
    NONE
}

data class IntentClassification(
    val intent: UserIntent,
    val confidence: Float
)

data class ConfidenceScore(
    val overallConfidence: Float,
    val vadConfidence: Float,
    val speechTargetConfidence: Float,
    val intentConfidence: Float,
    val meetsThreshold: Boolean
)

data class TrainingSample(
    val id: String,
    val audioBytes: ByteArray,
    val classification: SpeechTarget,
    val transcription: String? = null,
    val userFeedback: Boolean? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TrainingSample
        return id == other.id &&
                classification == other.classification &&
                transcription == other.transcription &&
                userFeedback == other.userFeedback &&
                timestamp == other.timestamp &&
                audioBytes.contentEquals(other.audioBytes)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + audioBytes.contentHashCode()
        result = 31 * result + classification.hashCode()
        result = 31 * result + (transcription?.hashCode() ?: 0)
        result = 31 * result + (userFeedback?.hashCode() ?: 0)
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

data class PersonalisationProfile(
    val version: Int = 1,
    val trainedSampleCount: Int = 0,
    val sensitivityOffset: Float = 0.0f,
    val customKeywords: List<String> = emptyList()
)
