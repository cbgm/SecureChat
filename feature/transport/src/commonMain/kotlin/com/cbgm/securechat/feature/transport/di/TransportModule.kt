package com.cbgm.securechat.feature.transport.di

import com.cbgm.securechat.core.protocol.identity.LocalEncryptionKeyPairProvider
import com.cbgm.securechat.core.protocol.identity.LocalSigningPublicKeyProvider
import com.cbgm.securechat.core.protocol.outbox.OutboxProcessor
import com.cbgm.securechat.core.protocol.outbox.OutboxRunner
import com.cbgm.securechat.core.protocol.transport.OutgoingWireSender
import com.cbgm.securechat.feature.chats.domain.repository.ChatsRepository
import com.cbgm.securechat.feature.transport.connection.DefaultRelayConnectionManager
import com.cbgm.securechat.feature.transport.connection.RelayConnectionManager
import com.cbgm.securechat.feature.transport.domain.ProcessOutbox
import com.cbgm.securechat.feature.transport.incoming.DefaultIncomingRelayRunner
import com.cbgm.securechat.feature.transport.incoming.IncomingRelayRunner
import com.cbgm.securechat.feature.transport.outbox.DefaultOutboxProcessor
import com.cbgm.securechat.feature.transport.outbox.DefaultOutboxRunner
import com.cbgm.securechat.feature.transport.relay.codec.createRelayJson
import com.cbgm.securechat.feature.transport.relay.identity.ContactByRelayIdResolver
import com.cbgm.securechat.feature.transport.relay.identity.ContactRelayIdResolver
import com.cbgm.securechat.feature.transport.relay.identity.DefaultContactByRelayIdResolver
import com.cbgm.securechat.feature.transport.relay.identity.DefaultContactRelayIdResolver
import com.cbgm.securechat.feature.transport.relay.identity.DefaultLocalRelayIdProvider
import com.cbgm.securechat.feature.transport.relay.identity.LocalRelayIdProvider
import com.cbgm.securechat.feature.transport.relay.identity.RelayIdGenerator
import com.cbgm.securechat.feature.transport.relay.identity.Sha256RelayIdGenerator
import com.cbgm.securechat.feature.transport.sender.WebSocketOutgoingWireSender
import com.cbgm.securechat.feature.transport.websocket.DefaultWebSocketTransportClient
import com.cbgm.securechat.feature.transport.websocket.WebSocketTransportClient
import com.cbgm.securechat.feature.transport.websocket.createPlatformHttpClient
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module

val transportModule =
    module {

        single<HttpClient> {
            createPlatformHttpClient()
        }

        single<Json>(
            qualifier =
                named(
                    RELAY_JSON_QUALIFIER
                )
        ) {
            createRelayJson()
        }

        single<RelayIdGenerator> {
            Sha256RelayIdGenerator()
        }

        single<LocalRelayIdProvider> {
            DefaultLocalRelayIdProvider(
                localSigningPublicKeyProvider =
                    get<
                            LocalSigningPublicKeyProvider
                            >(),

                relayIdGenerator =
                    get()
            )
        }

        single<ContactRelayIdResolver> {
            DefaultContactRelayIdResolver(
                getContact =
                    get(),

                relayIdGenerator =
                    get()
            )
        }

        single<WebSocketTransportClient> {
            DefaultWebSocketTransportClient(
                httpClient =
                    get(),

                json =
                    get(
                        qualifier =
                            named(
                                RELAY_JSON_QUALIFIER
                            )
                    )
            )
        }

        single<RelayConnectionManager> {
            DefaultRelayConnectionManager(
                webSocketTransportClient =
                    get(),

                localRelayIdProvider =
                    get(),

                relayTransportConfig =
                    get()
            )
        }

        single<ContactByRelayIdResolver> {
            DefaultContactByRelayIdResolver(
                contactRepository = get(),
                relayIdGenerator = get()
            )
        }

        single<IncomingRelayRunner> {
            DefaultIncomingRelayRunner(
                webSocketTransportClient =
                    get<WebSocketTransportClient>(),

                contactByRelayIdResolver =
                    get<ContactByRelayIdResolver>(),

                localEncryptionKeyPairProvider =
                    get<LocalEncryptionKeyPairProvider>(),

                chatsRepository =
                    get<ChatsRepository>()
            )
        }

        single<OutgoingWireSender> {
            WebSocketOutgoingWireSender(
                webSocketTransportClient =
                    get(),

                localRelayIdProvider =
                    get(),

                contactRelayIdResolver =
                    get(),

                relayTransportConfig =
                    get()
            )
        }

        single<OutboxProcessor> {
            DefaultOutboxProcessor(
                protocolOutbox =
                    get(),

                getContact =
                    get(),

                transportMessageCipher =
                    get(),

                transportPayloadCodec =
                    get(),

                outgoingWireSender =
                    get(),

                deliveryStateListener =
                    get(),

                messageDeliveryStatusDao =
                    get()
            )
        }

        single<OutboxRunner> {
            DefaultOutboxRunner(
                protocolOutbox =
                    get(),

                outboxProcessor =
                    get()
            )
        }

        single {
            ProcessOutbox(
                outboxProcessor =
                    get()
            )
        }
    }

private const val RELAY_JSON_QUALIFIER =
    "RelayJson"