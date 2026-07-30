package com.cbgm.securechat.feature.contacts.presentation.model

sealed interface ContactsScreenMode {
    data class Overview(
        val onContactClick: (
            contactId: String,
            contactName: String
        ) -> Unit,
        val onImportContact: () -> Unit,
        val onCreateGroup: () -> Unit,
        val onImportDeviceContacts: () -> Unit
    ) : ContactsScreenMode

    data class GroupSelection(
        val title: String,
        val selectedContactIds: Set<String>,
        val confirmEnabled: Boolean,
        val confirming: Boolean,
        val onTitleChanged: (String) -> Unit,
        val onContactSelected: (String) -> Unit,
        val onConfirmed: () -> Unit
    ) : ContactsScreenMode
}
