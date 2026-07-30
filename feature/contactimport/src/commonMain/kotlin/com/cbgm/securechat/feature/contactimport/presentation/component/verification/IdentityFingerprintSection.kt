package com.cbgm.securechat.feature.contactimport.presentation.component.verification

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.securechat.core.ui.theme.SecureChatTheme

@Composable
internal fun IdentityFingerprintSection(
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

@Preview
@Composable
private fun IdentityFingerprintSectionPreview() {
    SecureChatTheme {
        IdentityFingerprintSection(
            title = "Signing key",
            fingerprint = "A1 B2 C3 D4 E5 F6"
        )
    }
}
