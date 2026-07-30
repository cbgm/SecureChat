package com.cbgm.securechat.feature.contacts.domain.identity

interface ContactVerificationService {
    suspend fun verify(contactId: String): Result<Unit>

    suspend fun sendReceiptIfLocallyVerified(contactId: String): Result<Unit>
}
