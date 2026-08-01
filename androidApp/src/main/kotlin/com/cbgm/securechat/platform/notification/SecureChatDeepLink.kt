package com.cbgm.securechat.platform.notification

import android.content.Intent
import android.net.Uri

object SecureChatDeepLink {
    private const val SCHEME = "securechat"
    private const val CHAT_HOST = "chat"

    fun conversationUri(conversationId: String): Uri {
        require(conversationId.isNotBlank()) {
            "Conversation ID must not be blank"
        }

        return Uri
            .Builder()
            .scheme(SCHEME)
            .authority(CHAT_HOST)
            .appendPath(conversationId)
            .build()
    }

    fun conversationId(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_VIEW) {
            return null
        }

        val uri = intent.data ?: return null

        if (uri.scheme != SCHEME || uri.host != CHAT_HOST) {
            return null
        }

        return uri.pathSegments
            .singleOrNull()
            ?.takeIf(String::isNotBlank)
    }
}
