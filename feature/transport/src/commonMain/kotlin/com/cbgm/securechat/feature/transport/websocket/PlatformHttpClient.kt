package com.cbgm.securechat.feature.transport.websocket

import io.ktor.client.HttpClient

expect fun createPlatformHttpClient():
        HttpClient