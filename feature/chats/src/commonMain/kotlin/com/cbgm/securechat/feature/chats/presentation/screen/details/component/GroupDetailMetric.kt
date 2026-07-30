package com.cbgm.securechat.feature.chats.presentation.component.groupdetails

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.securechat.core.ui.component.SecureChatCardNoAnimation
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing

@Composable
internal fun GroupDetailMetric(
    value: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    SecureChatCardNoAnimation(modifier = modifier) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.small),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview
@Composable
private fun GroupDetailMetricPreview() {
    SecureChatTheme {
        GroupDetailMetric(
            value = 3,
            label = "Verified"
        )
    }
}
