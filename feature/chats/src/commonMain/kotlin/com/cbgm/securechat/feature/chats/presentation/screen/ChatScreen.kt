package com.cbgm.securechat.feature.chats.presentation.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.chats.domain.model.ChatMessage
import com.cbgm.securechat.feature.chats.domain.model.ContactSecurityState
import com.cbgm.securechat.feature.chats.domain.model.MessageContentStatus
import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryStatus
import com.cbgm.securechat.feature.chats.domain.model.MessageSecurity
import com.cbgm.securechat.feature.chats.presentation.model.ChatUiState
import com.cbgm.securechat.feature.chats.presentation.screen.component.ContactAvatar
import com.cbgm.securechat.feature.chats.presentation.screen.component.PatternBackground

private val Field = Color(0xFF102A46)
private val IncomingBubbleColor = Color(0xFF17324D)

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
    val chatListState = rememberLazyListState()

    val barsState = rememberMainBarsState(chatListState = chatListState)

    val topBarColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.background.copy(alpha = barsState.topBarAlpha),
        label = "TopBarColor"
    )

    val bottomBarColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.background.copy(alpha = barsState.bottomBarAlpha),
        label = "BottomBarColor"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        PatternBackground(
            modifier = Modifier.matchParentSize(),
            backgroundColor = MaterialTheme.colorScheme.background,
            alpha = 0.04f
        )

        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = Color.Transparent,

            topBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = topBarColor,
                            scrolledContainerColor = topBarColor,
                            titleContentColor = MaterialTheme.colorScheme.onBackground,
                            actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                            navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                        ),
                        title = {
                            Row(
                                modifier = Modifier.clickable(onClick = onClickHeader),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ContactAvatar(name = uiState.contactName, size = 36.dp)

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = uiState.contactName,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    )

                    SecurityBanner(
                        securityState = uiState.contactSecurityState,
                        onVerifyIdentity = onVerifyIdentity
                    )

                    uiState.errorMessage?.let { errorMessage ->
                        ErrorMessage(message = errorMessage)
                    }
                }
            },

            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bottomBarColor)
                ) {
                    MessageInput(
                        value = uiState.messageText,
                        onValueChange = onMessageTextChanged,
                        onSendClick = onSendClick,
                        enabled = !uiState.isLoadingContact
                    )
                }
            }
        ) { innerPadding ->
            when {
                uiState.isLoadingContact -> {
                    LoadingChatContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }

                uiState.messages.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        EmptyChatContent(
                            contactName = uiState.contactName,
                            securityState = uiState.contactSecurityState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                }

                else -> {
                    MessageList(
                        messages = uiState.messages,
                        listState = chatListState,
                        onRetryMessage = onRetryMessage,
                        topPadding = innerPadding.calculateTopPadding(),
                        bottomPadding = innerPadding.calculateBottomPadding(),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
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
            description = "You do not have this contact's SecureChat public keys.",
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

        // MUTUAL_KEYS_VERIFIED is handled above and returns early — no branch
        // needed here, so the dead commented-out entry that used to live in
        // this `when` has been removed.
        ContactSecurityState.MUTUAL_KEYS_VERIFIED -> error("Unreachable")
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
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = combinedState.description,
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }

            if (securityState == ContactSecurityState.MUTUAL_KEYS_UNVERIFIED) {
                TextButton(onClick = onVerifyIdentity) {
                    Text(
                        text = "Verify",
                        style = MaterialTheme.typography.bodySmall,
                        color = combinedState.contentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun VerifiedSecurityIndicator(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = "Verified end-to-end encrypted",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun MessageList(
    messages: List<ChatMessage>,
    listState: LazyListState,
    onRetryMessage: (String) -> Unit,
    topPadding: Dp,
    bottomPadding: Dp,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        state = listState,
        reverseLayout = true,
        contentPadding = PaddingValues(
            start = 12.dp,
            top = topPadding + MaterialTheme.spacing.small,
            end = 12.dp,
            bottom = bottomPadding + MaterialTheme.spacing.small
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = messages,
            key = { message -> message.id }
        ) { message ->
            MessageBubble(
                message = message,
                onRetryClick = { onRetryMessage(message.id) }
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
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
            } else {
                IncomingBubbleColor
            },
            contentColor = MaterialTheme.colorScheme.onBackground,
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
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(fraction = 0.78f),
            horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start
        ) {
            Surface(
                color = bubbleState.bubbleColor,
                contentColor = bubbleState.contentColor,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (message.isMine) 16.dp else 4.dp,
                    bottomEnd = if (message.isMine) 4.dp else 16.dp
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    if (bubbleState.isContentFailed) {
                        Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null)
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
private fun MessageSecurityIndicator(message: ChatMessage) {
    val text = when (message.contentStatus) {
        MessageContentStatus.INVALID_PACKET -> "Invalid packet"
        MessageContentStatus.INVALID_PLAINTEXT_PACKET -> "Invalid plaintext"
        MessageContentStatus.TRANSPORT_DECRYPTION_FAILED -> "Decryption failed"
        MessageContentStatus.READABLE -> when (message.security) {
            MessageSecurity.INSECURE -> "Not encrypted"
            MessageSecurity.END_TO_END_ENCRYPTED -> "Encrypted"
        }
    }

    val icon = when {
        message.contentStatus != MessageContentStatus.READABLE -> Icons.Default.ErrorOutline
        message.security == MessageSecurity.END_TO_END_ENCRYPTED -> Icons.Default.Lock
        else -> Icons.Default.LockOpen
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
        MessageDeliveryStatus.NOT_APPLICABLE -> Unit

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
                            modifier = Modifier.size(14.dp).padding(start = 1.dp)
                        )
                    }
                }
            )
        }

        MessageDeliveryStatus.READ -> {

            DeliveryLabel(
                text = "Read",
                textColor = MaterialTheme.colorScheme.secondary,
                icon = {
                    Row {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp).padding(start = 1.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            )
        }

        MessageDeliveryStatus.FAILED -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
    icon: @Composable () -> Unit,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon()

        Spacer(modifier = Modifier.width(3.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor
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
            .windowInsetsPadding(WindowInsets.navigationBars)
            .imePadding()
            .padding(MaterialTheme.spacing.base),
        verticalAlignment = Alignment.Bottom
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .background(
                    color = Field,
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(
                    horizontal = MaterialTheme.spacing.small + 4.dp,
                    vertical = MaterialTheme.spacing.base
                ),
            enabled = enabled,
            minLines = 1,
            maxLines = 5,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onBackground
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.secondary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
            decorationBox = { innerTextField -> innerTextField() }
        )

        IconButton(
            onClick = onSendClick,
            enabled = enabled && value.isNotBlank(),
            modifier = Modifier.padding(start = 4.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send message",
                tint = if (enabled && value.isNotBlank()) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                }
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
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Text(
                text = when (securityState) {
                    ContactSecurityState.NO_REMOTE_PUBLIC_KEYS ->
                        "This contact has no SecureChat public keys. Messages use plaintext transport."

                    ContactSecurityState.ONE_WAY_KEYS ->
                        "You have this contact's public keys, but they do not have yours yet. Messages remain plaintext."

                    ContactSecurityState.MUTUAL_KEYS_UNVERIFIED ->
                        "Messages are encrypted. Compare the safety number through a trusted channel."

                    ContactSecurityState.MUTUAL_KEYS_VERIFIED ->
                        "Messages use the verified SecureChat identity."
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
private fun LoadingChatContent(modifier: Modifier = Modifier) {
    // Matches the loading treatment used on the identity screen (spinner +
    // label) instead of static text with no motion — a lone static string
    // reads as a stuck/frozen screen while content is actually loading.
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        Text(
            text = "Loading chat…",
            style = MaterialTheme.typography.bodyMedium,
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

@Stable
private data class MainBarsState(
    val topBarAlpha: Float,
    val bottomBarAlpha: Float
)

@Composable
private fun rememberMainBarsState(chatListState: LazyListState): MainBarsState {
    val contentBehindTopBar by remember(chatListState) {
        derivedStateOf { chatListState.canScrollForward }
    }

    val contentBehindBottomBar by remember(chatListState) {
        derivedStateOf { chatListState.canScrollBackward }
    }

    val topBarAlpha by animateFloatAsState(
        targetValue = if (contentBehindTopBar) 0.97f else 1f,
        label = "TopBarAlpha"
    )

    val bottomBarAlpha by animateFloatAsState(
        targetValue = if (contentBehindBottomBar) 0.97f else 1f,
        label = "BottomBarAlpha"
    )

    return MainBarsState(topBarAlpha = topBarAlpha, bottomBarAlpha = bottomBarAlpha)
}

@Preview
@Composable
fun ChatScreenPreview() {
    SecureChatTheme {
        ChatScreen(
            uiState = ChatUiState(
                isLoadingContact = false,
                contactName = "Alex",
                contactSecurityState = ContactSecurityState.MUTUAL_KEYS_VERIFIED,
                errorMessage = "sfsfsjljljljljljlf"
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

@Preview
@Composable
fun ChatScreenMessagesPreview() {
    SecureChatTheme {
        ChatScreen(
            uiState = ChatUiState(
                isLoadingContact = false,
                contactName = "Alex",
                contactSecurityState = ContactSecurityState.MUTUAL_KEYS_VERIFIED,
                errorMessage = null,
                messages = listOf(
                    ChatMessage(
                        id = "1",
                        isMine = true,
                        text = "This is a test goes very long and hopefully brearks right",
                        security = MessageSecurity.INSECURE,
                        contentStatus = MessageContentStatus.READABLE,
                        deliveryStatus = MessageDeliveryStatus.SENDING,
                        timestamp = System.currentTimeMillis(),
                        contactId = "2"
                    ),
                    ChatMessage(
                        id = "2",
                        isMine = false,
                        text = "This is a test goes very long and hopefully brearks right",
                        security = MessageSecurity.END_TO_END_ENCRYPTED,
                        contentStatus = MessageContentStatus.READABLE,
                        deliveryStatus = MessageDeliveryStatus.QUEUED,
                        timestamp = System.currentTimeMillis(),
                        contactId = "1"
                    )
                )
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