package com.cbgm.securechat.feature.contacts.presentation.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.feature.contacts.domain.usecase.ImportDeviceContacts
import com.cbgm.securechat.feature.contacts.domain.usecase.ObserveContacts
import com.cbgm.securechat.feature.contacts.presentation.model.ContactsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContactsViewModel(
    private val observeContacts: ObserveContacts,
    private val importDeviceContacts: ImportDeviceContacts
) : ViewModel() {

    private val _uiState = MutableStateFlow<ContactsUiState>(ContactsUiState.Loading)

    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    init {
        observe()
    }

    fun onImportDeviceContacts() {
        viewModelScope.launch {
            importDeviceContacts().onFailure {}
        }
    }

    fun onDeviceContactsPermissionDenied() {
        _uiState.value =
            ContactsUiState.Error(message = "Contacts permission is required to import device contacts.")
    }

    private fun observe() {
        viewModelScope.launch {
            try {
                observeContacts().collect { contacts ->
                    _uiState.value = if (contacts.isEmpty()) {
                        ContactsUiState.Empty
                    } else {
                        ContactsUiState.Content(
                            contacts = contacts
                        )
                    }
                }
            } catch (error: Throwable) {
                _uiState.value =
                    ContactsUiState.Error(message = error.message ?: "Failed to load contacts")
            }
        }
    }
}