package com.cbgm.securechat.feature.contacts.data.identity

import com.cbgm.securechat.core.security.DirectIdentitySetupMode
import com.cbgm.securechat.core.security.DirectIdentitySetupModeRepository
import com.cbgm.securechat.feature.contacts.domain.identity.IdentityExchangeStarter
import com.cbgm.securechat.feature.contacts.domain.identity.IdentityInvitationService

class DefaultIdentityExchangeStarter(
    private val modeRepository: DirectIdentitySetupModeRepository,
    private val identityInvitationService: IdentityInvitationService,
    private val manualIdentityExchangeStarter: ManualIdentityExchangeStarter
) : IdentityExchangeStarter {
    override suspend fun ensureStarted(contactId: String): Result<Unit> =
        when (modeRepository.getMode()) {
            DirectIdentitySetupMode.AUTOMATIC_INVITATION ->
                identityInvitationService.start(contactId)
            DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING ->
                startManualExchange(contactId)
        }

    override suspend fun startManualExchange(contactId: String): Result<Unit> = manualIdentityExchangeStarter.ensureStarted(contactId)
}
