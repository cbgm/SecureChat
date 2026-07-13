package com.cbgm.securechat.feature.transport.websocket

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.websocket.WebSockets
import kotlin.time.Duration.Companion.seconds

actual fun createPlatformHttpClient():
        HttpClient {

    return HttpClient(
        Darwin
    ) {
        install(
            WebSockets
        ) {
            pingInterval =
                20.seconds
        }
    }
}