package com.cbgm.securechat.server.presence

import com.cbgm.securechat.server.protocol.ClientRouteRegistration
import com.cbgm.securechat.server.protocol.ErrorResponse
import com.cbgm.securechat.server.protocol.SecureChatNodeDescriptor
import com.cbgm.securechat.server.security.Signatures
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.routing

internal fun Application.installPresenceRoutes(runtime: PresenceRuntime) {
    routing {
        installHealthRoute(runtime)
        installRouteRegistration(runtime)
        installRouteRemoval(runtime)
        installRouteResolution(runtime)
    }
}

private fun Route.installHealthRoute(runtime: PresenceRuntime) {
    get("/health") {
        call.respondText(
            "ok persistence=${runtime.store.persistenceMode} " +
                "routes=${runtime.store.routeCount()}"
        )
    }
}

private fun Route.installRouteRegistration(runtime: PresenceRuntime) {
    put("/v1/routes/{routingId}") {
        val registration = call.receive<ClientRouteRegistration>()
        if (registration.route.routingId == call.parameters["routingId"]) {
            call.respondToRegistration(runtime.store.register(registration))
        } else {
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("ROUTING_ID_MISMATCH", "Path and body differ")
            )
        }
    }
}

private fun Route.installRouteRemoval(runtime: PresenceRuntime) {
    delete("/v1/routes/{routingId}/{connectionId}") {
        val routeKey = call.routeKey()
        when {
            routeKey == null ->
                call.respond(HttpStatusCode.BadRequest)

            !call.isAuthorizedRemoval(routeKey, runtime) ->
                call.respond(HttpStatusCode.Unauthorized)

            else -> {
                runtime.store.remove(routeKey.routingId, routeKey.connectionId)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private fun Route.installRouteResolution(runtime: PresenceRuntime) {
    get("/v1/routes/{routingId}") {
        val routingId = call.parameters["routingId"]
        if (routingId.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest)
        } else {
            call.respond(runtime.store.resolve(routingId))
        }
    }
}

private suspend fun ApplicationCall.respondToRegistration(result: PresenceResult) {
    when (result) {
        PresenceResult.Accepted ->
            respond(HttpStatusCode.NoContent)

        is PresenceResult.Rejected ->
            respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(result.code, "Route rejected")
            )
    }
}

private fun ApplicationCall.routeKey(): PresenceRouteKey? {
    val routingId = parameters["routingId"]
    val connectionId = parameters["connectionId"]
    return routingId?.let { resolvedRoutingId ->
        connectionId?.let { resolvedConnectionId ->
            PresenceRouteKey(
                routingId = resolvedRoutingId,
                connectionId = resolvedConnectionId
            )
        }
    }
}

private suspend fun ApplicationCall.isAuthorizedRemoval(
    routeKey: PresenceRouteKey,
    runtime: PresenceRuntime
): Boolean {
    val authentication = requestAuthentication()
    val descriptor =
        authentication?.let { authenticationValue ->
            runtime.fetchNodeDescriptor(authenticationValue.nodeId)
        }

    return if (authentication == null || descriptor == null) {
        false
    } else {
        val registeredNodeId =
            runtime.store
                .resolve(routeKey.routingId)
                .routes
                .firstOrNull { route ->
                    route.connectionId == routeKey.connectionId
                }?.nodeId
        listOf(
            descriptor.nodeId == authentication.nodeId,
            descriptor.nodeId == registeredNodeId,
            runtime.requestVerifier.verify(
                authentication = authentication,
                method = "DELETE",
                path = "/v1/routes/${routeKey.routingId}/${routeKey.connectionId}",
                body = "",
                publicKey = Signatures.decodePublicKey(descriptor.identityPublicKey)
            )
        ).all { condition -> condition }
    }
}

private suspend fun PresenceRuntime.fetchNodeDescriptor(
    nodeId: String
): SecureChatNodeDescriptor? {
    val response =
        httpClient.get(
            "${registryUrl.trimEnd('/')}/v1/nodes/$nodeId"
        )
    return if (response.status == HttpStatusCode.OK) {
        response.body<SecureChatNodeDescriptor>()
    } else {
        null
    }
}

private data class PresenceRouteKey(
    val routingId: String,
    val connectionId: String
)
