package com.cbgm.securechat.feature.contactimport.presentation.component.verification

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.base_cancel
import com.cbgm.securechat.resources.base_retry
import com.cbgm.securechat.resources.feature_contactimport_qr_verification_failed
import org.jetbrains.compose.resources.stringResource

@Composable
fun QrVerificationErrorDialog(
    message: String,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onRetry,
        title = {
            Text(text = stringResource(Res.string.feature_contactimport_qr_verification_failed))
        },
        text = {
            Text(text = message)
        },
        confirmButton = {
            TextButton(onClick = onRetry) {
                Text(text = stringResource(Res.string.base_retry))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(text = stringResource(Res.string.base_cancel))
            }
        }
    )
}

@Preview
@Composable
private fun QrVerificationErrorDialogPreview() {
    SecureChatTheme {
        QrVerificationErrorDialog(
            message = "The scanned identity does not match this contact.",
            onRetry = {},
            onCancel = {}
        )
    }
}
