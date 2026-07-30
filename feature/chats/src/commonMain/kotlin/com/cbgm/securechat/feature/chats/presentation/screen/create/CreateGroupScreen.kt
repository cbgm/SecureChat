package com.cbgm.securechat.feature.chats.presentation.screen

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.cbgm.securechat.feature.chats.presentation.model.CreateGroupUiState
import com.cbgm.securechat.feature.contacts.presentation.model.ContactsScreenMode
import com.cbgm.securechat.feature.contacts.presentation.model.ContactsUiState
import com.cbgm.securechat.feature.contacts.presentation.screen.ContactsScreen

@Composable
fun CreateGroupScreen(
    uiState: CreateGroupUiState,
    onBack: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onContactSelected: (String) -> Unit,
    onCreateGroup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    ContactsScreen(
        uiState = ContactsUiState.Content(groups = uiState.contactGroups),
        mode =
            ContactsScreenMode.GroupSelection(
                title = uiState.title,
                selectedContactIds = uiState.selectedContactIds,
                confirmEnabled = uiState.canCreate,
                confirming = uiState.isCreating,
                onTitleChanged = onTitleChanged,
                onContactSelected = onContactSelected,
                onConfirmed = onCreateGroup
            ),
        searchQuery = uiState.searchQuery,
        onSearchQueryChanged = onSearchQueryChanged,
        onBack = onBack,
        modifier = modifier,
        snackbarHostState = snackbarHostState
    )
}
