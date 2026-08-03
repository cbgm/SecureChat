package com.cbgm.securechat.server.federation

import com.cbgm.securechat.server.persistence.BoundedIdempotencyStore
import com.cbgm.securechat.server.persistence.ServiceEnvironment
import com.cbgm.securechat.server.protocol.EnvelopeAcceptanceState
import com.cbgm.securechat.server.protocol.ErrorResponse
import com.cbgm.securechat.server.protocol.FederatedEnvelope
import com.cbgm.securechat.server.protocol.FederationAcknowledgement
import com.cbgm.securechat.server.protocol.NodeCapability
import com.cbgm.securechat.server.protocol.serverJson
import com.cbgm.securechat.server.security.NodeIdentity
import com.cbgm.securechat.server.security.NodeIdentityStore
import com.cbgm.securechat.server.security.NodeRequestAuthentication
import com.cbgm.securechat.server.security.NodeRequestHeaders
import com.cbgm.securechat.server.security.NodeRequestSigner
import com.cbgm.securechat.server.security.NodeRequestVerifier
import com.cbgm.securechat.server.security.Signatures
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
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.nio.file.Path
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

fun main() {
    val identity =
        NodeIdentityStore(
            Path.of(ServiceEnvironment.string("NODE_IDENTITY_PATH", ".securechat-server/node.identity"))
        ).loadOrCreate()

    embeddedServer(
        factory = Netty,
        host = "0.0.0.0",
        port = ServiceEnvironment.int("PORT", 8093),
        module = { federationModule(identity) }
    ).start(wait = true)
}

fun Application.federationModule(
    identity: NodeIdentity,
    config: FederationConfig = FederationConfig.fromEnvironment(),
    suppliedHttpClient: HttpClient? = null
) {
    val httpClient =
        suppliedHttpClient ?: HttpClient(CIO) {
            install(ClientContentNegotiation) { json(serverJson) }
        }
    if (suppliedHttpClient == null) {
        monitor.subscribe(ApplicationStopped) { httpClient.close() }
    }

    val registry = HttpNodeRegistryClient(httpClient, config.nodeRegistryUrl)
    val localGateway = HttpLocalGatewayClient(httpClient, config.gatewayInternalUrl, config.internalApiToken)
    val router =
        FederationRouter(
            localNodeId = identity.nodeId,
            presenceDirectory = HttpPresenceDirectoryClient(httpClient, config.presenceDirectoryUrl),
            nodeRegistry = registry,
            localGateway = localGateway,
            remoteFederation = HttpRemoteFederationClient(httpClient, NodeRequestSigner(identity)),
            mailbox = HttpMailboxClient(httpClient)
        )
    val verifier = NodeRequestVerifier()
    val incomingIds = BoundedIdempotencyStore(maximumEntries = config.maximumDeduplicationEntries)
    val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    if (config.registerNode) {
        serviceScope.launch {
            NodeRegistrationAgent(
                httpClient = httpClient,
                identity = identity,
                config =
                    NodeRegistrationConfig(
                        registryUrl = config.nodeRegistryUrl,
                        clientEndpoint = config.clientEndpoint,
                        federationEndpoint = config.federationEndpoint,
                        mailboxEndpoint = config.mailboxEndpoint
                    )
            ).run()
        }
    }
    monitor.subscribe(ApplicationStopped) { serviceScope.cancel() }

    install(CallLogging)
    install(ContentNegotiation) { json(serverJson) }

    routing {
        get("/health") {
            call.respondText("ok pending=${router.pendingCount()}")
        }

        get("/v1/federation/capabilities") {
            call.respond(setOf(NodeCapability.FEDERATION))
        }

        post("/internal/v1/outgoing-envelopes") {
            if (!call.hasInternalAccess(config.internalApiToken)) {
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            }
            val acknowledgement = router.route(call.receive<FederatedEnvelope>())
            call.respond(HttpStatusCode.Accepted, acknowledgement)
        }

        post("/v1/federation/envelopes") {
            val body = call.receiveText()
            val envelope =
                runCatching { serverJson.decodeFromString<FederatedEnvelope>(body) }
                    .getOrElse {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_ENVELOPE", "Invalid envelope"))
                        return@post
                    }
            val authentication = call.requestAuthentication()
            val descriptor = authentication?.nodeId?.let { registry.find(it) }
            val authenticated =
                authentication != null &&
                    descriptor != null &&
                    verifier.verify(
                        authentication = authentication,
                        method = "POST",
                        path = "/v1/federation/envelopes",
                        body = body,
                        publicKey = Signatures.decodePublicKey(descriptor.identityPublicKey)
                    )
            if (!authenticated) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("INVALID_NODE_AUTH", "Node authentication failed"))
                return@post
            }

            if (incomingIds.contains(envelope.envelopeId)) {
                call.respond(
                    FederationAcknowledgement(
                        envelope.envelopeId,
                        EnvelopeAcceptanceState.STORED_AT_DESTINATION,
                        duplicate = true
                    )
                )
                return@post
            }

            val acknowledgement = localGateway.deliver(envelope)
            if (acknowledgement.state == EnvelopeAcceptanceState.STORED_AT_DESTINATION) {
                incomingIds.record(envelope.envelopeId, envelope.expiresAtEpochMilliseconds)
            }
            call.respond(HttpStatusCode.Accepted, acknowledgement)
        }
    }
}

