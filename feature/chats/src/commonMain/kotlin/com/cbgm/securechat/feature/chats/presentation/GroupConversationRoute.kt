package com.cbgm.securechat.feature.chats.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.securechat.feature.chats.presentation.screen.GroupConversationScreen
import com.cbgm.securechat.feature.chats.presentation.screen.GroupConversationViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun GroupConversationRoute(
    conversationId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<GroupConversationViewModel> { parametersOf(conversationId) }
    val conversation by viewModel.conversation.collectAsStateWithLifecycle()

    GroupConversationScreen(conversation = conversation, onBack = onBack, modifier = modifier)
}
