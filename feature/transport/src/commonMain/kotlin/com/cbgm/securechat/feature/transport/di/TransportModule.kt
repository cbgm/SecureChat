package com.cbgm.securechat.feature.transport.di

import com.cbgm.securechat.core.protocol.identity.LocalEncryptionKeyPairProvider
import com.cbgm.securechat.core.protocol.outbox.OutboxProcessor
import com.cbgm.securechat.core.protocol.outbox.OutboxRunner
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutbox
import com.cbgm.securechat.core.protocol.phone.LocalPhoneNumberProvider
import com.cbgm.securechat.core.protocol.phone.PhoneNumberNormalizer
import com.cbgm.securechat.core.protocol.transport.OutgoingWireSender
import com.cbgm.securechat.feature.chats.domain.repository.ChatsRepository
import com.cbgm.securechat.feature.contacts.domain.repository.ContactRepository
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContact
import com.cbgm.securechat.feature.transport.connection.DefaultRelayConnectionManager
import com.cbgm.securechat.feature.transport.connection.RelayConnectionManager
import com.cbgm.securechat.feature.transport.domain.ProcessOutbox
import com.cbgm.securechat.feature.transport.incoming.DefaultIncomingRelayRunner
import com.cbgm.securechat.feature.transport.incoming.IncomingRelayRunner
import com.cbgm.securechat.feature.transport.outbox.DefaultOutboxProcessor
import com.cbgm.securechat.feature.transport.outbox.DefaultOutboxRunner
import com.cbgm.securechat.feature.transport.relay.codec.createRelayJson
import com.cbgm.securechat.feature.transport.relay.config.RelayTransportConfig
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

        single<Json>(qualifier = named(RELAY_JSON_QUALIFIER)) {
            createRelayJson()
        }

        single<RelayIdGenerator> {
            Sha256RelayIdGenerator(phoneNumberNormalizer = get<PhoneNumberNormalizer>())
        }

        single<LocalRelayIdProvider> {
            DefaultLocalRelayIdProvider(
                localPhoneNumberProvider = get<LocalPhoneNumberProvider>(),
                relayIdGenerator = get<RelayIdGenerator>()
            )
        }

        single<ContactRelayIdResolver> {
            DefaultContactRelayIdResolver(
                getContact = get<GetContact>(),
                relayIdGenerator = get<RelayIdGenerator>()
            )
        }

        single<ContactByRelayIdResolver> {
            DefaultContactByRelayIdResolver(
                contactRepository = get<ContactRepository>(),
                relayIdGenerator = get<RelayIdGenerator>()
            )
        }

        single<WebSocketTransportClient> {
            DefaultWebSocketTransportClient(
                httpClient = get<HttpClient>(),
                json = get(qualifier = named(RELAY_JSON_QUALIFIER))
            )
        }

        single<RelayConnectionManager> {
            DefaultRelayConnectionManager(
                webSocketTransportClient = get<WebSocketTransportClient>(),
                localRelayIdProvider = get<LocalRelayIdProvider>(),
                relayTransportConfig = get<RelayTransportConfig>()
            )
        }

        single<OutgoingWireSender> {
            WebSocketOutgoingWireSender(
                webSocketTransportClient = get<WebSocketTransportClient>(),
                localRelayIdProvider = get<LocalRelayIdProvider>(),
                contactRelayIdResolver = get<ContactRelayIdResolver>(),
                relayTransportConfig = get<RelayTransportConfig>()
            )
        }

        single<OutboxProcessor> {
            DefaultOutboxProcessor(
                protocolOutbox = get<ProtocolOutbox>(),
                getContact = get<GetContact>(),
                transportMessageCipher = get(),
                transportPayloadCodec = get(),
                outgoingWireSender = get<OutgoingWireSender>(),
                deliveryStateListener = get(),
                messageDeliveryStatusDao = get()
            )
        }

        single<OutboxRunner> {
            DefaultOutboxRunner(
                protocolOutbox = get<ProtocolOutbox>(),
                outboxProcessor = get<OutboxProcessor>()
            )
        }

        single<IncomingRelayRunner> {
            DefaultIncomingRelayRunner(
                webSocketTransportClient = get<WebSocketTransportClient>(),
                contactByRelayIdResolver = get<ContactByRelayIdResolver>(),
                localEncryptionKeyPairProvider = get<LocalEncryptionKeyPairProvider>(),
                chatsRepository = get<ChatsRepository>()
            )
        }

        single {
            ProcessOutbox(outboxProcessor = get<OutboxProcessor>())
        }
    }

private const val RELAY_JSON_QUALIFIER = "RelayJson"