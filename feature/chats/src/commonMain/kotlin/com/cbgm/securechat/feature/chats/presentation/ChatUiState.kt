package com.cbgm.securechat.feature.chats.presentation

import com.cbgm.securechat.feature.chats.domain.model.ChatMessage
import com.cbgm.securechat.feature.chats.domain.model.ContactSecurityState

data class ChatUiState(
    val contactName: String = "Contact",
    val messageText: String = "",
    val messages: List<ChatMessage> = emptyList(),

    val contactSecurityState:
    ContactSecurityState =
        ContactSecurityState.NO_REMOTE_PUBLIC_KEYS,

    val isLoadingContact: Boolean = true,
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