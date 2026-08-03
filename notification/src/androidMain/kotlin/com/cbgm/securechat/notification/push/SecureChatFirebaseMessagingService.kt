package com.cbgm.securechat.notification.push

import com.cbgm.securechat.notification.application.AppVisibilityState
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.koin.android.ext.android.inject

class SecureChatFirebaseMessagingService : FirebaseMessagingService() {
    private val appVisibilityState by inject<AppVisibilityState>()

    private val pendingMessageSyncScheduler by inject<PendingMessageSyncScheduler>()

    private val pushTokenRegistrationScheduler by inject<PushTokenRegistrationScheduler>()

    override fun onMessageReceived(message: RemoteMessage) {
        if (message.data[KEY_TYPE] != TYPE_MESSAGES_AVAILABLE) {
            return
        }

        val wakeUpId = message.data[KEY_WAKE_UP_ID] ?: return

        pendingMessageSyncScheduler.enqueue(wakeUpId = wakeUpId)
    }

    override fun onNewToken(token: String) {
        pushTokenRegistrationScheduler.enqueue(token = token)
    }

    private companion object {
        const val KEY_TYPE = "type"
        const val KEY_WAKE_UP_ID = "wakeUpId"
        const val TYPE_MESSAGES_AVAILABLE = "messages_available"
    }
}
