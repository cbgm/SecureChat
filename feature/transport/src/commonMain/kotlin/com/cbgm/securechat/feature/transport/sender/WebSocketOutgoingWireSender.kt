package com.cbgm.securechat.feature.transport.sender

import com.cbgm.securechat.core.id.IdGenerator
import com.cbgm.securechat.core.protocol.transport.OutgoingWireSender
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.feature.transport.relay.config.RelayTransportConfig
import com.cbgm.securechat.feature.transport.relay.identity.LocalRelayIdProvider
import com.cbgm.securechat.feature.transport.relay.model.RelayEnvelope
import com.cbgm.securechat.feature.transport.websocket.WebSocketTransportClient

class WebSocketOutgoingWireSender(
    private val webSocketTransportClient: WebSocketTransportClient,
    private val localRelayIdProvider: LocalRelayIdProvider,
    private val relayTransportConfig: RelayTransportConfig
) : OutgoingWireSender {
    override suspend fun send(
        recipientAddress: String,
        encodedTransportPayload: String
    ): Result<Unit> =
        runCatching {
            require(recipientAddress.isNotBlank()) {
                "Recipient address must not be blank"
            }

            require(
                encodedTransportPayload.isNotBlank()
            ) {
                "Transport payload must not be blank"
            }

            val senderRelayId = localRelayIdProvider.getLocalRelayId().getOrThrow()

            val envelope =
                RelayEnvelope(
                    envelopeId = IdGenerator.generate(),
                    senderId = senderRelayId,
                    recipientId = recipientAddress,
                    payload = encodedTransportPayload,
                    createdAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                )

            webSocketTransportClient
                .sendEnvelopeAndAwaitAcceptance(
                    envelope = envelope,
                    timeoutMilliseconds = relayTransportConfig.acknowledgementTimeoutMilliseconds
                ).getOrThrow()
        }
}
