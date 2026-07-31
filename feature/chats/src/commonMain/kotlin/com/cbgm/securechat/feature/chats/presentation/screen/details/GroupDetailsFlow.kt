package com.cbgm.securechat.feature.chats.presentation.screen.details

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.securechat.core.ui.component.IdentityVerificationScreen
import com.cbgm.securechat.feature.chats.presentation.model.GroupDetailsUiState
import com.cbgm.securechat.feature.chats.presentation.screen.details.component.LeaveGroupDialog
import com.cbgm.securechat.feature.chats.presentation.screen.details.component.RemoveMemberDialog
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private enum class DetailsContent {
    Overview,
    VerifyIdentity,
    AddMembers
}

@Composable
fun GroupDetailsFlow(
    conversationId: String,
    onScanMemberQr: (String) -> Unit,
    onGroupLeft: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val verificationViewModel =
        koinViewModel<GroupVerificationViewModel> {
            parametersOf(conversationId)
        }
    val uiState by verificationViewModel.uiState.collectAsStateWithLifecycle()
    var content by rememberSaveable {
        mutableStateOf(DetailsContent.Overview)
    }
    var observedMembershipRevision by rememberSaveable {
        mutableIntStateOf(uiState.memberManagement.completedRevision)
    }

    var showLeaveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.memberManagement.completedRevision) {
        val revision = uiState.memberManagement.completedRevision
        if (revision > observedMembershipRevision) {
            observedMembershipRevision = revision
            content = DetailsContent.Overview
        }
    }

    LaunchedEffect(uiState.leave.isLeaveRequested) {
        if (uiState.leave.isLeaveRequested) {
            showLeaveDialog = false
            onGroupLeft()
        }
    }

    val visibleContent =
        when {
            content == DetailsContent.VerifyIdentity && uiState.selectedMember == null ->
                DetailsContent.Overview
            else -> content
        }

    AnimatedContent(
        targetState = visibleContent,
        modifier = modifier,
        transitionSpec = {
            if (targetState != DetailsContent.Overview) {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) togetherWith
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    )
            } else {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) togetherWith
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    )
            }
        }
    ) { target ->
        when (target) {
            DetailsContent.Overview -> {
                GroupDetailsScreen(
                    uiState = GroupDetailsUiState.Content(uiState.summary),
                    onBack = onClose,
                    onAddMembers = {
                        content = DetailsContent.AddMembers
                    },
                    onRemoveMember = { contactId ->
                        verificationViewModel.requestMemberRemoval(contactId)
                    },
                    onLeaveGroup = {
                        showLeaveDialog = true
                    },
                    onVerifyMember = {
                        verificationViewModel.selectMember(it)
                        content = DetailsContent.VerifyIdentity
                    }
                )
            }

            DetailsContent.VerifyIdentity -> {
                uiState.selectedMember?.let { member ->
                    IdentityVerificationScreen(
                        contactName = member.displayName,
                        safetyNumber = uiState.safetyNumber,
                        isLoadingSafetyNumber = uiState.isLoadingSafetyNumber,
                        isVerifying = uiState.isVerifying,
                        errorMessage = uiState.errorMessage,
                        onConfirm = verificationViewModel::verifySelectedMember,
                        onScanQrCode = {
                            verificationViewModel.dismissVerification()
                            member.contactId?.let(onScanMemberQr)
                        },
                        onBack = {
                            verificationViewModel.dismissVerification()
                            content = DetailsContent.Overview
                        }
                    )
                }
            }

            DetailsContent.AddMembers -> {
                AddGroupMembersScreen(
                    uiState = uiState.memberManagement,
                    onSearchQueryChanged = verificationViewModel::updateMemberSearchQuery,
                    onContactSelected = verificationViewModel::toggleMemberSelection,
                    onAddMembers = verificationViewModel::addSelectedMembers,
                    onBack = {
                        content = DetailsContent.Overview
                    }
                )
            }
        }
    }
    uiState.memberManagement.removalCandidate?.let { member ->
        RemoveMemberDialog(
            member = member,
            isRemoving = uiState.memberManagement.isUpdating,
            errorMessage = uiState.memberManagement.errorMessage,
            onApprove = verificationViewModel::confirmMemberRemoval,
            onDismiss = verificationViewModel::dismissMemberRemoval
        )
    }

    if (showLeaveDialog) {
        LeaveGroupDialog(
            isRemoving = uiState.memberManagement.isUpdating,
            errorMessage = uiState.memberManagement.errorMessage,
            onApprove = verificationViewModel::leaveGroup,
            onDismiss = {
                verificationViewModel.dismissLeaveError()
                showLeaveDialog = false
            }
        )
    }
}
