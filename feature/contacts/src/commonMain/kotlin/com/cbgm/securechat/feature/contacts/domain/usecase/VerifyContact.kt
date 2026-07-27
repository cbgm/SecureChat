package com.cbgm.securechat.feature.contacts.domain.usecase

import com.cbgm.securechat.feature.contacts.domain.identity.ContactVerificationService
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.repository.ContactRepository

class VerifyContact(
    private val repository: ContactRepository,
    private val contactVerificationService: ContactVerificationService
) {
    suspend operator fun invoke(contactId: String): Result<Contact> =
        runCatching {
            contactVerificationService.verify(contactId = contactId).getOrThrow()
            repository.getContact(contactId = contactId).getOrThrow() ?: error("Contact not found: $contactId")
        }
}
