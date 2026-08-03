package com.cbgm.securechat.server.mailbox

import com.cbgm.securechat.server.persistence.ServiceEnvironment
import com.cbgm.securechat.server.protocol.CreateMailboxRequest
import com.cbgm.securechat.server.protocol.EnvelopeAcceptanceState
import com.cbgm.securechat.server.protocol.ErrorResponse
import com.cbgm.securechat.server.protocol.FederationAcknowledgement
import com.cbgm.securechat.server.protocol.MailboxEnvelopeRequest
import com.cbgm.securechat.server.protocol.MailboxEnvelopesResponse
import com.cbgm.securechat.server.protocol.serverJson
import com.cbgm.securechat.server.security.BoundedRateLimiter
import com.cbgm.securechat.server.security.RateLimitPolicy
import com.cbgm.securechat.server.security.enforceRateLimit
import com.cbgm.securechat.server.security.hashedClientAddress
import com.cbgm.securechat.server.security.respondTooManyRequests
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun main() {
    embeddedServer(
        factory = Netty,
        host = "0.0.0.0",
        port = ServiceEnvironment.int("PORT", 8092),
        module = { mailboxModule() }
    ).start(wait = true)
}

fun Application.mailboxModule(
    config: MailboxConfig = MailboxConfig.fromEnvironment(),
    store: MailboxStorage = createMailboxStorage(config),
    pushNotifier: MailboxPushNotifier = MailboxPushNotifier.fromEnvironment()
) {
    val creationRateLimiter = BoundedRateLimiter(config.creationRateLimit)
    monitor.subscribe(ApplicationStopped) {
        store.close()
        pushNotifier.close()
    }

    install(CallLogging)
    install(ContentNegotiation) { json(serverJson) }
    if (config.trustProxyHeaders) {
        install(XForwardedHeaders)
    }

    routing {
        get("/health") {
            call.respondText(
                "ok persistence=${store.persistenceMode} mailboxes=${store.mailboxCount()}"
            )
        }

        post("/v1/mailboxes") {
            if (!call.enforceRateLimit(creationRateLimiter)) {
                return@post
            }
            when (
                val result =
                    store.createWithQuota(
                        request = call.receive<CreateMailboxRequest>(),
                        ownerKeyHash = call.hashedClientAddress(),
                        maximumMailboxes = config.maximumMailboxes,
                        maximumMailboxesPerOwner = config.maximumMailboxesPerClient
                    )
            ) {
                is MailboxCreationResult.Created ->
                    call.respond(HttpStatusCode.Created, result.response)

                MailboxCreationResult.OwnerQuotaExceeded ->
                    call.respondTooManyRequests(
                        retryAfterSeconds =
                            (config.creationRateLimit.windowMilliseconds / 1_000L)
                                .coerceAtLeast(1L),
                        code = "MAILBOX_CLIENT_QUOTA_EXCEEDED",
                        message = "Active mailbox quota exceeded"
                    )

                MailboxCreationResult.GlobalQuotaExceeded ->
                    call.respond(
                        HttpStatusCode.InsufficientStorage,
                        ErrorResponse(
                            code = "MAILBOX_GLOBAL_QUOTA_EXCEEDED",
                            message = "Mailbox service capacity exhausted"
                        )
                    )
            }
        }

        post("/v1/mailboxes/{mailboxId}/envelopes") {
            val mailboxId = call.parameters["mailboxId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val capability = call.bearerCapability() ?: return@post call.respond(HttpStatusCode.Unauthorized)
            val request = call.receive<MailboxEnvelopeRequest>()

            when (val result = store.store(mailboxId, capability, request.envelope)) {
                is MailboxResult.Stored ->
                    {
                        if (!result.duplicate) {
                            runCatching {
                                pushNotifier.notify(request.envelope.recipientDeviceRoutingId)
                            }
                        }
                        call.respond(
                            HttpStatusCode.Accepted,
                            FederationAcknowledgement(
                                envelopeId = request.envelope.envelopeId,
                                state = EnvelopeAcceptanceState.STORED_AT_DESTINATION,
                                duplicate = result.duplicate
                            )
                        )
                    }

                is MailboxResult.Rejected ->
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(result.code, "Envelope rejected"))
            }
        }

        get("/v1/mailboxes/{mailboxId}/envelopes") {
            val mailboxId = call.parameters["mailboxId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val capability = call.bearerCapability() ?: return@get call.respond(HttpStatusCode.Unauthorized)
            val pending = store.pending(mailboxId, capability) ?: return@get call.respond(HttpStatusCode.Unauthorized)
            call.respond(MailboxEnvelopesResponse(pending))
        }

        delete("/v1/mailboxes/{mailboxId}/envelopes/{envelopeId}") {
            val mailboxId = call.parameters["mailboxId"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            val envelopeId = call.parameters["envelopeId"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            val capability = call.bearerCapability() ?: return@delete call.respond(HttpStatusCode.Unauthorized)
            if (store.acknowledge(mailboxId, capability, envelopeId)) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.Unauthorized)
            }
        }

        delete("/v1/mailboxes/{mailboxId}") {
            val mailboxId =
                call.parameters["mailboxId"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest)
            val capability =
                call.bearerCapability()
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized)
            when (store.revoke(mailboxId, capability)) {
                MailboxRevocationResult.Revoked,
                MailboxRevocationResult.NotFound -> call.respond(HttpStatusCode.NoContent)

                MailboxRevocationResult.Unauthorized -> call.respond(HttpStatusCode.Unauthorized)
            }
        }
    }
}

