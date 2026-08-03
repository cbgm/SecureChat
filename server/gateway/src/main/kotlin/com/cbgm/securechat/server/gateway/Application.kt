package com.cbgm.securechat.server.gateway

import com.cbgm.securechat.server.persistence.ServiceEnvironment
import com.cbgm.securechat.server.protocol.EnvelopeAcceptanceState
import com.cbgm.securechat.server.protocol.FederatedEnvelope
import com.cbgm.securechat.server.protocol.FederationAcknowledgement
import com.cbgm.securechat.server.protocol.GatewayNodeInformation
import com.cbgm.securechat.server.protocol.serverJson
import com.cbgm.securechat.server.security.InternalApiAuthentication
import com.cbgm.securechat.server.security.NodeIdentity
import com.cbgm.securechat.server.security.NodeIdentityStore
import com.cbgm.securechat.server.security.NodeRequestSigner
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import java.nio.file.Path
import kotlin.time.Duration.Companion.seconds
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

fun main() {
    val config = GatewayConfig.fromEnvironment()
    val identity = NodeIdentityStore(Path.of(config.nodeIdentityPath)).loadOrCreate()

    embeddedServer(
        factory = Netty,
        host = "0.0.0.0",
        port = config.port,
        module = { gatewayModule(identity, config) }
    ).start(wait = true)
}

fun Application.gatewayModule(
    identity: NodeIdentity,
    config: GatewayConfig = GatewayConfig.fromEnvironment(),
    suppliedHttpClient: HttpClient? = null
) {
    val httpClient =
        suppliedHttpClient ?: HttpClient(CIO) {
            install(ClientContentNegotiation) {
                json(serverJson)
            }
        }

    if (suppliedHttpClient == null) {
        monitor.subscribe(ApplicationStopped) {
            httpClient.close()
        }
    }

    val registry = ConnectionRegistry()

    val handler =
        GatewayWebSocketHandler(
            nodeId = identity.nodeId,
            connections = registry,
            federation =
                HttpFederationClient(
                    httpClient = httpClient,
                    baseUrl = config.federationInternalUrl,
                    internalToken = config.federationInternalApiToken
                ),
            presence =
                HttpPresenceClient(
                    httpClient = httpClient,
                    baseUrl = config.presenceDirectoryUrl,
                    signer = NodeRequestSigner(identity)
                ),
            legacyPush =
                HttpLegacyPushClient(
                    httpClient = httpClient,
                    baseUrl = config.pushInternalUrl,
                    internalToken = config.pushInternalApiToken
                )
        )

    install(CallLogging)

    install(ContentNegotiation) {
        json(serverJson)
    }

    install(WebSockets) {
        pingPeriod = 20.seconds
        timeout = 60.seconds
        maxFrameSize = config.maximumFrameBytes
        masking = false
    }

    routing {
        get("/health") {
            call.respondText(
                "ok connections=${registry.count()}"
            )
        }

        get("/v1/gateway") {
            call.respond(
                GatewayNodeInformation(
                    nodeId = identity.nodeId,
                    routeLifetimeMilliseconds = config.routeLifetimeMilliseconds,
                    routeRefreshIntervalMilliseconds = config.routeRefreshIntervalMilliseconds
                )
            )
        }

        webSocket("/relay") {
            handler.handle(this)
        }

        post("/internal/v1/envelopes") {
            if (!call.hasInternalAccess(config.gatewayInternalApiToken)) {
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            }

            val envelope =
                call.receive<FederatedEnvelope>()

            val accepted =
                handler.acceptIncoming(envelope)

            if (accepted) {
                call.respond(
                    status = HttpStatusCode.Accepted,
                    message =
                        FederationAcknowledgement(
                            envelopeId = envelope.envelopeId,
                            state = EnvelopeAcceptanceState.STORED_AT_DESTINATION
                        )
                )
            } else {
                call.respond(HttpStatusCode.ServiceUnavailable)
            }
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.hasInternalAccess(
    expectedToken: String?
): Boolean =
    InternalApiAuthentication.isAuthorized(
        expectedToken,
        request.headers[InternalApiAuthentication.TOKEN_HEADER]
    )

data class GatewayConfig(
    val port: Int,
    val nodeIdentityPath: String,
    val federationInternalUrl: String,
    val pushInternalUrl: String,
    val presenceDirectoryUrl: String,
    val federationInternalApiToken: String?,
    val pushInternalApiToken: String?,
    val gatewayInternalApiToken: String?,
    val maximumFrameBytes: Long,
    val routeLifetimeMilliseconds: Long,
    val routeRefreshIntervalMilliseconds: Long
) {
    init {
        require(routeLifetimeMilliseconds > 0L) {
            "Route lifetime must be positive"
        }
        require(routeRefreshIntervalMilliseconds in 1 until routeLifetimeMilliseconds) {
            "Route refresh interval must be positive and shorter than the route lifetime"
        }
    }

    companion object {
        fun fromEnvironment(): GatewayConfig {
            val legacyToken = ServiceEnvironment.secret("INTERNAL_API_TOKEN")
            return GatewayConfig(
                port =
                    System
                        .getenv("PORT")
                        ?.toIntOrNull()
                        ?: 8094,
                nodeIdentityPath =
                    System.getenv("NODE_IDENTITY_PATH")
                        ?: ".securechat-server/node.identity",
                federationInternalUrl =
                    System.getenv("FEDERATION_INTERNAL_URL")
                        ?: "http://localhost:8093",
                pushInternalUrl =
                    System.getenv("PUSH_INTERNAL_URL")
                        ?: "http://localhost:8095",
                presenceDirectoryUrl =
                    System.getenv("PRESENCE_DIRECTORY_URL")
                        ?: "http://localhost:8091",
                federationInternalApiToken =
                    ServiceEnvironment.secret("FEDERATION_INTERNAL_API_TOKEN") ?: legacyToken,
                pushInternalApiToken =
                    ServiceEnvironment.secret("PUSH_INTERNAL_API_TOKEN") ?: legacyToken,
                gatewayInternalApiToken =
                    ServiceEnvironment.secret("GATEWAY_INTERNAL_API_TOKEN") ?: legacyToken,
                maximumFrameBytes =
                    System
                        .getenv("MAX_FRAME_BYTES")
                        ?.toLongOrNull()
                        ?: 1_048_576L,
                routeLifetimeMilliseconds =
                    System
                        .getenv("ROUTE_LIFETIME_MILLISECONDS")
                        ?.toLongOrNull()
                        ?: 90_000L,
                routeRefreshIntervalMilliseconds =
                    System
                        .getenv("ROUTE_REFRESH_INTERVAL_MILLISECONDS")
                        ?.toLongOrNull()
                        ?: 30_000L
            )
        }
    }
}
