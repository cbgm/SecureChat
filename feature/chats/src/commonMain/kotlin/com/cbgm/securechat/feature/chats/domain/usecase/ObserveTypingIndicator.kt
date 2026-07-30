package com.cbgm.securechat.feature.chats.domain.usecase

import com.cbgm.securechat.feature.chats.domain.repository.TypingIndicatorGateway
import kotlinx.coroutines.flow.Flow

class ObserveTypingIndicator(
    private val gateway: TypingIndicatorGateway
) {
    operator fun invoke(contactId: String): Flow<Boolean> = gateway.observeTyping(contactId = contactId)
}
