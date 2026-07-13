package com.cbgm.securechat.feature.chats.presentation

import androidx.compose.foundation.background
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.feature.chats.domain.model.ChatMessage
import com.cbgm.securechat.feature.chats.domain.model.ContactSecurityState
import com.cbgm.securechat.feature.chats.domain.model.MessageContentStatus
import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryStatus
import com.cbgm.securechat.feature.chats.domain.model.MessageSecurity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    uiState: ChatUiState,
    onMessageTextChanged: (String) -> Unit,
    onSendClick: () -> Unit,
    onRetryMessage: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier =
            modifier.fillMaxSize(),

        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text =
                                uiState.contactName,

                            maxLines = 1,

                            overflow =
                                TextOverflow.Ellipsis
                        )

                        SecurityHeaderLabel(
                            securityState =
                                uiState
                                    .contactSecurityState
                        )
                    }
                },

                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            imageVector =
                                Icons.AutoMirrored
                                    .Filled
                                    .ArrowBack,

                            contentDescription =
                                "Back"
                        )
                    }
                }
            )
        },

        bottomBar = {
            Column {
                HorizontalDivider()

                MessageInput(
                    value =
                        uiState.messageText,

                    onValueChange =
                        onMessageTextChanged,

                    onSendClick =
                        onSendClick,

                    enabled =
                        !uiState.isLoadingContact
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
        ) {
            SecurityBanner(
                securityState =
                    uiState.contactSecurityState
            )

            when {
                uiState.isLoadingContact -> {
                    LoadingChatContent(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .weight(1f)
                    )
                }

                uiState.messages.isEmpty() -> {
                    EmptyChatContent(
                        contactName =
                            uiState.contactName,

                        securityState =
                            uiState.contactSecurityState,

                        modifier =
                            Modifier
                                .fillMaxSize()
                                .weight(1f)
                    )
                }

                else -> {
                    MessageList(
                        messages =
                            uiState.messages,

                        onRetryMessage =
                            onRetryMessage,

                        modifier =
                            Modifier
                                .fillMaxSize()
                                .weight(1f)
                    )
                }
            }

            uiState.errorMessage
                ?.let { errorMessage ->
                    ErrorMessage(
                        message =
                            errorMessage
                    )
                }
        }
    }
}

@Composable
private fun SecurityHeaderLabel(
    securityState: ContactSecurityState,
    modifier: Modifier = Modifier
) {
    val text =
        when (securityState) {
            ContactSecurityState
                .NO_REMOTE_PUBLIC_KEYS -> {
                "Not encrypted · no public keys"
            }

            ContactSecurityState
                .ONE_WAY_KEYS -> {
                "Not encrypted · one-way keys"
            }

            ContactSecurityState
                .MUTUAL_KEYS_UNVERIFIED -> {
                "Encrypted · identity unverified"
            }

            ContactSecurityState
                .MUTUAL_KEYS_VERIFIED -> {
                "Encrypted · identity verified"
            }
        }

    Text(
        text = text,

        modifier =
            modifier,

        style =
            MaterialTheme
                .typography
                .labelSmall,

        color =
            MaterialTheme
                .colorScheme
                .onSurfaceVariant,

        maxLines = 1,

        overflow =
            TextOverflow.Ellipsis
    )
}

