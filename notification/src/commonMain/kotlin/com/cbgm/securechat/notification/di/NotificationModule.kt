package com.cbgm.securechat.notification.di

import com.cbgm.securechat.notification.application.AppVisibilityState
import com.cbgm.securechat.notification.application.ConversationNotificationCoordinator
import com.cbgm.securechat.notification.application.ObserveConversationNotificationEvents
import com.cbgm.securechat.notification.application.RegisterPushToken
import com.cbgm.securechat.notification.application.ResolveNotificationConversation
import com.cbgm.securechat.notification.application.SynchronizePendingMessages
import com.cbgm.securechat.notification.navigation.NotificationNavigationController
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val notificationModule =
    module {
        singleOf(::AppVisibilityState)
        singleOf(::NotificationNavigationController)
        singleOf(::ObserveConversationNotificationEvents)
        singleOf(::ConversationNotificationCoordinator)
        singleOf(::ResolveNotificationConversation)
        singleOf(::RegisterPushToken)
        singleOf(::SynchronizePendingMessages)
    }
