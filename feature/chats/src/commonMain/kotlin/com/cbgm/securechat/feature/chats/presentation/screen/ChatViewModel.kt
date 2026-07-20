package com.cbgm.securechat.feature.chats.presentation.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.feature.chats.domain.model.ContactSecurityState
import com.cbgm.securechat.feature.chats.domain.model.Conversation
import com.cbgm.securechat.feature.chats.domain.repository.ChatsRepository
import com.cbgm.securechat.feature.chats.domain.usecase.GetContactSafetyNumber
import com.cbgm.securechat.feature.chats.presentation.model.ChatUiState
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.ContactVerificationStatus
import com.cbgm.securechat.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.securechat.feature.contacts.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.collections.orEmpty

class ChatViewModel(
    private val contactId: String,
    private val fallbackContactName: String,
    private val chatsRepository: ChatsRepository,
    private val contactRepository: ContactRepository,
    private val getContactSafetyNumber: GetContactSafetyNumber
) : ViewModel() {

    private val messageText = MutableStateFlow("")

    private val errorMessage = MutableStateFlow<String?>(null)

    private val safetyNumber = MutableStateFlow("")

    private val isLoadingSafetyNumber = MutableStateFlow(false)

    private val isVerifyingIdentity = MutableStateFlow(false)

    private val contactFlow: Flow<Contact?> = contactRepository
        .observeContacts()
        .map { contacts ->
            contacts.firstOrNull { contact ->
                contact.id == contactId
            }
        }
        .distinctUntilChanged()

    private val conversationFlow: Flow<Conversation?> = chatsRepository
        .observeConversation(
            contactId = contactId
        )

    private val chatContentFlow: Flow<ChatContentState> = combine(
        conversationFlow,
        contactFlow
    ) { conversation, contact ->
        ChatContentState(
            conversation = conversation,
            contact = contact
        )
    }

    private val composerFlow: Flow<ComposerState> = combine(
        messageText,
        errorMessage
    ) { currentMessageText, currentError ->

        ComposerState(
            messageText = currentMessageText,
            errorMessage = currentError
        )
    }

    private val verificationFlow: Flow<VerificationState> = combine(
        safetyNumber,
        isLoadingSafetyNumber,
        isVerifyingIdentity
    ) { currentSafetyNumber,
        loadingSafetyNumber,
        verifyingIdentity ->

        VerificationState(
            safetyNumber = currentSafetyNumber,
            isLoadingSafetyNumber = loadingSafetyNumber,
            isVerifyingIdentity = verifyingIdentity
        )
    }

    private val screenContentFlow: Flow<ScreenContentState> = combine(
        chatContentFlow,
        composerFlow
    ) { chatContent, composer ->

        ScreenContentState(
            chatContent = chatContent,
            composer = composer
        )
    }

    val uiState: StateFlow<ChatUiState> = combine(
        screenContentFlow,
        verificationFlow
    ) { screenContent, verification ->

        val conversation = screenContent.chatContent.conversation

        val contact = screenContent.chatContent.contact

        val composer = screenContent.composer

        ChatUiState(
            contactId = contactId,
            contactName = resolveContactName(
                contact = contact,
                conversation = conversation
            ),
            messages = conversation?.messages?.reversed().orEmpty(),
            messageText = composer.messageText,
            contactSecurityState = contact.toSecurityState(),
            safetyNumber = verification.safetyNumber,
            isLoadingContact = contact == null,
            isLoadingSafetyNumber = verification.isLoadingSafetyNumber,
            isVerifyingIdentity = verification.isVerifyingIdentity,
            errorMessage = composer.errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
        initialValue = ChatUiState(
            contactId = contactId,
            contactName = fallbackContactName
        )
    )

    init {
        viewModelScope.launch {
            chatsRepository.createConversation(contactId = contactId)
        }

        observeContactSecurity()
    }

    fun onMessageTextChanged(value: String) {
        messageText.value = value
        errorMessage.value = null
    }

    fun sendMessage() {
        val normalizedText = messageText.value.trim()

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
                messageText.value = normalizedText
                errorMessage.value = error.message ?: "Message could not be sent"
            }
        }
    }

    fun retryMessage(messageId: String) {
        if (messageId.isBlank()) {
            return
        }

        errorMessage.value = null

        viewModelScope.launch {
            chatsRepository.retryMessage(messageId = messageId)
                .onFailure { error ->
                    errorMessage.value = error.message ?: "Message could not be queued again"
                }
        }
    }

    fun markConversationRead() {
        viewModelScope.launch {
            chatsRepository.markConversationRead(contactId = contactId)
                .onFailure { error ->
                    println(
                        "Could not mark conversation as read: " + error.message
                    )
                }
        }
    }

    fun verifyIdentity() {
        val currentSecurityState = uiState.value.contactSecurityState

        if (currentSecurityState != ContactSecurityState.MUTUAL_KEYS_UNVERIFIED) {
            return
        }

        if (safetyNumber.value.isBlank()) {
            errorMessage.value = "Safety number is unavailable"
            return
        }

        if (isVerifyingIdentity.value) {
            return
        }

        isVerifyingIdentity.value = true

        errorMessage.value = null

        viewModelScope.launch {
            try {
                contactRepository.markVerified(contactId = contactId).getOrThrow()
            } catch (
                error: Throwable
            ) {
                errorMessage.value = error.message ?: "Identity could not be verified"
            } finally {
                isVerifyingIdentity.value = false
            }
        }
    }

    fun refreshSafetyNumber() {
        val currentSecurityState =
            uiState.value.contactSecurityState

        val supportsSafetyNumber =
            currentSecurityState == ContactSecurityState.MUTUAL_KEYS_UNVERIFIED ||
                    currentSecurityState == ContactSecurityState.MUTUAL_KEYS_VERIFIED

        if (!supportsSafetyNumber) {
            safetyNumber.value = ""
            return
        }

        if (isLoadingSafetyNumber.value) {
            return
        }

        isLoadingSafetyNumber.value = true

        viewModelScope.launch {
            try {
                safetyNumber.value = getContactSafetyNumber(contactId = contactId).getOrThrow()
            } catch (
                error: Throwable
            ) {
                safetyNumber.value = ""

                println("Safety number generation failed: " + error.message)
            } finally {
                isLoadingSafetyNumber.value =
                    false
            }
        }
    }

    private fun observeContactSecurity() {
        viewModelScope.launch {
            contactFlow
                .map { contact ->
                    contact.toSecurityState()
                }
                .distinctUntilChanged()
                .collect { securityState ->

                    when (securityState) {
                        ContactSecurityState.MUTUAL_KEYS_UNVERIFIED,
                        ContactSecurityState.MUTUAL_KEYS_VERIFIED -> {
                            refreshSafetyNumber()
                        }

                        ContactSecurityState.NO_REMOTE_PUBLIC_KEYS,
                        ContactSecurityState.ONE_WAY_KEYS -> {
                            safetyNumber.value = ""
                        }
                    }
                }
        }
    }

    private fun resolveContactName(
        contact: Contact?,
        conversation: Conversation?
    ): String {
        return contact
            ?.displayName
            ?.takeIf {
                it.isNotBlank()
            }
            ?: conversation
                ?.contactName
                ?.takeIf {
                    it.isNotBlank()
                }
            ?: fallbackContactName
                .takeIf {
                    it.isNotBlank()
                }
            ?: "Unknown contact"
    }

    private fun Contact?.toSecurityState(): ContactSecurityState {

        val identity = this?.secureChatIdentity ?: return ContactSecurityState.NO_REMOTE_PUBLIC_KEYS

        if (identity.keyExchangeStatus != KeyExchangeStatus.MUTUAL) {
            return ContactSecurityState.ONE_WAY_KEYS
        }

        return if (identity.verificationStatus == ContactVerificationStatus.VERIFIED) {
            ContactSecurityState.MUTUAL_KEYS_VERIFIED
        } else {
            ContactSecurityState.MUTUAL_KEYS_UNVERIFIED
        }
    }

    private data class ChatContentState(
        val conversation: Conversation?,
        val contact: Contact?
    )

    private data class ComposerState(
        val messageText: String,
        val errorMessage: String?
    )

    private data class VerificationState(
        val safetyNumber: String,
        val isLoadingSafetyNumber: Boolean,
        val isVerifyingIdentity: Boolean
    )

    private data class ScreenContentState(
        val chatContent: ChatContentState,
        val composer: ComposerState
    )
}