package com.allen.hearme.core.media

import com.allen.hearme.core.common.result.HearMeResult

interface MediaPlaybackController {
    suspend fun pauseMedia(): HearMeResult<Unit>
    suspend fun resumeMedia(): HearMeResult<Unit>
    suspend fun lowerVolume(duckRatio: Float = 0.2f): HearMeResult<Unit>
    suspend fun restoreVolume(): HearMeResult<Unit>
    fun isMediaPlaying(): Boolean
}
