package com.cbgm.securechat.relay.push

interface PushNotificationSender {
    suspend fun notifyMessagesAvailable(recipientId: String)
}
