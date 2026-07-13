package com.cbgm.securechat.feature.chats.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.feature.chats.domain.model.ContactSecurityState
import com.cbgm.securechat.feature.chats.domain.repository.ChatsRepository
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.ContactVerificationStatus
import com.cbgm.securechat.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.securechat.feature.contacts.domain.repository.ContactRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(
    private val contactId: String,
    fallbackContactName: String,
    private val chatsRepository: ChatsRepository,
    contactRepository: ContactRepository
) : ViewModel() {

    private val messageText =
        MutableStateFlow("")

    private val errorMessage =
        MutableStateFlow<String?>(null)

    private val conversation =
        chatsRepository.observeConversation(
            contactId = contactId
        )

    private val contact =
        contactRepository
            .observeContacts()
            .map { contacts ->
                contacts.firstOrNull { contact ->
                    contact.id == contactId
                }
            }

    val uiState:
            StateFlow<ChatUiState> =
        combine(
            conversation,
            contact,
            messageText,
            errorMessage
        ) {
                currentConversation,
                currentContact,
                currentMessageText,
                currentErrorMessage ->

            ChatUiState(
                contactName =
                    currentContact
                        ?.displayName
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: fallbackContactName
                            .takeIf {
                                it.isNotBlank()
                            }
                        ?: "Contact",

                messageText =
                    currentMessageText,

                messages =
                    currentConversation
                        ?.messages
                        .orEmpty(),

                contactSecurityState =
                    currentContact
                        .toContactSecurityState(),

                isLoadingContact =
                    false,

                errorMessage =
                    currentErrorMessage
            )
        }
            .stateIn(
                scope = viewModelScope,

                started =
                    SharingStarted.WhileSubscribed(
                        stopTimeoutMillis =
                            5_000
                    ),

                initialValue =
                    ChatUiState(
                        contactName =
                            fallbackContactName
                                .takeIf {
                                    it.isNotBlank()
                                }
                                ?: "Contact",

                        isLoadingContact =
                            true
                    )
            )

    init {
        viewModelScope.launch {
            chatsRepository.createConversation(
                contactId = contactId
            )
        }
    }

    fun onMessageTextChanged(
        value: String
    ) {
        messageText.value = value
        errorMessage.value = null
    }

    fun sendMessage() {
        val normalizedText =
            messageText.value.trim()

        if (normalizedText.isEmpty()) {
            return
        }

        messageText.value = ""
        errorMessage.value = null

        viewModelScope.launch {
            runCatching {
                chatsRepository.sendMessage(
                    contactId = contactId,
                    text = normalizedText
                )
            }.onFailure { error ->
                messageText.value =
                    normalizedText

                errorMessage.value =
                    error.message
                        ?: "Message could not be sent"
            }
        }
    }

    private fun Contact?
            .toContactSecurityState():
            ContactSecurityState {

        val identity =
            this?.secureChatIdentity
                ?: return ContactSecurityState
                    .NO_REMOTE_PUBLIC_KEYS

        if (
            identity.encryptionPublicKey
                .isEmpty()
        ) {
            return ContactSecurityState
                .NO_REMOTE_PUBLIC_KEYS
        }

        if (
            identity.keyExchangeStatus ==
            KeyExchangeStatus.ONE_WAY
        ) {
            return ContactSecurityState
                .ONE_WAY_KEYS
        }

        return when (
            identity.verificationStatus
        ) {
            ContactVerificationStatus.UNVERIFIED -> {
                ContactSecurityState
                    .MUTUAL_KEYS_UNVERIFIED
            }

            ContactVerificationStatus.VERIFIED -> {
                ContactSecurityState
                    .MUTUAL_KEYS_VERIFIED
            }
        }
    }
}