package com.cbgm.securechat.feature.chats.presentation.screen.details.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.ui.component.SecureChatAlertDialog
import com.cbgm.securechat.core.ui.component.SecureChatApprovalButton
import com.cbgm.securechat.core.ui.component.SecureChatOutlinedButton
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.chats.presentation.component.groupdetails.GroupDetailsPreviewData
import com.cbgm.securechat.feature.chats.presentation.model.GroupMemberVerificationUiState
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.base_cancel
import com.cbgm.securechat.resources.feature_chats_group_remove_member
import com.cbgm.securechat.resources.feature_chats_group_remove_member_description
import org.jetbrains.compose.resources.stringResource

@Composable
fun RemoveMemberDialog(
    member: GroupMemberVerificationUiState,
    isRemoving: Boolean,
    errorMessage: String?,
    onApprove: () -> Unit,
    onDismiss: () -> Unit
) {
    SecureChatAlertDialog(
        onDismissRequest = {},
        title = stringResource(Res.string.feature_chats_group_remove_member),
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text =
                        stringResource(
                            Res.string.feature_chats_group_remove_member_description,
                            member.displayName
                        )
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            }

            errorMessage?.let { message ->
                Text(
                    text = message,
                    modifier = Modifier.padding(top = MaterialTheme.spacing.medium),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            SecureChatApprovalButton(
                onClick = onApprove,
                fillMaxWidth = false,
                content = {
                    if (isRemoving) {
                        CircularProgressIndicator(
                            modifier =
                                Modifier
                                    .size(20.dp)
                                    .padding(2.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(text = stringResource(Res.string.feature_chats_group_remove_member))
                    }
                }
            )
        },
        dismissButton = {
            SecureChatOutlinedButton(
                onClick = onDismiss,
                text = stringResource(Res.string.base_cancel),
                fillMaxWidth = false
            )
        }
    )
}

@Preview
@Composable
fun RemoveMemberDialogPreview() {
    SecureChatTheme {
        RemoveMemberDialog(
            member = GroupDetailsPreviewData.participant,
            isRemoving = true,
            errorMessage = null,
            onApprove = {},
            onDismiss = {}
        )
    }
}
