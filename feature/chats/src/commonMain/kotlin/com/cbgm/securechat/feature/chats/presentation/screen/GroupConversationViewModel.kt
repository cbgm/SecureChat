package com.cbgm.securechat.feature.chats.presentation.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.feature.chats.domain.model.ChatMessage
import com.cbgm.securechat.feature.chats.domain.usecase.MarkConversationRead
import com.cbgm.securechat.feature.chats.domain.usecase.ObserveConversation
import com.cbgm.securechat.feature.chats.domain.usecase.RetryMessage
import com.cbgm.securechat.feature.chats.domain.usecase.SendGroupMessage
import com.cbgm.securechat.feature.chats.presentation.model.ChatUiState
import com.cbgm.securechat.feature.contacts.domain.repository.ContactRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GroupConversationViewModel(
    private val conversationId: String,
    observeConversation: ObserveConversation,
    private val sendGroupMessage: SendGroupMessage,
    private val markConversationReadUseCase: MarkConversationRead,
    private val retryMessageUseCase: RetryMessage,
    contactRepository: ContactRepository
) : ViewModel() {
    private val messageText = MutableStateFlow("")
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ChatUiState> =
        combine(
            observeConversation(conversationId),
            contactRepository.observeContacts(),
            messageText,
            errorMessage
        ) { conversation, contacts, currentMessageText, currentError ->
            val namesById = contacts.associate { it.id to it.displayName.orEmpty() }
            val messages =
                conversation?.messages.orEmpty().map { message ->
                    message.copy(senderName = message.senderContactId?.let(namesById::get))
                }
            val memberCount = conversation?.participantContactIds?.let { it.size + 1 } ?: 0

            ChatUiState(
                contactName = conversation?.contactName.orEmpty(),
                messages = messages.reversed(),
                messageText = currentMessageText,
                errorMessage = currentError,
                isLoadingContact = conversation == null,
                isGroup = true,
                subtitle = "$memberCount members"
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ChatUiState(isGroup = true)
        )

    fun onMessageTextChanged(value: String) {
        messageText.value = value
        errorMessage.value = null
    }

    fun sendMessage() {
        val text = messageText.value.trim()
        if (text.isEmpty()) return

        messageText.value = ""
        viewModelScope.launch {
            sendGroupMessage(conversationId, text).onFailure { error ->
                messageText.value = text
                errorMessage.value = error.message ?: "Message could not be sent"
            }
        }
    }

    fun retryMessage(messageId: String) {
        viewModelScope.launch {
            retryMessageUseCase(messageId).onFailure { error ->
                errorMessage.value = error.message ?: "Message could not be queued again"
            }
        }
    }

    fun markConversationRead() {
        viewModelScope.launch { markConversationReadUseCase(conversationId) }
    }
}
