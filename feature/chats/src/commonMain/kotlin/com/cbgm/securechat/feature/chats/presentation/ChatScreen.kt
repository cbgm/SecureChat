package com.cbgm.securechat.feature.chats.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Warning
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    uiState: ChatUiState,
    onMessageTextChanged: (String) -> Unit,
    onSendClick: () -> Unit,
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
                            uiState
                                .contactSecurityState,

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
            ContactSecurityState.NO_PUBLIC_KEY -> {
                "Not end-to-end encrypted"
            }

            ContactSecurityState.PUBLIC_KEY_UNVERIFIED -> {
                "Encrypted · identity unverified"
            }

            ContactSecurityState.PUBLIC_KEY_VERIFIED -> {
                "End-to-end encrypted"
            }
        }

    Text(
        text = text,

        modifier = modifier,

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
            ContactSecurityState.NO_PUBLIC_KEY -> {
                Icons.Default.LockOpen
            }

            ContactSecurityState.PUBLIC_KEY_UNVERIFIED -> {
                Icons.Default.Warning
            }

            ContactSecurityState.PUBLIC_KEY_VERIFIED -> {
                Icons.Default.Lock
            }
        }

    val title =
        when (securityState) {
            ContactSecurityState.NO_PUBLIC_KEY -> {
                "Messages are not end-to-end encrypted"
            }

            ContactSecurityState.PUBLIC_KEY_UNVERIFIED -> {
                "Encrypted using an unverified identity"
            }

            ContactSecurityState.PUBLIC_KEY_VERIFIED -> {
                "End-to-end encrypted"
            }
        }

    val description =
        when (securityState) {
            ContactSecurityState.NO_PUBLIC_KEY -> {
                "This phone-book contact has no SecureChat public key. Messages can still be sent normally."
            }

            ContactSecurityState.PUBLIC_KEY_UNVERIFIED -> {
                "This contact has a public encryption key, but you have not verified the identity yet."
            }

            ContactSecurityState.PUBLIC_KEY_VERIFIED -> {
                "Messages use this contact's verified SecureChat public identity."
            }
        }

    Surface(
        modifier =
            modifier.fillMaxWidth(),

        color =
            when (securityState) {
                ContactSecurityState.NO_PUBLIC_KEY -> {
                    MaterialTheme
                        .colorScheme
                        .errorContainer
                }

                ContactSecurityState.PUBLIC_KEY_UNVERIFIED -> {
                    MaterialTheme
                        .colorScheme
                        .secondaryContainer
                }

                ContactSecurityState.PUBLIC_KEY_VERIFIED -> {
                    MaterialTheme
                        .colorScheme
                        .primaryContainer
                }
            }
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
                imageVector = icon,

                contentDescription = null,

                tint =
                    when (securityState) {
                        ContactSecurityState.NO_PUBLIC_KEY -> {
                            MaterialTheme
                                .colorScheme
                                .onErrorContainer
                        }

                        ContactSecurityState.PUBLIC_KEY_UNVERIFIED -> {
                            MaterialTheme
                                .colorScheme
                                .onSecondaryContainer
                        }

                        ContactSecurityState.PUBLIC_KEY_VERIFIED -> {
                            MaterialTheme
                                .colorScheme
                                .onPrimaryContainer
                        }
                    }
            )

            Column(
                modifier =
                    Modifier
                        .padding(start = 12.dp)
                        .weight(1f)
            ) {
                Text(
                    text = title,

                    style =
                        MaterialTheme
                            .typography
                            .titleSmall
                )

                Text(
                    text = description,

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

        reverseLayout = true
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
                    message
            )
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
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
            horizontalAlignment =
                if (message.isMine) {
                    Alignment.End
                } else {
                    Alignment.Start
                }
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(
                            fraction = 0.78f
                        )
                        .background(
                            color =
                                if (message.isMine) {
                                    MaterialTheme
                                        .colorScheme
                                        .primaryContainer
                                } else {
                                    MaterialTheme
                                        .colorScheme
                                        .surfaceVariant
                                },

                            shape =
                                RoundedCornerShape(
                                    size = 16.dp
                                )
                        )
                        .padding(
                            horizontal = 16.dp,
                            vertical = 10.dp
                        )
            ) {
                Text(
                    text =
                        message.text,

                    color =
                        if (message.isMine) {
                            MaterialTheme
                                .colorScheme
                                .onPrimaryContainer
                        } else {
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                        }
                )
            }
        }
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
            value = value,

            onValueChange =
                onValueChange,

            modifier =
                Modifier.weight(1f),

            enabled = enabled,

            placeholder = {
                Text(
                    text = "Message"
                )
            },

            maxLines = 5
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
                        ContactSecurityState.NO_PUBLIC_KEY -> {
                            "This conversation is currently not end-to-end encrypted."
                        }

                        ContactSecurityState.PUBLIC_KEY_UNVERIFIED -> {
                            "Messages will use the imported public key. Verify the identity when possible."
                        }

                        ContactSecurityState.PUBLIC_KEY_VERIFIED -> {
                            "Messages will use the verified SecureChat identity."
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
        modifier = modifier,

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
        text = message,

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