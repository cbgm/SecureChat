package com.cbgm.securechat.feature.transport.relay.identity

import com.cbgm.securechat.core.protocol.identity.LocalSigningPublicKeyProvider

class DefaultLocalRelayIdProvider(
    private val localSigningPublicKeyProvider: LocalSigningPublicKeyProvider,
    private val relayIdGenerator: RelayIdGenerator
) : LocalRelayIdProvider {
    override suspend fun getLocalRelayId(): Result<String> =
        runCatching {
            val signingPublicKey =
                localSigningPublicKeyProvider.getSigningPublicKey().getOrThrow()

            relayIdGenerator
                .deriveFromSigningPublicKey(signingPublicKey = signingPublicKey)
                .getOrThrow()
        }
}
