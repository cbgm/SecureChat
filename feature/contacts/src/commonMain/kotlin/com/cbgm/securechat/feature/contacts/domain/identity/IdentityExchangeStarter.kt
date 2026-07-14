package com.cbgm.securechat.feature.contacts.domain.identity

interface IdentityExchangeStarter {

    /**
     * Enqueues the local public identity for the contact when:
     *
     * - the contact has a remote SecureChat identity;
     * - key exchange is not already MUTUAL;
     * - this app session has not already queued the exchange.
     *
     * Contacts without public keys are ignored because there is no
     * SecureChat relay address available for them yet.
     */
    suspend fun ensureStarted(
        contactId: String
    ): Result<Unit>
}