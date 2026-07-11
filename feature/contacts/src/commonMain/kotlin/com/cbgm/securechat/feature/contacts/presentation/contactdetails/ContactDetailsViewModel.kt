package com.cbgm.securechat.feature.contacts.presentation.contactdetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContactDetailsViewModel(
    private val contactId: String,
    private val getContact: GetContact
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<ContactDetailsUiState>(
            ContactDetailsUiState.Loading
        )

    val uiState: StateFlow<ContactDetailsUiState> =
        _uiState.asStateFlow()

    init {
        loadContact()
    }

    fun loadContact() {
        viewModelScope.launch {
            _uiState.value =
                ContactDetailsUiState.Loading

            getContact(
                contactId = contactId
            )
                .onSuccess { contact ->
                    _uiState.value =
                        if (contact == null) {
                            ContactDetailsUiState.NotFound
                        } else {
                            ContactDetailsUiState.Content(
                                contact = contact
                            )
                        }
                }
                .onFailure { error ->
                    _uiState.value =
                        ContactDetailsUiState.Error(
                            message = error.message
                                ?: "Failed to load contact"
                        )
                }
        }
    }
}