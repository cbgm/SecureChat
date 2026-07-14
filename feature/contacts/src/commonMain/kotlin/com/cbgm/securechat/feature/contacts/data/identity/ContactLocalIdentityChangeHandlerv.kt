package com.cbgm.securechat.feature.contacts.data.identity

import com.cbgm.securechat.core.protocol.identity.LocalIdentityChangeHandler
import com.cbgm.securechat.feature.contacts.domain.repository.ContactKeyExchangeStore

class ContactLocalIdentityChangeHandler(
    private val contactKeyExchangeStore:
    ContactKeyExchangeStore
) : LocalIdentityChangeHandler {

    override suspend fun onLocalIdentityChanged():
            Result<Unit> {

        return contactKeyExchangeStore
            .resetAllAfterLocalIdentityChange()
    }
}