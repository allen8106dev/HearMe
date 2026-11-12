package com.allen.hearme.di

import com.allen.hearme.core.audio.AudioCaptureManager
import com.allen.hearme.core.bluetooth.EarphoneConnectionMonitor
import com.allen.hearme.core.media.MediaPlaybackController
import com.allen.hearme.feature.smartlistening.DefaultListeningDecisionEngine
import com.allen.hearme.feature.smartlistening.DefaultListeningOrchestrator
import com.allen.hearme.feature.smartlistening.ListeningDecisionEngine
import com.allen.hearme.feature.smartlistening.ListeningOrchestrator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AudioSystemModule {

    @Provides
    @Singleton
    fun provideListeningDecisionEngine(): ListeningDecisionEngine {
        return DefaultListeningDecisionEngine()
    }

    @Provides
    @Singleton
    fun provideListeningOrchestrator(
        audioCaptureManager: AudioCaptureManager,
        earphoneMonitor: EarphoneConnectionMonitor,
        mediaPlaybackController: MediaPlaybackController,
        decisionEngine: ListeningDecisionEngine
    ): ListeningOrchestrator {
        return DefaultListeningOrchestrator(
            audioCaptureManager = audioCaptureManager,
            earphoneMonitor = earphoneMonitor,
            mediaPlaybackController = mediaPlaybackController,
            decisionEngine = decisionEngine
        )
    }
}
