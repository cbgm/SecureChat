package com.cbgm.securechat.domain.usecase

import com.cbgm.securechat.domain.model.IdentityStatus
import com.cbgm.securechat.domain.repository.IdentityRepository

/**
 * Retrieves the current state of the user's local identity.
 *
 * Presentation code uses this use case instead of accessing
 * storage or the repository implementation directly.
 */
class GetIdentityStatus(
    private val repository: IdentityRepository
) {

    suspend operator fun invoke(): Result<IdentityStatus> {
        return repository.getStatus()
    }
}