package com.cbgm.securechat.feature.chats.presentation.model

import com.cbgm.securechat.feature.chats.domain.model.ChatMessage
import com.cbgm.securechat.feature.chats.domain.model.ContactSecurityState

data class ChatUiState(
    val contactId: String = "",
    val contactName: String = "",
    val messages: List<ChatMessage> =
        emptyList(),
    val messageText: String = "",
    val contactSecurityState:
    ContactSecurityState =
        ContactSecurityState
            .NO_REMOTE_PUBLIC_KEYS,
    val safetyNumber: String = "",
    val isLoadingContact: Boolean = true,
    val isLoadingSafetyNumber: Boolean = false,
    val isVerifyingIdentity: Boolean = false,
    val errorMessage: String? = null
) {
    val isEndToEndEncrypted: Boolean
        get() {
            return contactSecurityState ==
                    ContactSecurityState
                        .MUTUAL_KEYS_UNVERIFIED ||
                    contactSecurityState ==
                    ContactSecurityState
                        .MUTUAL_KEYS_VERIFIED
        }
}