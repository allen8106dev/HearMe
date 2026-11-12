package com.allen.hearme.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controller abstraction allowing ViewModels and UI to trigger the foreground
 * listening service without directly depending on Android framework Context.
 */
interface ServiceController {
    fun startListeningService(): Boolean
    fun stopListeningService()
}

@Singleton
class RealServiceController @Inject constructor(
    @ApplicationContext private val context: Context
) : ServiceController {
    override fun startListeningService(): Boolean {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            SmartListeningService.start(context)
            return true
        }
        return false
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

    override fun startListeningService(): Boolean {
        isRunning = true
        startCallCount++
        return true
    }

    override fun stopListeningService() {
        isRunning = false
        stopCallCount++
    }
}
