package com.cbgm.securechat.notification.push

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.cbgm.securechat.notification.work.PendingMessageSyncWorker

class PendingMessageSyncScheduler(
    private val context: Context
) {
    fun enqueue(wakeUpId: String) {
        require(wakeUpId.isNotBlank()) {
            "Wake-up ID must not be blank"
        }

        val request =
            OneTimeWorkRequestBuilder<PendingMessageSyncWorker>()
                .setInputData(
                    workDataOf(
                        PendingMessageSyncWorker.KEY_WAKE_UP_ID to wakeUpId
                    )
                ).setConstraints(
                    Constraints
                        .Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                ).setExpedited(
                    OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST
                ).build()

        WorkManager
            .getInstance(context)
            .enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                request
            )
    }

    private companion object {
        const val WORK_NAME = "securechat-pending-message-sync"
    }
}
