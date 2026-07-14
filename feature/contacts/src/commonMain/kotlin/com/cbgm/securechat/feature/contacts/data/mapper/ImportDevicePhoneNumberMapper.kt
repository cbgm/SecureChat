package com.cbgm.securechat.feature.contacts.data.mapper

import com.cbgm.securechat.core.id.IdGenerator
import com.cbgm.securechat.core.protocol.phone.PhoneNumberNormalizer
import com.cbgm.securechat.data.database.entity.ContactPhoneNumberEntity
import com.cbgm.securechat.feature.contacts.domain.model.ImportDevicePhoneNumber

internal fun ImportDevicePhoneNumber.toEntity(
    contactId: String,
    updatedAtEpochMilliseconds: Long,
    phoneNumberNormalizer: PhoneNumberNormalizer
): ContactPhoneNumberEntity {
    return ContactPhoneNumberEntity(
        id = IdGenerator.generate(),
        contactId = contactId,
        value = value,
        normalizedValue = phoneNumberNormalizer.normalize(value).getOrThrow(),
        type = type.name,
        label = label,
        updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
    )
}
