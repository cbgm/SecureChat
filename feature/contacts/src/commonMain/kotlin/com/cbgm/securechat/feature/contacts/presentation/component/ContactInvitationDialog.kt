package com.cbgm.securechat.feature.contacts.presentation.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(text = stringResource(Res.string.feature_contacts_contact_invitation_title))
        },
        text = {
            Text(
                text =
                    stringResource(
                        Res.string.feature_contacts_contact_invitation_description,
                        invitation.contactName
                            ?: stringResource(Res.string.base_unknown)
                    ) + "\n\n" +
                        stringResource(Res.string.feature_contacts_invitation_unverified_warning)
            )
        },
        confirmButton = {
            TextButton(onClick = onAccept, enabled = !isProcessing) {
                Text(text = stringResource(Res.string.feature_contacts_accept_invitation))
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline, enabled = !isProcessing) {
                Text(text = stringResource(Res.string.feature_contacts_decline_invitation))
            }
        }
    )
}
