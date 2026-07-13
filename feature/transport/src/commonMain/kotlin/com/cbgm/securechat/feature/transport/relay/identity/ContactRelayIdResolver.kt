package com.cbgm.securechat.feature.transport.relay.identity

interface ContactRelayIdResolver {

    suspend fun resolve(
        contactId: String
    ): Result<String>
}