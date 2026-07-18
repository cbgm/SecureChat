package com.cbgm.securechat.feature.chats.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.chats.domain.model.ChatMessage
import com.cbgm.securechat.feature.chats.domain.model.ContactSecurityState
import com.cbgm.securechat.feature.chats.domain.model.MessageContentStatus
import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryStatus
import com.cbgm.securechat.feature.chats.domain.model.MessageSecurity
import com.cbgm.securechat.feature.chats.presentation.model.ChatUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    uiState: ChatUiState,
    onMessageTextChanged: (String) -> Unit,
    onSendClick: () -> Unit,
    onClickHeader: () -> Unit,
    onRetryMessage: (String) -> Unit,
    onVerifyIdentity: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,

        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = {
                    Column(modifier = Modifier.clickable { onClickHeader() }) {
                        Text(
                            text = uiState.contactName,
                            maxLines = 1,
                            style = MaterialTheme.typography.titleSmall,
                            overflow = TextOverflow.Ellipsis
                        )

                        /*SecurityHeaderLabel(
                            securityState = uiState.contactSecurityState
                        )*/
                    }
                },

                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },

        bottomBar = {
            Column {
                HorizontalDivider()

                MessageInput(
                    value = uiState.messageText,
                    onValueChange = onMessageTextChanged,
                    onSendClick = onSendClick,
                    enabled = !uiState.isLoadingContact
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding)
        ) {
            SecurityBanner(
                securityState = uiState.contactSecurityState,
                onVerifyIdentity = onVerifyIdentity
            )

            when {
                uiState.isLoadingContact -> {
                    LoadingChatContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    )
                }

                uiState.messages.isEmpty() -> {
                    EmptyChatContent(
                        contactName = uiState.contactName,
                        securityState = uiState.contactSecurityState,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    )
                }

                else -> {
                    MessageList(
                        messages = uiState.messages,
                        onRetryMessage = onRetryMessage,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    )
                }
            }

            uiState.errorMessage?.let { errorMessage ->
                ErrorMessage(message = errorMessage)
            }
        }
    }
}

