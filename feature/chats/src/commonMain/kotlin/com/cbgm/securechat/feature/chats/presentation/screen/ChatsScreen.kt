package com.cbgm.securechat.feature.chats.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.ui.theme.Colors
import com.cbgm.securechat.core.ui.theme.SecureChatTheme

/**
 * UI model for one item in the conversations list.
 */
data class ChatListItem(
    val contactId: String,
    val contactName: String,
    val lastMessage: String,
    val timestamp: String
)

@Composable
fun ChatsScreen(
    chats: List<ChatListItem>,
    onAddChatClick: () -> Unit,
    onChatClick: (contactId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (chats.isEmpty()) {
        EmptyChatsContent(modifier = modifier.fillMaxSize())
    } else {
        LazyColumn(modifier = modifier.fillMaxSize()) {
            items(
                items = chats,
                key = { chat ->
                    chat.contactId
                }
            ) { chat ->
                ChatItem(
                    chat = chat,
                    onClick = {
                        onChatClick(chat.contactId)
                    }
                )

                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ChatItem(
    chat: ChatListItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        //colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = {
            Text(
                text = chat.contactName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = {
            Text(
                text = chat.lastMessage,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        trailingContent = {
            Column {
                Text(
                    text = chat.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = 15.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Colors.UnreadMessageIndicator
                )
            }
        }
    )
}

@Composable
private fun EmptyChatsContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No conversations yet",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Press + to choose a contact and start chatting.",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview
@Composable
fun ChatsScreenPreview() {
    SecureChatTheme {

        ChatsScreen(
            chats = listOf(
                ChatListItem(
                    contactId = "1",
                    contactName = "Alice",
                    lastMessage = "Hello!",
                    timestamp = "10:00 AM"
                ),
                ChatListItem(
                    contactId = "2",
                    contactName = "Bob",
                    lastMessage = "Hi there!",
                    timestamp = "9:30 AM"
                )
            ),
            onAddChatClick = {},
            onChatClick = {}
        )
    }
}
