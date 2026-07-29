package com.cbgm.securechat.feature.chats.presentation.screen.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.feature.chats.domain.usecase.CreateGroupConversation
import com.cbgm.securechat.feature.chats.presentation.model.CreateGroupEffect
import com.cbgm.securechat.feature.chats.presentation.model.CreateGroupEvent
import com.cbgm.securechat.feature.chats.presentation.model.CreateGroupUiState
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.usecase.ObserveContacts
import com.cbgm.securechat.feature.contacts.presentation.mapper.filterContacts
import com.cbgm.securechat.feature.contacts.presentation.mapper.groupContactsByInitial
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

    private val _effects = Channel<CreateGroupEffect>(capacity = Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var contacts: List<Contact> = emptyList()

    init {
        observeAvailableContacts()
    }

    fun onEvent(event: CreateGroupEvent) {
        when (event) {
            is CreateGroupEvent.TitleChanged -> updateTitle(event.title)
            is CreateGroupEvent.SearchQueryChanged -> updateSearchQuery(event.query)
            is CreateGroupEvent.ContactSelectionToggled -> toggleContactSelection(event.contactId)
            CreateGroupEvent.CreateClicked -> createGroup()
            CreateGroupEvent.Clear -> clearData()
        }
    }

    private fun clearData() {
        _uiState.update { CreateGroupUiState() }
    }

    private fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title, errorMessage = null) }
    }

    private fun updateSearchQuery(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                contactGroups = contacts.filterContacts(query).groupContactsByInitial()
            )
        }
    }

    private fun toggleContactSelection(contactId: String) {
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

    private fun createGroup() {
        val state = _uiState.value
        if (!state.canCreate || state.isCreating) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, errorMessage = null) }

            createGroupConversation(state.title, state.selectedContactIds)
                .onSuccess { conversationId ->
                    _uiState.update { it.copy(isCreating = false) }
                    _effects.send(CreateGroupEffect.GroupCreated(conversationId))
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            errorMessage = error.message ?: "Group could not be created"
                        )
                    }
                }
        }
    }

    private fun observeAvailableContacts() {
        viewModelScope.launch {
            observeContacts()
                .catch { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "Contacts could not be loaded")
                    }
                }.collect { observedContacts ->
                    contacts = observedContacts
                    _uiState.update { state ->
                        state.copy(
                            contactGroups =
                                contacts
                                    .filterContacts(state.searchQuery)
                                    .groupContactsByInitial(),
                            selectedContactIds =
                                state.selectedContactIds.filterTo(mutableSetOf()) { contactId ->
                                    contacts.any { it.id == contactId }
                                }
                        )
                    }
                }
        }
    }
}
