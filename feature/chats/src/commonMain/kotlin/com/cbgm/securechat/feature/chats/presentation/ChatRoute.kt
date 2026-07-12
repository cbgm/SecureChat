package com.cbgm.securechat.feature.chats.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ChatRoute(
    contactId: String,
    contactName: String,
    onBack: () -> Unit,
    viewModel: ChatViewModel = koinViewModel {
        parametersOf(
            contactId,
            contactName
        )
    }
) {
    val uiState by viewModel.uiState
        .collectAsStateWithLifecycle()

    ChatScreen(
        uiState = uiState,
        onMessageTextChanged =
            viewModel::onMessageTextChanged,
        onSendClick =
            viewModel::sendMessage,
        onBack = onBack
    )
}