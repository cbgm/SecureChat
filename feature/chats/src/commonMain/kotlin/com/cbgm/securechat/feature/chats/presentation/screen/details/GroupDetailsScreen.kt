package com.cbgm.securechat.feature.chats.presentation.screen.details

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.securechat.core.ui.component.SecureChatLazyScaffold
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.chats.presentation.component.groupdetails.GroupDetailsErrorContent
import com.cbgm.securechat.feature.chats.presentation.component.groupdetails.GroupDetailsLoadingContent
import com.cbgm.securechat.feature.chats.presentation.component.groupdetails.GroupDetailsPreviewData
import com.cbgm.securechat.feature.chats.presentation.component.groupdetails.GroupDetailsTopBar
import com.cbgm.securechat.feature.chats.presentation.component.groupdetails.GroupMemberList
import com.cbgm.securechat.feature.chats.presentation.model.GroupDetailsUiState

@Composable
fun GroupDetailsScreen(
    uiState: GroupDetailsUiState,
    onVerifyMember: (String) -> Unit,
    onAddMembers: () -> Unit,
    onRemoveMember: (String) -> Unit,
    onLeaveGroup: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SecureChatLazyScaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { containerColor ->
            GroupDetailsTopBar(
                containerColor = containerColor,
                onBack = onBack
            )
        }
    ) { innerPadding, listState ->
        when (uiState) {
            GroupDetailsUiState.Loading ->
                GroupDetailsLoadingContent(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                )

            is GroupDetailsUiState.Content ->
                GroupMemberList(
                    summary = uiState.summary,
                    innerPadding = innerPadding,
                    listState = listState,
                    onVerifyMember = onVerifyMember,
                    onAddMembers = onAddMembers,
                    onRemoveMember = onRemoveMember,
                    onLeaveGroup = onLeaveGroup
                )

            is GroupDetailsUiState.Error ->
                GroupDetailsErrorContent(
                    message = uiState.message,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(MaterialTheme.spacing.medium)
                )
        }
    }
}

@Preview
@Composable
private fun GroupDetailsScreenPreview() {
    SecureChatTheme {
        GroupDetailsScreen(
            uiState = GroupDetailsUiState.Content(GroupDetailsPreviewData.summary),
            onVerifyMember = {},
            onAddMembers = {},
            onRemoveMember = {},
            onLeaveGroup = {},
            onBack = {}
        )
    }
}
