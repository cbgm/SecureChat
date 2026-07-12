package com.cbgm.securechat.feature.chats.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.feature.chats.domain.repository.ChatsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class ChatViewModel(
    private val contactId: String,
    private val contactName: String,
    private val chatsRepository: ChatsRepository
) : ViewModel() {

    private val messageText =
        MutableStateFlow("")

    private val conversation =
        chatsRepository.observeConversation(
            contactId = contactId
        )

    val uiState: StateFlow<ChatUiState> =
        combine(
            conversation,
            messageText
        ) { conversation, currentMessageText ->
            ChatUiState(
                contactName = conversation?.contactName
                    ?: contactName,
                messageText = currentMessageText,
                messages = conversation?.messages
                    ?: emptyList()
            )
        }.stateIn(
            scope = viewModelScope,
            started =
                SharingStarted.WhileSubscribed(5_000),
            initialValue = ChatUiState(
                contactName = contactName
            )
        )

    fun onMessageTextChanged(value: String) {
        messageText.value = value
    }

    fun sendMessage() {
        val text = messageText.value.trim()

        if (text.isEmpty()) {
            return
        }

        chatsRepository.sendMessage(
            contactId = contactId,
            contactName = contactName,
            text = text
        )

        messageText.value = ""
    }
}