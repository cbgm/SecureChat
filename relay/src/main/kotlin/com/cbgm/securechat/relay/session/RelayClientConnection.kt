package com.cbgm.securechat.relay.session

import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RelayClientConnection(
    val relayId: String,
    private val session: DefaultWebSocketServerSession
) {
    private val sendMutex = Mutex()

    suspend fun sendText(text: String) {
        sendMutex.withLock {
            session.send(Frame.Text(text))
        }
    }
}
