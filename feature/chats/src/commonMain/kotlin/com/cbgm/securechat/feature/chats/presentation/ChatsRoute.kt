package com.cbgm.securechat.feature.chats.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.securechat.feature.chats.presentation.screen.ChatsScreen
import org.koin.compose.viewmodel.koinViewModel
import com.cbgm.securechat.feature.chats.presentation.screen.ChatsViewModel

@Composable
fun ChatsRoute(
    onAddChatClick: () -> Unit,
    onChatClick: (contactId: String, contactName: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ChatsScreen(
        chats = uiState.conversations,
        onAddChatClick = onAddChatClick,
        onChatClick = { contactId ->
            val conversation = uiState.conversations.firstOrNull { it.contactId == contactId }

            if (conversation != null) {
                onChatClick(conversation.contactId, conversation.contactName)
            }
        },
        modifier = modifier
    )
}