package com.cbgm.securechat.feature.contacts.domain.identity

interface IdentityExchangeStarter {
    /**
     * Starts the explicit direct-contact invitation handshake when the contact
     * is not already mutual and no unexpired invitation is active.
     *
     * A phone number is enough to route the signed invitation. Remote keys are
     * accepted only through the challenge-bound invitation response.
     */
    suspend fun ensureStarted(contactId: String): Result<Unit>
}
