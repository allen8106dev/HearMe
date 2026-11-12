package com.allen.hearme.core.audio

import com.allen.hearme.core.audio.model.AudioSegment
import com.allen.hearme.core.common.result.HearMeResult

interface AudioSegmentProcessor {
    suspend fun processSegment(segment: AudioSegment): HearMeResult<AudioSegment>
}
