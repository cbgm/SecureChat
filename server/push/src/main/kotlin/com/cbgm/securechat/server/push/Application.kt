package com.cbgm.securechat.server.push

import com.cbgm.securechat.server.persistence.ServiceEnvironment
import com.cbgm.securechat.server.protocol.PendingRelayEnvelopesResponse
import com.cbgm.securechat.server.protocol.PushDeviceRegistrationRequest
import com.cbgm.securechat.server.protocol.RelayEnvelope
import com.cbgm.securechat.server.protocol.serverJson
import com.cbgm.securechat.server.security.BoundedRateLimiter
import com.cbgm.securechat.server.security.InternalApiAuthentication
import com.cbgm.securechat.server.security.RateLimitPolicy
import com.cbgm.securechat.server.security.enforceRateLimit
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
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
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

fun main() {
    embeddedServer(
        factory = Netty,
        host = "0.0.0.0",
        port = ServiceEnvironment.int("PORT", 8095),
        module = { pushModule() }
    ).start(wait = true)
}

fun Application.pushModule(
    config: PushConfig = PushConfig.fromEnvironment()
) {
    val deviceRegistrationRateLimiter = BoundedRateLimiter(config.deviceRegistrationRateLimit)
    val stores =
        if (config.databaseUrl == null) {
            PushStores.inMemory(config)
        } else {
            createPostgresPushStores(config)
        }
    val devices = stores.devices
    val pendingEnvelopes = stores.pendingEnvelopes
    val wakeUps = stores.wakeUps
    val messaging = FirebasePushSender.createMessagingOrNull()
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val coordinator =
        PushCoordinator(
            pendingEnvelopes = pendingEnvelopes,
            sender = FirebasePushSender(messaging, devices, wakeUps),
            scope = scope
        )
    coordinator.resumePendingNotifications()
    monitor.subscribe(ApplicationStopped) {
        scope.cancel()
        stores.close()
    }

    install(CallLogging)
    install(ContentNegotiation) { json(serverJson) }
    if (config.trustProxyHeaders) {
        install(XForwardedHeaders)
    }

    routing {
        get("/health") {
            call.respondText(
                "ok fcmEnabled=${messaging != null} persistence=${stores.persistenceMode} " +
                    "devices=${devices.count()} " +
                    "pendingEnvelopes=${pendingEnvelopes.count()}"
            )
        }

        post("/push/devices") {
            if (!call.enforceRateLimit(deviceRegistrationRateLimiter)) {
                return@post
            }
            val request = call.receive<PushDeviceRegistrationRequest>()
            if (request.relayId.isBlank() || request.token.isBlank() || request.platform.isBlank()) {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }
            devices.register(PushDevice(request.relayId, request.token, request.platform))
            call.respond(HttpStatusCode.NoContent)
        }

        get("/push/wake/{wakeUpId}/inbox") {
            val recipientId = wakeUps.resolve(call.parameters["wakeUpId"])
            if (recipientId == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                call.respond(PendingRelayEnvelopesResponse(pendingEnvelopes.pending(recipientId)))
            }
        }

        post("/push/wake/{wakeUpId}/inbox/{envelopeId}/ack") {
            val recipientId = wakeUps.resolve(call.parameters["wakeUpId"])
            val envelopeId = call.parameters["envelopeId"]
            if (recipientId == null || envelopeId.isNullOrBlank()) {
                call.respond(HttpStatusCode.NotFound)
                return@post
            }
            pendingEnvelopes.remove(recipientId, envelopeId)
            call.respond(HttpStatusCode.NoContent)
        }

        post("/internal/v1/envelopes") {
            if (!call.hasInternalAccess(config.pushInternalApiToken)) {
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            }
            val accepted = coordinator.accept(call.receive<RelayEnvelope>())
            call.respond(if (accepted) HttpStatusCode.Accepted else HttpStatusCode.InsufficientStorage)
        }

        post("/internal/v1/wake-ups/{recipientId}") {
            if (!call.hasInternalAccess(config.pushInternalApiToken)) {
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            }
            val recipientId = call.parameters["recipientId"]
            if (recipientId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest)
            } else {
                coordinator.notifyRecipient(recipientId)
                call.respond(HttpStatusCode.Accepted)
            }
        }

        get("/internal/v1/recipients/{recipientId}/envelopes") {
            if (!call.hasInternalAccess(config.pushInternalApiToken)) {
                call.respond(HttpStatusCode.Unauthorized)
                return@get
            }
            val recipientId = call.parameters["recipientId"]
            if (recipientId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest)
            } else {
                call.respond(PendingRelayEnvelopesResponse(pendingEnvelopes.pending(recipientId)))
            }
        }

        post("/internal/v1/recipients/{recipientId}/envelopes/{envelopeId}/ack") {
            if (!call.hasInternalAccess(config.pushInternalApiToken)) {
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            }
            val recipientId = call.parameters["recipientId"]
            val envelopeId = call.parameters["envelopeId"]
            if (recipientId.isNullOrBlank() || envelopeId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }
            pendingEnvelopes.remove(recipientId, envelopeId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

data class PushConfig(
    val pushInternalApiToken: String?,
    val databaseUrl: String?,
    val databaseUser: String,
    val databasePassword: String,
    val databaseMaximumPoolSize: Int,
    val maximumEnvelopes: Int,
    val envelopeRetentionMilliseconds: Long,
    val wakeUpLifetimeMilliseconds: Long,
    val deviceRegistrationRateLimit: RateLimitPolicy =
        RateLimitPolicy(
            maximumRequests = DEFAULT_DEVICE_REGISTRATION_RATE_LIMIT_REQUESTS,
            windowMilliseconds = DEFAULT_DEVICE_REGISTRATION_RATE_LIMIT_WINDOW_MILLISECONDS
        ),
    val trustProxyHeaders: Boolean = false
) {
    init {
        require(databaseMaximumPoolSize > 0) {
            "Push database maximum pool size must be positive"
        }
        require(maximumEnvelopes > 0) {
            "Maximum envelope count must be positive"
        }
        require(envelopeRetentionMilliseconds > 0L) {
            "Envelope retention must be positive"
        }
        require(wakeUpLifetimeMilliseconds > 0L) {
            "Wake-up lifetime must be positive"
        }
    }

    companion object {
        fun fromEnvironment(): PushConfig =
            PushConfig(
                pushInternalApiToken =
                    ServiceEnvironment.secret("PUSH_INTERNAL_API_TOKEN")
                        ?: ServiceEnvironment.secret("INTERNAL_API_TOKEN"),
                databaseUrl = System.getenv("PUSH_DATABASE_URL")?.takeIf(String::isNotBlank),
                databaseUser = System.getenv("PUSH_DATABASE_USER").orEmpty(),
                databasePassword = ServiceEnvironment.secret("PUSH_DATABASE_PASSWORD").orEmpty(),
                databaseMaximumPoolSize =
                    System.getenv("PUSH_DATABASE_MAXIMUM_POOL_SIZE")?.toIntOrNull()
                        ?: DEFAULT_DATABASE_MAXIMUM_POOL_SIZE,
                maximumEnvelopes =
                    System.getenv("PUSH_MAXIMUM_ENVELOPES")?.toIntOrNull()
                        ?: DEFAULT_MAXIMUM_ENVELOPES,
                envelopeRetentionMilliseconds =
                    System.getenv("PUSH_ENVELOPE_RETENTION_MILLISECONDS")?.toLongOrNull()
                        ?: DEFAULT_ENVELOPE_RETENTION_MILLISECONDS,
                wakeUpLifetimeMilliseconds =
                    System.getenv("PUSH_WAKE_UP_LIFETIME_MILLISECONDS")?.toLongOrNull()
                        ?: DEFAULT_WAKE_UP_LIFETIME_MILLISECONDS,
                deviceRegistrationRateLimit =
                    RateLimitPolicy(
                        maximumRequests =
                            ServiceEnvironment.int(
                                "PUSH_DEVICE_REGISTRATION_RATE_LIMIT_REQUESTS",
                                DEFAULT_DEVICE_REGISTRATION_RATE_LIMIT_REQUESTS
                            ),
                        windowMilliseconds =
                            ServiceEnvironment.long(
                                "PUSH_DEVICE_REGISTRATION_RATE_LIMIT_WINDOW_MILLISECONDS",
                                DEFAULT_DEVICE_REGISTRATION_RATE_LIMIT_WINDOW_MILLISECONDS
                            ),
                        maximumTrackedClients =
                            ServiceEnvironment.int(
                                "PUSH_RATE_LIMIT_MAXIMUM_TRACKED_CLIENTS",
                                DEFAULT_RATE_LIMIT_MAXIMUM_TRACKED_CLIENTS
                            )
                    ),
                trustProxyHeaders =
                    ServiceEnvironment.string("TRUST_PROXY_HEADERS", "false").toBoolean()
            )

        private const val DEFAULT_DATABASE_MAXIMUM_POOL_SIZE = 10
        private const val DEFAULT_MAXIMUM_ENVELOPES = 100_000
        private const val DEFAULT_ENVELOPE_RETENTION_MILLISECONDS = 7L * 24L * 60L * 60L * 1_000L
        private const val DEFAULT_WAKE_UP_LIFETIME_MILLISECONDS = 15L * 60L * 1_000L
        private const val DEFAULT_DEVICE_REGISTRATION_RATE_LIMIT_REQUESTS = 60
        private const val DEFAULT_DEVICE_REGISTRATION_RATE_LIMIT_WINDOW_MILLISECONDS = 60_000L
        private const val DEFAULT_RATE_LIMIT_MAXIMUM_TRACKED_CLIENTS = 100_000
    }
}

private fun io.ktor.server.application.ApplicationCall.hasInternalAccess(expectedToken: String?): Boolean =
    InternalApiAuthentication.isAuthorized(
        expectedToken,
        request.headers[InternalApiAuthentication.TOKEN_HEADER]
    )
