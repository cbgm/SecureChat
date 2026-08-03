package com.cbgm.securechat.server.federation

import com.cbgm.securechat.server.persistence.BoundedIdempotencyStore
import com.cbgm.securechat.server.persistence.ServiceEnvironment
import com.cbgm.securechat.server.protocol.EnvelopeAcceptanceState
import com.cbgm.securechat.server.protocol.ErrorResponse
import com.cbgm.securechat.server.protocol.FederatedEnvelope
import com.cbgm.securechat.server.protocol.FederationAcknowledgement
import com.cbgm.securechat.server.protocol.NodeCapability
import com.cbgm.securechat.server.protocol.serverJson
import com.cbgm.securechat.server.security.InternalApiAuthentication
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
    val localGateway =
        HttpLocalGatewayClient(
            httpClient,
            config.gatewayInternalUrl,
            config.gatewayInternalApiToken
        )
    val outboundQueue = createOutboundEnvelopeStorage(config)
    val router =
        FederationRouter(
            localNodeId = identity.nodeId,
            presenceDirectory = HttpPresenceDirectoryClient(httpClient, config.presenceDirectoryUrl),
            nodeRegistry = registry,
            localGateway = localGateway,
            remoteFederation = HttpRemoteFederationClient(httpClient, NodeRequestSigner(identity)),
            mailbox = HttpMailboxClient(httpClient),
            queue = outboundQueue,
            retryBaseDelayMilliseconds = config.outboundRetryBaseDelayMilliseconds,
            retryMaximumDelayMilliseconds = config.outboundRetryMaximumDelayMilliseconds
        )
    val verifier = NodeRequestVerifier()
    val incomingIds = BoundedIdempotencyStore(maximumEntries = config.maximumDeduplicationEntries)
    val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    serviceScope.launch {
        OutboundEnvelopeRetryAgent(
            router = router,
            pollIntervalMilliseconds = config.outboundRetryPollIntervalMilliseconds,
            batchSize = config.outboundRetryBatchSize
        ).run()
    }
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
    monitor.subscribe(ApplicationStopped) {
        serviceScope.cancel()
        outboundQueue.close()
    }

    install(CallLogging)
    install(ContentNegotiation) { json(serverJson) }

    routing {
        get("/health") {
            call.respondText(
                "ok persistence=${outboundQueue.persistenceMode} pending=${router.pendingCount()}"
            )
        }

        get("/v1/federation/capabilities") {
            call.respond(setOf(NodeCapability.FEDERATION))
        }

        post("/internal/v1/outgoing-envelopes") {
            if (!call.hasInternalAccess(config.federationInternalApiToken)) {
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
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse("INVALID_NODE_AUTH", "Node authentication failed")
                )
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
    val databaseUrl: String?,
    val databaseUser: String,
    val databasePassword: String,
    val databaseMaximumPoolSize: Int,
    val nodeRegistryUrl: String,
    val presenceDirectoryUrl: String,
    val gatewayInternalUrl: String,
    val federationInternalApiToken: String?,
    val gatewayInternalApiToken: String?,
    val maximumDeduplicationEntries: Int,
    val registerNode: Boolean,
    val clientEndpoint: String,
    val federationEndpoint: String,
    val mailboxEndpoint: String,
    val outboundRetryPollIntervalMilliseconds: Long,
    val outboundRetryBaseDelayMilliseconds: Long,
    val outboundRetryMaximumDelayMilliseconds: Long,
    val outboundRetryBatchSize: Int
) {
    init {
        require(databaseMaximumPoolSize > 0)
        require(outboundRetryPollIntervalMilliseconds > 0L)
        require(outboundRetryBaseDelayMilliseconds > 0L)
        require(outboundRetryMaximumDelayMilliseconds >= outboundRetryBaseDelayMilliseconds)
        require(outboundRetryBatchSize > 0)
    }

    companion object {
        fun fromEnvironment(): FederationConfig =
            FederationConfig(
                databaseUrl = System.getenv("FEDERATION_DATABASE_URL")?.takeIf(String::isNotBlank),
                databaseUser = System.getenv("FEDERATION_DATABASE_USER").orEmpty(),
                databasePassword = ServiceEnvironment.secret("FEDERATION_DATABASE_PASSWORD").orEmpty(),
                databaseMaximumPoolSize =
                    ServiceEnvironment.int(
                        "FEDERATION_DATABASE_MAXIMUM_POOL_SIZE",
                        DEFAULT_DATABASE_MAXIMUM_POOL_SIZE
                    ),
                nodeRegistryUrl = ServiceEnvironment.string("NODE_REGISTRY_URL", "http://localhost:8090"),
                presenceDirectoryUrl =
                    ServiceEnvironment.string("PRESENCE_DIRECTORY_URL", "http://localhost:8091"),
                gatewayInternalUrl = ServiceEnvironment.string("GATEWAY_INTERNAL_URL", "http://localhost:8094"),
                federationInternalApiToken =
                    ServiceEnvironment.secret("FEDERATION_INTERNAL_API_TOKEN")
                        ?: ServiceEnvironment.secret("INTERNAL_API_TOKEN"),
                gatewayInternalApiToken =
                    ServiceEnvironment.secret("GATEWAY_INTERNAL_API_TOKEN")
                        ?: ServiceEnvironment.secret("INTERNAL_API_TOKEN"),
                maximumDeduplicationEntries =
                    ServiceEnvironment.int("MAX_DEDUPLICATION_ENTRIES", 100_000),
                registerNode = ServiceEnvironment.string("REGISTER_NODE", "true").toBoolean(),
                clientEndpoint = ServiceEnvironment.string("CLIENT_ENDPOINT", "ws://localhost:8094/relay"),
                federationEndpoint = ServiceEnvironment.string("FEDERATION_ENDPOINT", "http://localhost:8093"),
                mailboxEndpoint = ServiceEnvironment.string("MAILBOX_ENDPOINT", "http://localhost:8092"),
                outboundRetryPollIntervalMilliseconds =
                    ServiceEnvironment.long(
                        "FEDERATION_RETRY_POLL_INTERVAL_MILLISECONDS",
                        DEFAULT_RETRY_POLL_INTERVAL_MILLISECONDS
                    ),
                outboundRetryBaseDelayMilliseconds =
                    ServiceEnvironment.long(
                        "FEDERATION_RETRY_BASE_DELAY_MILLISECONDS",
                        DEFAULT_RETRY_BASE_DELAY_MILLISECONDS
                    ),
                outboundRetryMaximumDelayMilliseconds =
                    ServiceEnvironment.long(
                        "FEDERATION_RETRY_MAXIMUM_DELAY_MILLISECONDS",
                        DEFAULT_RETRY_MAXIMUM_DELAY_MILLISECONDS
                    ),
                outboundRetryBatchSize =
                    ServiceEnvironment.int("FEDERATION_RETRY_BATCH_SIZE", DEFAULT_RETRY_BATCH_SIZE)
            )

        private const val DEFAULT_DATABASE_MAXIMUM_POOL_SIZE = 10
        private const val DEFAULT_RETRY_POLL_INTERVAL_MILLISECONDS = 1_000L
        private const val DEFAULT_RETRY_BASE_DELAY_MILLISECONDS = 5_000L
        private const val DEFAULT_RETRY_MAXIMUM_DELAY_MILLISECONDS = 5L * 60L * 1_000L
        private const val DEFAULT_RETRY_BATCH_SIZE = 100
    }
}

internal fun createOutboundEnvelopeStorage(config: FederationConfig): OutboundEnvelopeStorage {
    val databaseUrl = config.databaseUrl ?: return OutboundEnvelopeQueue()
    val database =
        PostgresOutboundEnvelopeDatabase(
            PostgresOutboundEnvelopeDatabaseConfig(
                jdbcUrl = databaseUrl,
                username = config.databaseUser,
                password = config.databasePassword,
                maximumPoolSize = config.databaseMaximumPoolSize
            )
        )
    return PostgresOutboundEnvelopeStorage(database)
}

private fun io.ktor.server.application.ApplicationCall.hasInternalAccess(expectedToken: String?): Boolean =
    InternalApiAuthentication.isAuthorized(
        expectedToken,
        request.headers[InternalApiAuthentication.TOKEN_HEADER]
    )

private fun io.ktor.server.application.ApplicationCall.requestAuthentication(): NodeRequestAuthentication? {
    val nodeId = request.headers[NodeRequestHeaders.NODE_ID] ?: return null
    val timestamp = request.headers[NodeRequestHeaders.TIMESTAMP]?.toLongOrNull() ?: return null
    val nonce = request.headers[NodeRequestHeaders.NONCE] ?: return null
    val signature = request.headers[NodeRequestHeaders.SIGNATURE] ?: return null
    return NodeRequestAuthentication(nodeId, timestamp, nonce, signature)
}
