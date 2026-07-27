package com.cbgm.securechat.feature.contacts.data.identity

import com.cbgm.securechat.feature.contacts.domain.identity.IdentityExchangeStarter
import com.cbgm.securechat.feature.contacts.domain.identity.IdentityInvitationService

class DefaultIdentityExchangeStarter(
    private val identityInvitationService: IdentityInvitationService
) : IdentityExchangeStarter {
    override suspend fun ensureStarted(contactId: String): Result<Unit> = identityInvitationService.start(contactId)
}
