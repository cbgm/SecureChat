package com.cbgm.securechat.feature.identity.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.feature.identity.presentation.model.ShareIdentityUiState
import com.cbgm.securechat.feature.identity.qr.QrCode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareIdentityScreen(
    uiState: ShareIdentityUiState,
    onIncludeDisplayNameChanged: (Boolean) -> Unit,
    onDisplayNameChanged: (String) -> Unit,
    onGenerateClick: () -> Unit,
    onBack: () -> Unit,
    showBackButton: Boolean = true,
    onCopyIdentity: () -> Unit,
    onShareIdentity: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    var showOverflowMenu by
    remember {
        mutableStateOf(false)
    }

    var showRawIdentity by
    remember {
        mutableStateOf(false)
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = {
            SnackbarHost(
                hostState =
                    snackbarHostState
            )
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Share identity"
                    )
                },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(
                            onClick = onBack
                        ) {
                            Icon(
                                imageVector =
                                    Icons.AutoMirrored
                                        .Filled
                                        .ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    if (
                        !uiState
                            .encodedIdentity
                            .isNullOrBlank()
                    ) {
                        Box {
                            IconButton(
                                onClick = {
                                    showOverflowMenu =
                                        true
                                }
                            ) {
                                Icon(
                                    imageVector =
                                        Icons.Default.MoreVert,
                                    contentDescription =
                                        "More options"
                                )
                            }

                            DropdownMenu(
                                expanded =
                                    showOverflowMenu,
                                onDismissRequest = {
                                    showOverflowMenu =
                                        false
                                }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text =
                                                "Copy identity"
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector =
                                                Icons.Default
                                                    .ContentCopy,
                                            contentDescription =
                                                null
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu =
                                            false

                                        onCopyIdentity()
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text =
                                                if (
                                                    showRawIdentity
                                                ) {
                                                    "Hide raw identity"
                                                } else {
                                                    "Show raw identity"
                                                }
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector =
                                                Icons.Default
                                                    .Visibility,
                                            contentDescription =
                                                null
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu =
                                            false

                                        showRawIdentity =
                                            !showRawIdentity
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text =
                                                "Regenerate"
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector =
                                                Icons.Default.Refresh,
                                            contentDescription =
                                                null
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu =
                                            false

                                        onGenerateClick()
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 24.dp,
                    vertical = 20.dp
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            if (
                uiState.encodedIdentity
                    .isNullOrBlank()
            ) {
                IdentityOptionsContent(
                    uiState = uiState,

                    onIncludeDisplayNameChanged =
                        onIncludeDisplayNameChanged,

                    onDisplayNameChanged =
                        onDisplayNameChanged,


                    onGenerateClick =
                        onGenerateClick
                )
            } else {
                GeneratedIdentityContent(
                    encodedIdentity =
                        uiState.encodedIdentity,

                    isGenerating =
                        uiState.isGenerating,

                    showRawIdentity =
                        showRawIdentity,

                    onShareIdentity =
                        onShareIdentity,

                    onRegenerate =
                        onGenerateClick
                )
            }

            uiState.errorMessage
                ?.let { message ->
                    Spacer(
                        modifier =
                            Modifier.height(20.dp)
                    )

                    Text(
                        text = message,
                        color =
                            MaterialTheme
                                .colorScheme
                                .error,
                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,
                        textAlign =
                            TextAlign.Center
                    )
                }
        }
    }
}

@Composable
private fun IdentityOptionsContent(
    uiState: ShareIdentityUiState,
    onIncludeDisplayNameChanged: (Boolean) -> Unit,
    onDisplayNameChanged: (String) -> Unit,
    onGenerateClick: () -> Unit
) {
    Column(
        modifier =
            Modifier.fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text =
                "Create a QR code containing your SecureChat public identity.",
            style =
                MaterialTheme.typography
                    .titleMedium
        )

        Text(
            text =
                "Your public encryption and signing keys are always included. Your private keys never leave this device.",
            style =
                MaterialTheme.typography
                    .bodyMedium,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
        )

        HorizontalDivider()

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Checkbox(
                checked =
                    uiState
                        .includeDisplayName,

                onCheckedChange =
                    onIncludeDisplayNameChanged
            )

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {
                Text(
                    text =
                        "Include display name",
                    style =
                        MaterialTheme.typography
                            .bodyLarge
                )

                Text(
                    text =
                        "Your approved phone number is always included. Add a display name optionally.",
                    style =
                        MaterialTheme.typography
                            .bodySmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }
        }

        OutlinedTextField(
            value = uiState.phoneNumber,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            label = {
                Text(text = "Phone number")
            },
            supportingText = {
                Text(text = "Always included so contacts merge into the same chat.")
            },
            singleLine = true
        )

        if (uiState.includeDisplayName) {
            OutlinedTextField(
                value = uiState.displayName,
                onValueChange = onDisplayNameChanged,
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(text = "Display name")
                },
                singleLine = true
            )
        }

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        Button(
            onClick =
                onGenerateClick,

            enabled =
                !uiState.isGenerating,

            modifier =
                Modifier.fillMaxWidth()
        ) {
            if (uiState.isGenerating) {
                CircularProgressIndicator(
                    modifier =
                        Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text =
                        "Create QR code"
                )
            }
        }
    }
}

@Composable
private fun GeneratedIdentityContent(
    encodedIdentity: String,
    isGenerating: Boolean,
    showRawIdentity: Boolean,
    onShareIdentity: () -> Unit,
    onRegenerate: () -> Unit
) {
    Text(
        text =
            "Your SecureChat identity",
        style =
            MaterialTheme.typography
                .headlineSmall,
        textAlign =
            TextAlign.Center
    )

    Spacer(
        modifier =
            Modifier.height(8.dp)
    )

    Text(
        text =
            "Another SecureChat user can scan this QR code to add you.",
        style =
            MaterialTheme.typography
                .bodyMedium,
        color =
            MaterialTheme.colorScheme
                .onSurfaceVariant,
        textAlign =
            TextAlign.Center
    )

    Spacer(
        modifier =
            Modifier.height(24.dp)
    )

    QrCode(
        content =
            encodedIdentity,
        modifier =
            Modifier.size(280.dp)
    )

    Spacer(
        modifier =
            Modifier.height(24.dp)
    )

    Button(
        onClick =
            onShareIdentity,

        modifier =
            Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector =
                Icons.Default.Share,
            contentDescription = null
        )

        Spacer(
            modifier =
                Modifier.size(8.dp)
        )

        Text(
            text =
                "Share identity"
        )
    }

    Spacer(
        modifier =
            Modifier.height(8.dp)
    )

    TextButton(
        onClick =
            onRegenerate,

        enabled =
            !isGenerating
    ) {
        if (isGenerating) {
            CircularProgressIndicator(
                modifier =
                    Modifier.size(18.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text =
                    "Change display name"
            )
        }
    }

    if (showRawIdentity) {
        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        HorizontalDivider()

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        Text(
            text =
                "Raw identity",
            style =
                MaterialTheme.typography
                    .titleSmall,
            modifier =
                Modifier.fillMaxWidth()
        )

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        Text(
            text =
                encodedIdentity,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            style =
                MaterialTheme.typography
                    .bodySmall,
            fontFamily =
                FontFamily.Monospace
        )
    }
}
