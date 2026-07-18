package com.cbgm.securechat.feature.onboarding.presentation.screen.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.cbgm.securechat.core.ui.component.SecureChatApprovalButton
import com.cbgm.securechat.core.ui.component.SecureChatSecondaryButton
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.identity.domain.model.PublicIdentity
import com.cbgm.securechat.feature.identity.presentation.model.IdentityUiState

private val Field = Color(0xFF102A46)


@Composable
fun PhonePage(
    identityState: IdentityUiState,
    isCreating: Boolean,
    canRetryAutomatic: Boolean,
    onChooseAnotherNumber: () -> Unit,
    onRetryAutomaticNumber: () -> Unit,
    onPhoneNumberChanged: (String) -> Unit,
    onApproveAndCreate: () -> Unit,
    onNameChanged: (String) -> Unit
) {
    Column(
        Modifier.padding(MaterialTheme.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        when (identityState) {
            IdentityUiState.Loading -> {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(MaterialTheme.spacing.small))
                Text(
                    text = if (isCreating) "Generating secure identity…" else "Preparing phone setup…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            is IdentityUiState.NoIdentity -> {
                Text(
                    text = "Approve phone number",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(MaterialTheme.spacing.base))
                Text(
                    text = "We use it as your stable contact and routing identity. You can edit it before continuing.",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .74f),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(MaterialTheme.spacing.medium))
                OutlinedTextField(
                    value = identityState.phoneNumber,
                    onValueChange = onPhoneNumberChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Phone number")
                    },
                    placeholder = {
                        Text("+491701234567")
                    },
                    supportingText = {
                        Text(
                            text = identityState.phoneNumberError
                                ?: if (identityState.phoneNumber.isBlank()) "No automatic number found. Enter it manually or choose one." else "Detected automatically. Confirm or change it.",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                    isError = identityState.phoneNumberError != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedContainerColor = Field,
                        unfocusedContainerColor = Field,
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .18f),
                        focusedLabelColor = MaterialTheme.colorScheme.secondary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .72f),
                        cursorColor = MaterialTheme.colorScheme.secondary
                    )
                )
                Spacer(Modifier.height(MaterialTheme.spacing.small))
                OutlinedTextField(
                    value = identityState.name,
                    onValueChange = onNameChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Your name")
                    },
                    placeholder = {
                        Text("Your name")
                    },
                    supportingText = {
                        Text(
                            text = "Input your Name",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedContainerColor = Field,
                        unfocusedContainerColor = Field,
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .18f),
                        focusedLabelColor = MaterialTheme.colorScheme.secondary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .72f),
                        cursorColor = MaterialTheme.colorScheme.secondary
                    )
                )
                Spacer(Modifier.height(MaterialTheme.spacing.base))
                if (canRetryAutomatic) {
                    SecureChatSecondaryButton(
                        onClick = onRetryAutomaticNumber,
                        text = "Try SIM number again"

                    )
                    Spacer(Modifier.height(MaterialTheme.spacing.base))
                }
                SecureChatSecondaryButton(
                    onClick = onChooseAnotherNumber,
                    text = if (identityState.phoneNumber.isBlank()) "Choose phone number" else "Choose another number"
                )
                Spacer(Modifier.height(MaterialTheme.spacing.small))
                SecureChatApprovalButton(
                    onClick = onApproveAndCreate,
                    enabled = identityState.phoneNumber.isNotBlank() && identityState.name.isNotBlank(),
                    text = "Approve and create identity"
                )
            }

            is IdentityUiState.Ready -> {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(MaterialTheme.spacing.medium))
                Text(
                    text = "Identity ready. Opening SecureChat…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            IdentityUiState.IncompleteIdentity -> {
                Text(
                    text = "The local identity is incomplete. SecureChat will not silently replace existing keys.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }


            is IdentityUiState.Error -> {
                Text(
                    text = identityState.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview
@Composable
fun PhonePagePreview() {
    SecureChatTheme {
        PhonePage(
            identityState = IdentityUiState.Ready(
                localPhoneNumber = "445446",
                publicIdentity = PublicIdentity(
                    ByteArray(size = 0),
                    ByteArray(size = 0)
                ),
                ),
            isCreating = false,
            canRetryAutomatic = true,
            onChooseAnotherNumber = {},
            onRetryAutomaticNumber = {},
            onPhoneNumberChanged = {},
            onApproveAndCreate = {},
            onNameChanged = {}
        )
    }
}

@Preview
@Composable
fun PhonePageNoIdentityPreview() {
    SecureChatTheme {
        PhonePage(
            identityState = IdentityUiState.NoIdentity(),
            isCreating = false,
            canRetryAutomatic = true,
            onChooseAnotherNumber = {},
            onRetryAutomaticNumber = {},
            onPhoneNumberChanged = {},
            onApproveAndCreate = {},
            onNameChanged = {}
        )
    }
}