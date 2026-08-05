package com.cbgm.securechat.server.presence

import com.cbgm.securechat.server.observability.installServerObservability
import com.cbgm.securechat.server.protocol.serverJson
import com.cbgm.securechat.server.security.NodeRequestVerifier
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

internal data class PresenceRuntime(
    val store: PresenceStorage,
    val httpClient: HttpClient,
    val registryUrl: String,
    val requestVerifier: NodeRequestVerifier
)

internal fun createPresenceRuntime(store: PresenceStorage): PresenceRuntime =
    PresenceRuntime(
        store = store,
        httpClient =
            HttpClient(CIO) {
                install(ClientContentNegotiation) {
                    json(serverJson)
                }
            },
        registryUrl =
            System
                .getenv("NODE_REGISTRY_URL")
                ?.takeIf(String::isNotBlank)
                ?: DEFAULT_NODE_REGISTRY_URL,
        requestVerifier = NodeRequestVerifier()
    )

internal fun Application.configurePresenceLifecycle(runtime: PresenceRuntime) {
    monitor.subscribe(ApplicationStopped) {
        runtime.httpClient.close()
        runtime.store.close()
    }
}

internal fun Application.installPresencePlugins(runtime: PresenceRuntime) {
    installServerObservability("presence-directory") {
        runtime.store.routeCount()
        true
    }
    install(ContentNegotiation) {
        json(serverJson)
    }
}

private const val DEFAULT_NODE_REGISTRY_URL = "http://localhost:8090"
