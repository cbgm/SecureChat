package com.cbgm.securechat.feature.contacts.devicecontacts

interface DeviceContactsDataSource {
    /**
     * Returns all contacts visible to SecureChat.
     *
     * The platform implementation is responsible for
     * permission checks.
     */
    suspend fun getContacts(): Result<List<DeviceContact>>
}
