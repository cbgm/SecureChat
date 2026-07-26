package com.cbgm.securechat.messaging.data.typing

import com.cbgm.securechat.feature.chats.domain.repository.TypingIndicatorGateway
import com.cbgm.securechat.feature.transport.websocket.WebSocketTransportClient
import com.cbgm.securechat.messaging.domain.relay.ContactRelayIdResolver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow

class RelayTypingIndicatorGateway(
    private val webSocketTransportClient: WebSocketTransportClient,
    private val contactRelayIdResolver: ContactRelayIdResolver
) : TypingIndicatorGateway {
    override fun observeTyping(contactId: String): Flow<Boolean> =
        flow {
            val contactRelayId =
                contactRelayIdResolver
                    .resolve(contactId = contactId)
                    .getOrElse {
                        return@flow
                    }

            webSocketTransportClient.incomingTypingEvents
                .filter { event ->
                    event.senderId == contactRelayId
                }.collect { event ->
                    emit(event.isTyping)
                }
        }.distinctUntilChanged()

    override suspend fun sendTypingState(
        contactId: String,
        isTyping: Boolean
    ): Result<Unit> =
        contactRelayIdResolver
            .resolve(contactId = contactId)
            .fold(
                onSuccess = { contactRelayId ->
                    webSocketTransportClient.sendTypingState(
                        recipientId = contactRelayId,
                        isTyping = isTyping
                    )
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
}
