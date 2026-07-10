package com.cbgm.securechat.domain.repository

import com.cbgm.securechat.domain.model.IdentityStatus
import com.cbgm.securechat.domain.model.PublicIdentity

interface IdentityRepository {

    /**
     * Returns the complete local identity state.
     */
    suspend fun getStatus(): Result<IdentityStatus>

    /**
     * Convenience function returning true only when the identity
     * is complete and ready for use.
     */
    suspend fun hasIdentity(): Result<Boolean>

    /**
     * Creates a new identity.
     *
     * This must fail if complete or partial identity data
     * already exists.
     */
    suspend fun createIdentity(): Result<PublicIdentity>

    /**
     * Loads the public part of the local identity.
     *
     * Private keys are never exposed through this repository method.
     */
    suspend fun getIdentity(): Result<PublicIdentity?>
}