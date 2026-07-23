package com.cbgm.securechat.core.protocol.identity

interface LocalPublicIdentityProvider {
    suspend fun getLocalPublicIdentity(): Result<LocalPublicIdentity>
}
