package com.cbgm.securechat.feature.chats.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.securechat.feature.chats.presentation.component.chat.VerifyIdentityDialog
import com.cbgm.securechat.feature.chats.presentation.screen.ChatScreen
import com.cbgm.securechat.feature.chats.presentation.screen.chat.GroupConversationViewModel
import com.cbgm.securechat.feature.chats.presentation.screen.chat.GroupVerificationViewModel
import com.cbgm.securechat.feature.chats.presentation.screen.chat.component.GroupVerificationSheet
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun GroupConversationRoute(
    conversationId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = koinViewModel<GroupConversationViewModel> { parametersOf(conversationId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val verificationViewModel =
        koinViewModel<GroupVerificationViewModel> {
            parametersOf(conversationId)
        }
    val verificationUiState by verificationViewModel.uiState.collectAsStateWithLifecycle()
    var showMembersSheet by remember(conversationId) { mutableStateOf(false) }

    val openMembers = {
        showMembersSheet = true
        verificationViewModel.synchronize()
    }
    val openVerification = {
        openMembers()
    }

    LaunchedEffect(conversationId) {
        viewModel.markConversationRead()
        verificationViewModel.synchronize()
    }

    DisposableEffect(conversationId) {
        onDispose {
            viewModel.stopTyping()
        }
    }

    val verificationSummary = verificationUiState.summary
    val hasVerificationSnapshot = verificationSummary.hasAuthoritativeState

    ChatScreen(
        uiState =
            uiState.copy(
                groupMemberCount =
                    if (hasVerificationSnapshot) {
                        verificationSummary.totalMemberCount
                    } else {
                        uiState.groupMemberCount
                    },
                groupReadyMemberCount =
                    if (hasVerificationSnapshot) {
                        verificationSummary.activeParticipantCount
                    } else {
                        uiState.groupReadyMemberCount
                    },
                groupMutuallyVerifiedCount =
                    verificationSummary.mutuallyVerifiedParticipantCount,
                groupVerifiableMemberCount =
                    if (hasVerificationSnapshot) {
                        verificationSummary.activeParticipantCount
                    } else {
                        uiState.groupReadyMemberCount
                    },
                errorMessage = verificationUiState.errorMessage ?: uiState.errorMessage
            ),
        onMessageTextChanged = viewModel::onMessageTextChanged,
        onSendClick = viewModel::sendMessage,
        onClickHeader = openMembers,
        onGroupVerificationClick = openVerification,
        onRetryMessage = viewModel::retryMessage,
        onAcceptGroupInvitation = viewModel::acceptInvitation,
        onDeclineGroupInvitation = viewModel::declineInvitation,
        onVerifyIdentity = {},
        onManualIdentitySetup = {},
        onBack = onBack,
        modifier = modifier
    )

    if (showMembersSheet) {
        GroupVerificationSheet(
            summary = verificationUiState.summary,
            onMemberClick = { contactId ->
                showMembersSheet = false
                verificationViewModel.selectMember(contactId)
            },
            onDismiss = {
                showMembersSheet = false
            }
        )
    }

    verificationUiState.selectedMember?.let { member ->
        VerifyIdentityDialog(
            contactName = member.displayName,
            safetyNumber = verificationUiState.safetyNumber,
            isLoadingSafetyNumber = verificationUiState.isLoadingSafetyNumber,
            isVerifying = verificationUiState.isVerifying,
            onConfirm = verificationViewModel::verifySelectedMember,
            onScanQrCode = null,
            onDismiss = verificationViewModel::dismissVerification
        )
    }
}
