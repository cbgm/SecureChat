package com.cbgm.securechat.feature.identity.presentation.screen

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.extensions.toHexString
import com.cbgm.securechat.core.ui.component.SecureChatApprovalButton
import com.cbgm.securechat.core.ui.component.SecureChatCard
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.identity.domain.model.PublicIdentity
import com.cbgm.securechat.feature.identity.presentation.model.IdentityUiState

private val Field = Color(0xFF102A46)

@Composable
fun IdentityScreen(
    uiState: IdentityUiState,
    onRequestPhoneNumberHint: () -> Unit,
    onPhoneNumberChanged: (String) -> Unit,
    onCreateIdentity: () -> Unit,
    onRetry: () -> Unit,
    onShareIdentity: () -> Unit,
    onImportContact: () -> Unit,
    onContacts: () -> Unit,
    scrollState: ScrollState,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.primary
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding()
                )
                .verticalScroll(scrollState)
                .padding(MaterialTheme.spacing.screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (uiState) {
                IdentityUiState.Loading -> {
                    LoadingContent()
                }

                is IdentityUiState.NoIdentity -> {
                    NoIdentityContent(
                        phoneNumber = uiState.phoneNumber,
                        phoneNumberError = uiState.phoneNumberError,
                        onRequestPhoneNumberHint = onRequestPhoneNumberHint,
                        onPhoneNumberChanged = onPhoneNumberChanged,
                        onCreateIdentity = onCreateIdentity
                    )
                }

                is IdentityUiState.Ready -> {
                    ReadyIdentityContent(
                        publicIdentity = uiState.publicIdentity,
                        localPhoneNumber = uiState.localPhoneNumber,
                        onShareIdentity = onShareIdentity,
                        onImportContact = onImportContact,
                        onContacts = onContacts
                    )
                }

                IdentityUiState.IncompleteIdentity -> {
                    IncompleteIdentityContent(
                        onRetry = onRetry
                    )
                }

                is IdentityUiState.Error -> {
                    ErrorContent(
                        message = uiState.message,
                        onRetry = onRetry
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(
            modifier = Modifier.height(MaterialTheme.spacing.medium)
        )

        Text(
            text = "Checking secure identity…",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun NoIdentityContent(
    phoneNumber: String,
    phoneNumberError: String?,
    onRequestPhoneNumberHint: () -> Unit,
    onPhoneNumberChanged: (String) -> Unit,
    onCreateIdentity: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SecureChat",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Enter your phone number and create your cryptographic identity.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(
                alpha = 0.72f
            )
        )

        Spacer(
            modifier = Modifier.height(MaterialTheme.spacing.medium)
        )

        Text(
            text = "Use international format, for example +491701234567.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(
            modifier = Modifier.height(MaterialTheme.spacing.medium)
        )

        SecureChatCard {
            Column(
                modifier = Modifier.padding(
                    MaterialTheme.spacing.medium
                )
            ) {
                OutlinedButton(
                    onClick = onRequestPhoneNumberHint,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Choose phone number from device"
                    )
                }

                Spacer(
                    modifier = Modifier.height(
                        MaterialTheme.spacing.small
                    )
                )

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = onPhoneNumberChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            text = "Your phone number"
                        )
                    },
                    placeholder = {
                        Text(
                            text = "+491701234567"
                        )
                    },
                    supportingText = {
                        Text(
                            text = phoneNumberError
                                ?: "This number becomes your stable SecureChat relay address.",
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    isError = phoneNumberError != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone
                    ),
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor =
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedTextColor =
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedContainerColor = Field,
                        unfocusedContainerColor = Field,
                        focusedBorderColor =
                            MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor =
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = 0.18f
                            ),
                        focusedLabelColor =
                            MaterialTheme.colorScheme.secondary,
                        unfocusedLabelColor =
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = 0.72f
                            ),
                        cursorColor =
                            MaterialTheme.colorScheme.secondary,
                        errorBorderColor =
                            MaterialTheme.colorScheme.error,
                        errorLabelColor =
                            MaterialTheme.colorScheme.error,
                        errorCursorColor =
                            MaterialTheme.colorScheme.error
                    )
                )

                Spacer(
                    modifier = Modifier.height(
                        MaterialTheme.spacing.medium
                    )
                )

                SecureChatApprovalButton(
                    onClick = onCreateIdentity,
                    enabled = phoneNumber.isNotBlank(),
                    text = "Approve number and create identity"
                )
            }
        }
    }
}

