package com.cbgm.securechat.feature.contacts.domain.repository

import com.cbgm.securechat.feature.contacts.domain.model.RemoteIdentityUpdate

enum class RemoteIdentityOrigin {
    LOCAL_IMPORT,
    REMOTE_PACKET
}

interface ContactKeyExchangeStore {
    suspend fun storeRemoteIdentity(
        contactId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray,
        origin: RemoteIdentityOrigin
    ): Result<RemoteIdentityUpdate>

    suspend fun markMutual(
        contactId: String,
        expectedRemoteEncryptionPublicKey: ByteArray,
        expectedRemoteSigningPublicKey: ByteArray
    ): Result<Unit>

    suspend fun resetAllAfterLocalIdentityChange(): Result<Unit>
}
