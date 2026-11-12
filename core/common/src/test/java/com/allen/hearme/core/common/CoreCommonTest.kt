package com.allen.hearme.core.common

import com.allen.hearme.core.common.result.HearMeResult
import com.allen.hearme.core.common.time.DefaultTimeProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreCommonTest {

    @Test
    fun resultSuccessReturnsData() {
        val success = HearMeResult.Success("test_data")
        assertTrue(success.isSuccess)
        assertEquals("test_data", success.getOrNull())
    }

    @Test
    fun resultErrorReturnsException() {
        val exception = IllegalStateException("failure")
        val error = HearMeResult.Error(exception)
        assertTrue(error.isError)
        assertEquals(exception, error.exceptionOrNull())
    }

    @Test
    fun timeProviderReturnsCurrentTime() {
        val provider = DefaultTimeProvider()
        assertTrue(provider.currentTimeMillis() > 0)
        assertTrue(provider.currentEpochSeconds() > 0)
    }
}
