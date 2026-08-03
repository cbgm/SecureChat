package com.cbgm.securechat.platform.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.cbgm.securechat.MainActivity

object SecureChatNotificationIntentFactory {
    fun createConversationIntent(
        context: Context,
        conversationId: String
    ): PendingIntent {
        val intent =
            Intent(
                Intent.ACTION_VIEW,
                SecureChatDeepLink.conversationUri(conversationId),
                context,
                MainActivity::class.java
            ).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

        return PendingIntent.getActivity(
            context,
            conversationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun createLauncherIntent(context: Context): PendingIntent {
        val intent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

        return PendingIntent.getActivity(
            context,
            WAKE_UP_NOTIFICATION_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private const val WAKE_UP_NOTIFICATION_REQUEST_CODE = 10_001
}
