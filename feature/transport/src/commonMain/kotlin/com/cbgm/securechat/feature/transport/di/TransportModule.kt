package com.cbgm.securechat.feature.transport.di

import com.cbgm.securechat.core.protocol.phone.LocalPhoneNumberProvider
import com.cbgm.securechat.core.protocol.phone.PhoneNumberNormalizer
import com.cbgm.securechat.core.protocol.transport.OutgoingWireSender
import com.cbgm.securechat.feature.transport.connection.DefaultRelayConnectionManager
import com.cbgm.securechat.feature.transport.connection.RelayConnectionManager
import com.cbgm.securechat.feature.transport.push.HttpPushTokenRegistrationGateway
import com.cbgm.securechat.feature.transport.push.PushTokenRegistrationGateway
import com.cbgm.securechat.feature.transport.relay.codec.createRelayJson
import com.cbgm.securechat.feature.transport.relay.config.RelayTransportConfig
import com.cbgm.securechat.feature.transport.relay.identity.DefaultLocalRelayIdProvider
import com.cbgm.securechat.feature.transport.relay.identity.LocalRelayIdProvider
import com.cbgm.securechat.feature.transport.relay.identity.RelayIdGenerator
import com.cbgm.securechat.feature.transport.relay.identity.Sha256RelayIdGenerator
import com.cbgm.securechat.feature.transport.relay.inbox.HttpPendingRelayEnvelopeGateway
import com.cbgm.securechat.feature.transport.relay.inbox.PendingRelayEnvelopeGateway
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
            createPlatformHttpClient(
                json = get(qualifier = named(RELAY_JSON_QUALIFIER))
            )
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

        single<PendingRelayEnvelopeGateway> {
            HttpPendingRelayEnvelopeGateway(
                httpClient = get<HttpClient>(),
                relayTransportConfig = get<RelayTransportConfig>()
            )
        }

        single<PushTokenRegistrationGateway> {
            HttpPushTokenRegistrationGateway(
                httpClient = get<HttpClient>(),
                localRelayIdProvider = get<LocalRelayIdProvider>(),
                relayTransportConfig = get<RelayTransportConfig>()
            )
        }

        single<OutgoingWireSender> {
            WebSocketOutgoingWireSender(
                webSocketTransportClient = get<WebSocketTransportClient>(),
                localRelayIdProvider = get<LocalRelayIdProvider>(),
                relayTransportConfig = get<RelayTransportConfig>()
            )
        }
    }

private const val RELAY_JSON_QUALIFIER = "RelayJson"
