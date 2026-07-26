package com.cbgm.securechat.feature.chats.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.securechat.feature.chats.presentation.model.CreateGroupEffect
import com.cbgm.securechat.feature.chats.presentation.model.CreateGroupEvent
import com.cbgm.securechat.feature.chats.presentation.screen.CreateGroupScreen
import com.cbgm.securechat.feature.chats.presentation.screen.create.CreateGroupViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CreateGroupRoute(
    onBack: () -> Unit,
    onGroupCreated: (conversationId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateGroupViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CreateGroupEffect.GroupCreated -> onGroupCreated(effect.conversationId)
            }
        }
    }

    CreateGroupScreen(
        uiState = uiState,
        onBack = onBack,
        onTitleChanged = { viewModel.onEvent(CreateGroupEvent.TitleChanged(it)) },
        onSearchQueryChanged = { viewModel.onEvent(CreateGroupEvent.SearchQueryChanged(it)) },
        onContactSelected = { viewModel.onEvent(CreateGroupEvent.ContactSelectionToggled(it)) },
        onCreateGroup = { viewModel.onEvent(CreateGroupEvent.CreateClicked) },
        modifier = modifier
    )
}
