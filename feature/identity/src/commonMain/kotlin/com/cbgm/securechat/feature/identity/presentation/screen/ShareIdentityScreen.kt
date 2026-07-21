package com.cbgm.securechat.feature.identity.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.ui.component.SecureChatApprovalButton
import com.cbgm.securechat.core.ui.component.SecureChatCard
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.identity.presentation.model.ShareIdentityUiState
import com.cbgm.securechat.feature.identity.qr.QrCode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareIdentityScreen(
    uiState: ShareIdentityUiState,
    onGenerateClick: () -> Unit,
    onBack: () -> Unit,
    onShareIdentity: () -> Unit,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = true
) {
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showRawIdentity by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = {
                    Text(
                        text = "Share identity",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    if (!uiState.encodedIdentity.isNullOrBlank()) {
                        Box {
                            IconButton(onClick = { showOverflowMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More options"
                                )
                            }

                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (showRawIdentity) "Hide raw identity" else "Show raw identity",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (showRawIdentity) {
                                                Icons.Default.VisibilityOff
                                            } else {
                                                Icons.Default.Visibility
                                            },
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        showRawIdentity = !showRawIdentity
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.encodedIdentity.isNullOrBlank()) {
                IdentityOptionsContent(
                    uiState = uiState,
                    onGenerateClick = onGenerateClick
                )
            } else {
                GeneratedIdentityContent(
                    encodedIdentity = uiState.encodedIdentity,
                    showRawIdentity = showRawIdentity,
                    onShareIdentity = onShareIdentity
                )
            }

            uiState.errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun IdentityOptionsContent(
    uiState: ShareIdentityUiState,
    onGenerateClick: () -> Unit
) {
    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

    // Visual anchor — gives the empty state a focal point instead of jumping
    // straight into two paragraphs of text.
    Box(
        modifier = Modifier
            .size(88.dp)
            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f), CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.QrCode2,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(40.dp)
        )
    }

    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

    Text(
        text = "Create a QR code containing your SecureChat public identity.",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

    Text(
        text = "Your public encryption and signing keys are always included. Your private keys never leave this device.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

    SecureChatApprovalButton(
        onClick = onGenerateClick,
        enabled = !uiState.isGenerating,
        text = if (!uiState.isGenerating) "Create QR code" else "",
        content = {
            if (uiState.isGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.background
                )
            }
        }
    )
}

@Composable
private fun GeneratedIdentityContent(
    encodedIdentity: String,
    showRawIdentity: Boolean,
    onShareIdentity: () -> Unit
) {
    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

    Text(
        text = "Your SecureChat identity",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

    Text(
        text = "Another SecureChat user can scan this QR code to add you.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

    SecureChatCard {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Framing the QR itself in a light card with a thin accent border
            // makes it read as the "object" being shared, not just floating art.
            Box(
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                QrCode(
                    content = encodedIdentity,
                    modifier = Modifier.size(240.dp)
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

            // Filled accent button instead of outlined — this is the primary
            // action on this screen and should carry the most visual weight.
            Button(
                onClick = onShareIdentity,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.background
                )
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.size(MaterialTheme.spacing.base))
                Text(text = "Share identity", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    AnimatedVisibility(
        visible = showRawIdentity,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Column {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RAW IDENTITY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                    .padding(MaterialTheme.spacing.medium)
            ) {
                Text(
                    text = encodedIdentity,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Preview
@Composable
fun ShareIdentityScreenPreview() {
    SecureChatTheme {
        ShareIdentityScreen(
            uiState = ShareIdentityUiState(encodedIdentity = "test identity"),
            onGenerateClick = {},
            onBack = {},
            onShareIdentity = {}
        )
    }
}