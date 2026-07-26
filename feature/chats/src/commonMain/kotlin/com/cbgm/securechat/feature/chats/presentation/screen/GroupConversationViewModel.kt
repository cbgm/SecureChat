package com.cbgm.securechat.feature.chats.presentation.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.feature.chats.domain.model.Conversation
import com.cbgm.securechat.feature.chats.domain.usecase.MarkConversationRead
import com.cbgm.securechat.feature.chats.domain.usecase.ObserveConversation
import com.cbgm.securechat.feature.chats.domain.usecase.ObserveTypingIndicator
import com.cbgm.securechat.feature.chats.domain.usecase.RetryMessage
import com.cbgm.securechat.feature.chats.domain.usecase.SendGroupMessage
import com.cbgm.securechat.feature.chats.domain.usecase.SetTypingIndicator
import com.cbgm.securechat.feature.chats.presentation.model.ChatUiState
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.securechat.feature.contacts.domain.usecase.ObserveContacts
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class GroupConversationViewModel(
    private val conversationId: String,
    observeConversation: ObserveConversation,
    private val sendGroupMessage: SendGroupMessage,
    private val markConversationReadUseCase: MarkConversationRead,
    private val retryMessageUseCase: RetryMessage,
    observeContacts: ObserveContacts,
    private val observeTypingIndicator: ObserveTypingIndicator,
    private val setTypingIndicator: SetTypingIndicator
) : ViewModel() {
    private val messageText = MutableStateFlow("")
    private val errorMessage = MutableStateFlow<String?>(null)
    private val typingContactIds = MutableStateFlow<Set<String>>(emptySet())
    private val participantContactIds = MutableStateFlow<Set<String>>(emptySet())

    private var localTypingStopJob: Job? = null
    private var isLocalTyping = false
    private val typingObserverJobs = mutableMapOf<String, Job>()
    private val remoteTypingTimeoutJobs = mutableMapOf<String, Job>()

    private val conversationFlow: Flow<Conversation?> = observeConversation(conversationId)
    private val contactsFlow: Flow<List<Contact>> = observeContacts()

    val uiState: StateFlow<ChatUiState> =
        combine(
            conversationFlow,
            contactsFlow,
            messageText,
            errorMessage,
            typingContactIds
        ) { conversation, contacts, currentMessageText, currentError, currentTypingContactIds ->
            val contactsById = contacts.associateBy { it.id }
            val messages =
                conversation?.messages.orEmpty().map { message ->
                    val sender = message.senderContactId?.let(contactsById::get)
                    val senderIsInContacts = sender?.deviceContactLinkStatus == DeviceContactLinkStatus.LINKED
                    val senderName = sender.displayNameForChat(senderIsInContacts)

                    message.copy(
                        senderName = senderName,
                        senderIsInContacts = senderIsInContacts
                    )
                }
            val typingDisplayName =
                currentTypingContactIds
                    .mapNotNull(contactsById::get)
                    .map { contact ->
                        val isInContacts = contact.deviceContactLinkStatus == DeviceContactLinkStatus.LINKED
                        contact.displayNameForChat(isInContacts)
                    }.filter(String::isNotBlank)
                    .joinToString(", ")
            val memberCount = conversation?.participantContactIds?.let { it.size + 1 } ?: 0

            ChatUiState(
                contactName = conversation?.contactName.orEmpty(),
                messages = messages.reversed(),
                messageText = currentMessageText,
                isContactTyping = currentTypingContactIds.isNotEmpty(),
                typingDisplayName = typingDisplayName,
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

    init {
        observeParticipants()
    }

    fun onMessageTextChanged(value: String) {
        messageText.value = value
        errorMessage.value = null

        localTypingStopJob?.cancel()
        localTypingStopJob = null

        if (value.isBlank()) {
            stopTyping()
            return
        }

        if (!isLocalTyping) {
            isLocalTyping = true
            sendTypingState(isTyping = true)
        }

        localTypingStopJob =
            viewModelScope.launch {
                delay(LOCAL_TYPING_TIMEOUT_MILLISECONDS.milliseconds)
                stopTypingNow()
            }
    }

    fun stopTyping() {
        localTypingStopJob?.cancel()
        localTypingStopJob = null

        if (!isLocalTyping) return

        isLocalTyping = false
        sendTypingState(isTyping = false)
    }

    fun sendMessage() {
        val text = messageText.value.trim()
        if (text.isEmpty()) return

        messageText.value = ""
        stopTyping()

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

    private fun observeParticipants() {
        viewModelScope.launch {
            conversationFlow
                .map { conversation -> conversation?.participantContactIds.orEmpty().toSet() }
                .distinctUntilChanged()
                .collect { contactIds ->
                    participantContactIds.value = contactIds
                    updateTypingObservers(contactIds)
                }
        }
    }

    private fun updateTypingObservers(contactIds: Set<String>) {
        val removedContactIds = typingObserverJobs.keys - contactIds
        removedContactIds.forEach { contactId ->
            typingObserverJobs.remove(contactId)?.cancel()
            remoteTypingTimeoutJobs.remove(contactId)?.cancel()
            typingContactIds.update { it - contactId }
        }

        (contactIds - typingObserverJobs.keys).forEach { contactId ->
            typingObserverJobs[contactId] =
                viewModelScope.launch {
                    observeTypingIndicator(contactId).collect { isTyping ->
                        remoteTypingTimeoutJobs.remove(contactId)?.cancel()
                        typingContactIds.update { current ->
                            if (isTyping) current + contactId else current - contactId
                        }

                        if (isTyping) {
                            remoteTypingTimeoutJobs[contactId] =
                                viewModelScope.launch {
                                    delay(REMOTE_TYPING_TIMEOUT_MILLISECONDS.milliseconds)
                                    typingContactIds.update { it - contactId }
                                }
                        }
                    }
                }
        }
    }

    private fun sendTypingState(isTyping: Boolean) {
        viewModelScope.launch { sendTypingStateNow(isTyping) }
    }

    private suspend fun sendTypingStateNow(isTyping: Boolean) {
        participantContactIds.value.forEach { contactId ->
            setTypingIndicator(contactId, isTyping)
                .onFailure { error ->
                    println("Could not send group typing state for $contactId: ${error.message}")
                }
        }
    }

    private suspend fun stopTypingNow() {
        if (!isLocalTyping) return

        isLocalTyping = false
        sendTypingStateNow(isTyping = false)
    }

    private fun Contact?.displayNameForChat(isInContacts: Boolean): String {
        if (this == null) return "Unknown contact"

        return if (isInContacts) {
            displayName?.takeIf(String::isNotBlank)
                ?: preferredPhoneNumber?.value
                ?: "Unknown contact"
        } else {
            preferredPhoneNumber?.value
                ?: displayName?.takeIf(String::isNotBlank)
                ?: "Unknown contact"
        }
    }

    private companion object {
        const val LOCAL_TYPING_TIMEOUT_MILLISECONDS = 1500
        const val REMOTE_TYPING_TIMEOUT_MILLISECONDS = 3000
    }
}
