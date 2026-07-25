package com.cbgm.securechat.feature.chats.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.securechat.feature.chats.presentation.screen.CreateGroupScreen
import com.cbgm.securechat.feature.chats.presentation.screen.CreateGroupViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CreateGroupRoute(
    onBack: () -> Unit,
    onGroupCreated: (conversationId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateGroupViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.groupCreated.collect(onGroupCreated)
    }

    CreateGroupScreen(
        uiState = uiState,
        onBack = onBack,
        onTitleChanged = viewModel::onTitleChanged,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onContactSelected = viewModel::onContactSelected,
        onCreateGroup = viewModel::onCreateGroup,
        modifier = modifier,
    )
}
