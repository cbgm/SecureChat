package com.cbgm.securechat.feature.chats.presentation.screen.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.feature.chats.domain.model.ContactSecurityState
import com.cbgm.securechat.feature.chats.domain.model.Conversation
import com.cbgm.securechat.feature.chats.domain.usecase.MarkConversationRead
import com.cbgm.securechat.feature.chats.domain.usecase.ObserveConversation
import com.cbgm.securechat.feature.chats.domain.usecase.ObserveTypingIndicator
import com.cbgm.securechat.feature.chats.domain.usecase.RetryMessage
import com.cbgm.securechat.feature.chats.domain.usecase.SendMessage
import com.cbgm.securechat.feature.chats.domain.usecase.SetTypingIndicator
import com.cbgm.securechat.feature.chats.presentation.model.ChatUiState
import com.cbgm.securechat.feature.contacts.domain.identity.IdentityExchangeStarter
import com.cbgm.securechat.feature.contacts.domain.identity.IdentityInvitationService
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.ContactVerificationStatus
import com.cbgm.securechat.feature.contacts.domain.model.IdentityHandshakeState
import com.cbgm.securechat.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContactSafetyNumber
import com.cbgm.securechat.feature.contacts.domain.usecase.ObserveContact
import com.cbgm.securechat.feature.contacts.domain.usecase.VerifyContact
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
import kotlinx.coroutines.launch
import kotlin.collections.orEmpty
import kotlin.time.Duration.Companion.milliseconds

