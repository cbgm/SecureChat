package com.cbgm.securechat.feature.chats.domain.usecase

import com.cbgm.securechat.feature.chats.domain.repository.ChatsRepository

class LeaveGroup(
    private val repository: ChatsRepository
) {
    suspend operator fun invoke(conversationId: String): Result<Unit> = repository.leaveGroup(conversationId)
}
