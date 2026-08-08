package com.cbgm.securechat.feature.transport.relay.identity

import com.cbgm.securechat.core.protocol.phone.LocalPhoneNumberProvider

class DefaultLocalBootstrapRelayIdProvider(
    private val localPhoneNumberProvider: LocalPhoneNumberProvider,
    private val relayIdGenerator: RelayIdGenerator
) : LocalBootstrapRelayIdProvider {
    override suspend fun getLocalBootstrapRelayId(): Result<String> =
        runCatching {
            val localPhoneNumber = localPhoneNumberProvider.getLocalPhoneNumber().getOrThrow()
            relayIdGenerator.deriveFromPhoneNumber(localPhoneNumber).getOrThrow()
        }
}
