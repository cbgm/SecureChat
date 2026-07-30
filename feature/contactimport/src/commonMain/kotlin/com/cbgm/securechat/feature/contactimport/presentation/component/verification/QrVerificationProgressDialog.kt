package com.cbgm.securechat.feature.contactimport.presentation.component.verification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.feature_contactimport_verifying_identity_qr
import org.jetbrains.compose.resources.stringResource

@Composable
fun QrVerificationProgressDialog() {
    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(text = stringResource(Res.string.feature_contactimport_verifying_identity_qr))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        },
        confirmButton = {}
    )
}

@Preview
@Composable
private fun QrVerificationProgressDialogPreview() {
    SecureChatTheme {
        QrVerificationProgressDialog()
    }
}
