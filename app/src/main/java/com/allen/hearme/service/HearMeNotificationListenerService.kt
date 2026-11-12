package com.allen.hearme.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * NotificationListenerService used by [RealMediaPlaybackController] as the
 * [ComponentName] argument required by [android.media.session.MediaSessionManager.getActiveSessions].
 *
 * Without this service being granted Notification Listener access in Android Settings,
 * [MediaSessionManager.getActiveSessions] will throw a [SecurityException].
 *
 * This service does NOT read or process notification content.  It exists solely to
 * satisfy the system requirement that the app hold the
 * android.permission.BIND_NOTIFICATION_LISTENER_SERVICE privilege so that HearMe can
 * discover and control active media sessions.
 *
 * Session observation is handled in [RealMediaPlaybackController]; this service only
 * tracks whether it is currently connected so the controller can expose that state.
 */
class HearMeNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isConnected.value = true
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isConnected.value = false
    }

    // No-op: HearMe is not interested in individual notifications.
    override fun onNotificationPosted(sbn: StatusBarNotification?) = Unit
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = Unit

    companion object {
        private val _isConnected = MutableStateFlow(false)

        /**
         * Whether the system has connected our [NotificationListenerService],
         * i.e. the user has granted the Notification Listener access.
         */
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    }
}
