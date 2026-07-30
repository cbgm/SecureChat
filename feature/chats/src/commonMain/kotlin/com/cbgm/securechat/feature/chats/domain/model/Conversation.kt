package com.cbgm.securechat.feature.chats.domain.model

data class Conversation(
    val id: String,
    val contactId: String,
    val contactName: String,
    val messages: List<ChatMessage>,
    val unreadCount: Int,
    val isGroup: Boolean = false,
    val participantContactIds: List<String> = emptyList(),
    val pendingParticipantCount: Int = 0,
    val isGroupReady: Boolean = true,
    val groupState: GroupConversationState = GroupConversationState.READY,
    val isIncomingGroupInvitation: Boolean = false,
    val groupMemberInvitationStates: List<GroupMemberInvitationState> = emptyList()
) {
    val lastMessage: ChatMessage?
        get() = messages.maxByOrNull { it.timestamp }
}
