package com.cbgm.securechat.server.presence

import com.cbgm.securechat.server.observability.installServerObservability
import com.cbgm.securechat.server.persistence.ServiceEnvironment
import com.cbgm.securechat.server.protocol.ClientRouteRegistration
import com.cbgm.securechat.server.protocol.ErrorResponse
import com.cbgm.securechat.server.protocol.SecureChatNodeDescriptor
import com.cbgm.securechat.server.protocol.serverJson
import com.cbgm.securechat.server.security.NodeRequestAuthentication
import com.cbgm.securechat.server.security.NodeRequestHeaders
import com.cbgm.securechat.server.security.NodeRequestVerifier
import com.cbgm.securechat.server.security.Signatures
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

fun main() {
    embeddedServer(
        factory = Netty,
        host = "0.0.0.0",
        port = System.getenv("PORT")?.toIntOrNull() ?: 8091,
        module = { presenceDirectoryModule() }
    ).start(wait = true)
}

fun Application.presenceDirectoryModule(
    store: PresenceStorage = createPresenceStorage(PresenceConfig.fromEnvironment())
) {
    val httpClient =
        HttpClient(CIO) {
            install(ClientContentNegotiation) { json(serverJson) }
        }
    monitor.subscribe(ApplicationStopped) {
        httpClient.close()
        store.close()
    }
    val registryUrl = System.getenv("NODE_REGISTRY_URL") ?: "http://localhost:8090"
    val requestVerifier = NodeRequestVerifier()

    installServerObservability("presence-directory") {
        store.routeCount()
        true
    }
    install(ContentNegotiation) { json(serverJson) }

    routing {
        get("/health") {
            call.respondText(
                "ok persistence=${store.persistenceMode} routes=${store.routeCount()}"
            )
        }

        put("/v1/routes/{routingId}") {
            val registration = call.receive<ClientRouteRegistration>()
            if (registration.route.routingId != call.parameters["routingId"]) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("ROUTING_ID_MISMATCH", "Path and body differ"))
                return@put
            }

            when (val result = store.register(registration)) {
                PresenceResult.Accepted -> call.respond(HttpStatusCode.NoContent)
                is PresenceResult.Rejected ->
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(result.code, "Route rejected"))
            }
        }

        delete("/v1/routes/{routingId}/{connectionId}") {
            val routingId = call.parameters["routingId"]
            val connectionId = call.parameters["connectionId"]
            if (routingId == null || connectionId == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@delete
            }
            val authentication = call.requestAuthentication()
            val descriptor =
                authentication?.nodeId?.let { nodeId ->
                    val response = httpClient.get("${registryUrl.trimEnd('/')}/v1/nodes/$nodeId")
                    if (response.status == HttpStatusCode.OK) response.body<SecureChatNodeDescriptor>() else null
                }
            val authorized =
                authentication != null &&
                    descriptor != null &&
                    descriptor.nodeId == authentication.nodeId &&
                    descriptor.nodeId ==
                    store
                        .resolve(routingId)
                        .routes
                        .firstOrNull { it.connectionId == connectionId }
                        ?.nodeId &&
                    requestVerifier.verify(
                        authentication = authentication,
                        method = "DELETE",
                        path = "/v1/routes/$routingId/$connectionId",
                        body = "",
                        publicKey = Signatures.decodePublicKey(descriptor.identityPublicKey)
                    )
            if (!authorized) {
                call.respond(HttpStatusCode.Unauthorized)
                return@delete
            }
            store.remove(routingId, connectionId)
            call.respond(HttpStatusCode.NoContent)
        }

        get("/v1/routes/{routingId}") {
            val routingId = call.parameters["routingId"]
            if (routingId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest)
            } else {
                call.respond(store.resolve(routingId))
            }
        }
    }
}

data class PresenceConfig(
    val redisUrl: String?,
    val redisPassword: String?,
    val redisKeyPrefix: String,
    val maximumTtlMilliseconds: Long
) {
    init {
        require(redisKeyPrefix.isNotBlank()) {
            "Presence Redis key prefix must not be blank"
        }
        require(maximumTtlMilliseconds > 0L) {
            "Maximum route TTL must be positive"
        }
    }

    companion object {
        fun fromEnvironment(): PresenceConfig =
            PresenceConfig(
                redisUrl = System.getenv("PRESENCE_REDIS_URL")?.takeIf(String::isNotBlank),
                redisPassword = ServiceEnvironment.secret("PRESENCE_REDIS_PASSWORD"),
                redisKeyPrefix =
                    System.getenv("PRESENCE_REDIS_KEY_PREFIX")?.takeIf(String::isNotBlank)
                        ?: DEFAULT_REDIS_KEY_PREFIX,
                maximumTtlMilliseconds =
                    System.getenv("PRESENCE_MAXIMUM_TTL_MILLISECONDS")?.toLongOrNull()
                        ?: DEFAULT_MAXIMUM_TTL_MILLISECONDS
            )

        private const val DEFAULT_REDIS_KEY_PREFIX = "securechat:presence"
        private const val DEFAULT_MAXIMUM_TTL_MILLISECONDS = 120_000L
    }
}

internal fun createPresenceStorage(config: PresenceConfig): PresenceStorage {
    val redisUrl = config.redisUrl
    return if (redisUrl == null) {
        PresenceStore(maximumTtlMilliseconds = config.maximumTtlMilliseconds)
    } else {
        RedisPresenceStore(
            redisUrl = redisUrl,
            redisPassword = config.redisPassword,
            maximumTtlMilliseconds = config.maximumTtlMilliseconds,
            keyPrefix = config.redisKeyPrefix
        )
    }
}

private fun io.ktor.server.application.ApplicationCall.requestAuthentication(): NodeRequestAuthentication? {
    val nodeId = request.headers[NodeRequestHeaders.NODE_ID] ?: return null
    val timestamp = request.headers[NodeRequestHeaders.TIMESTAMP]?.toLongOrNull() ?: return null
    val nonce = request.headers[NodeRequestHeaders.NONCE] ?: return null
    val signature = request.headers[NodeRequestHeaders.SIGNATURE] ?: return null
    return NodeRequestAuthentication(nodeId, timestamp, nonce, signature)
}
