package com.cbgm.securechat.notification.navigation

sealed interface NotificationNavigationTarget {
    data class Conversation(
        val conversationId: String
    ) : NotificationNavigationTarget
}
