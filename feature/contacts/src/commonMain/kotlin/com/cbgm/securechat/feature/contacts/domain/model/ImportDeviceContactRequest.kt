package com.cbgm.securechat.feature.contacts.domain.model

/**
 * Contact information imported from the current device's
 * address book.
 *
 * SecureChat keys are intentionally absent. They may be attached
 * later when the person shares a SecureChat identity.
 */
data class ImportDeviceContactRequest(
    /**
     * Stable identifier supplied by the operating system.
     */
    val deviceContactId: String,
    val displayName: String?,
    /**
     * Every usable phone number exposed by the device contact.
     */
    val phoneNumbers: List<ImportDevicePhoneNumber>,
)
