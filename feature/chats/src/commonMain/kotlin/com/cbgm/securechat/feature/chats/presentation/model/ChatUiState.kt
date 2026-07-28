package com.cbgm.securechat.feature.chats.presentation.model

import com.cbgm.securechat.core.security.DirectIdentitySetupMode
import com.cbgm.securechat.feature.chats.domain.model.ChatMessage
import com.cbgm.securechat.feature.chats.domain.model.ContactSecurityState
import com.cbgm.securechat.feature.chats.domain.model.GroupConversationState
import com.cbgm.securechat.feature.chats.domain.model.GroupMemberInvitationStatus
import com.cbgm.securechat.feature.contacts.domain.model.IdentityHandshakeState

data class GroupMemberProgressUi(
    val displayName: String,
    val status: GroupMemberInvitationStatus
)

data class ChatUiState(
    val contactId: String = "",
    val contactName: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val messageText: String = "",
    val isContactTyping: Boolean = false,
    val typingDisplayName: String = "",
    val contactSecurityState: ContactSecurityState = ContactSecurityState.NO_REMOTE_PUBLIC_KEYS,
    val identityHandshakeState: IdentityHandshakeState? = null,
    val directIdentitySetupMode: DirectIdentitySetupMode = DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING,
    val safetyNumber: String = "",
    val isLoadingContact: Boolean = true,
    val isLoadingSafetyNumber: Boolean = false,
    val isVerifyingIdentity: Boolean = false,
    val errorMessage: String? = null,
    val isGroup: Boolean = false,
    val subtitle: String = "",
    val isMessageInputEnabled: Boolean = true,
    val groupState: GroupConversationState = GroupConversationState.READY,
    val groupMemberCount: Int = 0,
    val groupReadyMemberCount: Int = 0,
    val groupMutuallyVerifiedCount: Int = 0,
    val groupVerifiableMemberCount: Int = 0,
    val groupPendingCount: Int = 0,
    val showGroupInvitationActions: Boolean = false,
    val groupMemberProgress: List<GroupMemberProgressUi> = emptyList()
)
