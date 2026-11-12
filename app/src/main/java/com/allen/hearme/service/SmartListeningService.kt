package com.allen.hearme.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.allen.hearme.MainActivity
import com.allen.hearme.R
import com.allen.hearme.core.audio.AudioCaptureManager
import com.allen.hearme.core.audio.SpeechActivityDetector
import com.allen.hearme.core.common.dispatcher.DispatcherProvider
import com.allen.hearme.core.media.MediaPlaybackController
import com.allen.hearme.feature.smartlistening.ListeningOrchestrator
import com.allen.hearme.feature.smartlistening.model.ListeningState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground Service hosting active microphone audio capture and real-time VAD.
 *
 * Adheres strictly to Android 14+ (API 34) FOREGROUND_SERVICE_MICROPHONE policies:
 * - Runs only after explicit user interaction in the UI and RECORD_AUDIO permission grant.
 * - Displays a non-dismissible ongoing notification with a one-tap Stop action.
 * - Releases all AudioRecord and coroutine resources immediately upon destruction or interruption.
 */
@AndroidEntryPoint
class SmartListeningService : Service() {

    @Inject
    lateinit var audioCaptureManager: AudioCaptureManager

    @Inject
    lateinit var speechDetector: SpeechActivityDetector

    @Inject
    lateinit var orchestrator: ListeningOrchestrator

    @Inject
    lateinit var mediaPlaybackController: MediaPlaybackController

    @Inject
    lateinit var dispatchers: DispatcherProvider

    private var serviceJob = Job()
    private lateinit var serviceScope: CoroutineScope
    private var processingJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        serviceScope = CoroutineScope(dispatchers.io + serviceJob)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_LISTENING) {
            stopListeningService()
            return START_NOT_STICKY
        }

        val hasMicPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasMicPermission) {
            _liveServiceState.value = ListeningState.MICROPHONE_UNAVAILABLE
            // Satisfy Android foreground service contract before stopping
            startForegroundNotification(isPermissionGranted = false)
            stopListeningService()
            return START_NOT_STICKY
        }

        startForegroundNotification(isPermissionGranted = true)
        startAudioProcessingLoop()

        return START_STICKY
    }

    private fun startForegroundNotification(isPermissionGranted: Boolean) {
        val notification = buildNotification(isSpeechDetected = false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val foregroundServiceType = if (isPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            } else {
                0
            }
            try {
                startForeground(NOTIFICATION_ID, notification, foregroundServiceType)
            } catch (_: Exception) {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        if (isPermissionGranted) {
            _isServiceRunning.value = true
            _liveServiceState.value = ListeningState.ACTIVE_LISTENING
        }
    }

    private fun startAudioProcessingLoop() {
        processingJob?.cancel()
        processingJob = serviceScope.launch {
            // Start physical capture
            val captureResult = audioCaptureManager.startCapture()
            if (!captureResult.isSuccess) {
                _liveServiceState.value = ListeningState.ERROR
                stopListeningService()
                return@launch
            }

            // Stream PCM chunks through VAD detector
            audioCaptureManager.audioStream.collect { chunk ->
                val activity = speechDetector.detectActivity(chunk)
                _liveRmsEnergy.value = activity.energyRms
                _liveSpeechConfidence.value = activity.confidence
                _isSpeechActive.value = activity.hasSpeech

                if (activity.hasSpeech) {
                    if (_liveServiceState.value != ListeningState.SPEECH_DETECTED) {
                        _liveServiceState.value = ListeningState.SPEECH_DETECTED
                        updateNotification(isSpeechDetected = true)
                        serviceScope.launch {
                            mediaPlaybackController.pauseMedia()
                        }
                    }
                } else {
                    if (_liveServiceState.value == ListeningState.SPEECH_DETECTED) {
                        _liveServiceState.value = ListeningState.ACTIVE_LISTENING
                        updateNotification(isSpeechDetected = false)
                        serviceScope.launch {
                            mediaPlaybackController.restorePlayback()
                        }
                    }
                }
            }
        }
    }

    private fun stopListeningService() {
        processingJob?.cancel()
        processingJob = null
        serviceScope.launch {
            mediaPlaybackController.restorePlayback()
            audioCaptureManager.stopCapture()
            speechDetector.reset()
            _isServiceRunning.value = false
            _liveServiceState.value = ListeningState.STANDBY
            _isSpeechActive.value = false
            _liveRmsEnergy.value = 0.0f
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        _isServiceRunning.value = false
        _liveServiceState.value = ListeningState.STANDBY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Smart Listening",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Displays live microphone awareness status for HearMe."
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(isSpeechDetected: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, SmartListeningService::class.java).apply {
            action = ACTION_STOP_LISTENING
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (isSpeechDetected) {
            "Speech detected in your environment"
        } else {
            "Listening for speech directed to you"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("HearMe Smart Listening")
            .setContentText(contentText)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop Listening",
                stopPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(isSpeechDetected: Boolean) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, buildNotification(isSpeechDetected))
    }

    companion object {
        const val CHANNEL_ID = "hearme_listening_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_LISTENING = "com.allen.hearme.action.START_LISTENING"
        const val ACTION_STOP_LISTENING = "com.allen.hearme.action.STOP_LISTENING"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        private val _liveServiceState = MutableStateFlow(ListeningState.STANDBY)
        val liveServiceState: StateFlow<ListeningState> = _liveServiceState.asStateFlow()

        private val _liveRmsEnergy = MutableStateFlow(0.0f)
        val liveRmsEnergy: StateFlow<Float> = _liveRmsEnergy.asStateFlow()

        private val _liveSpeechConfidence = MutableStateFlow(0.0f)
        val liveSpeechConfidence: StateFlow<Float> = _liveSpeechConfidence.asStateFlow()

        private val _isSpeechActive = MutableStateFlow(false)
        val isSpeechActive: StateFlow<Boolean> = _isSpeechActive.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, SmartListeningService::class.java).apply {
                action = ACTION_START_LISTENING
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, SmartListeningService::class.java).apply {
                action = ACTION_STOP_LISTENING
            }
            context.startService(intent)
        }
    }
}
