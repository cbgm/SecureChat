package com.cbgm.securechat.feature.transport.relay.identity

import com.cbgm.securechat.core.protocol.phone.LocalPhoneNumberProvider

class DefaultLocalRelayIdProvider(
    private val localPhoneNumberProvider: LocalPhoneNumberProvider,
    private val relayIdGenerator: RelayIdGenerator,
) : LocalRelayIdProvider {
    override suspend fun getLocalRelayId(): Result<String> =
        runCatching {
            val localPhoneNumber = localPhoneNumberProvider.getLocalPhoneNumber().getOrThrow()

            relayIdGenerator.deriveFromPhoneNumber(phoneNumber = localPhoneNumber).getOrThrow()
        }
}
