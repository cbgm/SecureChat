package com.cbgm.securechat.feature.chats.domain.usecase

import com.cbgm.securechat.feature.chats.domain.repository.ChatsRepository

class RemoveGroupMember(
    private val repository: ChatsRepository
) {
    suspend operator fun invoke(
        conversationId: String,
        contactId: String
    ): Result<Unit> = repository.removeGroupMember(conversationId, contactId)
}
