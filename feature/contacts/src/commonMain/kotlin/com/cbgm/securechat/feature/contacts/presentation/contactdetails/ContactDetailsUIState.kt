package com.cbgm.securechat.feature.contacts.presentation.contactdetails

import com.cbgm.securechat.feature.contacts.domain.model.Contact

sealed interface ContactDetailsUiState {

    data object Loading : ContactDetailsUiState

    data class Content(
        val contact: Contact
    ) : ContactDetailsUiState

    data object NotFound : ContactDetailsUiState

    data class Error(
        val message: String
    ) : ContactDetailsUiState
}