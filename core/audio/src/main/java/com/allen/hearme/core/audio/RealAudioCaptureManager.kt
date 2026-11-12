package com.allen.hearme.core.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import com.allen.hearme.core.audio.model.AudioChunk
import com.allen.hearme.core.audio.model.AudioConfig
import com.allen.hearme.core.common.dispatcher.DispatcherProvider
import com.allen.hearme.core.common.logging.HearMeLogger
import com.allen.hearme.core.common.result.HearMeResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Production implementation of [AudioCaptureManager] utilizing Android's [AudioRecord].
 *
 * Captures 16-bit Mono PCM audio at 16kHz on a dedicated IO coroutine dispatcher.
 * Buffers are retained transiently in memory, processed in chunks, and never written to raw disk storage.
 */
class RealAudioCaptureManager(
    private val context: Context,
    private val dispatchers: DispatcherProvider,
    private val logger: HearMeLogger = object : HearMeLogger {
        override fun d(tag: String, message: String) {}
        override fun i(tag: String, message: String) {}
        override fun w(tag: String, message: String, throwable: Throwable?) {}
        override fun e(tag: String, message: String, throwable: Throwable?) {}
    }
) : AudioCaptureManager {

    private val _isCapturing = MutableStateFlow(false)
    override val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private val _audioStream = MutableSharedFlow<AudioChunk>(replay = 1, extraBufferCapacity = 64)
    override val audioStream: Flow<AudioChunk> = _audioStream.asSharedFlow()

    private var audioRecord: AudioRecord? = null
    private var captureJob: Job? = null
    private val captureScope = CoroutineScope(dispatchers.io)

    override suspend fun startCapture(config: AudioConfig): HearMeResult<Unit> = withContext(dispatchers.io) {
        if (_isCapturing.value) {
            return@withContext HearMeResult.Success(Unit)
        }

        // Verify microphone recording permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            val error = SecurityException("android.permission.RECORD_AUDIO permission not granted")
            logger.e(TAG, "Cannot start capture without RECORD_AUDIO permission", error)
            return@withContext HearMeResult.Error(error)
        }

        try {
            val audioSource = MediaRecorder.AudioSource.VOICE_RECOGNITION
            val channelConfig = if (config.channelCount == 1) AudioFormat.CHANNEL_IN_MONO else AudioFormat.CHANNEL_IN_STEREO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT

            val minBufferSize = AudioRecord.getMinBufferSize(
                config.sampleRateHz,
                channelConfig,
                audioFormat
            )

            if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                val error = IllegalArgumentException("Unsupported AudioRecord configuration: sampleRate=${config.sampleRateHz}")
                logger.e(TAG, "Invalid AudioRecord buffer configuration", error)
                return@withContext HearMeResult.Error(error)
            }

            // Allocate buffer size at 2x minimum buffer size or at least 2048 bytes
            val bufferSize = maxOf(minBufferSize * 2, 2048)

            @SuppressLint("MissingPermission")
            val record = AudioRecord(
                audioSource,
                config.sampleRateHz,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                val error = IllegalStateException("AudioRecord failed to initialize (state != STATE_INITIALIZED)")
                logger.e(TAG, "AudioRecord initialization error", error)
                return@withContext HearMeResult.Error(error)
            }

            try {
                record.startRecording()
            } catch (e: Exception) {
                record.release()
                logger.e(TAG, "Failed to start recording on AudioRecord", e)
                return@withContext HearMeResult.Error(e)
            }

            if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                record.release()
                val error = IOException("AudioRecord failed to enter RECORDING state (recordingState=${record.recordingState})")
                logger.e(TAG, "Recording state error", error)
                return@withContext HearMeResult.Error(error)
            }

            audioRecord = record
            _isCapturing.value = true
            logger.i(TAG, "Audio recording started successfully at ${config.sampleRateHz} Hz mono")

            captureJob = captureScope.launch {
                readLoop(record, config.sampleRateHz, config.channelCount, bufferSize)
            }

            HearMeResult.Success(Unit)
        } catch (t: Throwable) {
            logger.e(TAG, "Unexpected error during AudioRecord initialization", t)
            releaseInternal()
            HearMeResult.Error(t)
        }
    }

    private suspend fun readLoop(
        record: AudioRecord,
        sampleRate: Int,
        channelCount: Int,
        readBufferSize: Int
    ) = withContext(dispatchers.io) {
        val chunkSizeBytes = 1280 // 1280 bytes = 640 16-bit samples = 40ms @ 16kHz mono
        val buffer = ByteArray(chunkSizeBytes)

        while (isActive && _isCapturing.value) {
            val bytesRead = try {
                record.read(buffer, 0, buffer.size)
            } catch (e: Exception) {
                logger.w(TAG, "Exception during AudioRecord.read", e)
                -1
            }

            if (bytesRead > 0) {
                val dataCopy = buffer.copyOf(bytesRead)
                val chunk = AudioChunk(
                    timestampMs = System.currentTimeMillis(),
                    data = dataCopy,
                    sampleRateHz = sampleRate,
                    channelCount = channelCount
                )
                _audioStream.emit(chunk)
            } else if (bytesRead < 0) {
                logger.w(TAG, "AudioRecord.read returned error code: $bytesRead")
                if (bytesRead == AudioRecord.ERROR_INVALID_OPERATION || bytesRead == AudioRecord.ERROR_DEAD_OBJECT) {
                    logger.e(TAG, "Unrecoverable AudioRecord read error ($bytesRead); stopping capture")
                    _isCapturing.value = false
                    break
                }
            }
        }
    }

    override suspend fun stopCapture() = withContext(dispatchers.io) {
        if (!_isCapturing.value && audioRecord == null) {
            return@withContext
        }
        _isCapturing.value = false
        val job = captureJob
        captureJob = null
        job?.cancel()
        try {
            job?.join()
        } catch (_: Exception) {}
        releaseInternal()
        logger.i(TAG, "Audio recording stopped and hardware resources released")
    }

    private fun releaseInternal() {
        try {
            audioRecord?.let {
                if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            logger.w(TAG, "Exception releasing AudioRecord instance", e)
        } finally {
            audioRecord = null
            _isCapturing.value = false
        }
    }

    companion object {
        private const val TAG = "RealAudioCaptureManager"
    }
}
