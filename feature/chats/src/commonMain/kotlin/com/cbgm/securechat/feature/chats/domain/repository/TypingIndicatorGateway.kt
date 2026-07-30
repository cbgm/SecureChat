package com.cbgm.securechat.feature.chats.domain.repository

import kotlinx.coroutines.flow.Flow

interface TypingIndicatorGateway {
    fun observeTyping(contactId: String): Flow<Boolean>

    suspend fun sendTypingState(
        contactId: String,
        isTyping: Boolean
    ): Result<Unit>
}
