package com.cbgm.securechat.feature.chats.presentation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.securechat.feature.chats.presentation.screen.ChatsScreen
import com.cbgm.securechat.feature.chats.presentation.screen.ChatsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ChatsRoute(
    onChatClick: (conversationId: String, contactId: String, contactName: String, isGroup: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState,
    innerPadding: PaddingValues,
    viewModel: ChatsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ChatsScreen(
        uiState = uiState,
        onChatClick = { chat ->
            onChatClick(chat.conversationId, chat.contactId, chat.contactName, chat.isGroup)
        },
        listState = listState,
        innerPadding = innerPadding,
        modifier = modifier
    )
}
