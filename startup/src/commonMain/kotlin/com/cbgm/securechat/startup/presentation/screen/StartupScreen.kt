package com.cbgm.securechat.startup.presentation.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.ui.component.PulsingLogo
import com.cbgm.securechat.core.ui.component.SecureChatApprovalButton
import com.cbgm.securechat.core.ui.component.SecureChatCard
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.identity.presentation.model.IdentityUiState
import com.cbgm.securechat.startup.presentation.model.StartupUiState

@Composable
fun StartupScreen(
    uiState: StartupUiState,
    identityUiState: IdentityUiState,
    onRequestPhoneNumberHint: () -> Unit,
    onPhoneNumberChanged: (String) -> Unit,
    onCreateIdentity: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.primary)
            .padding(MaterialTheme.spacing.screenPadding)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PulsingLogo(modifier = Modifier.size(200.dp))

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "SecureChat",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))

            Text(
                text = "Private. Encrypted. Yours.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f)
            )

            Spacer(
                modifier = Modifier.height(MaterialTheme.spacing.medium)
            )

            SecureChatCard(modifier = Modifier.widthIn(max = 520.dp)) {
                AnimatedContent(
                    targetState = uiState,
                    transitionSpec = {
                        fadeIn(
                            animationSpec = tween(300)
                        ) togetherWith fadeOut(
                            animationSpec = tween(180)
                        )
                    },
                    label = "startupState"
                ) { state ->
                    Box(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
                        StartupStateContent(
                            uiState = state,
                            identityUiState = identityUiState,
                            onRequestPhoneNumberHint = onRequestPhoneNumberHint,
                            onPhoneNumberChanged = onPhoneNumberChanged,
                            onCreateIdentity = onCreateIdentity,
                            onRetry = onRetry
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
        }
    }
}

@Composable
private fun StartupStateContent(
    uiState: StartupUiState,
    identityUiState: IdentityUiState,
    onRequestPhoneNumberHint: () -> Unit,
    onPhoneNumberChanged: (String) -> Unit,
    onCreateIdentity: () -> Unit,
    onRetry: () -> Unit
) {
    when (uiState) {
        StartupUiState.Loading -> {
            StartupProgress(message = "Preparing SecureChat…")
        }

        StartupUiState.Ready -> {
            StartupProgress(message = "Opening SecureChat…")
        }

        StartupUiState.IdentityRequired -> {
            StartupIdentityContent(
                identityUiState = identityUiState,
                onRequestPhoneNumberHint = onRequestPhoneNumberHint,
                onPhoneNumberChanged = onPhoneNumberChanged,
                onCreateIdentity = onCreateIdentity,
                onRetry = onRetry
            )
        }

        is StartupUiState.Error -> {
            StartupErrorContent(
                message = uiState.message,
                onRetry = onRetry
            )
        }
    }
}

@Composable
private fun StartupIdentityContent(
    identityUiState: IdentityUiState,
    onRequestPhoneNumberHint: () -> Unit,
    onPhoneNumberChanged: (String) -> Unit,
    onCreateIdentity: () -> Unit,
    onRetry: () -> Unit
) {
    when (identityUiState) {
        IdentityUiState.Loading -> {
            StartupProgress(message = "Generating secure identity…")
        }

        is IdentityUiState.NoIdentity -> {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Verify your phone number",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Spacer(
                    modifier = Modifier.height(MaterialTheme.spacing.base)
                )

                Text(
                    text = "Your contacts use your phone number to securely find you on SecureChat.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                    textAlign = TextAlign.Center
                )

                Spacer(
                    modifier = Modifier.height(MaterialTheme.spacing.medium)
                )

                OutlinedTextField(
                    value = identityUiState.phoneNumber,
                    onValueChange = onPhoneNumberChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(text = "Phone number")
                    },
                    placeholder = {
                        Text(text = "+491701234567", style = MaterialTheme.typography.bodyMedium,)
                    },
                    supportingText = {
                        Text(
                            text = identityUiState.phoneNumberError
                                ?: if (identityUiState.phoneNumber.isBlank()
                                ) {
                                    "Choose a number from your device or enter it manually."
                                } else {
                                    "Detected automatically. You can edit it or choose another number."
                                }
                        )
                    },
                    isError = identityUiState.phoneNumberError != null,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.60f),
                        focusedContainerColor = StartupPhoneFieldBackground,
                        unfocusedContainerColor = StartupPhoneFieldBackground,
                        errorContainerColor = StartupPhoneFieldBackground,
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f),
                        errorBorderColor = MaterialTheme.colorScheme.error,
                        focusedLabelColor = MaterialTheme.colorScheme.secondary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.76f),
                        errorLabelColor = MaterialTheme.colorScheme.error,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                        focusedSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.66f),
                        unfocusedSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.66f),
                        errorSupportingTextColor = MaterialTheme.colorScheme.error,
                        cursorColor = MaterialTheme.colorScheme.secondary,
                        errorCursorColor = MaterialTheme.colorScheme.error
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                OutlinedButton(
                    onClick = onRequestPhoneNumberHint,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (identityUiState.phoneNumber.isBlank()) {
                            "Choose phone number"
                        } else {
                            "Choose another number"
                        }
                    )
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                SecureChatApprovalButton(
                    onClick = onCreateIdentity,
                    enabled = identityUiState.phoneNumber.isNotBlank(),
                    text = "Continue"
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))

                Text(
                    text = "Your encryption keys are generated only after you approve your number.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        is IdentityUiState.Ready -> {
            StartupProgress(message = "Identity ready. Opening SecureChat…")
        }

        IdentityUiState.IncompleteIdentity -> {
            StartupErrorContent(
                message = "Only part of the local identity is available. SecureChat will not generate replacement keys automatically.",
                onRetry = onRetry
            )
        }

        is IdentityUiState.Error -> {
            StartupErrorContent(
                message = identityUiState.message,
                onRetry = onRetry
            )
        }
    }
}

@Composable
private fun StartupErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SecureChat could not finish setup.",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(
            modifier = Modifier.height(MaterialTheme.spacing.base)
        )

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        SecureChatApprovalButton(
            onClick = onRetry,
            text = "Retry"
        )
    }
}

@Composable
private fun StartupProgress(
    message: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
            textAlign = TextAlign.Center
        )
    }
}

@Preview
@Composable
fun StartupScreenPreview() {
    SecureChatTheme {
        StartupScreen(
            uiState = StartupUiState.IdentityRequired,
            identityUiState = IdentityUiState.NoIdentity(),
            onRequestPhoneNumberHint = {},
            onPhoneNumberChanged = {},
            onCreateIdentity = {},
            onRetry = {}
        )
    }
}

private val StartupPhoneFieldBackground =
    Color(0xFF0B2035)
