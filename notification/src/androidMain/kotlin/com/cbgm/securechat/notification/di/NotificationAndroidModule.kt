package com.cbgm.securechat.notification.di

import androidx.work.WorkerParameters
import com.cbgm.securechat.notification.push.PendingMessageSyncScheduler
import com.cbgm.securechat.notification.push.PushTokenRegistrationScheduler
import com.cbgm.securechat.notification.work.PendingMessageSyncWorker
import com.cbgm.securechat.notification.work.PushTokenRegistrationWorker
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.dsl.module

val notificationAndroidModule =
    module {
        single {
            PendingMessageSyncScheduler(
                context = androidContext()
            )
        }

        single {
            PushTokenRegistrationScheduler(
                context = androidContext()
            )
        }

        worker { parameters ->
            PendingMessageSyncWorker(
                appContext = androidContext(),
                workerParameters = parameters.get<WorkerParameters>(),
                synchronizePendingMessages = get(),
                appVisibilityState = get(),
                conversationNotificationPresenter = get()
            )
        }

        worker { parameters ->
            PushTokenRegistrationWorker(
                appContext = androidContext(),
                workerParameters = parameters.get<WorkerParameters>(),
                registerPushToken = get()
            )
        }
    }
