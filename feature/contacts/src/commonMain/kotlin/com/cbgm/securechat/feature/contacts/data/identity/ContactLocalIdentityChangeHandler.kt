package com.cbgm.securechat.feature.contacts.data.identity

import com.cbgm.securechat.core.protocol.identity.LocalIdentityChangeHandler
import com.cbgm.securechat.core.protocol.mailbox.MailboxCapabilityLifecycle
import com.cbgm.securechat.core.protocol.mailbox.NoOpMailboxCapabilityLifecycle
import com.cbgm.securechat.feature.contacts.domain.repository.ContactKeyExchangeStore

class ContactLocalIdentityChangeHandler(
    private val contactKeyExchangeStore: ContactKeyExchangeStore,
    private val mailboxCapabilityLifecycle: MailboxCapabilityLifecycle =
        NoOpMailboxCapabilityLifecycle
) : LocalIdentityChangeHandler {
    override suspend fun onLocalIdentityChanged(): Result<Unit> =
        runCatching {
            val revocationError = mailboxCapabilityLifecycle.revokeAll().exceptionOrNull()
            contactKeyExchangeStore.resetAllAfterLocalIdentityChange().getOrThrow()
            revocationError?.let { throw it }
        }
}