data class FederationConfig(
    val nodeRegistryUrl: String,
    val presenceDirectoryUrl: String,
    val gatewayInternalUrl: String,
    val internalApiToken: String?,
    val maximumDeduplicationEntries: Int,
    val registerNode: Boolean,
    val clientEndpoint: String,
    val federationEndpoint: String,
    val mailboxEndpoint: String
) {
    companion object {
        fun fromEnvironment(): FederationConfig =
            FederationConfig(
                nodeRegistryUrl = ServiceEnvironment.string("NODE_REGISTRY_URL", "http://localhost:8090"),
                presenceDirectoryUrl =
                    ServiceEnvironment.string("PRESENCE_DIRECTORY_URL", "http://localhost:8091"),
                gatewayInternalUrl = ServiceEnvironment.string("GATEWAY_INTERNAL_URL", "http://localhost:8094"),
                internalApiToken = System.getenv("INTERNAL_API_TOKEN")?.takeIf(String::isNotBlank),
                maximumDeduplicationEntries =
                    ServiceEnvironment.int("MAX_DEDUPLICATION_ENTRIES", 100_000),
                registerNode = ServiceEnvironment.string("REGISTER_NODE", "true").toBoolean(),
                clientEndpoint = ServiceEnvironment.string("CLIENT_ENDPOINT", "ws://localhost:8094/relay"),
                federationEndpoint = ServiceEnvironment.string("FEDERATION_ENDPOINT", "http://localhost:8093"),
                mailboxEndpoint = ServiceEnvironment.string("MAILBOX_ENDPOINT", "http://localhost:8092")
            )
    }
}

private fun io.ktor.server.application.ApplicationCall.hasInternalAccess(expectedToken: String?): Boolean = expectedToken == null || request.headers[INTERNAL_TOKEN_HEADER] == expectedToken

private fun io.ktor.server.application.ApplicationCall.requestAuthentication(): NodeRequestAuthentication? {
    val nodeId = request.headers[NodeRequestHeaders.NODE_ID] ?: return null
    val timestamp = request.headers[NodeRequestHeaders.TIMESTAMP]?.toLongOrNull() ?: return null
    val nonce = request.headers[NodeRequestHeaders.NONCE] ?: return null
    val signature = request.headers[NodeRequestHeaders.SIGNATURE] ?: return null
    return NodeRequestAuthentication(nodeId, timestamp, nonce, signature)
}
