package com.cbgm.securechat.domain.usecase

import com.cbgm.securechat.domain.model.PublicIdentity
import com.cbgm.securechat.domain.repository.IdentityRepository

class CreateIdentity(
    private val repository: IdentityRepository
) {
    suspend operator fun invoke(): Result<PublicIdentity> {
        return repository.createIdentity()
    }
}