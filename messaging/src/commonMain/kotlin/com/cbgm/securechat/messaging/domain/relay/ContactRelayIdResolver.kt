package com.cbgm.securechat.messaging.domain.relay

interface ContactRelayIdResolver {
    suspend fun resolve(contactId: String): Result<String>
}
