package com.cbgm.securechat.feature.contacts.presentation.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.usecase.ImportDeviceContacts
import com.cbgm.securechat.feature.contacts.domain.usecase.ObserveContacts
import com.cbgm.securechat.feature.contacts.presentation.model.ContactGroupEntity
import com.cbgm.securechat.feature.contacts.presentation.model.ContactsUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ContactsViewModel(
    private val observeContacts: ObserveContacts,
    private val importDeviceContacts: ImportDeviceContacts,
) : ViewModel() {
    private val _searchQuery =
        MutableStateFlow("")

    val searchQuery: StateFlow<String> =
        _searchQuery.asStateFlow()

    private val _errorMessages =
        MutableSharedFlow<String>()

    val errorMessages =
        _errorMessages.asSharedFlow()

    val uiState: StateFlow<ContactsUiState> =
        combine(
            observeContacts(),
            searchQuery,
        ) { contacts, query ->
            contacts.toUiState(query)
        }.catch { error ->
            emit(
                ContactsUiState.Error(
                    message =
                        error.message
                            ?: "Failed to load contacts",
                ),
            )
        }.stateIn(
            scope = viewModelScope,
            started =
                SharingStarted.WhileSubscribed(
                    stopTimeoutMillis = 5_000,
                ),
            initialValue = ContactsUiState.Loading,
        )

    fun onImportDeviceContacts() {
        viewModelScope.launch {
            importDeviceContacts()
                .onFailure { error ->
                    _errorMessages.emit(
                        error.message
                            ?: "Failed to import contacts",
                    )
                }
        }
    }

    fun onUpdateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun onDeviceContactsPermissionDenied() {
        viewModelScope.launch {
            _errorMessages.emit(
                "Contacts permission is required to import device contacts.",
            )
        }
    }

    private fun List<Contact>.toUiState(
        query: String,
    ): ContactsUiState {
        if (isEmpty()) {
            return ContactsUiState.Empty
        }

        return ContactsUiState.Content(
            groups =
                filterByQuery(query)
                    .groupByLetter(),
        )
    }

    private fun List<Contact>.filterByQuery(
        query: String,
    ): List<Contact> {
        val trimmedQuery = query.trim()

        if (trimmedQuery.isEmpty()) {
            return this
        }

        val normalizedPhoneQuery =
            trimmedQuery.filter(Char::isDigit)

        return filter { contact ->
            val matchesName =
                contact.displayName?.contains(
                    other = trimmedQuery,
                    ignoreCase = true,
                ) == true

            val matchesPhone =
                normalizedPhoneQuery.isNotEmpty() &&
                    contact.phoneNumbers.any { phoneNumber ->
                        phoneNumber.value
                            .filter(Char::isDigit)
                            .contains(normalizedPhoneQuery)
                    }

            matchesName || matchesPhone
        }
    }

    private fun List<Contact>.groupByLetter(): List<ContactGroupEntity> =
        sortedBy { contact ->
            contact.displayName
                .orEmpty()
                .lowercase()
        }.groupBy { contact ->
            contact.displayName
                ?.trim()
                ?.firstOrNull()
                ?.uppercaseChar()
                ?.takeIf(Char::isLetter)
                ?.toString()
                ?: "#"
        }.map { (title, contacts) ->
            ContactGroupEntity(
                title = title,
                contacts = contacts,
            )
        }
}
