package com.cbgm.securechat.feature.chats.presentation.screen

import androidx.compose.runtime.Composable
import com.cbgm.securechat.feature.chats.presentation.model.GroupDetailsUiState
import com.cbgm.securechat.feature.chats.presentation.model.GroupVerificationSummaryUiState
import com.cbgm.securechat.feature.chats.presentation.screen.chat.component.ChatDetailsOverviewScreen

@Composable
fun GroupDetailsOverviewRoute(
    summary: GroupVerificationSummaryUiState,
    onBack: () -> Unit,
    onVerifyMember: (String) -> Unit
) {
    ChatDetailsOverviewScreen(
        uiState = GroupDetailsUiState.Content(summary),
        onVerifyMember = onVerifyMember,
        onBack = onBack
    )
}
