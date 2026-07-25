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
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.base_cancel
import com.cbgm.securechat.resources.base_verify_contact
import com.cbgm.securechat.resources.feature_chats_compare_safety_number_contact
import com.cbgm.securechat.resources.feature_chats_confirm_matching_numbers_only
import com.cbgm.securechat.resources.feature_chats_numbers_match
import com.cbgm.securechat.resources.feature_chats_safety_number_unavailable
import org.jetbrains.compose.resources.stringResource

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
            if (!isVerifying) {
                onDismiss()
            }
        },
        title = {
            Text(text = stringResource(Res.string.base_verify_contact, contactName))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = stringResource(Res.string.feature_chats_compare_safety_number_contact, contactName)
                )

                when {
                    isLoadingSafetyNumber -> {
                        CircularProgressIndicator(modifier = Modifier.padding(vertical = 16.dp))
                    }

                    safetyNumber.isBlank() -> {
                        Text(
                            text = stringResource(Res.string.feature_chats_safety_number_unavailable),
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
                    text = stringResource(Res.string.feature_chats_confirm_matching_numbers_only),
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
                        text = stringResource(Res.string.feature_chats_numbers_match)
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isVerifying
            ) {
                Text(text = stringResource(Res.string.base_cancel))
            }
        }
    )
}

@Preview
@Composable
private fun VerifyIdentityDialogPreview() {
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
