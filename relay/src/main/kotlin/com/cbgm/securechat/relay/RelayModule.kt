package com.cbgm.securechat.relay

import com.cbgm.securechat.relay.codec.createRelayJson
import com.cbgm.securechat.relay.routing.DefaultRelayEnvelopeRouter
import com.cbgm.securechat.relay.routing.RelayEnvelopeRouter
import com.cbgm.securechat.relay.session.InMemoryRelayConnectionRegistry
import com.cbgm.securechat.relay.session.RelayConnectionRegistry
import com.cbgm.securechat.relay.websocket.RelayWebSocketHandler
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.json.Json

fun Application.relayModule() {
    install(
        CallLogging
    )

    install(
        WebSockets
    ) {
        pingPeriod =
            20.seconds

        timeout =
            60.seconds

        maxFrameSize =
            1_048_576L

        masking =
            false
    }

    val json:
            Json =
        createRelayJson()

    val connectionRegistry:
            RelayConnectionRegistry =
        InMemoryRelayConnectionRegistry()

    val envelopeRouter:
            RelayEnvelopeRouter =
        DefaultRelayEnvelopeRouter(
            connectionRegistry =
                connectionRegistry,

            json =
                json
        )

    val handler =
        RelayWebSocketHandler(
            connectionRegistry =
                connectionRegistry,

            envelopeRouter =
                envelopeRouter,

            json =
                json
        )

    routing {
        get("/") {
            call.respondText(
                "SecureChat relay is running"
            )
        }

        get("/health") {
            val connectedClients =
                connectionRegistry
                    .connectedCount()

            call.respondText(
                "ok connectedClients=$connectedClients"
            )
        }

        webSocket("/relay") {
            handler.handle(
                session =
                    this
            )
        }
    }
}