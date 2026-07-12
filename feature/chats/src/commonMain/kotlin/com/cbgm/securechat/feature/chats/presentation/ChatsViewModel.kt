package com.cbgm.securechat.feature.chats.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.feature.chats.domain.repository.ChatsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ChatsViewModel(
    chatsRepository: ChatsRepository
) : ViewModel() {

    val uiState: StateFlow<ChatsUiState> =
        chatsRepository.conversations
            .map { conversations ->
                val items = conversations
                    .sortedByDescending {
                        it.lastMessage?.timestamp ?: 0L
                    }
                    .map { conversation ->
                        ChatListItem(
                            contactId =
                                conversation.contactId,
                            contactName =
                                conversation.contactName,
                            lastMessage =
                                conversation.lastMessage
                                    ?.text
                                    ?: "No messages yet",
                            timestamp = ""
                        )
                    }

                ChatsUiState(
                    conversations = items
                )
            }
            .stateIn(
                scope = viewModelScope,
                started =
                    SharingStarted.WhileSubscribed(5_000),
                initialValue = ChatsUiState()
            )
}