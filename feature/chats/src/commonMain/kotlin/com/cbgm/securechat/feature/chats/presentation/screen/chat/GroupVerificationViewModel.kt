package com.cbgm.securechat.feature.chats.presentation.screen.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.feature.chats.domain.usecase.ObserveGroupVerification
import com.cbgm.securechat.feature.chats.domain.usecase.SynchronizeGroupVerification
import com.cbgm.securechat.feature.chats.domain.usecase.VerifyGroupMember
import com.cbgm.securechat.feature.chats.presentation.model.GroupMemberVerificationUi
import com.cbgm.securechat.feature.chats.presentation.model.GroupVerificationSummaryUi
import com.cbgm.securechat.feature.chats.presentation.model.buildGroupVerificationSummary
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContactSafetyNumber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroupVerificationUiState(
    val summary: GroupVerificationSummaryUi = GroupVerificationSummaryUi(),
    val selectedMember: GroupMemberVerificationUi? = null,
    val safetyNumber: String = "",
    val isLoadingSafetyNumber: Boolean = false,
    val isVerifying: Boolean = false,
    val errorMessage: String? = null
)

class GroupVerificationViewModel(
    private val conversationId: String,
    observeGroupVerification: ObserveGroupVerification,
    private val synchronizeGroupVerification: SynchronizeGroupVerification,
    private val verifyGroupMember: VerifyGroupMember,
    private val getContactSafetyNumber: GetContactSafetyNumber
) : ViewModel() {
    private val dialogState = MutableStateFlow(GroupVerificationDialogState())
    private val summaryFlow =
        observeGroupVerification(conversationId).map { groupState ->
            buildGroupVerificationSummary(
                isLocalAdmin = groupState.context.isLocalAdmin,
                ownerContactId = groupState.context.ownerContactId,
                ownerDisplayName = groupState.ownerDisplayName,
                ownInvitationId = groupState.context.ownInvitationId,
                rows = groupState.pairs
            )
        }

    val uiState: StateFlow<GroupVerificationUiState> =
        combine(
            summaryFlow,
            dialogState
        ) { summary, dialog ->
            GroupVerificationUiState(
                summary = summary,
                selectedMember =
                    dialog.selectedContactId?.let { selectedContactId ->
                        summary.members.firstOrNull { member ->
                            member.contactId == selectedContactId &&
                                member.canVerify
                        }
                    },
                safetyNumber = dialog.safetyNumber,
                isLoadingSafetyNumber = dialog.isLoadingSafetyNumber,
                isVerifying = dialog.isVerifying,
                errorMessage = dialog.errorMessage
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue = GroupVerificationUiState()
        )

    init {
        synchronize()
    }

    fun synchronize() {
        viewModelScope.launch {
            synchronizeGroupVerification(conversationId)
                .onFailure { error ->
                    dialogState.update { state ->
                        state.copy(
                            errorMessage =
                                error.message
                                    ?: "Group verification state could not be synchronized"
                        )
                    }
                }
        }
    }

    fun selectMember(contactId: String) {
        val canVerify =
            uiState.value.summary.members.any { candidate ->
                candidate.contactId == contactId &&
                    candidate.canVerify
            }
        if (!canVerify || dialogState.value.isLoadingSafetyNumber) {
            return
        }

        dialogState.value =
            GroupVerificationDialogState(
                selectedContactId = contactId,
                isLoadingSafetyNumber = true
            )

        viewModelScope.launch {
            getContactSafetyNumber
                .invoke(contactId = contactId)
                .onSuccess { safetyNumber ->
                    dialogState.update { current ->
                        if (current.selectedContactId != contactId) {
                            current
                        } else {
                            current.copy(
                                safetyNumber = safetyNumber.singleLine,
                                isLoadingSafetyNumber = false
                            )
                        }
                    }
                }.onFailure { error ->
                    dialogState.update { current ->
                        if (current.selectedContactId != contactId) {
                            current
                        } else {
                            current.copy(
                                safetyNumber = "",
                                isLoadingSafetyNumber = false,
                                errorMessage =
                                    error.message
                                        ?: "Safety number could not be generated"
                            )
                        }
                    }
                }
        }
    }

    fun verifySelectedMember() {
        val current = dialogState.value
        val contactId = current.selectedContactId ?: return

        if (
            current.safetyNumber.isBlank() ||
            current.isLoadingSafetyNumber ||
            current.isVerifying
        ) {
            return
        }

        dialogState.update { state ->
            state.copy(
                isVerifying = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            verifyGroupMember(
                groupId = conversationId,
                contactId = contactId
            ).onSuccess {
                dialogState.value = GroupVerificationDialogState()
            }.onFailure { error ->
                dialogState.update { state ->
                    state.copy(
                        isVerifying = false,
                        errorMessage =
                            error.message
                                ?: "Group member could not be verified"
                    )
                }
            }
        }
    }

    fun dismissVerification() {
        if (!dialogState.value.isVerifying) {
            dialogState.value = GroupVerificationDialogState()
        }
    }

    private data class GroupVerificationDialogState(
        val selectedContactId: String? = null,
        val safetyNumber: String = "",
        val isLoadingSafetyNumber: Boolean = false,
        val isVerifying: Boolean = false,
        val errorMessage: String? = null
    )
}
