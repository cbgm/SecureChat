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
import com.cbgm.securechat.feature.chats.domain.model.ContactSecurityState
import com.cbgm.securechat.feature.chats.presentation.component.chat.VerifyIdentityDialog
import com.cbgm.securechat.feature.chats.presentation.screen.ChatScreen
import com.cbgm.securechat.feature.chats.presentation.screen.chat.ChatViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ChatRoute(
    conversationId: String,
    contactId: String,
    contactName: String,
    onBack: () -> Unit,
    onClickHeader: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel =
        koinViewModel {
            parametersOf(
                conversationId,
                contactId,
                contactName
            )
        }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showVerificationDialog by remember(contactId) { mutableStateOf(false) }

    LaunchedEffect(key1 = contactId) {
        viewModel.markConversationRead()
    }

    DisposableEffect(key1 = contactId) {
        onDispose {
            viewModel.stopTyping()
        }
    }

    val incomingMessageIds =
        uiState.messages
            .asSequence()
            .filter { message ->
                !message.isMine
            }.map { message ->
                message.id
            }.toList()

    LaunchedEffect(key1 = incomingMessageIds) {
        if (incomingMessageIds.isNotEmpty()) {
            viewModel.markConversationRead()
        }
    }

    LaunchedEffect(key1 = uiState.contactSecurityState) {
        if (uiState.contactSecurityState == ContactSecurityState.MUTUAL_KEYS_VERIFIED) {
            showVerificationDialog = false
        }
    }

    ChatScreen(
        uiState = uiState,
        onMessageTextChanged = viewModel::onMessageTextChanged,
        onSendClick = viewModel::sendMessage,
        onRetryMessage = viewModel::retryMessage,
        onVerifyIdentity = {
            viewModel.refreshSafetyNumber()
            showVerificationDialog = true
        },
        onClickHeader = onClickHeader,
        onBack = onBack,
        modifier = modifier
    )

    if (
        showVerificationDialog
    ) {
        VerifyIdentityDialog(
            contactName = uiState.contactName,
            safetyNumber = uiState.safetyNumber,
            isLoadingSafetyNumber = uiState.isLoadingSafetyNumber,
            isVerifying = uiState.isVerifyingIdentity,
            onConfirm = viewModel::verifyIdentity,
            onDismiss = {
                showVerificationDialog = false
            }
        )
    }
}
