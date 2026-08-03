package com.cbgm.securechat.feature.contacts.domain.usecase

import com.cbgm.securechat.core.protocol.mailbox.MailboxCapabilityLifecycle
import com.cbgm.securechat.core.protocol.mailbox.NoOpMailboxCapabilityLifecycle
import com.cbgm.securechat.core.security.ContactBlocklistRepository
import com.cbgm.securechat.feature.contacts.domain.identity.IdentityInvitationService
import com.cbgm.securechat.feature.contacts.domain.repository.ContactRepository

class BlockContact(
    private val blocklistRepository: ContactBlocklistRepository,
    private val contactRepository: ContactRepository,
    private val identityInvitationService: IdentityInvitationService,
    private val mailboxCapabilityLifecycle: MailboxCapabilityLifecycle =
        NoOpMailboxCapabilityLifecycle
) {
    suspend operator fun invoke(contactId: String): Result<Unit> =
        runCatching {
            blocklistRepository.block(contactId)
            val authorizationError =
                identityInvitationService.revokeDirectChatAuthorization(contactId).exceptionOrNull()
            val mailboxError = mailboxCapabilityLifecycle.revokeForContact(contactId).exceptionOrNull()
            authorizationError?.let { throw it }
            mailboxError?.let { throw it }
        }

    suspend fun byPhoneNumber(phoneNumber: String): Result<Unit> =
        runCatching {
            val contact = contactRepository.findOrCreateByPhoneNumber(phoneNumber).getOrThrow()
            invoke(contact.id).getOrThrow()
        }
}
