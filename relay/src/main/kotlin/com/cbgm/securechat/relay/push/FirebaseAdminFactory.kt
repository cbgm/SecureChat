package com.cbgm.securechat.relay.push

import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import org.slf4j.LoggerFactory

object FirebaseAdminFactory {
    private val logger = LoggerFactory.getLogger(FirebaseAdminFactory::class.java)

    fun createMessagingOrNull(): FirebaseMessaging? =
        runCatching {
            val app =
                FirebaseApp
                    .getApps()
                    .firstOrNull()
                    ?: FirebaseApp.initializeApp()

            FirebaseMessaging.getInstance(app)
        }.onFailure { error ->
            logger.warn(
                "Firebase Admin is not configured; push delivery is disabled. " +
                    "Set GOOGLE_APPLICATION_CREDENTIALS to enable FCM.",
                error
            )
        }.getOrNull()
}
