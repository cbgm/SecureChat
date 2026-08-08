package com.cbgm.securechat.feature.transport.relay.identity

interface LocalBootstrapRelayIdProvider {
    suspend fun getLocalBootstrapRelayId(): Result<String>
}
