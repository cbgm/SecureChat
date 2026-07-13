package com.cbgm.securechat.feature.contacts.data.mapper

import com.cbgm.securechat.data.database.entity.ContactPhoneNumberEntity
import com.cbgm.securechat.data.database.entity.ContactPublicIdentityEntity
import com.cbgm.securechat.data.database.model.ContactWithPublicIdentity
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.ContactPhoneNumber
import com.cbgm.securechat.feature.contacts.domain.model.ContactPhoneNumberType
import com.cbgm.securechat.feature.contacts.domain.model.ContactVerificationStatus
import com.cbgm.securechat.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.securechat.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.securechat.feature.contacts.domain.model.SecureChatIdentity

fun ContactWithPublicIdentity.toDomain(): Contact {
    return Contact(
        id = contact.id,

        displayName = contact.displayName,

        phoneNumbers =
            phoneNumbers.map { phoneNumber ->
                phoneNumber.toDomain()
            },

        preferredPhoneNumberId =
            contact.preferredPhoneNumberId,

        deviceContactId =
            contact.deviceContactId,

        deviceContactLinkStatus =
            contact
                .deviceContactLinkStatus
                .toDeviceContactLinkStatus(),

        secureChatIdentity =
            publicIdentity?.toDomain(),

        createdAtEpochMilliseconds =
            contact.createdAtEpochMilliseconds,

        updatedAtEpochMilliseconds =
            contact.updatedAtEpochMilliseconds
    )
}

private fun ContactPhoneNumberEntity.toDomain():
        ContactPhoneNumber {

    return ContactPhoneNumber(
        id = id,
        value = value,
        type =
            type.toContactPhoneNumberType(),
        label = label
    )
}

private fun ContactPublicIdentityEntity.toDomain():
        SecureChatIdentity {

    return SecureChatIdentity(
        encryptionPublicKey =
            encryptionPublicKey.copyOf(),

        signingPublicKey =
            signingPublicKey.copyOf(),

        verificationStatus =
            verificationStatus
                .toContactVerificationStatus(),

        keyExchangeStatus =
            keyExchangeStatus
                .toKeyExchangeStatus(),

        updatedAtEpochMilliseconds =
            updatedAtEpochMilliseconds
    )
}

private fun String.toContactPhoneNumberType():
        ContactPhoneNumberType {

    return when (this) {
        ContactPhoneNumberType.MOBILE.name ->
            ContactPhoneNumberType.MOBILE

        ContactPhoneNumberType.WORK_MOBILE.name ->
            ContactPhoneNumberType.WORK_MOBILE

        ContactPhoneNumberType.HOME.name ->
            ContactPhoneNumberType.HOME

        ContactPhoneNumberType.WORK.name ->
            ContactPhoneNumberType.WORK

        ContactPhoneNumberType.MAIN.name ->
            ContactPhoneNumberType.MAIN

        ContactPhoneNumberType.CUSTOM.name ->
            ContactPhoneNumberType.CUSTOM

        ContactPhoneNumberType.OTHER.name ->
            ContactPhoneNumberType.OTHER

        else ->
            error(
                "Unknown contact phone-number type: $this"
            )
    }
}

private fun String.toDeviceContactLinkStatus():
        DeviceContactLinkStatus {

    return when (this) {
        DeviceContactLinkStatus.NOT_LINKED.name ->
            DeviceContactLinkStatus.NOT_LINKED

        DeviceContactLinkStatus.LINKED.name ->
            DeviceContactLinkStatus.LINKED

        DeviceContactLinkStatus.MISSING.name ->
            DeviceContactLinkStatus.MISSING

        else ->
            error(
                "Unknown device-contact link status: $this"
            )
    }
}

private fun String.toContactVerificationStatus():
        ContactVerificationStatus {

    return when (this) {
        ContactVerificationStatus.UNVERIFIED.name ->
            ContactVerificationStatus.UNVERIFIED

        ContactVerificationStatus.VERIFIED.name ->
            ContactVerificationStatus.VERIFIED

        else ->
            error(
                "Unknown verification status: $this"
            )
    }
}

private fun String.toKeyExchangeStatus():
        KeyExchangeStatus {

    return when (this) {
        KeyExchangeStatus.ONE_WAY.name ->
            KeyExchangeStatus.ONE_WAY

        KeyExchangeStatus.MUTUAL.name ->
            KeyExchangeStatus.MUTUAL

        else ->
            error(
                "Unknown key-exchange status: $this"
            )
    }
}