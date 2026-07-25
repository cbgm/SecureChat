package com.cbgm.securechat.feature.chats.presentation.screen

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.cbgm.securechat.feature.chats.presentation.model.CreateGroupUiState
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
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    ContactsScreen(
        uiState = ContactsUiState.Content(groups = uiState.contactGroups),
        onBack = onBack,
        onContactClick = { _, _ -> },
        onImportContact = {},
        onCreateGroup = {},
        onImportDeviceContacts = {},
        onSearchQueryChanged = onSearchQueryChanged,
        searchQuery = uiState.searchQuery,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
        selectionMode = true,
        selectedContactIds = uiState.selectedContactIds,
        selectionConfirmEnabled = uiState.canCreate,
        selectionConfirming = uiState.isCreating,
        onContactSelected = onContactSelected,
        onSelectionConfirmed = onCreateGroup,
        selectionTitle = uiState.title,
        onSelectionTitleChanged = onTitleChanged
    )
}
