package com.cbgm.securechat.feature.contacts.presentation.screen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.crypto.safety.SafetyNumber
import com.cbgm.securechat.core.ui.theme.SecureChatTheme

@Composable
fun SafetyNumberVerificationDialog(
    contactName: String,
    safetyNumber: SafetyNumber,
    hasConfirmedComparison: Boolean,
    isSaving: Boolean,
    errorMessage: String?,
    onConfirmedChanged: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!isSaving) {
                onDismiss()
            }
        },
        icon = {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null
            )
        },
        title = {
            Text(text = "Verify $contactName")
        },
        text = {
            // Hier rufen wir die neue Inhalts-Komponente auf
            SafetyNumberVerificationContent(
                contactName = contactName,
                safetyNumber = safetyNumber,
                hasConfirmedComparison = hasConfirmedComparison,
                isSaving = isSaving,
                errorMessage = errorMessage,
                onConfirmedChanged = onConfirmedChanged
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = hasConfirmedComparison && !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator()
                } else {
                    Text(text = "Confirm verification")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text(text = "Cancel")
            }
        }
    )
}

// NEU: Diese Komponente hält nur das Layout und kann perfekt gerendert werden
@Composable
fun SafetyNumberVerificationContent(
    contactName: String,
    safetyNumber: SafetyNumber,
    hasConfirmedComparison: Boolean,
    isSaving: Boolean,
    errorMessage: String?,
    onConfirmedChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Compare the complete safety number with $contactName through a trusted phone or video call.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Both devices must display exactly the same number.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(20.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = safetyNumber.formatted,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = hasConfirmedComparison,
                    enabled = !isSaving,
                    role = Role.Checkbox,
                    onValueChange = onConfirmedChanged
                )
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Checkbox(
                checked = hasConfirmedComparison,
                onCheckedChange = null,
                enabled = !isSaving
            )

            Text(
                text = "We compared the entire safety number through a trusted channel, and it matched.",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSafetyNumberVerificationDialog() {
    SecureChatTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            // WICHTIG: Wir previewen hier nur den Content, nicht den Dialog selbst
            SafetyNumberVerificationContent(
                contactName = "Alex",
                safetyNumber = SafetyNumber(
                    groups = listOf(
                        "11111", "11111", "11111", "11111",
                        "11111", "11111", "11111", "11111",
                        "11111", "11111", "11111", "11111",
                        "11111", "11111", "11111", "11111"
                    )
                ),
                isSaving = false,
                errorMessage = null,
                hasConfirmedComparison = true,
                onConfirmedChanged = {}
            )
        }
    }
}
