package com.cbgm.securechat.feature.contacts.presentation.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.usecase.ImportDeviceContacts
import com.cbgm.securechat.feature.contacts.domain.usecase.ObserveContacts
import com.cbgm.securechat.feature.contacts.presentation.mapper.filterContacts
import com.cbgm.securechat.feature.contacts.presentation.mapper.groupContactsByInitial
import com.cbgm.securechat.feature.contacts.presentation.model.ContactsEffect
import com.cbgm.securechat.feature.contacts.presentation.model.ContactsEvent
import com.cbgm.securechat.feature.contacts.presentation.model.ContactsUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ContactsViewModel(
    private val observeContacts: ObserveContacts,
    private val importDeviceContacts: ImportDeviceContacts
) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _effects = Channel<ContactsEffect>(capacity = Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    val uiState: StateFlow<ContactsUiState> =
        combine(
            observeContacts(),
            searchQuery
        ) { contacts, query ->
            contacts.toUiState(query)
        }.catch { error ->
            emit(ContactsUiState.Error(error.message ?: "Failed to load contacts"))
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = ContactsUiState.Loading
        )

    fun onEvent(event: ContactsEvent) {
        when (event) {
            is ContactsEvent.SearchQueryChanged -> _searchQuery.value = event.query
            ContactsEvent.ImportDeviceContacts -> importContacts()
            ContactsEvent.DeviceContactsPermissionDenied -> showPermissionDenied()
        }
    }

    private fun importContacts() {
        viewModelScope.launch {
            importDeviceContacts()
                .onFailure { error ->
                    _effects.send(
                        ContactsEffect.ShowError(error.message ?: "Failed to import contacts")
                    )
                }
        }
    }

    private fun showPermissionDenied() {
        viewModelScope.launch {
            _effects.send(
                ContactsEffect.ShowError(
                    "Contacts permission is required to import device contacts."
                )
            )
        }
    }

    private fun List<Contact>.toUiState(query: String): ContactsUiState {
        if (isEmpty()) return ContactsUiState.Empty

        return ContactsUiState.Content(
            groups = filterContacts(query).groupContactsByInitial()
        )
    }
}
