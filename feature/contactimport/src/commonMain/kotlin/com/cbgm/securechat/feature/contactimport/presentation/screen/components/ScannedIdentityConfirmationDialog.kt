package com.cbgm.securechat.feature.contactimport.presentation.screen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.feature.contactimport.presentation.model.ScannedIdentityPreview
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.base_cancel
import com.cbgm.securechat.resources.feature_contactimport_encryption_key
import com.cbgm.securechat.resources.feature_contactimport_in_person_qr_title
import com.cbgm.securechat.resources.feature_contactimport_qr_trust_warning
import com.cbgm.securechat.resources.feature_contactimport_securechat_identity_found
import com.cbgm.securechat.resources.feature_contactimport_signing_key
import com.cbgm.securechat.resources.feature_contactimport_unnamed_securechat_contact
import org.jetbrains.compose.resources.stringResource

@Composable
fun ScannedIdentityConfirmationDialog(
    preview: ScannedIdentityPreview,
    confirmButtonText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(Res.string.feature_contactimport_in_person_qr_title))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = preview.displayName ?: stringResource(Res.string.feature_contactimport_unnamed_securechat_contact),
                    style = MaterialTheme.typography.titleMedium
                )

                preview.phoneNumber?.let { phoneNumber ->
                    Text(
                        text = phoneNumber,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(Res.string.feature_contactimport_securechat_identity_found),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = stringResource(Res.string.feature_contactimport_qr_trust_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(8.dp))

                FingerprintSection(
                    title = stringResource(Res.string.feature_contactimport_signing_key),
                    fingerprint = preview.signingKeyFingerprint
                )

                FingerprintSection(
                    title = stringResource(Res.string.feature_contactimport_encryption_key),
                    fingerprint = preview.encryptionKeyFingerprint
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text(text = confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(stringResource(Res.string.base_cancel))
            }
        }
    )
}

@Composable
private fun FingerprintSection(
    title: String,
    fingerprint: String
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = fingerprint,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace
        )
    }
}
