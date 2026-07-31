package com.cbgm.securechat.feature.contacts.presentation.screen.blocklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.usecase.BlockContact
import com.cbgm.securechat.feature.contacts.domain.usecase.ObserveContactBlocklist
import com.cbgm.securechat.feature.contacts.domain.usecase.UnblockContact
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BlockedContactsUiState(
    val blockedContacts: List<Contact> = emptyList(),
    val availableContacts: List<Contact> = emptyList(),
    val showAddContacts: Boolean = false,
    val phoneNumber: String = "",
    val phoneNumberError: String? = null,
    val processingContactId: String? = null
)

sealed interface BlockedContactsEffect {
    data class ShowError(
        val message: String
    ) : BlockedContactsEffect
}

class BlockedContactsViewModel(
    observeContactBlocklist: ObserveContactBlocklist,
    private val blockContact: BlockContact,
    private val unblockContact: UnblockContact
) : ViewModel() {
    private val _uiState = MutableStateFlow(BlockedContactsUiState())
    val uiState: StateFlow<BlockedContactsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<BlockedContactsEffect>(capacity = Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            observeContactBlocklist().collect { blocklist ->
                _uiState.update {
                    it.copy(
                        blockedContacts = blocklist.blockedContacts,
                        availableContacts = blocklist.availableContacts
                    )
                }
            }
        }
    }

    fun showAddContacts() {
        _uiState.update {
            it.copy(
                showAddContacts = true,
                phoneNumber = "",
                phoneNumberError = null
            )
        }
    }

    fun dismissAddContacts() {
        _uiState.update {
            it.copy(
                showAddContacts = false,
                phoneNumber = "",
                phoneNumberError = null
            )
        }
    }

    fun updatePhoneNumber(phoneNumber: String) {
        _uiState.update {
            it.copy(
                phoneNumber = phoneNumber,
                phoneNumberError = null
            )
        }
    }

    fun block(contactId: String) {
        updateContact(contactId) {
            blockContact(contactId)
        }
    }

    fun blockPhoneNumber() {
        val phoneNumber = _uiState.value.phoneNumber.trim()

        if (phoneNumber.isEmpty() || _uiState.value.processingContactId != null) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    processingContactId = PHONE_NUMBER_OPERATION_ID,
                    phoneNumberError = null
                )
            }

            blockContact
                .byPhoneNumber(phoneNumber)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            showAddContacts = false,
                            phoneNumber = "",
                            phoneNumberError = null
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            phoneNumberError = error.message ?: "Phone number could not be blocked"
                        )
                    }
                }

            _uiState.update { it.copy(processingContactId = null) }
        }
    }

    fun unblock(contactId: String) {
        updateContact(contactId) {
            unblockContact(contactId)
        }
    }

    private fun updateContact(
        contactId: String,
        operation: suspend () -> Result<Unit>
    ) {
        if (_uiState.value.processingContactId != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(processingContactId = contactId) }

            operation()
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            showAddContacts = false,
                            phoneNumber = "",
                            phoneNumberError = null
                        )
                    }
                }.onFailure { error ->
                    _effects.send(
                        BlockedContactsEffect.ShowError(
                            message = error.message ?: "Blocked contacts could not be updated"
                        )
                    )
                }

            _uiState.update { it.copy(processingContactId = null) }
        }
    }

    private companion object {
        const val PHONE_NUMBER_OPERATION_ID = "phone-number"
    }
}
