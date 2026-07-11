package com.cbgm.securechat.feature.contacts.data.mapper

import com.cbgm.securechat.core.id.IdGenerator
import com.cbgm.securechat.data.database.entity.ContactPhoneNumberEntity
import com.cbgm.securechat.feature.contacts.domain.model.ContactPhoneNumberType
import com.cbgm.securechat.feature.contacts.devicecontacts.DevicePhoneNumber
import com.cbgm.securechat.feature.contacts.devicecontacts.DevicePhoneNumberType

internal fun DevicePhoneNumber.toEntity(
    contactId: String,
    updatedAt: Long
): ContactPhoneNumberEntity {

    return ContactPhoneNumberEntity(
        id = IdGenerator.generate(),
        contactId = contactId,
        value = value,
        type = type.toDomain().name,
        label = label,
        updatedAtEpochMilliseconds = updatedAt
    )
}

private fun
        DevicePhoneNumberType.toDomain():
        ContactPhoneNumberType {

    return when (this) {

        DevicePhoneNumberType.MOBILE ->
            ContactPhoneNumberType.MOBILE

        DevicePhoneNumberType.WORK_MOBILE ->
            ContactPhoneNumberType.WORK_MOBILE

        DevicePhoneNumberType.HOME ->
            ContactPhoneNumberType.HOME

        DevicePhoneNumberType.WORK ->
            ContactPhoneNumberType.WORK

        DevicePhoneNumberType.MAIN ->
            ContactPhoneNumberType.MAIN

        DevicePhoneNumberType.CUSTOM ->
            ContactPhoneNumberType.CUSTOM

        DevicePhoneNumberType.OTHER ->
            ContactPhoneNumberType.OTHER
    }
}