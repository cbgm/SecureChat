package com.cbgm.securechat.feature.contacts.presentation.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.feature.contacts.domain.identity.IdentityInvitationService
import com.cbgm.securechat.feature.contacts.domain.model.PendingContactInvitation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ContactInvitationViewModel(
    private val identityInvitationService: IdentityInvitationService
) : ViewModel() {
    val pendingInvitations: StateFlow<List<PendingContactInvitation>> =
        identityInvitationService
            .observePendingIncoming()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = emptyList()
            )

    private val _processingInvitationId = MutableStateFlow<String?>(null)
    val processingInvitationId: StateFlow<String?> = _processingInvitationId.asStateFlow()

    private val _effects = Channel<ContactInvitationEffect>(capacity = Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun accept(invitationId: String) {
        updateInvitation(invitationId) {
            identityInvitationService.accept(invitationId)
        }
    }

    fun decline(invitationId: String) {
        updateInvitation(invitationId) {
            identityInvitationService.decline(invitationId)
        }
    }

    private fun updateInvitation(
        invitationId: String,
        operation: suspend () -> Result<Unit>
    ) {
        if (_processingInvitationId.value != null) return

        viewModelScope.launch {
            _processingInvitationId.value = invitationId

            operation()
                .onFailure { error ->
                    _effects.send(
                        ContactInvitationEffect.ShowError(
                            message = error.message ?: "Contact invitation could not be updated"
                        )
                    )
                }

            _processingInvitationId.value = null
        }
    }
}

sealed interface ContactInvitationEffect {
    data class ShowError(
        val message: String
    ) : ContactInvitationEffect
}
