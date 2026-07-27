package com.cbgm.securechat.feature.chats.presentation.screen.chat.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.base_cancel
import com.cbgm.securechat.resources.feature_chats_import_contact_identity
import com.cbgm.securechat.resources.feature_chats_manual_identity_setup_description
import com.cbgm.securechat.resources.feature_chats_manual_identity_setup_title
import com.cbgm.securechat.resources.feature_identity_share_my_identity
import org.jetbrains.compose.resources.stringResource

@Composable
fun ManualIdentitySetupDialog(
    onShareIdentity: () -> Unit,
    onImportIdentity: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(Res.string.feature_chats_manual_identity_setup_title))
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(Res.string.feature_chats_manual_identity_setup_description))

                TextButton(
                    onClick = onShareIdentity,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(Res.string.feature_identity_share_my_identity))
                }

                TextButton(
                    onClick = onImportIdentity,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(Res.string.feature_chats_import_contact_identity))
                }
            }
        },
        confirmButton = { },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.base_cancel))
            }
        }
    )
}
