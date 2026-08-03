package com.cbgm.securechat.relay.push

import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.FcmOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import org.slf4j.LoggerFactory

class FirebasePushNotificationSender(
    private val firebaseMessaging: FirebaseMessaging?,
    private val pushDeviceStore: PushDeviceStore,
    private val pushWakeUpStore: PushWakeUpStore
) : PushNotificationSender {
    private val logger = LoggerFactory.getLogger(FirebasePushNotificationSender::class.java)

    override suspend fun notifyMessagesAvailable(recipientId: String) {
        val messaging = firebaseMessaging ?: return

        val devices =
            pushDeviceStore
                .getForRelayId(relayId = recipientId)
                .filter { device ->
                    device.platform == PLATFORM_ANDROID
                }

        if (devices.isEmpty()) {
            return
        }

        val wakeUpId = pushWakeUpStore.create(recipientId = recipientId)

        devices.forEach { device ->
            val message =
                Message
                    .builder()
                    .setToken(device.token)
                    .putData(KEY_TYPE, TYPE_MESSAGES_AVAILABLE)
                    .putData(KEY_WAKE_UP_ID, wakeUpId)
                    .setFcmOptions(
                        FcmOptions.withAnalyticsLabel(SECURE_CHAT_WAKEUP_LABEL)
                    ).setAndroidConfig(
                        AndroidConfig
                            .builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setCollapseKey(COLLAPSE_KEY)
                            .build()
                    ).build()

            try {
                val messageId = messaging.send(message)

                logger.info(
                    "FCM wake-up sent: messageId={}, wakeUpId={}",
                    messageId,
                    wakeUpId
                )
            } catch (error: FirebaseMessagingException) {
                if (error.messagingErrorCode == MessagingErrorCode.UNREGISTERED) {
                    pushDeviceStore.removeToken(token = device.token)
                }

                logger.error(
                    "FCM wake-up failed for recipient {}",
                    recipientId,
                    error
                )
            }
        }
    }

    private companion object {
        const val KEY_TYPE = "type"
        const val KEY_WAKE_UP_ID = "wakeUpId"
        const val TYPE_MESSAGES_AVAILABLE = "messages_available"
        const val COLLAPSE_KEY = "securechat-messages"
        const val PLATFORM_ANDROID = "ANDROID"
        const val SECURE_CHAT_WAKEUP_LABEL = "securechat_wakeup"
    }
}
