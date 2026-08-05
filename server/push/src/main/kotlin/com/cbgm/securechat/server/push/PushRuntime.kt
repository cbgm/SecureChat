package com.cbgm.securechat.server.push

import com.cbgm.securechat.server.observability.installServerObservability
import com.cbgm.securechat.server.protocol.serverJson
import com.cbgm.securechat.server.security.BoundedRateLimiter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

internal data class PushRuntime(
    val stores: PushStores,
    val devices: PushDeviceStore,
    val pendingEnvelopes: PendingEnvelopeStore,
    val wakeUps: WakeUpStore,
    val coordinator: PushCoordinator,
    val scope: CoroutineScope,
    val deviceRegistrationRateLimiter: BoundedRateLimiter,
    val fcmEnabled: Boolean
)

internal fun createPushRuntime(config: PushConfig): PushRuntime {
    val stores =
        if (config.databaseUrl == null) {
            PushStores.inMemory(config)
        } else {
            createPostgresPushStores(config)
        }
    val messaging = FirebasePushSender.createMessagingOrNull()
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val coordinator =
        PushCoordinator(
            pendingEnvelopes = stores.pendingEnvelopes,
            sender =
                FirebasePushSender(
                    messaging = messaging,
                    devices = stores.devices,
                    wakeUps = stores.wakeUps
                ),
            scope = scope
        )
    coordinator.resumePendingNotifications()

    return PushRuntime(
        stores = stores,
        devices = stores.devices,
        pendingEnvelopes = stores.pendingEnvelopes,
        wakeUps = stores.wakeUps,
        coordinator = coordinator,
        scope = scope,
        deviceRegistrationRateLimiter =
            BoundedRateLimiter(config.deviceRegistrationRateLimit),
        fcmEnabled = messaging != null
    )
}

internal fun Application.configurePushLifecycle(runtime: PushRuntime) {
    monitor.subscribe(ApplicationStopped) {
        runtime.scope.cancel()
        runtime.stores.close()
    }
}

internal fun Application.installPushPlugins(
    runtime: PushRuntime,
    config: PushConfig
) {
    installServerObservability("push") {
        runtime.devices.count()
        runtime.pendingEnvelopes.count()
        true
    }
    install(ContentNegotiation) {
        json(serverJson)
    }
    if (config.trustProxyHeaders) {
        install(XForwardedHeaders)
    }
}
