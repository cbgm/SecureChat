package com.cbgm.securechat.feature.chats.presentation.screen.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun VerifyIdentityDialog(
    contactName: String,
    safetyNumber: String,
    isLoadingSafetyNumber: Boolean,
    isVerifying: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = {
            if (
                !isVerifying
            ) {
                onDismiss()
            }
        },

        title = {
            Text(text = "Verify $contactName")
        },

        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Compare this safety number with $contactName through a trusted channel, such as a phone call or in person."
                )

                when {
                    isLoadingSafetyNumber -> {
                        CircularProgressIndicator(modifier = Modifier.padding(vertical = 16.dp))
                    }

                    safetyNumber.isBlank() -> {
                        Text(
                            text = "Safety number unavailable",
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }

                    else -> {
                        Text(
                            text = safetyNumber,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Text(
                    text = "Only confirm when both devices display exactly the same number.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },

        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isVerifying && !isLoadingSafetyNumber && safetyNumber.isNotBlank()
            ) {
                if (isVerifying) {
                    CircularProgressIndicator()
                } else {
                    Text(
                        text = "Numbers match"
                    )
                }
            }
        },

        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isVerifying
            ) {
                Text(text = "Cancel")
            }
        }
    )
}

@Preview
@Composable
fun VerifyIdentityDialogPreview() {
    MaterialTheme {
        VerifyIdentityDialog(
            contactName = "Alice",
            safetyNumber = "12345",
            isLoadingSafetyNumber = false,
            isVerifying = false,
            onConfirm = {},
            onDismiss = {}
        )
    }
}