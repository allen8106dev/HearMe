package com.allen.hearme.service

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controller abstraction allowing ViewModels and UI to trigger the foreground
 * listening service without directly depending on Android framework Context.
 */
interface ServiceController {
    fun startListeningService()
    fun stopListeningService()
}

@Singleton
class RealServiceController @Inject constructor(
    @ApplicationContext private val context: Context
) : ServiceController {
    override fun startListeningService() {
        SmartListeningService.start(context)
    }

    override fun stopListeningService() {
        SmartListeningService.stop(context)
    }
}

class FakeServiceController : ServiceController {
    var isRunning: Boolean = false
        private set
    var startCallCount: Int = 0
        private set
    var stopCallCount: Int = 0
        private set

    override fun startListeningService() {
        isRunning = true
        startCallCount++
    }

    override fun stopListeningService() {
        isRunning = false
        stopCallCount++
    }
}
