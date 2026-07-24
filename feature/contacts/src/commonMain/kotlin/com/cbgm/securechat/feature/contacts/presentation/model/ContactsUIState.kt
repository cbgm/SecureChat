package com.cbgm.securechat.feature.contacts.presentation.model

import com.cbgm.securechat.feature.contacts.domain.model.Contact

sealed interface ContactsUiState {
    data object Loading : ContactsUiState

    data object Empty : ContactsUiState

    data class Content(
        val groups: List<ContactGroupEntity>,
    ) : ContactsUiState

    data class Error(
        val message: String,
    ) : ContactsUiState
}