@Composable
private fun SecurityHeaderLabel(
    securityState: ContactSecurityState,
    modifier: Modifier = Modifier
) {
    val text = when (securityState) {
        ContactSecurityState.NO_REMOTE_PUBLIC_KEYS -> {
            "Not encrypted · no public keys"
        }

        ContactSecurityState.ONE_WAY_KEYS -> {
            "Not encrypted · one-way keys"
        }

        ContactSecurityState.MUTUAL_KEYS_UNVERIFIED -> {
            "Encrypted · identity unverified"
        }

        ContactSecurityState.MUTUAL_KEYS_VERIFIED -> {
            "Encrypted · identity verified"
        }
    }

    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun SecurityBanner(
    securityState: ContactSecurityState,
    onVerifyIdentity: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (securityState == ContactSecurityState.MUTUAL_KEYS_VERIFIED) {
        VerifiedSecurityIndicator(modifier = modifier)
        return
    }

    data class CombinedState(
        val icon: ImageVector,
        val title: String,
        val description: String,
        val containerColor: Color,
        val contentColor: Color,
    )

    val combinedState = when (securityState) {
        ContactSecurityState.NO_REMOTE_PUBLIC_KEYS -> CombinedState(
            icon = Icons.Default.LockOpen,
            title = "Messages are not end-to-end encrypted",
            description = "You do not have this contact’s SecureChat public keys.",
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        )

        ContactSecurityState.ONE_WAY_KEYS -> CombinedState(
            icon = Icons.Default.LockOpen,
            title = "Key exchange is incomplete",
            description = "Both parties have not completed the identity exchange.",
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        )

        ContactSecurityState.MUTUAL_KEYS_UNVERIFIED -> CombinedState(
            icon = Icons.Default.Warning,
            title = "Encrypted using an unverified identity",
            description = "Compare the safety number through another trusted channel.",
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        )

        /*ContactSecurityState.MUTUAL_KEYS_VERIFIED -> CombinedState(
            icon = Icons.Default.Lock,
            title = "End-to-end encrypted",
            description = "",
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        )*/
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = combinedState.containerColor,
        contentColor = combinedState.contentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = combinedState.icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )

            Column(
                modifier = Modifier
                    .padding(start = MaterialTheme.spacing.small)
                    .weight(1f)
            ) {
                Text(
                    text = combinedState.title,
                    style = MaterialTheme.typography.labelMedium
                )

                Text(
                    text = combinedState.description,
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }

            if (securityState == ContactSecurityState.MUTUAL_KEYS_UNVERIFIED) {
                TextButton(
                    onClick = onVerifyIdentity,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface)
                ) {
                    Text(
                        text = "Verify",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun VerifiedSecurityIndicator(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondary//.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(5.dp))

            Text(
                text = "Verified end-to-end encrypted",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MessageList(
    messages: List<ChatMessage>,
    onRetryMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(top = MaterialTheme.spacing.small, start = 12.dp, end = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        reverseLayout = true
    ) {
        items(
            items = messages.reversed(),
            key = { message ->
                message.id
            }
        ) { message ->
            MessageBubble(
                message = message,
                onRetryClick = {
                    onRetryMessage(message.id)
                }
            )
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    data class MessageBubbleState(
        val text: String,
        val isContentFailed: Boolean,
        val bubbleColor: Color,
        val contentColor: Color,
    )

    val bubbleState = when (message.contentStatus) {
        MessageContentStatus.READABLE -> MessageBubbleState(
            text = message.text,
            isContentFailed = false,
            bubbleColor = if (message.isMine) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.secondary
            },
            contentColor = if (message.isMine) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )

        MessageContentStatus.INVALID_PACKET -> MessageBubbleState(
            text = "Invalid message packet",
            isContentFailed = true,
            bubbleColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        )

        MessageContentStatus.INVALID_PLAINTEXT_PACKET -> MessageBubbleState(
            text = "Unable to read plaintext message",
            isContentFailed = true,
            bubbleColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        )

        MessageContentStatus.TRANSPORT_DECRYPTION_FAILED -> MessageBubbleState(
            text = "Unable to decrypt secure message",
            isContentFailed = true,
            bubbleColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        )
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isMine) {
            Arrangement.End
        } else {
            Arrangement.Start
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(fraction = 0.78f),
            horizontalAlignment = if (message.isMine) {
                Alignment.End
            } else {
                Alignment.Start
            }
        ) {
            Surface(
                //color = bubbleState.bubbleColor,
                border = BorderStroke(2.dp, bubbleState.bubbleColor),
                contentColor = bubbleState.bubbleColor, shape = RoundedCornerShape(size = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    if (bubbleState.isContentFailed) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null
                        )

                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.base))
                    }

                    Text(
                        text = bubbleState.text,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            MessageMetadata(
                message = message,
                onRetryClick = onRetryClick,
                modifier = Modifier.padding(top = 3.dp, start = 4.dp, end = 4.dp)
            )
        }
    }
}

@Composable
private fun MessageMetadata(
    message: ChatMessage,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        MessageSecurityIndicator(message = message)

        if (message.isMine && message.deliveryStatus != MessageDeliveryStatus.NOT_APPLICABLE) {
            OutgoingDeliveryIndicator(
                deliveryStatus = message.deliveryStatus,
                onRetryClick = onRetryClick
            )
        }
    }
}

@Composable
private fun MessageSecurityIndicator(
    message: ChatMessage
) {
    val text = when (message.contentStatus) {
        MessageContentStatus.INVALID_PACKET -> {
            "Invalid packet"
        }

        MessageContentStatus.INVALID_PLAINTEXT_PACKET -> {
            "Invalid plaintext"
        }

        MessageContentStatus.TRANSPORT_DECRYPTION_FAILED -> {
            "Decryption failed"
        }

        MessageContentStatus.READABLE -> {
            when (message.security) {
                MessageSecurity.INSECURE -> {
                    "Not encrypted"
                }

                MessageSecurity.END_TO_END_ENCRYPTED -> {
                    "Encrypted"
                }
            }
        }
    }

    val icon = when {
        message.contentStatus != MessageContentStatus.READABLE -> {
            Icons.Default.ErrorOutline
        }

        message.security == MessageSecurity.END_TO_END_ENCRYPTED -> {
            Icons.Default.Lock
        }

        else -> {
            Icons.Default.LockOpen
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(MaterialTheme.spacing.base))

        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OutgoingDeliveryIndicator(
    deliveryStatus: MessageDeliveryStatus,
    onRetryClick: () -> Unit
) {
    when (deliveryStatus) {
        MessageDeliveryStatus.NOT_APPLICABLE -> {
            Unit
        }

        MessageDeliveryStatus.QUEUED -> {
            DeliveryLabel(
                text = "Queued",
                icon = {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            )
        }

        MessageDeliveryStatus.SENDING -> {
            DeliveryLabel(
                text = "Sending",
                icon = {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp
                    )
                }
            )
        }

        MessageDeliveryStatus.SENT -> {
            DeliveryLabel(
                text = "Sent",
                icon = {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            )
        }

        MessageDeliveryStatus.DELIVERED -> {
            DeliveryLabel(
                text = "Delivered",
                icon = {
                    Row {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )

                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier
                                .size(14.dp)
                                .padding(start = 1.dp)
                        )
                    }
                }
            )
        }

        MessageDeliveryStatus.READ -> {
            DeliveryLabel(
                text = "Read",
                icon = {
                    Row {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )

                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier
                                .size(14.dp)
                                .padding(start = 1.dp)
                        )
                    }
                }
            )
        }

        MessageDeliveryStatus.FAILED -> {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.width(3.dp))

                Text(
                    text = "Failed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )

                IconButton(
                    onClick = onRetryClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry message",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun DeliveryLabel(
    text: String,
    icon: @Composable () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()

        Spacer(modifier = Modifier.width(3.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MessageInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.primary)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .imePadding()
            .padding(8.dp),

        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            enabled = enabled,
            placeholder = {
                Text(
                    text = "Message",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
        )

        IconButton(
            onClick = onSendClick,
            enabled = enabled && value.isNotBlank()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send message",
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun EmptyChatContent(
    contactName: String,
    securityState: ContactSecurityState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(horizontal = MaterialTheme.spacing.large),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Start a conversation with $contactName",
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center
            )

            Text(
                text = when (securityState) {
                    ContactSecurityState.NO_REMOTE_PUBLIC_KEYS -> {
                        "This contact has no SecureChat public keys. Messages use plaintext transport."
                    }

                    ContactSecurityState.ONE_WAY_KEYS -> {
                        "You have this contact’s public keys, but they do not have yours yet. Messages remain plaintext."
                    }

                    ContactSecurityState.MUTUAL_KEYS_UNVERIFIED -> {
                        "Messages are encrypted. Compare the safety number through a trusted channel."
                    }

                    ContactSecurityState.MUTUAL_KEYS_VERIFIED -> {
                        "Messages use the verified SecureChat identity."
                    }
                },

                modifier = Modifier.padding(top = MaterialTheme.spacing.base),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LoadingChatContent(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Loading chat…",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = message,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(
                horizontal = MaterialTheme.spacing.small,
                vertical = MaterialTheme.spacing.base
            ),
        color = MaterialTheme.colorScheme.onErrorContainer,
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center
    )
}


@Preview
@Composable
fun ChatScreenPreview() {
    SecureChatTheme {
        ChatScreen(
            uiState = ChatUiState(
                isLoadingContact = false,
                contactSecurityState = ContactSecurityState.MUTUAL_KEYS_VERIFIED,
                messages = listOf(
                    ChatMessage(
                        id = "1",
                        text = "Hello!",
                        isMine = false,
                        security = MessageSecurity.INSECURE,
                        contentStatus = MessageContentStatus.READABLE,
                        deliveryStatus = MessageDeliveryStatus.SENT,
                        contactId = "12",
                        timestamp = System.currentTimeMillis()
                    )
                )
                //errorMessage = "sfsfsf"
            ),
            onMessageTextChanged = {},
            onSendClick = {},
            onClickHeader = {},
            onRetryMessage = {},
            onVerifyIdentity = {},
            onBack = {}
        )
    }
}