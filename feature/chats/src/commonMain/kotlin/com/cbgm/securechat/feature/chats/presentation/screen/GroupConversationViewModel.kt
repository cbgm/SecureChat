package com.cbgm.securechat.feature.chats.presentation.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.feature.chats.domain.model.GroupConversation
import com.cbgm.securechat.feature.chats.domain.repository.ChatsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class GroupConversationViewModel(
    conversationId: String,
    chatsRepository: ChatsRepository,
) : ViewModel() {
    val conversation: StateFlow<GroupConversation?> =
        chatsRepository.observeGroupConversation(conversationId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )
}
