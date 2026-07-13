package com.cbgm.securechat.feature.transport.relay.identity

interface ContactByRelayIdResolver {

    suspend fun resolveContactId(
        relayId: String
    ): Result<String?>
}