package com.cbgm.securechat.feature.contacts.devicecontacts

data class AddDeviceContactRequest(
    val displayName: String?,
    val phoneNumber: String,
    val email: String? = null,
    val company: String? = null
)

sealed interface AddDeviceContactResult {
    data object Added : AddDeviceContactResult

    data object AlreadyExists : AddDeviceContactResult

    data object PermissionDenied : AddDeviceContactResult

    data object InvalidPhoneNumber : AddDeviceContactResult

    data class Failure(
        val throwable: Throwable
    ) : AddDeviceContactResult
}

interface DeviceContactWriter {
    /**
     * Adds the contact directly to the phone contacts.
     *
     * If the phone number already exists, nothing is added.
     */
    suspend fun addIfNotExists(request: AddDeviceContactRequest): AddDeviceContactResult
}
