package com.cbgm.securechat.feature.chats.presentation.screen.details

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.feature.chats.presentation.model.GroupMemberManagementUiState
import com.cbgm.securechat.feature.contacts.presentation.model.ContactsScreenMode
import com.cbgm.securechat.feature.contacts.presentation.model.ContactsUiState
import com.cbgm.securechat.feature.contacts.presentation.screen.ContactsScreen
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.feature_chats_group_add_members
import org.jetbrains.compose.resources.stringResource

@Composable
fun AddGroupMembersScreen(
    uiState: GroupMemberManagementUiState,
    onSearchQueryChanged: (String) -> Unit,
    onContactSelected: (String) -> Unit,
    onAddMembers: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val title = stringResource(Res.string.feature_chats_group_add_members)

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    ContactsScreen(
        uiState =
            ContactsUiState.Content(
                groups = uiState.availableContactGroups
            ),
        mode =
            ContactsScreenMode.MemberSelection(
                title = title,
                selectedContactIds = uiState.selectedContactIds,
                confirmEnabled = uiState.canAddSelected,
                confirming = uiState.isUpdating,
                onContactSelected = onContactSelected,
                onConfirmed = onAddMembers
            ),
        searchQuery = uiState.searchQuery,
        onSearchQueryChanged = onSearchQueryChanged,
        onBack = onBack,
        modifier = modifier,
        snackbarHostState = snackbarHostState
    )
}

@Preview
@Composable
private fun AddGroupMembersScreenPreview() {
    SecureChatTheme {
        AddGroupMembersScreen(
            uiState = GroupMemberManagementUiState(),
            onSearchQueryChanged = {},
            onContactSelected = {},
            onAddMembers = {},
            onBack = {}
        )
    }
}
