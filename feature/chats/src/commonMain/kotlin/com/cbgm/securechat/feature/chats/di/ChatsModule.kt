package com.cbgm.securechat.feature.chats.di

import com.cbgm.securechat.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.securechat.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.securechat.core.protocol.outbox.OutboxDeliveryStateListener
import com.cbgm.securechat.feature.chats.data.outbox.ChatOutboxDeliveryStateListener
import com.cbgm.securechat.feature.chats.data.protocol.ChatMessagePacketHandler
import com.cbgm.securechat.feature.chats.data.protocol.DeliveryReceiptPacketHandler
import com.cbgm.securechat.feature.chats.data.protocol.GroupChatMessagePacketHandler
import com.cbgm.securechat.feature.chats.data.protocol.GroupCreatedPacketHandler
import com.cbgm.securechat.feature.chats.data.protocol.ReadReceiptPacketHandler
import com.cbgm.securechat.feature.chats.data.repository.DefaultChatsRepository
import com.cbgm.securechat.feature.chats.domain.repository.ChatsRepository
import com.cbgm.securechat.feature.chats.domain.usecase.CreateGroupConversation
import com.cbgm.securechat.feature.chats.domain.usecase.GetContactSafetyNumber
import com.cbgm.securechat.feature.chats.domain.usecase.ObserveGroupConversation
import com.cbgm.securechat.feature.chats.domain.usecase.SendGroupMessage
import com.cbgm.securechat.feature.chats.presentation.screen.ChatViewModel
import com.cbgm.securechat.feature.chats.presentation.screen.ChatsViewModel
import com.cbgm.securechat.feature.chats.presentation.screen.CreateGroupViewModel
import com.cbgm.securechat.feature.chats.presentation.screen.GroupConversationViewModel
import com.cbgm.securechat.feature.contacts.domain.repository.ContactRepository
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val chatsModule =
    module {

        singleOf(::ChatMessagePacketHandler) {
            bind<TypedProtocolPacketHandler>()
        }

        singleOf(::ReadReceiptPacketHandler) {
            bind<TypedProtocolPacketHandler>()
        }

        singleOf(::DeliveryReceiptPacketHandler) {
            bind<TypedProtocolPacketHandler>()
        }

        singleOf(::GroupCreatedPacketHandler) {
            bind<TypedProtocolPacketHandler>()
        }

        singleOf(::GroupChatMessagePacketHandler) {
            bind<TypedProtocolPacketHandler>()
        }

        single {
            GetContactSafetyNumber(
                localPublicIdentityProvider = get<LocalPublicIdentityProvider>(),
                contactRepository = get<ContactRepository>()
            )
        }

        single<OutboxDeliveryStateListener> {
            ChatOutboxDeliveryStateListener(
                messageDeliveryStatusDao = get()
            )
        }

        single { CreateGroupConversation(repository = get()) }
        single { ObserveGroupConversation(repository = get()) }
        single { SendGroupMessage(repository = get()) }

        single<ChatsRepository> {
            DefaultChatsRepository(
                chatDao = get(),
                messageDeliveryStatusDao = get(),
                getContact = get(),
                localPublicIdentityProvider = get(),
                protocolOutbox = get(),
                incomingTransportMessageDecoder = get(),
                packetCodec = get(),
                protocolPacketHandler = get(),
                identityExchangeStarter = get()
            )
        }

        viewModel {
            ChatsViewModel(chatsRepository = get())
        }

        viewModel {
            CreateGroupViewModel(observeContacts = get(), createGroupConversation = get())
        }

        viewModel { parameters ->
            GroupConversationViewModel(conversationId = parameters.get(), observeGroupConversation = get())
        }

        viewModel { parameters ->
            ChatViewModel(
                contactId = parameters.get(),
                fallbackContactName = parameters.get(),
                chatsRepository = get<ChatsRepository>(),
                contactRepository = get<ContactRepository>(),
                getContactSafetyNumber = get<GetContactSafetyNumber>(),
                typingIndicatorGateway = get()
            )
        }
    }
