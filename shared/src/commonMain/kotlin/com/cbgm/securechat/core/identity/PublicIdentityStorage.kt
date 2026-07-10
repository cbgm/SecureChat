package com.cbgm.securechat.core.identity

import com.cbgm.securechat.domain.model.PublicIdentity

interface PublicIdentityStorage {

    suspend fun save(
        identity: PublicIdentity
    ): Result<Unit>

    suspend fun load(): Result<PublicIdentity?>

    suspend fun exists(): Result<Boolean>

    /**
     * Deletes the locally stored public identity.
     *
     * Used for rollback and later for explicit identity reset.
     */
    suspend fun delete(): Result<Unit>
}