@Composable
private fun SecurityBanner(
    securityState: ContactSecurityState,
    modifier: Modifier = Modifier
) {
    val icon =
        when (securityState) {
            ContactSecurityState
                .NO_REMOTE_PUBLIC_KEYS,

            ContactSecurityState
                .ONE_WAY_KEYS -> {
                Icons.Default.LockOpen
            }

            ContactSecurityState
                .MUTUAL_KEYS_UNVERIFIED -> {
                Icons.Default.Warning
            }

            ContactSecurityState
                .MUTUAL_KEYS_VERIFIED -> {
                Icons.Default.Lock
            }
        }

    val title =
        when (securityState) {
            ContactSecurityState
                .NO_REMOTE_PUBLIC_KEYS -> {
                "Messages are not end-to-end encrypted"
            }

            ContactSecurityState
                .ONE_WAY_KEYS -> {
                "Key exchange is incomplete"
            }

            ContactSecurityState
                .MUTUAL_KEYS_UNVERIFIED -> {
                "Encrypted using an unverified identity"
            }

            ContactSecurityState
                .MUTUAL_KEYS_VERIFIED -> {
                "End-to-end encrypted"
            }
        }

    val description =
        when (securityState) {
            ContactSecurityState
                .NO_REMOTE_PUBLIC_KEYS -> {
                "You do not have this contact’s SecureChat public keys. Messages use plaintext transport."
            }

            ContactSecurityState
                .ONE_WAY_KEYS -> {
                "You have this contact’s public keys, but they do not yet have yours. Messages remain plaintext."
            }

            ContactSecurityState
                .MUTUAL_KEYS_UNVERIFIED -> {
                "Both parties have exchanged public keys. Messages are encrypted, but the safety number has not been verified."
            }

            ContactSecurityState
                .MUTUAL_KEYS_VERIFIED -> {
                "Both parties have exchanged public keys and verified the safety number."
            }
        }

    val containerColor =
        when (securityState) {
            ContactSecurityState
                .NO_REMOTE_PUBLIC_KEYS,

            ContactSecurityState
                .ONE_WAY_KEYS -> {
                MaterialTheme
                    .colorScheme
                    .errorContainer
            }

            ContactSecurityState
                .MUTUAL_KEYS_UNVERIFIED -> {
                MaterialTheme
                    .colorScheme
                    .secondaryContainer
            }

            ContactSecurityState
                .MUTUAL_KEYS_VERIFIED -> {
                MaterialTheme
                    .colorScheme
                    .primaryContainer
            }
        }

    val contentColor =
        when (securityState) {
            ContactSecurityState
                .NO_REMOTE_PUBLIC_KEYS,

            ContactSecurityState
                .ONE_WAY_KEYS -> {
                MaterialTheme
                    .colorScheme
                    .onErrorContainer
            }

            ContactSecurityState
                .MUTUAL_KEYS_UNVERIFIED -> {
                MaterialTheme
                    .colorScheme
                    .onSecondaryContainer
            }

            ContactSecurityState
                .MUTUAL_KEYS_VERIFIED -> {
                MaterialTheme
                    .colorScheme
                    .onPrimaryContainer
            }
        }

    Surface(
        modifier =
            modifier.fillMaxWidth(),

        color =
            containerColor,

        contentColor =
            contentColor
    ) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                ),

            verticalAlignment =
                Alignment.Top
        ) {
            Icon(
                imageVector =
                    icon,

                contentDescription =
                    null
            )

            Column(
                modifier =
                    Modifier
                        .padding(
                            start = 12.dp
                        )
                        .weight(1f)
            ) {
                Text(
                    text =
                        title,

                    style =
                        MaterialTheme
                            .typography
                            .titleSmall
                )

                Text(
                    text =
                        description,

                    modifier =
                        Modifier.padding(
                            top = 2.dp
                        ),

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )
            }
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
        modifier =
            modifier.padding(
                horizontal = 12.dp
            ),

        verticalArrangement =
            Arrangement.spacedBy(
                8.dp
            ),

        reverseLayout =
            true
    ) {
        items(
            items =
                messages.reversed(),

            key = { message ->
                message.id
            }
        ) { message ->
            MessageBubble(
                message =
                    message,

                onRetryClick = {
                    onRetryMessage(
                        message.id
                    )
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
    val displayedText =
        when (message.contentStatus) {
            MessageContentStatus.READABLE -> {
                message.text
            }

            MessageContentStatus.INVALID_PACKET -> {
                "Invalid message packet"
            }

            MessageContentStatus.INVALID_PLAINTEXT_PACKET -> {
                "Unable to read plaintext message"
            }

            MessageContentStatus.TRANSPORT_DECRYPTION_FAILED -> {
                "Unable to decrypt secure message"
            }
        }

    val contentFailed =
        message.contentStatus !=
                MessageContentStatus.READABLE

    val bubbleColor =
        when {
            contentFailed -> {
                MaterialTheme
                    .colorScheme
                    .errorContainer
            }

            message.isMine -> {
                MaterialTheme
                    .colorScheme
                    .primaryContainer
            }

            else -> {
                MaterialTheme
                    .colorScheme
                    .surfaceVariant
            }
        }

    val bubbleContentColor =
        when {
            contentFailed -> {
                MaterialTheme
                    .colorScheme
                    .onErrorContainer
            }

            message.isMine -> {
                MaterialTheme
                    .colorScheme
                    .onPrimaryContainer
            }

            else -> {
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
            }
        }

    Row(
        modifier =
            modifier.fillMaxWidth(),

        horizontalArrangement =
            if (message.isMine) {
                Arrangement.End
            } else {
                Arrangement.Start
            }
    ) {
        Column(
            modifier =
                Modifier.fillMaxWidth(
                    fraction = 0.78f
                ),

            horizontalAlignment =
                if (message.isMine) {
                    Alignment.End
                } else {
                    Alignment.Start
                }
        ) {
            Surface(
                color =
                    bubbleColor,

                contentColor =
                    bubbleContentColor,

                shape =
                    RoundedCornerShape(
                        size = 16.dp
                    )
            ) {
                Row(
                    modifier =
                        Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 10.dp
                        ),

                    verticalAlignment =
                        Alignment.Top
                ) {
                    if (contentFailed) {
                        Icon(
                            imageVector =
                                Icons.Default
                                    .ErrorOutline,

                            contentDescription =
                                null
                        )

                        Spacer(
                            modifier =
                                Modifier.width(8.dp)
                        )
                    }

                    Text(
                        text =
                            displayedText,

                        modifier =
                            Modifier.weight(1f)
                    )
                }
            }

            MessageMetadata(
                message =
                    message,

                onRetryClick =
                    onRetryClick,

                modifier =
                    Modifier.padding(
                        top = 3.dp,
                        start = 4.dp,
                        end = 4.dp
                    )
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
        modifier =
            modifier,

        verticalAlignment =
            Alignment.CenterVertically,

        horizontalArrangement =
            Arrangement.spacedBy(
                4.dp
            )
    ) {
        MessageSecurityIndicator(
            message =
                message
        )

        if (message.isMine) {
            OutgoingDeliveryIndicator(
                deliveryStatus =
                    message.deliveryStatus,

                onRetryClick =
                    onRetryClick
            )
        }
    }
}

@Composable
private fun MessageSecurityIndicator(
    message: ChatMessage
) {
    val text =
        when (message.contentStatus) {
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

    val icon =
        when {
            message.contentStatus !=
                    MessageContentStatus.READABLE -> {
                Icons.Default.ErrorOutline
            }

            message.security ==
                    MessageSecurity.END_TO_END_ENCRYPTED -> {
                Icons.Default.Lock
            }

            else -> {
                Icons.Default.LockOpen
            }
        }

    Row(
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Icon(
            imageVector =
                icon,

            contentDescription =
                null,

            modifier =
                Modifier.size(
                    14.dp
                ),

            tint =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )

        Spacer(
            modifier =
                Modifier.width(
                    3.dp
                )
        )

        Text(
            text =
                text,

            style =
                MaterialTheme
                    .typography
                    .labelSmall,

            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
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
                        imageVector =
                            Icons.Default.Schedule,

                        contentDescription =
                            null,

                        modifier =
                            Modifier.size(
                                14.dp
                            )
                    )
                }
            )
        }

        MessageDeliveryStatus.SENDING -> {
            DeliveryLabel(
                text = "Sending",
                icon = {
                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(
                                12.dp
                            ),

                        strokeWidth =
                            1.5.dp
                    )
                }
            )
        }

        MessageDeliveryStatus.SENT -> {
            DeliveryLabel(
                text = "Sent",
                icon = {
                    Icon(
                        imageVector =
                            Icons.Default.Check,

                        contentDescription =
                            null,

                        modifier =
                            Modifier.size(
                                14.dp
                            )
                    )
                }
            )
        }

        MessageDeliveryStatus.FAILED -> {
            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Icon(
                    imageVector =
                        Icons.Default.ErrorOutline,

                    contentDescription =
                        null,

                    modifier =
                        Modifier.size(
                            14.dp
                        ),

                    tint =
                        MaterialTheme
                            .colorScheme
                            .error
                )

                Spacer(
                    modifier =
                        Modifier.width(
                            3.dp
                        )
                )

                Text(
                    text =
                        "Failed",

                    style =
                        MaterialTheme
                            .typography
                            .labelSmall,

                    color =
                        MaterialTheme
                            .colorScheme
                            .error
                )

                IconButton(
                    onClick =
                        onRetryClick,

                    modifier =
                        Modifier.size(
                            28.dp
                        )
                ) {
                    Icon(
                        imageVector =
                            Icons.Default.Refresh,

                        contentDescription =
                            "Retry message",

                        modifier =
                            Modifier.size(
                                16.dp
                            ),

                        tint =
                            MaterialTheme
                                .colorScheme
                                .error
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
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        icon()

        Spacer(
            modifier =
                Modifier.width(
                    3.dp
                )
        )

        Text(
            text =
                text,

            style =
                MaterialTheme
                    .typography
                    .labelSmall,

            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
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
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    color =
                        MaterialTheme
                            .colorScheme
                            .surface
                )
                .windowInsetsPadding(
                    WindowInsets.navigationBars
                )
                .imePadding()
                .padding(8.dp),

        verticalAlignment =
            Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value =
                value,

            onValueChange =
                onValueChange,

            modifier =
                Modifier.weight(1f),

            enabled =
                enabled,

            placeholder = {
                Text(
                    text =
                        "Message"
                )
            },

            maxLines =
                5
        )

        IconButton(
            onClick =
                onSendClick,

            enabled =
                enabled &&
                        value.isNotBlank()
        ) {
            Icon(
                imageVector =
                    Icons.AutoMirrored
                        .Filled
                        .Send,

                contentDescription =
                    "Send message"
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
        modifier =
            modifier.padding(
                horizontal = 32.dp
            ),

        contentAlignment =
            Alignment.Center
    ) {
        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Text(
                text =
                    "Start a conversation with $contactName",

                style =
                    MaterialTheme
                        .typography
                        .bodyLarge,

                textAlign =
                    TextAlign.Center
            )

            Text(
                text =
                    when (securityState) {
                        ContactSecurityState
                            .NO_REMOTE_PUBLIC_KEYS -> {
                            "This contact has no SecureChat public keys. Messages use plaintext transport."
                        }

                        ContactSecurityState
                            .ONE_WAY_KEYS -> {
                            "You have this contact’s public keys, but they do not have yours yet. Messages remain plaintext."
                        }

                        ContactSecurityState
                            .MUTUAL_KEYS_UNVERIFIED -> {
                            "Messages are encrypted. Compare the safety number through a trusted channel."
                        }

                        ContactSecurityState
                            .MUTUAL_KEYS_VERIFIED -> {
                            "Messages use the verified SecureChat identity."
                        }
                    },

                modifier =
                    Modifier.padding(
                        top = 8.dp
                    ),

                style =
                    MaterialTheme
                        .typography
                        .bodySmall,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,

                textAlign =
                    TextAlign.Center
            )
        }
    }
}

@Composable
private fun LoadingChatContent(
    modifier: Modifier = Modifier
) {
    Box(
        modifier =
            modifier,

        contentAlignment =
            Alignment.Center
    ) {
        Text(
            text =
                "Loading contact…",

            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    Text(
        text =
            message,

        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme
                        .colorScheme
                        .errorContainer
                )
                .padding(
                    horizontal = 16.dp,
                    vertical = 8.dp
                ),

        color =
            MaterialTheme
                .colorScheme
                .onErrorContainer,

        style =
            MaterialTheme
                .typography
                .bodySmall,

        fontStyle =
            FontStyle.Italic,

        textAlign =
            TextAlign.Center
    )
}