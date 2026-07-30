package com.cbgm.securechat.feature.contactimport.presentation.component.verification

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.securechat.core.ui.component.SecureChatAlertDialog
import com.cbgm.securechat.core.ui.component.SecureChatApprovalButton
import com.cbgm.securechat.core.ui.component.SecureChatSecondaryButton
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
    SecureChatAlertDialog(
        onDismissRequest = {},
        title = stringResource(Res.string.feature_contactimport_qr_verification_failed),
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            SecureChatApprovalButton(
                fillMaxWidth = false,
                onClick = onRetry,
                text = stringResource(Res.string.base_retry)
            )
        },
        dismissButton = {
            SecureChatSecondaryButton(
                fillMaxWidth = false,
                onClick = onCancel,
                text = stringResource(Res.string.base_cancel)
            )
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
