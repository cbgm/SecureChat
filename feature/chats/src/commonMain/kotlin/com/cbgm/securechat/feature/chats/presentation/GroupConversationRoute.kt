package com.cbgm.securechat.feature.chats.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.securechat.feature.chats.presentation.screen.ChatScreen
import com.cbgm.securechat.feature.chats.presentation.screen.chat.GroupConversationViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun GroupConversationRoute(
    conversationId: String,
    onBack: () -> Unit,
    onOpenDetails: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel = koinViewModel<GroupConversationViewModel> { parametersOf(conversationId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(conversationId) { viewModel.markConversationRead() }

    DisposableEffect(conversationId) {
        onDispose { viewModel.stopTyping() }
    }

    ChatScreen(
        uiState = uiState,
        onMessageTextChanged = viewModel::onMessageTextChanged,
        onSendClick = viewModel::sendMessage,
        onClickHeader = onOpenDetails,
        onRetryMessage = viewModel::retryMessage,
        onAcceptGroupInvitation = viewModel::acceptInvitation,
        onDeclineGroupInvitation = viewModel::declineInvitation,
        onVerifyIdentity = {},
        onManualIdentitySetup = {},
        onBack = onBack,
        modifier = modifier
    )
}