class ChatViewModel(
    private val conversationId: String,
    private val contactId: String,
    private val fallbackContactName: String,
    private val observeConversation: ObserveConversation,
    private val sendMessageUseCase: SendMessage,
    private val markConversationReadUseCase: MarkConversationRead,
    private val retryFailedMessage: RetryMessage,
    private val identityExchangeStarter: IdentityExchangeStarter,
    identityInvitationService: IdentityInvitationService,
    observeContact: ObserveContact,
    private val getContactSafetyNumber: GetContactSafetyNumber,
    private val verifyContact: VerifyContact,
    private val observeTypingIndicator: ObserveTypingIndicator,
    private val setTypingIndicator: SetTypingIndicator
) : ViewModel() {
    private val messageText = MutableStateFlow("")

    private val errorMessage = MutableStateFlow<String?>(null)

    private val safetyNumber = MutableStateFlow("")

    private val isLoadingSafetyNumber = MutableStateFlow(false)

    private val isVerifyingIdentity = MutableStateFlow(false)

    private val isContactTyping = MutableStateFlow(false)

    private var localTypingStopJob: Job? = null

    private var remoteTypingTimeoutJob: Job? = null

    private var isLocalTyping = false

    private val contactFlow: Flow<Contact?> =
        observeContact(contactId = contactId)

    private val conversationFlow: Flow<Conversation?> =
        observeConversation(conversationId)

    private val identityHandshakeStateFlow: Flow<IdentityHandshakeState?> =
        identityInvitationService.observeState(contactId)

    private val chatContentFlow: Flow<ChatContentState> =
        combine(
            conversationFlow,
            contactFlow,
            identityHandshakeStateFlow
        ) { conversation, contact, identityHandshakeState ->
            ChatContentState(
                conversation = conversation,
                contact = contact,
                identityHandshakeState = identityHandshakeState
            )
        }

    private val composerFlow: Flow<ComposerState> =
        combine(
            messageText,
            errorMessage,
            isContactTyping
        ) { currentMessageText, currentError, contactTyping ->

            ComposerState(
                messageText = currentMessageText,
                errorMessage = currentError,
                isContactTyping = contactTyping
            )
        }

    private val verificationFlow: Flow<VerificationState> =
        combine(
            safetyNumber,
            isLoadingSafetyNumber,
            isVerifyingIdentity
        ) {
            currentSafetyNumber,
            loadingSafetyNumber,
            verifyingIdentity
            ->

            VerificationState(
                safetyNumber = currentSafetyNumber,
                isLoadingSafetyNumber = loadingSafetyNumber,
                isVerifyingIdentity = verifyingIdentity
            )
        }

    private val screenContentFlow: Flow<ScreenContentState> =
        combine(
            chatContentFlow,
            composerFlow
        ) { chatContent, composer ->

            ScreenContentState(
                chatContent = chatContent,
                composer = composer
            )
        }

    val uiState: StateFlow<ChatUiState> =
        combine(
            screenContentFlow,
            verificationFlow
        ) { screenContent, verification ->

            val conversation = screenContent.chatContent.conversation

            val contact = screenContent.chatContent.contact

            val composer = screenContent.composer

            ChatUiState(
                contactId = contactId,
                contactName =
                    resolveContactName(
                        contact = contact,
                        conversation = conversation
                    ),
                messages = conversation?.messages?.reversed().orEmpty(),
                messageText = composer.messageText,
                isContactTyping = composer.isContactTyping,
                contactSecurityState = contact.toSecurityState(),
                identityHandshakeState = screenContent.chatContent.identityHandshakeState,
                safetyNumber = verification.safetyNumber,
                isLoadingContact = contact == null,
                isLoadingSafetyNumber = verification.isLoadingSafetyNumber,
                isVerifyingIdentity = verification.isVerifyingIdentity,
                errorMessage = composer.errorMessage
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue =
                ChatUiState(
                    contactId = contactId,
                    contactName = fallbackContactName
                )
        )

    init {
        startIdentityExchange()
        observeContactSecurity()
        observeIncomingTypingEvents()
    }

    private fun startIdentityExchange() {
        viewModelScope.launch {
            identityExchangeStarter
                .ensureStarted(contactId)
                .onFailure { error ->
                    errorMessage.value = error.message ?: "Contact invitation could not be started"
                }
        }
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

        if (!isLocalTyping) {
            return
        }

        isLocalTyping = false
        sendTypingState(isTyping = false)
    }

    fun sendMessage() {
        val normalizedText = messageText.value.trim()

        if (normalizedText.isEmpty()) {
            return
        }

        messageText.value = ""
        errorMessage.value = null
        stopTyping()

        viewModelScope.launch {
            runCatching {
                sendMessageUseCase(
                    conversationId = conversationId,
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
            retryFailedMessage(messageId)
                .onFailure { error ->
                    errorMessage.value = error.message ?: "Message could not be queued again"
                }
        }
    }

    fun markConversationRead() {
        viewModelScope.launch {
            markConversationReadUseCase(conversationId)
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
                verifyContact(contactId = contactId).getOrThrow()
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
                safetyNumber.value =
                    getContactSafetyNumber
                        .invoke(contactId = contactId)
                        .getOrThrow()
                        .singleLine
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

    private fun observeIncomingTypingEvents() {
        viewModelScope.launch {
            observeTypingIndicator(contactId = contactId)
                .collect { isTyping ->
                    remoteTypingTimeoutJob?.cancel()
                    isContactTyping.value = isTyping

                    if (isTyping) {
                        remoteTypingTimeoutJob =
                            viewModelScope.launch {
                                delay(REMOTE_TYPING_TIMEOUT_MILLISECONDS.milliseconds)
                                isContactTyping.value = false
                            }
                    }
                }
        }
    }

    private fun sendTypingState(isTyping: Boolean) {
        viewModelScope.launch {
            sendTypingStateNow(isTyping = isTyping)
        }
    }

    private suspend fun sendTypingStateNow(isTyping: Boolean) {
        setTypingIndicator(
            contactId = contactId,
            isTyping = isTyping
        ).onFailure { error ->
            println(
                "Could not send typing state for $contactId: ${error.message}"
            )
        }
    }

    private suspend fun stopTypingNow() {
        if (!isLocalTyping) {
            return
        }

        isLocalTyping = false
        sendTypingStateNow(isTyping = false)
    }

    private fun observeContactSecurity() {
        viewModelScope.launch {
            contactFlow
                .map { contact ->
                    contact.toSecurityState()
                }.distinctUntilChanged()
                .collect { securityState ->

                    when (securityState) {
                        ContactSecurityState.MUTUAL_KEYS_UNVERIFIED,
                        ContactSecurityState.MUTUAL_KEYS_VERIFIED
                        -> {
                            refreshSafetyNumber()
                        }

                        ContactSecurityState.NO_REMOTE_PUBLIC_KEYS,
                        ContactSecurityState.ONE_WAY_KEYS
                        -> {
                            safetyNumber.value = ""
                        }
                    }
                }
        }
    }

    private fun resolveContactName(
        contact: Contact?,
        conversation: Conversation?
    ): String =
        contact
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
        val contact: Contact?,
        val identityHandshakeState: IdentityHandshakeState?
    )

    private data class ComposerState(
        val messageText: String,
        val errorMessage: String?,
        val isContactTyping: Boolean
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

    private companion object {
        const val LOCAL_TYPING_TIMEOUT_MILLISECONDS = 1500
        const val REMOTE_TYPING_TIMEOUT_MILLISECONDS = 3000
    }
}
