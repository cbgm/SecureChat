package com.cbgm.securechat.feature.chats.presentation.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.core.logging.SecureChatLog
import com.cbgm.securechat.feature.chats.domain.usecase.DeleteConversation
import com.cbgm.securechat.feature.chats.domain.usecase.ObserveConversations
import com.cbgm.securechat.feature.chats.presentation.mapper.toChatListItem
import com.cbgm.securechat.feature.chats.presentation.model.ChatsUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatsViewModel(
    observeConversations: ObserveConversations,
    private val deleteConversationUseCase: DeleteConversation
) : ViewModel() {
    private val logger = SecureChatLog.withTag("ChatsViewModel")
    val uiState: StateFlow<ChatsUiState> =
        observeConversations()
            .map { conversations ->
                if (conversations.isEmpty()) {
                    return@map ChatsUiState.Empty
                } else {
                    ChatsUiState.Content(conversations.map { it.toChatListItem() })
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ChatsUiState.Loading
            )

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            deleteConversationUseCase(conversationId)
                .onFailure { error ->
                    logger.error(error) { "Conversation deletion failed" }
                }
        }
    }
}
