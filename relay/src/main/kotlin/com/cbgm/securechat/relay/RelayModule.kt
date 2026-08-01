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

fun Application.relayModule() {
    install(CallLogging)

    val json: Json = createRelayJson()

    install(ContentNegotiation) {
        json(json)
    }

    install(WebSockets) {
        pingPeriod = 20.seconds
        timeout = 60.seconds
        maxFrameSize = MAX_FRAME_SIZE
        masking = false
    }

    val connectionRegistry: RelayConnectionRegistry = InMemoryRelayConnectionRegistry()
    val pendingEnvelopeStore: PendingEnvelopeStore = InMemoryPendingEnvelopeStore()
    val pushDeviceStore: PushDeviceStore = InMemoryPushDeviceStore()
    val pushWakeUpStore: PushWakeUpStore = InMemoryPushWakeUpStore()

    val pushNotificationSender: PushNotificationSender =
        FirebasePushNotificationSender(
            firebaseMessaging = FirebaseAdminFactory.createMessagingOrNull(),
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

    val handler =
        RelayWebSocketHandler(
            connectionRegistry = connectionRegistry,
            envelopeRouter = envelopeRouter,
            pendingEnvelopeStore = pendingEnvelopeStore,
            pushFallbackScheduler = pushFallbackScheduler,
            json = json
        )

    routing {
        get("/") {
            call.respondText(
                "SecureChat relay is running"
            )
        }

        get("/health") {
            val connectedClients = connectionRegistry.connectedCount()
            val pendingEnvelopes = pendingEnvelopeStore.pendingCount()
            val pushDevices = pushDeviceStore.count()

            call.respondText(
                "ok connectedClients=$connectedClients " +
                    "pendingEnvelopes=$pendingEnvelopes " +
                    "pushDevices=$pushDevices"
            )
        }

        post("/push/devices") {
            val request = call.receive<PushDeviceRegistrationRequest>()

            if (
                request.relayId.isBlank() ||
                request.token.isBlank() ||
                request.platform.isBlank()
            ) {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }

            pushDeviceStore.register(
                PushDevice(
                    relayId = request.relayId,
                    token = request.token,
                    platform = request.platform
                )
            )

            call.respond(HttpStatusCode.NoContent)
        }

        get("/push/wake/{wakeUpId}/inbox") {
            val wakeUpId = call.parameters["wakeUpId"]
            val recipientId =
                wakeUpId
                    ?.takeIf(String::isNotBlank)
                    ?.let { pushWakeUpStore.resolve(it) }

            if (recipientId == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            call.respond(
                PendingRelayEnvelopesResponse(
                    envelopes =
                        pendingEnvelopeStore.getPendingForRecipient(
                            recipientId = recipientId
                        )
                )
            )
        }

        post("/push/wake/{wakeUpId}/inbox/{envelopeId}/ack") {
            val wakeUpId = call.parameters["wakeUpId"]
            val envelopeId = call.parameters["envelopeId"]
            val recipientId =
                wakeUpId
                    ?.takeIf(String::isNotBlank)
                    ?.let { pushWakeUpStore.resolve(it) }

            if (recipientId == null || envelopeId.isNullOrBlank()) {
                call.respond(HttpStatusCode.NotFound)
                return@post
            }

            pendingEnvelopeStore.remove(
                recipientId = recipientId,
                envelopeId = envelopeId
            )

            call.respond(HttpStatusCode.NoContent)
        }

        webSocket("/relay") {
            handler.handle(session = this)
        }
    }
}
