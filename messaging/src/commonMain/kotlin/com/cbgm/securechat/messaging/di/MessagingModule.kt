package com.cbgm.securechat.messaging.di

import com.cbgm.securechat.core.protocol.handler.IncomingMessageHandler
import com.cbgm.securechat.core.protocol.identity.LocalEncryptionKeyPairProvider
import com.cbgm.securechat.core.protocol.outbox.OutboxProcessor
import com.cbgm.securechat.core.protocol.outbox.OutboxRunner
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutbox
import com.cbgm.securechat.core.protocol.transport.OutgoingWireSender
import com.cbgm.securechat.data.database.dao.ContactDao
import com.cbgm.securechat.feature.chats.domain.repository.TypingIndicatorGateway
import com.cbgm.securechat.feature.contacts.domain.repository.ContactRepository
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContact
import com.cbgm.securechat.feature.transport.relay.identity.RelayIdGenerator
import com.cbgm.securechat.feature.transport.websocket.WebSocketTransportClient
import com.cbgm.securechat.messaging.data.relay.DefaultContactByRelayIdResolver
import com.cbgm.securechat.messaging.data.relay.DefaultContactRelayIdResolver
import com.cbgm.securechat.messaging.data.typing.RelayTypingIndicatorGateway
import com.cbgm.securechat.messaging.domain.relay.ContactByRelayIdResolver
import com.cbgm.securechat.messaging.domain.relay.ContactRelayIdResolver
import com.cbgm.securechat.messaging.runtime.incoming.DefaultIncomingRelayRunner
import com.cbgm.securechat.messaging.runtime.incoming.IncomingRelayRunner
import com.cbgm.securechat.messaging.runtime.outbox.DefaultOutboxProcessor
import com.cbgm.securechat.messaging.runtime.outbox.DefaultOutboxRunner
import org.koin.dsl.module

val messagingModule =
    module {
        single<ContactRelayIdResolver> {
            DefaultContactRelayIdResolver(
                getContact = get<GetContact>(),
                contactRelayIdDao = get(),
                relayIdGenerator = get<RelayIdGenerator>()
            )
        }

        single<ContactByRelayIdResolver> {
            DefaultContactByRelayIdResolver(
                contactRepository = get<ContactRepository>(),
                contactDao = get<ContactDao>(),
                contactRelayIdDao = get(),
                relayIdGenerator = get<RelayIdGenerator>()
            )
        }

        single<TypingIndicatorGateway> {
            RelayTypingIndicatorGateway(
                webSocketTransportClient = get<WebSocketTransportClient>(),
                contactRelayIdResolver = get<ContactRelayIdResolver>()
            )
        }

        single<OutboxProcessor> {
            DefaultOutboxProcessor(
                protocolOutbox = get<ProtocolOutbox>(),
                getContact = get<GetContact>(),
                transportMessageCipher = get(),
                transportPayloadCodec = get(),
                packetCodec = get(),
                contactRelayIdResolver = get<ContactRelayIdResolver>(),
                outgoingWireSender = get<OutgoingWireSender>(),
                deliveryStateListener = get()
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
                incomingMessageHandler = get<IncomingMessageHandler>()
            )
        }
    }
