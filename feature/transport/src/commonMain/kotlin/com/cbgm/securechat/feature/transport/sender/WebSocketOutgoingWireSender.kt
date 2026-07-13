package com.cbgm.securechat.feature.transport.sender

import com.cbgm.securechat.core.protocol.transport.OutgoingWireSender
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.feature.transport.relay.config.RelayTransportConfig
import com.cbgm.securechat.feature.transport.relay.identity.ContactRelayIdResolver
import com.cbgm.securechat.feature.transport.relay.identity.LocalRelayIdProvider
import com.cbgm.securechat.feature.transport.relay.model.RelayEnvelope
import com.cbgm.securechat.feature.transport.websocket.WebSocketTransportClient
import kotlin.random.Random

class WebSocketOutgoingWireSender(
    private val webSocketTransportClient:
    WebSocketTransportClient,

    private val localRelayIdProvider:
    LocalRelayIdProvider,

    private val contactRelayIdResolver:
    ContactRelayIdResolver,

    private val relayTransportConfig:
    RelayTransportConfig
) : OutgoingWireSender {

    override suspend fun send(
        contactId: String,
        encodedTransportPayload: String
    ): Result<Unit> {

        return runCatching {
            require(contactId.isNotBlank()) {
                "Contact ID must not be blank"
            }

            require(
                encodedTransportPayload
                    .isNotBlank()
            ) {
                "Transport payload must not be blank"
            }

            val senderId =
                localRelayIdProvider
                    .getLocalRelayId()
                    .getOrThrow()

            val recipientId =
                contactRelayIdResolver
                    .resolve(
                        contactId = contactId
                    )
                    .getOrThrow()

            val envelope =
                RelayEnvelope(
                    envelopeId =
                        createEnvelopeId(),

                    senderId =
                        senderId,

                    recipientId =
                        recipientId,

                    payload =
                        encodedTransportPayload,

                    createdAtEpochMilliseconds =
                        SystemClock
                            .nowEpochMilliseconds()
                )

            webSocketTransportClient
                .sendEnvelopeAndAwaitAcceptance(
                    envelope =
                        envelope,

                    timeoutMilliseconds =
                        relayTransportConfig
                            .acknowledgementTimeoutMilliseconds
                )
                .getOrThrow()
        }
    }

    private fun createEnvelopeId():
            String {

        val timestamp =
            SystemClock
                .nowEpochMilliseconds()

        val random =
            Random.nextLong()
                .toString()
                .replace(
                    oldValue = "-",
                    newValue = ""
                )

        return "envelope-$timestamp-$random"
    }
}