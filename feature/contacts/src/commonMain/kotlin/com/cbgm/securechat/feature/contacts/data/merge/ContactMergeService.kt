package com.cbgm.securechat.feature.contacts.data.merge

import com.cbgm.securechat.core.id.IdGenerator
import com.cbgm.securechat.data.database.dao.ContactDao
import com.cbgm.securechat.feature.contacts.domain.model.ImportDevicePhoneNumber

class ContactMergeService(
    private val contactDao: ContactDao
) {

    suspend fun findOrCreateForSecureChatIdentity(
        signingPublicKey: ByteArray,
        phoneNumber: String?
    ): ContactMergeResult {

        val bySigningKey =
            contactDao.findBySigningPublicKey(
                signingPublicKey
            )

        if (bySigningKey != null) {
            return ContactMergeResult(
                contactId = bySigningKey.contact.id,
                isNewContact = false
            )
        }

        if (phoneNumber != null) {
            val byPhone =
                contactDao.findByPhoneNumber(
                    phoneNumber
                )

            if (byPhone != null) {
                return ContactMergeResult(
                    contactId = byPhone.contact.id,
                    isNewContact = false
                )
            }
        }

        return ContactMergeResult(
            contactId = IdGenerator.generate(),
            isNewContact = true
        )
    }

    suspend fun findOrCreateForDeviceContact(
        deviceContactId: String,
        phoneNumbers: List<ImportDevicePhoneNumber>
    ): ContactMergeResult {

        val byDeviceId =
            contactDao.findByDeviceContactId(
                deviceContactId
            )

        if (byDeviceId != null) {
            return ContactMergeResult(
                contactId = byDeviceId.contact.id,
                isNewContact = false
            )
        }

        phoneNumbers.forEach { phone ->

            val byPhone =
                contactDao.findByPhoneNumber(
                    phone.value
                )

            if (byPhone != null) {
                return ContactMergeResult(
                    contactId = byPhone.contact.id,
                    isNewContact = false
                )
            }
        }

        return ContactMergeResult(
            contactId = IdGenerator.generate(),
            isNewContact = true
        )
    }
}