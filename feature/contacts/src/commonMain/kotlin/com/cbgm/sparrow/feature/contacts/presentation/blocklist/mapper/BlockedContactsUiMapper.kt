package com.cbgm.sparrow.feature.contacts.presentation.blocklist.mapper

import com.cbgm.sparrow.feature.contacts.domain.model.ContactBlocklist
import com.cbgm.sparrow.feature.contacts.presentation.blocklist.model.BlockedContactsUiState

internal fun ContactBlocklist.toBlockedContactsUiState(
    showAddContacts: Boolean,
    phoneNumber: String,
    phoneNumberError: String?,
    processingContactId: String?
): BlockedContactsUiState =
    BlockedContactsUiState(
        blockedContacts = blockedContacts,
        availableContacts = availableContacts,
        showAddContacts = showAddContacts,
        phoneNumber = phoneNumber,
        phoneNumberError = phoneNumberError,
        processingContactId = processingContactId
    )
