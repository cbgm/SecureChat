package com.cbgm.securechat.feature.chats.presentation.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.feature.chats.domain.repository.ChatsRepository
import com.cbgm.securechat.feature.chats.presentation.mapper.ConversionEntityMapper.toChatListItem
import com.cbgm.securechat.feature.chats.presentation.model.ChatsUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

class ChatsViewModel(
    chatsRepository: ChatsRepository,
) : ViewModel() {
    val uiState: StateFlow<ChatsUiState> =
        chatsRepository
            .observeConversations()
            .map { conversations ->
                ChatsUiState(
                    isLoading = false,
                    conversations = conversations.map { it.toChatListItem() },
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ChatsUiState(isLoading = true),
            )
}
