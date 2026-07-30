package com.cbgm.securechat.feature.transport.websocket

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import java.util.concurrent.TimeUnit

actual fun createPlatformHttpClient(): HttpClient =
    HttpClient(
        OkHttp
    ) {
        install(
            WebSockets
        )

        engine {
            config {
                retryOnConnectionFailure(
                    true
                )

                pingInterval(
                    20L,
                    TimeUnit.SECONDS
                )
            }
        }
    }
