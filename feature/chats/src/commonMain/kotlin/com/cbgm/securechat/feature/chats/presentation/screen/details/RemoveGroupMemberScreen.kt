package com.cbgm.securechat.feature.chats.presentation.screen.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.chats.presentation.component.groupdetails.GroupDetailsPreviewData
import com.cbgm.securechat.feature.chats.presentation.model.GroupMemberVerificationUiState
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.feature_chats_group_remove_member
import com.cbgm.securechat.resources.feature_chats_group_remove_member_description
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoveGroupMemberScreen(
    member: GroupMemberVerificationUiState,
    isRemoving: Boolean,
    errorMessage: String?,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = stringResource(Res.string.feature_chats_group_remove_member))
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        enabled = !isRemoving
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(MaterialTheme.spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text =
                    stringResource(
                        Res.string.feature_chats_group_remove_member_description,
                        member.displayName
                    ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            errorMessage?.let { message ->
                Text(
                    text = message,
                    modifier = Modifier.padding(top = MaterialTheme.spacing.medium),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(
                onClick = onConfirm,
                enabled = !isRemoving,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = MaterialTheme.spacing.large),
                shape = MaterialTheme.shapes.small
            ) {
                if (isRemoving) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(2.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(text = stringResource(Res.string.feature_chats_group_remove_member))
                }
            }
        }
    }
}

@Preview
@Composable
private fun RemoveGroupMemberScreenPreview() {
    SecureChatTheme {
        RemoveGroupMemberScreen(
            member = GroupDetailsPreviewData.participant,
            isRemoving = false,
            errorMessage = null,
            onConfirm = {},
            onBack = {}
        )
    }
}
