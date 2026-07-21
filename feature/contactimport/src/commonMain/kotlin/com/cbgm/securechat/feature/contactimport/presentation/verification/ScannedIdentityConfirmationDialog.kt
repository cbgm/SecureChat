package com.cbgm.securechat.feature.contactimport.presentation.verification

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

@Composable
fun ScannedIdentityConfirmationDialog(
    preview: ScannedIdentityPreview,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text(text = "Import SecureChat contact")
        },

        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = preview.displayName ?: "Unnamed SecureChat contact",
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
                    text = "SecureChat identity found",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                FingerprintSection(
                    title = "Signing key",
                    fingerprint = preview.signingKeyFingerprint
                )

                FingerprintSection(
                    title = "Encryption key",
                    fingerprint = preview.encryptionKeyFingerprint
                )
            }
        },

        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text("Import")
            }
        },

        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
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