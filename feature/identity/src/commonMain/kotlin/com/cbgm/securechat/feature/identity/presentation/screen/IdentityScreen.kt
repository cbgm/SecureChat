package com.cbgm.securechat.feature.identity.presentation.screen

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.feature_identity_identity_ready
import org.jetbrains.compose.resources.stringResource

private val Field = Color(0xFF102A46)

@Composable
fun IdentityScreen(
    uiState: IdentityUiState,
    onRequestPhoneNumberHint: () -> Unit,
    onPhoneNumberChanged: (String) -> Unit,
    onCreateIdentity: () -> Unit,
    onRetry: () -> Unit,
    onShareIdentity: () -> Unit,
    scrollState: ScrollState,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding(),
                start = MaterialTheme.spacing.screenPadding,
                end = MaterialTheme.spacing.screenPadding,
            ),
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
                    onCreateIdentity = onCreateIdentity,
                )
            }

            is IdentityUiState.Ready -> {
                ReadyIdentityContent(
                    publicIdentity = uiState.publicIdentity,
                    localPhoneNumber = uiState.localPhoneNumber,
                    onShareIdentity = onShareIdentity,
                )
            }

            IdentityUiState.IncompleteIdentity -> {
                IncompleteIdentityContent(onRetry = onRetry)
            }

            is IdentityUiState.Error -> {
                ErrorContent(message = uiState.message, onRetry = onRetry)
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        Text(
            text = "Checking secure identity…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun IconBadge(
    icon: ImageVector,
    tint: Color = MaterialTheme.colorScheme.secondary,
) {
    Box(
        modifier =
            Modifier
                .size(80.dp)
                .background(tint.copy(alpha = 0.12f), CircleShape)
                .border(1.dp, tint.copy(alpha = 0.3f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(36.dp),
        )
    }
}

@Composable
private fun NoIdentityContent(
    phoneNumber: String,
    phoneNumberError: String?,
    onRequestPhoneNumberHint: () -> Unit,
    onPhoneNumberChanged: (String) -> Unit,
    onCreateIdentity: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        IconBadge(icon = Icons.Default.Shield)

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        Text(
            text = "SecureChat",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Enter your phone number and create your cryptographic identity.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

        SecureChatCard {
            Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
                OutlinedButton(
                    onClick = onRequestPhoneNumberHint,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "Choose phone number from device")
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = onPhoneNumberChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = "Your phone number") },
                    placeholder = { Text(text = "+491701234567") },
                    supportingText = {
                        Text(
                            text =
                                phoneNumberError
                                    ?: "This number becomes your stable SecureChat relay address.",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                    isError = phoneNumberError != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    textStyle =
                        MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedContainerColor = Field,
                            unfocusedContainerColor = Field,
                            focusedBorderColor = MaterialTheme.colorScheme.secondary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f),
                            focusedLabelColor = MaterialTheme.colorScheme.secondary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                            cursorColor = MaterialTheme.colorScheme.secondary,
                            errorBorderColor = MaterialTheme.colorScheme.error,
                            errorLabelColor = MaterialTheme.colorScheme.error,
                            errorCursorColor = MaterialTheme.colorScheme.error,
                        ),
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                SecureChatApprovalButton(
                    onClick = onCreateIdentity,
                    enabled = phoneNumber.isNotBlank(),
                    text = "Approve number and create identity",
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
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        IconBadge(icon = Icons.Default.VerifiedUser)

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        Text(
            text = stringResource(Res.string.feature_identity_identity_ready),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Your private keys are protected locally",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Field,
        ) {
            Text(
                text = localPhoneNumber,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

        SecureChatCard {
            Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
                PublicKeySection(
                    icon = Icons.Default.Lock,
                    title = "Encryption public key",
                    description = "Used for encrypted conversations.",
                    key = publicIdentity.encryptionPublicKey,
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                PublicKeySection(
                    icon = Icons.Default.Key,
                    title = "Signing public key",
                    description = "Used to verify identity information.",
                    key = publicIdentity.signingPublicKey,
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                Button(
                    onClick = onShareIdentity,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = Color(0xFF071A2E),
                        ),
                ) {
                    Text(text = "Share my identity", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun PublicKeySection(
    icon: ImageVector,
    title: String,
    description: String,
    key: ByteArray,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp),
            )

            Spacer(modifier = Modifier.size(MaterialTheme.spacing.base))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))

        Text(
            text = key.toHexString(),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(color = Field, shape = MaterialTheme.shapes.medium)
                    .padding(MaterialTheme.spacing.base),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun IncompleteIdentityContent(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        IconBadge(icon = Icons.Default.ErrorOutline, tint = MaterialTheme.colorScheme.error)

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        Text(
            text = "Incomplete identity",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        Text(
            text = "Only part of your identity is available. Replacement keys will not be generated automatically.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        SecureChatApprovalButton(onClick = onRetry, text = "Check again")
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        IconBadge(icon = Icons.Default.ErrorOutline, tint = MaterialTheme.colorScheme.error)

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        SecureChatApprovalButton(onClick = onRetry, text = "Retry")
    }
}

@Preview(showBackground = true)
@Composable
private fun NoIdentityPreview() {
    SecureChatTheme {
        IdentityScreen(
            uiState = IdentityUiState.NoIdentity(phoneNumber = "+491701111111"),
            onRequestPhoneNumberHint = {},
            onPhoneNumberChanged = {},
            onCreateIdentity = {},
            onRetry = {},
            onShareIdentity = {},
            scrollState = ScrollState(0),
            innerPadding = PaddingValues(0.dp),
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
                    publicIdentity =
                        PublicIdentity(
                            encryptionPublicKey = byteArrayOf(1, 2, 3),
                            signingPublicKey = byteArrayOf(4, 5, 6),
                        ),
                    localPhoneNumber = "+491701111111",
                ),
            onRequestPhoneNumberHint = {},
            onPhoneNumberChanged = {},
            onCreateIdentity = {},
            onRetry = {},
            onShareIdentity = {},
            scrollState = ScrollState(0),
            innerPadding = PaddingValues(0.dp),
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
            scrollState = ScrollState(0),
            innerPadding = PaddingValues(0.dp),
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
            scrollState = ScrollState(0),
            innerPadding = PaddingValues(0.dp),
        )
    }
}
