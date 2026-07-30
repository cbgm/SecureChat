package com.cbgm.securechat.feature.chats.presentation.component.groupdetails

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.ui.theme.SecureChatTheme

@Composable
internal fun GroupDetailsLoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
    }
}

@Preview
@Composable
private fun GroupDetailsLoadingContentPreview() {
    SecureChatTheme {
        GroupDetailsLoadingContent(modifier = Modifier.size(160.dp))
    }
}
