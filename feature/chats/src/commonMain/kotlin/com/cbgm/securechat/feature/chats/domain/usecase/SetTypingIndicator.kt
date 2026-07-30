package com.cbgm.securechat.feature.chats.domain.usecase

import com.cbgm.securechat.feature.chats.domain.repository.TypingIndicatorGateway

class SetTypingIndicator(
    private val gateway: TypingIndicatorGateway
) {
    suspend operator fun invoke(
        contactId: String,
        isTyping: Boolean
    ): Result<Unit> =
        gateway.sendTypingState(
            contactId = contactId,
            isTyping = isTyping
        )
}
