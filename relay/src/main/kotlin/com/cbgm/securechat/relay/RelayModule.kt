package com.cbgm.securechat.relay

import com.cbgm.securechat.relay.codec.createRelayJson
import com.cbgm.securechat.relay.model.PendingRelayEnvelopesResponse
import com.cbgm.securechat.relay.model.PushDeviceRegistrationRequest
import com.cbgm.securechat.relay.push.FirebaseAdminFactory
import com.cbgm.securechat.relay.push.FirebasePushNotificationSender
import com.cbgm.securechat.relay.push.InMemoryPushDeviceStore
import com.cbgm.securechat.relay.push.InMemoryPushWakeUpStore
import com.cbgm.securechat.relay.push.PushDevice
import com.cbgm.securechat.relay.push.PushDeviceStore
import com.cbgm.securechat.relay.push.PushFallbackScheduler
import com.cbgm.securechat.relay.push.PushNotificationSender
import com.cbgm.securechat.relay.push.PushWakeUpStore
import com.cbgm.securechat.relay.routing.DefaultRelayEnvelopeRouter
import com.cbgm.securechat.relay.routing.RelayEnvelopeRouter
import com.cbgm.securechat.relay.session.InMemoryRelayConnectionRegistry
import com.cbgm.securechat.relay.session.RelayConnectionRegistry
import com.cbgm.securechat.relay.store.InMemoryPendingEnvelopeStore
import com.cbgm.securechat.relay.store.PendingEnvelopeStore
import com.cbgm.securechat.relay.websocket.RelayWebSocketHandler
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

private const val MAX_FRAME_SIZE = 1_048_576L

private val WEB_SOCKET_PING_PERIOD = 20.seconds
private val WEB_SOCKET_TIMEOUT = 60.seconds

fun Application.relayModule() {
    val json = createRelayJson()
    val dependencies = createRelayDependencies(json)

    installRelayPlugins(json)
    configureRelayRouting(dependencies)
}

private fun Application.installRelayPlugins(
    json: Json
) {
    install(CallLogging)

    install(ContentNegotiation) {
        json(json)
    }

    install(WebSockets) {
        pingPeriod = WEB_SOCKET_PING_PERIOD
        timeout = WEB_SOCKET_TIMEOUT
        maxFrameSize = MAX_FRAME_SIZE
        masking = false
    }
}

private fun createRelayDependencies(
    json: Json
): RelayDependencies {
    val connectionRegistry: RelayConnectionRegistry =
        InMemoryRelayConnectionRegistry()

    val pendingEnvelopeStore: PendingEnvelopeStore =
        InMemoryPendingEnvelopeStore()

    val pushDeviceStore: PushDeviceStore =
        InMemoryPushDeviceStore()

    val pushWakeUpStore: PushWakeUpStore =
        InMemoryPushWakeUpStore()

    val pushNotificationSender: PushNotificationSender =
        FirebasePushNotificationSender(
            firebaseMessaging =
                FirebaseAdminFactory.createMessagingOrNull(),
            pushDeviceStore = pushDeviceStore,
            pushWakeUpStore = pushWakeUpStore
        )

    val pushFallbackScheduler =
        PushFallbackScheduler(
            pendingEnvelopeStore = pendingEnvelopeStore,
            pushNotificationSender = pushNotificationSender
        )

    val envelopeRouter: RelayEnvelopeRouter =
        DefaultRelayEnvelopeRouter(
            connectionRegistry = connectionRegistry,
            pendingEnvelopeStore = pendingEnvelopeStore,
            json = json
        )

    val webSocketHandler =
        RelayWebSocketHandler(
            connectionRegistry = connectionRegistry,
            envelopeRouter = envelopeRouter,
            pendingEnvelopeStore = pendingEnvelopeStore,
            pushFallbackScheduler = pushFallbackScheduler,
            json = json
        )

    return RelayDependencies(
        connectionRegistry = connectionRegistry,
        pendingEnvelopeStore = pendingEnvelopeStore,
        pushDeviceStore = pushDeviceStore,
        pushWakeUpStore = pushWakeUpStore,
        webSocketHandler = webSocketHandler
    )
}

