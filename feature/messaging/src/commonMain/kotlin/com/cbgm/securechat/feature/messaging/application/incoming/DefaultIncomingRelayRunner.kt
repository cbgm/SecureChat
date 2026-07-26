package com.cbgm.securechat.feature.messaging.application.incoming

import com.cbgm.securechat.core.protocol.handler.IncomingMessageHandler
import com.cbgm.securechat.core.protocol.identity.LocalEncryptionKeyPairProvider
import com.cbgm.securechat.feature.messaging.domain.relay.ContactByRelayIdResolver
import com.cbgm.securechat.feature.transport.websocket.WebSocketTransportClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DefaultIncomingRelayRunner(
    private val webSocketTransportClient: WebSocketTransportClient,
    private val contactByRelayIdResolver: ContactByRelayIdResolver,
    private val localEncryptionKeyPairProvider: LocalEncryptionKeyPairProvider,
    private val incomingMessageHandler: IncomingMessageHandler
) : IncomingRelayRunner {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var collectionJob: Job? = null

    override fun start() {
        if (collectionJob?.isActive == true) {
            return
        }

        collectionJob =
            scope.launch {
                webSocketTransportClient
                    .incomingEnvelopes
                    .collect { envelope ->
                        processEnvelope(
                            envelopeId = envelope.envelopeId,
                            senderRelayId = envelope.senderId,
                            encodedTransportPayload = envelope.payload
                        )
                    }
            }
    }

    override fun stop() {
        collectionJob?.cancel()
        collectionJob = null
    }

    private suspend fun processEnvelope(
        envelopeId: String,
        senderRelayId: String,
        encodedTransportPayload: String
    ) {
        try {
            val contactId =
                contactByRelayIdResolver
                    .resolveContactId(
                        relayId = senderRelayId
                    ).getOrThrow()
                    ?: run {
                        println("Incoming envelope ignored: " + "unknown sender $senderRelayId")

                        return
                    }

            val keyPair = localEncryptionKeyPairProvider.getEncryptionKeyPair().getOrThrow()

            incomingMessageHandler.handle(
                contactId = contactId,
                encodedTransportPayload = encodedTransportPayload,
                localEncryptionPublicKey = keyPair.publicKey,
                localEncryptionPrivateKey = keyPair.privateKey
            )

            webSocketTransportClient
                .acknowledgeIncomingEnvelope(
                    envelopeId = envelopeId
                ).getOrThrow()

            println(
                "Incoming envelope stored and acknowledged: " +
                    "envelopeId=$envelopeId, contactId=$contactId"
            )
        } catch (
            error: CancellationException
        ) {
            throw error
        } catch (
            error: Throwable
        ) {
            println("Incoming envelope failed: ${error.message}")
        }
    }
}
