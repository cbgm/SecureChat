package com.cbgm.securechat.feature.chats.di

import com.cbgm.securechat.feature.chats.data.InMemoryChatsRepository
import com.cbgm.securechat.feature.chats.domain.repository.ChatsRepository
import com.cbgm.securechat.feature.chats.presentation.ChatViewModel
import com.cbgm.securechat.feature.chats.presentation.ChatsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val chatsModule = module {
    viewModel { parameters ->
        ChatViewModel(
            contactId = parameters.get(),
            contactName = parameters.get(),
            chatsRepository = get()
        )
    }

    single<ChatsRepository> {
        InMemoryChatsRepository()
    }

    viewModel {
        ChatsViewModel(
            chatsRepository = get()
        )
    }

    viewModel { parameters ->
        ChatViewModel(
            contactId = parameters.get(),
            contactName = parameters.get(),
            chatsRepository = get()
        )
    }

}