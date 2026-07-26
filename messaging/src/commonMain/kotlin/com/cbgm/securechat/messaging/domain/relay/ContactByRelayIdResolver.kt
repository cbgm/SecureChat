package com.cbgm.securechat.messaging.domain.relay

interface ContactByRelayIdResolver {
    suspend fun resolveContactId(relayId: String): Result<String?>
}
