package com.cbgm.securechat.feature.transport.incoming

import com.cbgm.securechat.core.protocol.identity.LocalEncryptionKeyPairProvider
import com.cbgm.securechat.feature.chats.domain.repository.ChatsRepository
import com.cbgm.securechat.feature.transport.relay.identity.ContactByRelayIdResolver
import com.cbgm.securechat.feature.transport.websocket.WebSocketTransportClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DefaultIncomingRelayRunner(
    private val webSocketTransportClient:
    WebSocketTransportClient,

    private val contactByRelayIdResolver:
    ContactByRelayIdResolver,

    private val localEncryptionKeyPairProvider:
    LocalEncryptionKeyPairProvider,

    private val chatsRepository:
    ChatsRepository
) : IncomingRelayRunner {

    private val scope =
        CoroutineScope(
            SupervisorJob() +
                    Dispatchers.Default
        )

    private var collectionJob:
            Job? = null

    override fun start() {
        if (
            collectionJob?.isActive ==
            true
        ) {
            return
        }

        collectionJob =
            scope.launch {
                webSocketTransportClient
                    .incomingEnvelopes
                    .collect { envelope ->
                        processEnvelope(
                            senderRelayId =
                                envelope.senderId,

                            recipientRelayId =
                                envelope.recipientId,

                            encodedTransportPayload =
                                envelope.payload
                        )
                    }
            }
    }

    override fun stop() {
        collectionJob?.cancel()
        collectionJob = null
    }

    private suspend fun processEnvelope(
        senderRelayId: String,
        recipientRelayId: String,
        encodedTransportPayload: String
    ) {
        try {
            val contactId =
                contactByRelayIdResolver
                    .resolveContactId(
                        relayId =
                            senderRelayId
                    )
                    .getOrThrow()
                    ?: run {
                        println(
                            "Incoming envelope ignored: " +
                                    "unknown sender $senderRelayId"
                        )

                        return
                    }

            val keyPair =
                localEncryptionKeyPairProvider
                    .getEncryptionKeyPair()
                    .getOrThrow()

            chatsRepository.receiveMessage(
                contactId =
                    contactId,

                encodedTransportPayload =
                    encodedTransportPayload,

                localEncryptionPublicKey =
                    keyPair.publicKey,

                localEncryptionPrivateKey =
                    keyPair.privateKey
            )

            println(
                "Incoming envelope stored for contact $contactId"
            )
        } catch (
            error: CancellationException
        ) {
            throw error
        } catch (
            error: Throwable
        ) {
            println(
                "Incoming envelope failed: ${error.message}"
            )
        }
    }
}