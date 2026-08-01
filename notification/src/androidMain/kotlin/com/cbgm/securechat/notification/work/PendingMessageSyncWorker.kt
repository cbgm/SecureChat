package com.cbgm.securechat.notification.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cbgm.securechat.notification.application.AppVisibilityState
import com.cbgm.securechat.notification.application.SynchronizePendingMessages
import com.cbgm.securechat.notification.presentation.ConversationNotificationPresenter

class PendingMessageSyncWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
    private val synchronizePendingMessages: SynchronizePendingMessages,
    private val appVisibilityState: AppVisibilityState,
    private val conversationNotificationPresenter: ConversationNotificationPresenter
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val wakeUpId =
            inputData.getString(KEY_WAKE_UP_ID)
                ?: return Result.failure()

        return synchronizePendingMessages(wakeUpId = wakeUpId).fold(
            onSuccess = { syncResult ->
                if (!appVisibilityState.isVisible.value) {
                    syncResult.notifications.forEach { notification ->
                        conversationNotificationPresenter.show(notification)
                    }
                }
                Result.success()
            },
            onFailure = {
                if (runAttemptCount >= MAX_RETRY_COUNT) {
                    Result.failure()
                } else {
                    Result.retry()
                }
            }
        )
    }

    companion object {
        const val KEY_WAKE_UP_ID = "wake-up-id"

        private const val MAX_RETRY_COUNT = 5
    }
}
