package com.cbgm.securechat.relay

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    embeddedServer(
        factory = Netty,
        host = "0.0.0.0",
        port = 8080,
        module = {
            relayModule()
        }
    ).start(
        wait = true
    )
}
