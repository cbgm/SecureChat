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
        chatsRepository
            .observeConversations()
            .map { conversations ->
                ChatsUiState(
                    conversations =
                        conversations.map {
                                conversation ->

                            ChatListItem(
                                contactId =
                                    conversation.contactId,
                                contactName =
                                    conversation.contactName,
                                lastMessage =
                                    conversation
                                        .lastMessage
                                        ?.text
                                        ?: "No messages yet",
                                timestamp =
                                    conversation
                                        .lastMessage
                                        ?.timestamp
                                        ?.toString()
                                        .orEmpty()
                            )
                        }
                )
            }
            .stateIn(
                scope = viewModelScope,
                started =
                    SharingStarted.WhileSubscribed(
                        stopTimeoutMillis = 5_000
                    ),
                initialValue = ChatsUiState()
            )
}