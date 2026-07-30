package com.cbgm.securechat.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.feature_chats_group_decline
import org.jetbrains.compose.resources.stringResource

@Composable
fun SecureChatApprovalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "",
    fillMaxWidth: Boolean = true,
    content: @Composable () -> Unit = {},
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
        modifier =
            modifier.then(
                if (fillMaxWidth) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier
                }
            )
    ) {
        if (text.isNotBlank()) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (content != {}) {
            content()
        }
    }
}

@Composable
fun SecureChatBannerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "",
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    fillMaxWidth: Boolean = true,
    content: @Composable () -> Unit = {},
    enabled: Boolean = true
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        colors =
            ButtonDefaults.buttonColors(
                contentColor = textColor,
                containerColor = Color.Transparent
            ),
        modifier =
            modifier.then(
                if (fillMaxWidth) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier
                }
            )
    ) {
        if (text.isNotBlank()) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (content != {}) {
            content()
        }
    }
}

@Composable
fun SecureChatSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "",
    fillMaxWidth: Boolean = true,
    content: @Composable () -> Unit = {},
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
        modifier =
            modifier.then(
                if (fillMaxWidth) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier
                }
            )
    ) {
        if (text.isNotBlank()) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (content != {}) {
            content()
        }
    }
}

@Composable
fun SecureChatOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "",
    fillMaxWidth: Boolean = true,
    content: @Composable () -> Unit = {},
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        border =
            BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
        colors =
            ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
        modifier =
            modifier.then(
                if (fillMaxWidth) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier
                }
            )
    ) {
        if (text.isNotBlank()) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (content != {}) {
            content()
        }
    }
}

@Preview
@Composable
private fun SecureChatButtonPreview() {
    SecureChatTheme {
        Column {
            SecureChatApprovalButton(
                onClick = {},
                text = "Continue"
            )
            SecureChatSecondaryButton(
                onClick = {},
                text = "Continue"
            )
            SecureChatOutlinedButton(
                onClick = {},
                text = "Continue"
            )
            SecureChatBannerButton(
                onClick = {},
                text = "Continue"
            )
        }
    }
}
