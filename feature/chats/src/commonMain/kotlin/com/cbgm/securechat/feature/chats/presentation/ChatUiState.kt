package com.cbgm.securechat.feature.chats.presentation

import com.cbgm.securechat.feature.chats.domain.model.ChatMessage
import com.cbgm.securechat.feature.chats.domain.model.ContactSecurityState

data class ChatUiState(
    val contactName: String = "Contact",

    val messageText: String = "",

    val messages: List<ChatMessage> = emptyList(),

    val contactSecurityState: ContactSecurityState =
        ContactSecurityState.NO_PUBLIC_KEY,

    val isLoadingContact: Boolean = true,

    val errorMessage: String? = null
) {

    /**
     * End-to-end encryption can be used whenever a complete
     * SecureChat public identity is attached to this contact.
     *
     * Verification changes the trust indicator, not whether the
     * encryption key is technically usable.
     */
    val hasEncryptionPublicKey: Boolean
        get() {
            return contactSecurityState !=
                    ContactSecurityState.NO_PUBLIC_KEY
        }
}