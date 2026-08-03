package com.cbgm.securechat.server.registry

import com.cbgm.securechat.server.persistence.ServiceEnvironment
import com.cbgm.securechat.server.protocol.ErrorResponse
import com.cbgm.securechat.server.protocol.NodeDirectory
import com.cbgm.securechat.server.protocol.NodeHeartbeatRequest
import com.cbgm.securechat.server.protocol.NodeRegistrationRequest
import com.cbgm.securechat.server.protocol.serverJson
import com.cbgm.securechat.server.security.NodeIdentity
import com.cbgm.securechat.server.security.NodeIdentityStore
import com.cbgm.securechat.server.security.ProtocolSignatures
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
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
import java.nio.file.Path

fun main() {
    val identity =
        NodeIdentityStore(
            Path.of(ServiceEnvironment.string("REGISTRY_IDENTITY_PATH", ".securechat-server/registry.identity"))
        ).loadOrCreate()

    embeddedServer(
        factory = Netty,
        host = "0.0.0.0",
        port = ServiceEnvironment.int("PORT", 8090),
        module = { nodeRegistryModule(identity) }
    ).start(wait = true)
}

fun Application.nodeRegistryModule(
    identity: NodeIdentity,
    store: NodeRegistryStore = NodeRegistryStore()
) {
    install(CallLogging)
    install(ContentNegotiation) { json(serverJson) }

    routing {
        get("/health") {
            call.respondText("ok nodes=${store.healthyNodes().size}")
        }

        post("/v1/nodes") {
            when (val result = store.register(call.receive<NodeRegistrationRequest>().descriptor)) {
                RegistrationResult.Accepted -> call.respond(HttpStatusCode.Created)
                is RegistrationResult.Rejected ->
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(result.code, "Node registration rejected"))
            }
        }

        post("/v1/nodes/{nodeId}/heartbeat") {
            val heartbeat = call.receive<NodeHeartbeatRequest>()
            if (heartbeat.nodeId != call.parameters["nodeId"]) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("NODE_ID_MISMATCH", "Path and body differ"))
                return@post
            }

            when (val result = store.heartbeat(heartbeat)) {
                RegistrationResult.Accepted -> call.respond(HttpStatusCode.NoContent)
                is RegistrationResult.Rejected ->
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse(result.code, "Heartbeat rejected"))
            }
        }

        get("/v1/nodes") {
            val generatedAt = System.currentTimeMillis()
            val directory =
                NodeDirectory(
                    generatedAtEpochMilliseconds = generatedAt,
                    validUntilEpochMilliseconds = generatedAt + 60_000L,
                    nodes = store.healthyNodes()
                )
            call.respond(ProtocolSignatures.signDirectory(directory, identity))
        }

        get("/v1/nodes/{nodeId}") {
            val descriptor = call.parameters["nodeId"]?.let(store::findHealthy)
            if (descriptor == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                call.respond(descriptor)
            }
        }
    }
}