private fun Application.configureRelayRouting(
    dependencies: RelayDependencies
) {
    routing {
        registerStatusRoutes(
            connectionRegistry =
                dependencies.connectionRegistry,
            pendingEnvelopeStore =
                dependencies.pendingEnvelopeStore,
            pushDeviceStore =
                dependencies.pushDeviceStore
        )

        registerPushDeviceRoutes(
            pushDeviceStore =
                dependencies.pushDeviceStore
        )

        registerPushWakeUpRoutes(
            pushWakeUpStore =
                dependencies.pushWakeUpStore,
            pendingEnvelopeStore =
                dependencies.pendingEnvelopeStore
        )

        registerRelayWebSocket(
            handler =
                dependencies.webSocketHandler
        )
    }
}

private fun Route.registerStatusRoutes(
    connectionRegistry: RelayConnectionRegistry,
    pendingEnvelopeStore: PendingEnvelopeStore,
    pushDeviceStore: PushDeviceStore
) {
    get("/") {
        call.respondText(
            "SecureChat relay is running"
        )
    }

    get("/health") {
        val connectedClients =
            connectionRegistry.connectedCount()

        val pendingEnvelopes =
            pendingEnvelopeStore.pendingCount()

        val pushDevices =
            pushDeviceStore.count()

        call.respondText(
            buildString {
                append("ok")
                append(" connectedClients=")
                append(connectedClients)
                append(" pendingEnvelopes=")
                append(pendingEnvelopes)
                append(" pushDevices=")
                append(pushDevices)
            }
        )
    }
}

private fun Route.registerPushDeviceRoutes(
    pushDeviceStore: PushDeviceStore
) {
    post("/push/devices") {
        val request =
            call.receive<PushDeviceRegistrationRequest>()

        if (!request.isValid()) {
            call.respond(HttpStatusCode.BadRequest)
            return@post
        }

        pushDeviceStore.register(
            request.toPushDevice()
        )

        call.respond(HttpStatusCode.NoContent)
    }
}

private fun Route.registerPushWakeUpRoutes(
    pushWakeUpStore: PushWakeUpStore,
    pendingEnvelopeStore: PendingEnvelopeStore
) {
    get("/push/wake/{wakeUpId}/inbox") {
        val recipientId =
            pushWakeUpStore.resolveRecipientId(
                wakeUpId =
                    call.parameters["wakeUpId"]
            )

        if (recipientId == null) {
            call.respond(HttpStatusCode.NotFound)
            return@get
        }

        val pendingEnvelopes =
            pendingEnvelopeStore.getPendingForRecipient(
                recipientId = recipientId
            )

        call.respond(
            PendingRelayEnvelopesResponse(
                envelopes = pendingEnvelopes
            )
        )
    }

    post("/push/wake/{wakeUpId}/inbox/{envelopeId}/ack") {
        val recipientId =
            pushWakeUpStore.resolveRecipientId(
                wakeUpId =
                    call.parameters["wakeUpId"]
            )

        val envelopeId =
            call.parameters["envelopeId"]
                ?.takeIf(String::isNotBlank)

        if (recipientId == null || envelopeId == null) {
            call.respond(HttpStatusCode.NotFound)
            return@post
        }

        pendingEnvelopeStore.remove(
            recipientId = recipientId,
            envelopeId = envelopeId
        )

        call.respond(HttpStatusCode.NoContent)
    }
}

private fun Route.registerRelayWebSocket(
    handler: RelayWebSocketHandler
) {
    webSocket("/relay") {
        handler.handle(
            session = this
        )
    }
}

private fun PushDeviceRegistrationRequest.isValid(): Boolean =
    relayId.isNotBlank() &&
        token.isNotBlank() &&
        platform.isNotBlank()

private fun PushDeviceRegistrationRequest.toPushDevice(): PushDevice =
    PushDevice(
        relayId = relayId,
        token = token,
        platform = platform
    )

private suspend fun PushWakeUpStore.resolveRecipientId(
    wakeUpId: String?
): String? =
    wakeUpId
        ?.takeIf(String::isNotBlank)
        ?.let { validWakeUpId ->
            resolve(validWakeUpId)
        }

private data class RelayDependencies(
    val connectionRegistry: RelayConnectionRegistry,
    val pendingEnvelopeStore: PendingEnvelopeStore,
    val pushDeviceStore: PushDeviceStore,
    val pushWakeUpStore: PushWakeUpStore,
    val webSocketHandler: RelayWebSocketHandler
)
