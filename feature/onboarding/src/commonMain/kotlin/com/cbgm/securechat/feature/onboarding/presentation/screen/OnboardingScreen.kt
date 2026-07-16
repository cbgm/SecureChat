package com.cbgm.securechat.feature.onboarding.presentation.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import com.cbgm.securechat.core.ui.component.PulsingLogo
import com.cbgm.securechat.core.ui.component.SecureChatApprovalButton
import com.cbgm.securechat.core.ui.component.SecureChatCard
import com.cbgm.securechat.core.ui.component.SecureChatSecondaryButton
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.identity.domain.model.PublicIdentity
import com.cbgm.securechat.feature.identity.presentation.model.IdentityUiState
import com.cbgm.securechat.feature.onboarding.presentation.model.OnboardingPage
import com.cbgm.securechat.feature.onboarding.presentation.model.OnboardingUiState

private val Field = Color(0xFF102A46)

@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    identityState: IdentityUiState,
    onNext: () -> Unit,
    onRequestPermissions: () -> Unit,
    onChooseAnotherNumber: () -> Unit,
    onRetryAutomaticNumber: () -> Unit,
    onPhoneNumberChanged: (String) -> Unit,
    onApproveAndCreate: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
            .padding(MaterialTheme.spacing.screenPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PulsingLogo(modifier = Modifier.size(200.dp))
            Spacer(Modifier.height(MaterialTheme.spacing.small))
            Text(
                text = "SecureChat",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Private. Encrypted. Yours.",
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .72f),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(MaterialTheme.spacing.medium))

            SecureChatCard {
                AnimatedContent(
                    targetState = state.page,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "onboardingPage"
                ) { page ->
                    Column(
                        Modifier.padding(MaterialTheme.spacing.times(3)),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        when (page) {
                            OnboardingPage.WELCOME -> WelcomePage(onNext)
                            OnboardingPage.PRIVACY -> PrivacyPage(onNext)
                            OnboardingPage.PERMISSIONS -> PermissionsPage(
                                onRequestPermissions
                            )

                            OnboardingPage.PHONE -> PhonePage(
                                identityState = identityState,
                                isCreating = state.isCreatingIdentity,
                                canRetryAutomatic = state.phonePermissionGranted,
                                onChooseAnotherNumber = onChooseAnotherNumber,
                                onRetryAutomaticNumber = onRetryAutomaticNumber,
                                onPhoneNumberChanged = onPhoneNumberChanged,
                                onApproveAndCreate = onApproveAndCreate
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WelcomePage(onNext: () -> Unit) {
    Text(
        text = "Welcome to SecureChat",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onPrimary,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(MaterialTheme.spacing.small))
    Text(
        text = "A private messenger built around end-to-end encryption and an identity that stays on your device.",
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .74f),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(MaterialTheme.spacing.medium))
    SecureChatApprovalButton(
        onClick = onNext,
        text = "Continue"
    )
}

@Composable
private fun PrivacyPage(onNext: () -> Unit) {
    Text(
        text = "Your privacy comes first",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onPrimary,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(MaterialTheme.spacing.small))
    ListingRow(
        index = "01",
        title = "End-to-end encryption",
        description = "Messages are encrypted automatically when both identities are available."
    )
    ListingRow(
        index = "02",
        title = "Your contacts stay local",
        description = "Contacts are used to match people by phone number on your device."
    )
    ListingRow(
        index = "03",
        title = "Your identity belongs to you",
        description = "Private identity keys remain protected on this device."
    )
    Spacer(Modifier.height(MaterialTheme.spacing.medium))
    SecureChatApprovalButton(
        onClick = onNext,
        text = "Continue"
    )
}

@Composable
private fun PermissionsPage(onRequestPermissions: () -> Unit) {

    Text(
        text = "Permissions",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onPrimary,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(MaterialTheme.spacing.base))
    Text(
        text = "SecureChat asks only for features you choose to use.",
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .74f),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(MaterialTheme.spacing.medium))
    ListingRow(
        index = "01",
        title = "Notifications",
        description = "Receive new-message alerts."
    )
    ListingRow(
        index = "02",
        title = "Contacts",
        description = "Find existing phone-book contacts."
    )
    ListingRow(
        index = "03",
        title = "Camera",
        description = "Scan SecureChat identity QR codes."
    )
    ListingRow(
        index = "04",
        title = "Phone number",
        description = "Try to fill your SIM number automatically without opening a picker."
    )
    Spacer(Modifier.height(MaterialTheme.spacing.medium))
    SecureChatApprovalButton(
        onClick = onRequestPermissions,
        text = "Allow and continue"
    )
    Spacer(Modifier.height(MaterialTheme.spacing.base))
    Text(
        text = "Denied permissions can be enabled later in system settings.",
        style = MaterialTheme.typography.labelMedium,
        color = Color.White.copy(alpha = .58f),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun PhonePage(
    identityState: IdentityUiState,
    isCreating: Boolean,
    canRetryAutomatic: Boolean,
    onChooseAnotherNumber: () -> Unit,
    onRetryAutomaticNumber: () -> Unit,
    onPhoneNumberChanged: (String) -> Unit,
    onApproveAndCreate: () -> Unit
) {
    when (identityState) {
        IdentityUiState.Loading -> {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(14.dp))
            Text(
                text = if (isCreating) "Generating secure identity…" else "Preparing phone setup…",
                color = Color.White
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
                enabled = identityState.phoneNumber.isNotBlank(),
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

@Composable
private fun ListingRow(index: String, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = MaterialTheme.spacing.base),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = index,
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Column(Modifier.padding(start = MaterialTheme.spacing.medium)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .66f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Preview
@Composable
fun OnboardingScreenPreview() {
    SecureChatTheme {
        OnboardingScreen(
            state = OnboardingUiState(page = OnboardingPage.PERMISSIONS),
            identityState = IdentityUiState.Ready(
                localPhoneNumber = "445446", publicIdentity = PublicIdentity(
                    ByteArray(size = 0), ByteArray(size = 0)
                )
            ),
            onNext = {},
            onRequestPermissions = {},
            onChooseAnotherNumber = {},
            onRetryAutomaticNumber = {},
            onPhoneNumberChanged = {},
            onApproveAndCreate = {}
        )
    }
}

@Preview
@Composable
fun OnboardingScreen2Preview() {
    SecureChatTheme {
        OnboardingScreen(
            state = OnboardingUiState(page = OnboardingPage.PHONE),
            identityState = IdentityUiState.NoIdentity(),
            onNext = {},
            onRequestPermissions = {},
            onChooseAnotherNumber = {},
            onRetryAutomaticNumber = {},
            onPhoneNumberChanged = {},
            onApproveAndCreate = {}
        )
    }
}
