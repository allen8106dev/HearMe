package com.allen.hearme.core.audio.model

data class AudioConfig(
    val sampleRateHz: Int = 16000,
    val channelCount: Int = 1,
    val bitDepth: Int = 16,
    val bufferSizeBytes: Int = 1024
)

data class AudioChunk(
    val timestampMs: Long,
    val data: ByteArray,
    val sampleRateHz: Int = 16000,
    val channelCount: Int = 1
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AudioChunk
        return timestampMs == other.timestampMs &&
                sampleRateHz == other.sampleRateHz &&
                channelCount == other.channelCount &&
                data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = timestampMs.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + sampleRateHz
        result = 31 * result + channelCount
        return result
    }
}

data class AudioSegment(
    val id: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val rawBytes: ByteArray,
    val durationMs: Long = endTimeMs - startTimeMs
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AudioSegment
        return id == other.id &&
                startTimeMs == other.startTimeMs &&
                endTimeMs == other.endTimeMs &&
                durationMs == other.durationMs &&
                rawBytes.contentEquals(other.rawBytes)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + startTimeMs.hashCode()
        result = 31 * result + endTimeMs.hashCode()
        result = 31 * result + rawBytes.contentHashCode()
        result = 31 * result + durationMs.hashCode()
        return result
    }
}

data class SpeechActivity(
    val hasSpeech: Boolean,
    val confidence: Float,
    val energyRms: Float
)
