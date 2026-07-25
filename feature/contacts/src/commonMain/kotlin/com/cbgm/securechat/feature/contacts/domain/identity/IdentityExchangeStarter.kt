package com.cbgm.securechat.feature.contacts.domain.identity

interface IdentityExchangeStarter {
    /**
     * Enqueues the local public identity for the contact when:
     *
     * - key exchange is not already MUTUAL;
     * - this app session has not already queued the exchange.
     *
     * A phone number is enough to address a contact through the relay.
     * Remote public keys are learned from the returned IdentityPacket.
     */
    suspend fun ensureStarted(contactId: String): Result<Unit>
}
