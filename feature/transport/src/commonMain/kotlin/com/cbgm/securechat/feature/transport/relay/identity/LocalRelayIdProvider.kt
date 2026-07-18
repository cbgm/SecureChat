package com.cbgm.securechat.feature.transport.relay.identity

interface LocalRelayIdProvider {

    suspend fun getLocalRelayId(): Result<String>
}