package com.cbgm.securechat.feature.chats.di

import com.cbgm.securechat.feature.chats.data.repository.DefaultChatsRepository
import com.cbgm.securechat.feature.chats.domain.repository.ChatsRepository
import com.cbgm.securechat.feature.chats.presentation.ChatViewModel
import com.cbgm.securechat.feature.chats.presentation.ChatsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val chatsModule =
    module {

        single<ChatsRepository> {
            DefaultChatsRepository(
                chatDao =
                    get(),

                getContact =
                    get(),

                transportMessageCipher =
                    get(),

                transportPayloadCodec =
                    get(),

                incomingTransportMessageDecoder =
                    get()
            )
        }

        viewModel {
            ChatsViewModel(
                chatsRepository = get()
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