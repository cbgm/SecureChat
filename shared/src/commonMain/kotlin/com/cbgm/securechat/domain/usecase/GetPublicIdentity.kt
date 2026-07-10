package com.cbgm.securechat.domain.usecase

import com.cbgm.securechat.domain.model.PublicIdentity
import com.cbgm.securechat.domain.repository.IdentityRepository

/**
 * Loads the public part of the user's local identity.
 *
 * Private keys are never returned here.
 */
class GetPublicIdentity(
    private val repository: IdentityRepository
) {

    suspend operator fun invoke(): Result<PublicIdentity?> {
        return repository.getIdentity()
    }
}