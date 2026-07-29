package com.cbgm.securechat.feature.chats.presentation.screen.details

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.securechat.feature.chats.presentation.screen.GroupDetailsOverviewRoute
import com.cbgm.securechat.feature.chats.presentation.screen.GroupDetailsVerifyRoute
import com.cbgm.securechat.feature.chats.presentation.screen.chat.GroupChatVerificationViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private enum class DetailsContent {
    Overview,
    VerifyIdentity
}

@Composable
fun GroupDetailsFlow(
    conversationId: String,
    onScanMemberQr: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val verificationViewModel =
        koinViewModel<GroupChatVerificationViewModel> {
            parametersOf(conversationId)
        }
    val uiState by verificationViewModel.uiState.collectAsStateWithLifecycle()
    var content by rememberSaveable {
        mutableStateOf(DetailsContent.Overview)
    }

    val visibleContent =
        if (
            content == DetailsContent.VerifyIdentity &&
            uiState.selectedMember == null
        ) {
            DetailsContent.Overview
        } else {
            content
        }

    AnimatedContent(
        targetState = visibleContent,
        modifier = modifier,
        transitionSpec = {
            if (targetState == DetailsContent.VerifyIdentity) {
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
                GroupDetailsOverviewRoute(
                    summary = uiState.summary,
                    onBack = onClose,
                    onVerifyMember = {
                        verificationViewModel.selectMember(it)
                        content = DetailsContent.VerifyIdentity
                    }
                )
            }

            DetailsContent.VerifyIdentity -> {
                uiState.selectedMember?.let { member ->
                    GroupDetailsVerifyRoute(
                        member = member,
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
        }
    }
}
