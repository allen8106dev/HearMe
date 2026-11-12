package com.allen.hearme.core.media

import com.allen.hearme.core.common.result.HearMeResult
import com.allen.hearme.core.media.model.InterruptionRecord
import com.allen.hearme.core.media.model.MediaTrackInfo
import kotlinx.coroutines.flow.StateFlow

interface MediaPlaybackController {
    /** Currently-active media track, or null when nothing is playing. */
    val activeMediaTrack: StateFlow<MediaTrackInfo?>

    /** Whether the user has granted Notification Listener access to this app. */
    val hasNotificationListenerAccess: StateFlow<Boolean>

    suspend fun pauseMedia(): HearMeResult<Unit>
    suspend fun resumeMedia(): HearMeResult<Unit>
    suspend fun lowerVolume(duckRatio: Float = 0.2f): HearMeResult<Unit>
    suspend fun restoreVolume(): HearMeResult<Unit>

    /** Restore playback to the state it was in before HearMe's last interruption. */
    suspend fun restorePlayback(): HearMeResult<Unit>

    /** Cancel any pending restoration without touching playback state. */
    fun cancelInterruption()

    /** Returns the active interruption record, or null if none exists. */
    fun getActiveInterruption(): InterruptionRecord?

    fun isMediaPlaying(): Boolean
}
