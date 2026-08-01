package com.cbgm.securechat.notification.presentation

import com.cbgm.securechat.notification.model.ConversationNotification

interface ConversationNotificationPresenter {
    fun show(notification: ConversationNotification)

    fun cancel(conversationId: String)
}
