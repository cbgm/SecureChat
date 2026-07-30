package com.cbgm.securechat.feature.contacts.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.cbgm.securechat.core.ui.component.SecureChatAlertDialog
import com.cbgm.securechat.core.ui.component.SecureChatApprovalButton
import com.cbgm.securechat.core.ui.component.SecureChatSecondaryButton
import com.cbgm.securechat.feature.contacts.domain.model.PendingContactInvitation
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.base_unknown
import com.cbgm.securechat.resources.feature_contacts_accept_invitation
import com.cbgm.securechat.resources.feature_contacts_contact_invitation_description
import com.cbgm.securechat.resources.feature_contacts_contact_invitation_title
import com.cbgm.securechat.resources.feature_contacts_decline_invitation
import com.cbgm.securechat.resources.feature_contacts_invitation_unverified_warning
import org.jetbrains.compose.resources.stringResource

@Composable
fun ContactInvitationDialog(
    invitation: PendingContactInvitation,
    isProcessing: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    SecureChatAlertDialog(
        onDismissRequest = {},
        title = stringResource(Res.string.feature_contacts_contact_invitation_title),
        text = {
            Column {
                Text(
                    text =
                        stringResource(
                            Res.string.feature_contacts_contact_invitation_description,
                            invitation.contactName
                                ?: stringResource(Res.string.base_unknown)
                        ),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(Res.string.feature_contacts_invitation_unverified_warning),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        },
        confirmButton = {
            SecureChatApprovalButton(
                fillMaxWidth = false,
                onClick = onAccept,
                enabled = !isProcessing,
                text = stringResource(Res.string.feature_contacts_accept_invitation)
            )
        },
        dismissButton = {
            SecureChatSecondaryButton(
                fillMaxWidth = false,
                onClick = onDecline,
                enabled = !isProcessing,
                text = stringResource(Res.string.feature_contacts_decline_invitation)
            )
        }
    )
}