@Composable
private fun ReadyIdentityContent(
    publicIdentity: PublicIdentity,
    localPhoneNumber: String,
    onShareIdentity: () -> Unit,
    onImportContact: () -> Unit,
    onContacts: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Identity ready",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Your private keys are protected locally",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onPrimary.copy(
                alpha = 0.74f
            )
        )

        Spacer(
            modifier = Modifier.height(MaterialTheme.spacing.small)
        )

        Text(
            text = "Relay phone: $localPhoneNumber",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(
            modifier = Modifier.height(MaterialTheme.spacing.large)
        )

        SecureChatCard {
            Column(
                modifier = Modifier.padding(
                    MaterialTheme.spacing.medium
                )
            ) {
                PublicKeySection(
                    title = "Encryption public key",
                    description = "Used for encrypted conversations.",
                    key = publicIdentity.encryptionPublicKey
                )

                Spacer(
                    modifier = Modifier.height(
                        MaterialTheme.spacing.medium
                    )
                )

                PublicKeySection(
                    title = "Signing public key",
                    description = "Used to verify identity information.",
                    key = publicIdentity.signingPublicKey
                )

                Spacer(
                    modifier = Modifier.height(
                        MaterialTheme.spacing.medium
                    )
                )

                Button(
                    onClick = onShareIdentity,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Share my identity"
                    )
                }
            }
        }
    }
}

@Composable
private fun PublicKeySection(
    title: String,
    description: String,
    key: ByteArray
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(
            modifier = Modifier.height(MaterialTheme.spacing.base)
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(
            modifier = Modifier.height(MaterialTheme.spacing.base)
        )

        Text(
            text = key.toHexString(),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium
                )
                .padding(MaterialTheme.spacing.base),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun IncompleteIdentityContent(
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Incomplete identity",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(MaterialTheme.spacing.small)
        )

        Text(
            text = "Only part of your identity is available. Replacement keys will not be generated automatically.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(
            modifier = Modifier.height(MaterialTheme.spacing.medium)
        )

        SecureChatApprovalButton(
            onClick = onRetry,
            text = "Check again"
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(MaterialTheme.spacing.small)
        )

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )

        Spacer(
            modifier = Modifier.height(MaterialTheme.spacing.medium)
        )

        SecureChatApprovalButton(
            onClick = onRetry,
            text = "Retry"
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NoIdentityPreview() {
    SecureChatTheme {
        IdentityScreen(
            uiState =
                IdentityUiState.NoIdentity(
                    phoneNumber = "+491701111111"
                ),

            onRequestPhoneNumberHint = {},
            onPhoneNumberChanged = {},
            onCreateIdentity = {},
            onRetry = {},
            onShareIdentity = {},
            onImportContact = {},
            onContacts = {},
            scrollState = ScrollState(0),
            innerPadding = PaddingValues(0.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReadyIdentityPreview() {
    SecureChatTheme {
        IdentityScreen(
            uiState =
                IdentityUiState.Ready(
                    publicIdentity = PublicIdentity(
                        encryptionPublicKey = byteArrayOf(
                            1,
                            2,
                            3
                        ),
                        signingPublicKey = byteArrayOf(
                            4,
                            5,
                            6
                        )
                    ),

                    localPhoneNumber = "+491701111111"
                ),

            onRequestPhoneNumberHint = {},
            onPhoneNumberChanged = {},
            onCreateIdentity = {},
            onRetry = {},
            onShareIdentity = {},
            onImportContact = {},
            onContacts = {},
            scrollState = ScrollState(0),
            innerPadding = PaddingValues(0.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun IncompleteIdentityPreview() {
    SecureChatTheme {
        IdentityScreen(
            uiState = IdentityUiState.IncompleteIdentity,
            onRequestPhoneNumberHint = {},
            onPhoneNumberChanged = {},
            onCreateIdentity = {},
            onRetry = {},
            onShareIdentity = {},
            onImportContact = {},
            onContacts = {},
            scrollState = ScrollState(0),
            innerPadding = PaddingValues(0.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingIdentityPreview() {
    SecureChatTheme {
        IdentityScreen(
            uiState = IdentityUiState.Error("gdfgdgdg"),
            onRequestPhoneNumberHint = {},
            onPhoneNumberChanged = {},
            onCreateIdentity = {},
            onRetry = {},
            onShareIdentity = {},
            onImportContact = {},
            onContacts = {},
            scrollState = ScrollState(0),
            innerPadding = PaddingValues(0.dp)
        )
    }
}