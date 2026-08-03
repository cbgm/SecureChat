package com.cbgm.securechat.server.mailbox

import com.cbgm.securechat.server.persistence.ServiceEnvironment
import com.cbgm.securechat.server.protocol.CreateMailboxRequest
import com.cbgm.securechat.server.protocol.EnvelopeAcceptanceState
import com.cbgm.securechat.server.protocol.ErrorResponse
import com.cbgm.securechat.server.protocol.FederationAcknowledgement
import com.cbgm.securechat.server.protocol.MailboxEnvelopeRequest
import com.cbgm.securechat.server.protocol.MailboxEnvelopesResponse
import com.cbgm.securechat.server.protocol.serverJson
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
    store: MailboxStorage = createMailboxStorage(MailboxConfig.fromEnvironment()),
    pushNotifier: MailboxPushNotifier = MailboxPushNotifier.fromEnvironment()
) {
    monitor.subscribe(ApplicationStopped) {
        store.close()
        pushNotifier.close()
    }

    install(CallLogging)
    install(ContentNegotiation) { json(serverJson) }

    routing {
        get("/health") {
            call.respondText(
                "ok persistence=${store.persistenceMode} mailboxes=${store.mailboxCount()}"
            )
        }

        post("/v1/mailboxes") {
            call.respond(HttpStatusCode.Created, store.create(call.receive<CreateMailboxRequest>()))
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
    }
}

data class MailboxConfig(
    val databaseUrl: String?,
    val databaseUser: String,
    val databasePassword: String,
    val databaseMaximumPoolSize: Int,
    val maximumEnvelopeBytes: Int,
    val maximumMailboxBytes: Long
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
                        ?: DEFAULT_MAXIMUM_MAILBOX_BYTES
            )

        private const val DEFAULT_DATABASE_MAXIMUM_POOL_SIZE = 10
        private const val DEFAULT_MAXIMUM_ENVELOPE_BYTES = 1_048_576
        private const val DEFAULT_MAXIMUM_MAILBOX_BYTES = 100L * 1_048_576L
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
