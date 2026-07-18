package com.cbgm.securechat.feature.identity.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.ui.component.SecureChatApprovalButton
import com.cbgm.securechat.core.ui.component.SecureChatCard
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.identity.presentation.model.ShareIdentityUiState
import com.cbgm.securechat.feature.identity.qr.QrCode

val SecureChatChromeColor = Color(0xFF071A2E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareIdentityScreen(
    uiState: ShareIdentityUiState,
    onGenerateClick: () -> Unit,
    onBack: () -> Unit,
    onCopyIdentity: () -> Unit,
    onShareIdentity: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = true
) {
    var showOverflowMenu by remember { mutableStateOf(false) }

    var showRawIdentity by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
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
                        style = MaterialTheme.typography.titleSmall
                    )
                },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(
                            onClick = onBack
                        ) {
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
                              IconButton(
                                  onClick = {
                                      showOverflowMenu = true
                                  }
                              ) {
                                  Icon(
                                      imageVector = Icons.Default.MoreVert,
                                      contentDescription = "More options"
                                  )
                              }

                              DropdownMenu(
                                  expanded = showOverflowMenu,
                                  onDismissRequest = {
                                      showOverflowMenu = false
                                  },
                                  modifier = Modifier.background(MaterialTheme.colorScheme.onBackground)
                              ) {

                                  DropdownMenuItem(
                                      text = {
                                          Text(
                                              text = if (showRawIdentity) {
                                                  "Hide raw identity"
                                              } else {
                                                  "Show raw identity"
                                              }
                                          )
                                      },
                                      leadingIcon = {
                                          Icon(
                                              imageVector = Icons.Default.Visibility,
                                              contentDescription = null,
                                              tint = MaterialTheme.colorScheme.onSurface
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
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 24.dp,
                    vertical = 20.dp
                ),
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
                    isGenerating = uiState.isGenerating,
                    showRawIdentity = showRawIdentity,
                    onShareIdentity = onShareIdentity,
                    onRegenerate = onGenerateClick
                )
            }

            uiState.errorMessage
                ?.let { message ->
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        Text(
            text = "Create a QR code containing your SecureChat public identity.",
            style = MaterialTheme.typography.titleSmall
        )

        Text(
            text = "Your public encryption and signing keys are always included. Your private keys never leave this device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        HorizontalDivider()

        SecureChatApprovalButton(
            onClick = onGenerateClick,
            enabled = !uiState.isGenerating,
            text = if (!uiState.isGenerating) "Create QR code" else "",
            content = {
                if (uiState.isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        )
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
        text = "Your SecureChat identity",
        style = MaterialTheme.typography.titleSmall,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))

    Text(
        text = "Another SecureChat user can scan this QR code to add you.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

    SecureChatCard {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            QrCode(
                content = encodedIdentity,
                modifier = Modifier.size(280.dp)
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

            OutlinedButton(
                onClick = onShareIdentity,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.size(MaterialTheme.spacing.base))

                Text(text = "Share identity")
            }
        }
    }


    Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))

    if (showRawIdentity) {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        Text(
            text = "Raw identity",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))

        Text(
            text = encodedIdentity,
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.small),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace
        )
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
            onCopyIdentity = {},
            onShareIdentity = {},
            snackbarHostState = SnackbarHostState()
        )
    }
}