data class MailboxConfig(
    val databaseUrl: String?,
    val databaseUser: String,
    val databasePassword: String,
    val databaseMaximumPoolSize: Int,
    val maximumEnvelopeBytes: Int,
    val maximumMailboxBytes: Long,
    val maximumMailboxes: Int = DEFAULT_MAXIMUM_MAILBOXES,
    val maximumMailboxesPerClient: Int = DEFAULT_MAXIMUM_MAILBOXES_PER_CLIENT,
    val creationRateLimit: RateLimitPolicy =
        RateLimitPolicy(
            maximumRequests = DEFAULT_CREATION_RATE_LIMIT_REQUESTS,
            windowMilliseconds = DEFAULT_CREATION_RATE_LIMIT_WINDOW_MILLISECONDS
        ),
    val trustProxyHeaders: Boolean = false
) {
    init {
        require(databaseMaximumPoolSize > 0) {
            "Mailbox database maximum pool size must be positive"
        }
        require(maximumEnvelopeBytes > 0) {
            "Maximum envelope bytes must be positive"
        }
        require(maximumMailboxBytes > 0L) {
            "Maximum mailbox bytes must be positive"
        }
        require(maximumMailboxes > 0) { "Maximum mailbox count must be positive" }
        require(maximumMailboxesPerClient > 0) {
            "Per-client mailbox count must be positive"
        }
    }

    companion object {
        fun fromEnvironment(): MailboxConfig =
            MailboxConfig(
                databaseUrl = System.getenv("MAILBOX_DATABASE_URL")?.takeIf(String::isNotBlank),
                databaseUser = System.getenv("MAILBOX_DATABASE_USER").orEmpty(),
                databasePassword = ServiceEnvironment.secret("MAILBOX_DATABASE_PASSWORD").orEmpty(),
                databaseMaximumPoolSize =
                    System.getenv("MAILBOX_DATABASE_MAXIMUM_POOL_SIZE")?.toIntOrNull()
                        ?: DEFAULT_DATABASE_MAXIMUM_POOL_SIZE,
                maximumEnvelopeBytes =
                    System.getenv("MAILBOX_MAXIMUM_ENVELOPE_BYTES")?.toIntOrNull()
                        ?: DEFAULT_MAXIMUM_ENVELOPE_BYTES,
                maximumMailboxBytes =
                    System.getenv("MAILBOX_MAXIMUM_MAILBOX_BYTES")?.toLongOrNull()
                        ?: DEFAULT_MAXIMUM_MAILBOX_BYTES,
                maximumMailboxes =
                    ServiceEnvironment.int("MAILBOX_MAXIMUM_MAILBOXES", DEFAULT_MAXIMUM_MAILBOXES),
                maximumMailboxesPerClient =
                    ServiceEnvironment.int(
                        "MAILBOX_MAXIMUM_MAILBOXES_PER_CLIENT",
                        DEFAULT_MAXIMUM_MAILBOXES_PER_CLIENT
                    ),
                creationRateLimit =
                    RateLimitPolicy(
                        maximumRequests =
                            ServiceEnvironment.int(
                                "MAILBOX_CREATION_RATE_LIMIT_REQUESTS",
                                DEFAULT_CREATION_RATE_LIMIT_REQUESTS
                            ),
                        windowMilliseconds =
                            ServiceEnvironment.long(
                                "MAILBOX_CREATION_RATE_LIMIT_WINDOW_MILLISECONDS",
                                DEFAULT_CREATION_RATE_LIMIT_WINDOW_MILLISECONDS
                            ),
                        maximumTrackedClients =
                            ServiceEnvironment.int(
                                "MAILBOX_RATE_LIMIT_MAXIMUM_TRACKED_CLIENTS",
                                DEFAULT_RATE_LIMIT_MAXIMUM_TRACKED_CLIENTS
                            )
                    ),
                trustProxyHeaders =
                    ServiceEnvironment.string("TRUST_PROXY_HEADERS", "false").toBoolean()
            )

        private const val DEFAULT_DATABASE_MAXIMUM_POOL_SIZE = 10
        private const val DEFAULT_MAXIMUM_ENVELOPE_BYTES = 1_048_576
        private const val DEFAULT_MAXIMUM_MAILBOX_BYTES = 100L * 1_048_576L
        private const val DEFAULT_MAXIMUM_MAILBOXES = 100_000
        private const val DEFAULT_MAXIMUM_MAILBOXES_PER_CLIENT = 100
        private const val DEFAULT_CREATION_RATE_LIMIT_REQUESTS = 30
        private const val DEFAULT_CREATION_RATE_LIMIT_WINDOW_MILLISECONDS = 60L * 60L * 1_000L
        private const val DEFAULT_RATE_LIMIT_MAXIMUM_TRACKED_CLIENTS = 100_000
    }
}

internal fun createMailboxStorage(config: MailboxConfig): MailboxStorage {
    val databaseUrl = config.databaseUrl
    if (databaseUrl == null) {
        return MailboxStore(
            maximumEnvelopeBytes = config.maximumEnvelopeBytes,
            maximumMailboxBytes = config.maximumMailboxBytes
        )
    }

    val database =
        PostgresMailboxDatabase(
            PostgresMailboxDatabaseConfig(
                jdbcUrl = databaseUrl,
                username = config.databaseUser,
                password = config.databasePassword,
                maximumPoolSize = config.databaseMaximumPoolSize
            )
        )
    return PostgresMailboxStore(
        database = database,
        maximumEnvelopeBytes = config.maximumEnvelopeBytes,
        maximumMailboxBytes = config.maximumMailboxBytes
    )
}

private fun ApplicationCall.bearerCapability(): String? =
    request.headers[HttpHeaders.Authorization]
        ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
        ?.substringAfter(' ')
        ?.takeIf(String::isNotBlank)
