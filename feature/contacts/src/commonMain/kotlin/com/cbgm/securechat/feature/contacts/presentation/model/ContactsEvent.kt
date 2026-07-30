package com.cbgm.securechat.feature.contacts.presentation.model

sealed interface ContactsEvent {
    data class SearchQueryChanged(
        val query: String
    ) : ContactsEvent

    data object ImportDeviceContacts : ContactsEvent

    data object DeviceContactsPermissionDenied : ContactsEvent
}
