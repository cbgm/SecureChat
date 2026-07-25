package com.cbgm.securechat.feature.chats.presentation.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.feature.chats.domain.usecase.CreateGroupConversation
import com.cbgm.securechat.feature.chats.presentation.model.CreateGroupUiState
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.usecase.ObserveContacts
import com.cbgm.securechat.feature.contacts.presentation.model.ContactGroupEntity
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CreateGroupViewModel(
    private val observeContacts: ObserveContacts,
    private val createGroupConversation: CreateGroupConversation
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateGroupUiState())
    val uiState: StateFlow<CreateGroupUiState> = _uiState.asStateFlow()

    private val groupCreatedChannel = Channel<String>(capacity = Channel.BUFFERED)
    val groupCreated = groupCreatedChannel.receiveAsFlow()

    private var contacts: List<Contact> = emptyList()

    init {
        observeAvailableContacts()
    }

    fun onTitleChanged(title: String) {
        _uiState.update { it.copy(title = title, errorMessage = null) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                contactGroups = contacts.filterByQuery(query).groupByLetter()
            )
        }
    }

    fun onContactSelected(contactId: String) {
        _uiState.update { state ->
            val selectedContactIds =
                state.selectedContactIds.toMutableSet().apply {
                    if (!add(contactId)) remove(contactId)
                }

            state.copy(
                selectedContactIds = selectedContactIds,
                errorMessage = null
            )
        }
    }

    fun onCreateGroup() {
        val state = _uiState.value
        if (!state.canCreate) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, errorMessage = null) }
            runCatching {
                createGroupConversation(state.title, state.selectedContactIds).getOrThrow()
            }.onSuccess { conversationId ->
                _uiState.update { it.copy(isCreating = false) }
                groupCreatedChannel.send(conversationId)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isCreating = false, errorMessage = error.message ?: "Group could not be created")
                }
            }
        }
    }

    private fun observeAvailableContacts() {
        viewModelScope.launch {
            observeContacts()
                .catch { error ->
                    _uiState.update { it.copy(errorMessage = error.message ?: "Contacts could not be loaded") }
                }.collect { observedContacts ->
                    contacts = observedContacts
                    _uiState.update { state ->
                        state.copy(
                            contactGroups = contacts.filterByQuery(state.searchQuery).groupByLetter(),
                            selectedContactIds =
                                state.selectedContactIds.filterTo(mutableSetOf()) { contactId ->
                                    contacts.any { it.id == contactId }
                                }
                        )
                    }
                }
        }
    }

    private fun List<Contact>.filterByQuery(query: String): List<Contact> {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return this

        val normalizedPhoneQuery = trimmedQuery.filter(Char::isDigit)
        return filter { contact ->
            contact.displayName?.contains(trimmedQuery, ignoreCase = true) == true ||
                normalizedPhoneQuery.isNotEmpty() &&
                contact.phoneNumbers.any { phoneNumber ->
                    phoneNumber.value.filter(Char::isDigit).contains(normalizedPhoneQuery)
                }
        }
    }

    private fun List<Contact>.groupByLetter(): List<ContactGroupEntity> =
        sortedBy { it.displayName.orEmpty().lowercase() }
            .groupBy { contact ->
                contact.displayName
                    ?.trim()
                    ?.firstOrNull()
                    ?.uppercaseChar()
                    ?.takeIf(Char::isLetter)
                    ?.toString()
                    ?: "#"
            }.map { (title, contacts) -> ContactGroupEntity(title = title, contacts = contacts) }
}
