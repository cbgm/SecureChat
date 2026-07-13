package com.cbgm.securechat.feature.chats.di

import com.cbgm.securechat.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.securechat.core.protocol.outbox.OutboxDeliveryStateListener
import com.cbgm.securechat.feature.chats.data.outbox.ChatOutboxDeliveryStateListener
import com.cbgm.securechat.feature.chats.data.protocol.ChatMessagePacketHandler
import com.cbgm.securechat.feature.chats.data.repository.DefaultChatsRepository
import com.cbgm.securechat.feature.chats.domain.repository.ChatsRepository
import com.cbgm.securechat.feature.chats.presentation.ChatViewModel
import com.cbgm.securechat.feature.chats.presentation.ChatsViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val chatsModule =
    module {

        singleOf(
            ::ChatMessagePacketHandler
        ) {
            bind<
                    TypedProtocolPacketHandler
                    >()
        }

        single<OutboxDeliveryStateListener> {
            ChatOutboxDeliveryStateListener(
                messageDeliveryStatusDao =
                    get()
            )
        }

        single<ChatsRepository> {
            DefaultChatsRepository(
                chatDao = get(),
                messageDeliveryStatusDao = get(),
                getContact = get(),
                protocolOutbox = get(),
                incomingTransportMessageDecoder = get(),
                packetCodec = get(),
                protocolPacketHandler = get()
            )
        }

        viewModel {
            ChatsViewModel(
                chatsRepository =
                    get()
            )
        }

        viewModel { parameters ->
            ChatViewModel(
                contactId =
                    parameters.get(),

                fallbackContactName =
                    parameters.get(),

                chatsRepository =
                    get(),

                contactRepository =
                    get()
            )
        }
    }