package com.cbgm.securechat.feature.onboarding.presentation.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.ui.component.PulsingLogo
import com.cbgm.securechat.core.ui.component.SecureChatCard
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.identity.domain.model.PublicIdentity
import com.cbgm.securechat.feature.identity.presentation.model.IdentityUiState
import com.cbgm.securechat.feature.onboarding.presentation.model.OnboardingPage
import com.cbgm.securechat.feature.onboarding.presentation.model.OnboardingUiState
import com.cbgm.securechat.feature.onboarding.presentation.screen.pages.PermissionsPage
import com.cbgm.securechat.feature.onboarding.presentation.screen.pages.PhonePage
import com.cbgm.securechat.feature.onboarding.presentation.screen.pages.PrivacyPage
import com.cbgm.securechat.feature.onboarding.presentation.screen.pages.WelcomePage

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
    onApproveAndCreate: () -> Unit,
    onNameChanged: (String) -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primary)
                .padding(MaterialTheme.spacing.screenPadding),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PulsingLogo(modifier = Modifier.size(200.dp))
            Spacer(Modifier.height(MaterialTheme.spacing.small))
            Text(
                text = "SecureChat",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Private. Encrypted. Yours.",
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .72f),
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(MaterialTheme.spacing.medium))

            SecureChatCard {
                AnimatedContent(
                    targetState = state.page,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "onboardingPage",
                ) { page ->

                    when (page) {
                        OnboardingPage.WELCOME -> WelcomePage(onNext)
                        OnboardingPage.PRIVACY -> PrivacyPage(onNext)
                        OnboardingPage.PERMISSIONS ->
                            PermissionsPage(
                                onRequestPermissions,
                            )

                        OnboardingPage.PHONE ->
                            PhonePage(
                                identityState = identityState,
                                isCreating = state.isCreatingIdentity,
                                canRetryAutomatic = state.phonePermissionGranted,
                                onChooseAnotherNumber = onChooseAnotherNumber,
                                onRetryAutomaticNumber = onRetryAutomaticNumber,
                                onPhoneNumberChanged = onPhoneNumberChanged,
                                onApproveAndCreate = onApproveAndCreate,
                                onNameChanged = onNameChanged,
                            )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun OnboardingScreenPreview() {
    SecureChatTheme {
        OnboardingScreen(
            state = OnboardingUiState(page = OnboardingPage.PERMISSIONS),
            identityState =
                IdentityUiState.Ready(
                    localPhoneNumber = "445446",
                    publicIdentity =
                        PublicIdentity(
                            ByteArray(size = 0),
                            ByteArray(size = 0),
                        ),
                ),
            onNext = {},
            onRequestPermissions = {},
            onChooseAnotherNumber = {},
            onRetryAutomaticNumber = {},
            onPhoneNumberChanged = {},
            onApproveAndCreate = {},
            onNameChanged = {},
        )
    }
}

@Preview
@Composable
private fun OnboardingScreen2Preview() {
    SecureChatTheme {
        OnboardingScreen(
            state = OnboardingUiState(page = OnboardingPage.PHONE),
            identityState = IdentityUiState.NoIdentity(),
            onNext = {},
            onRequestPermissions = {},
            onChooseAnotherNumber = {},
            onRetryAutomaticNumber = {},
            onPhoneNumberChanged = {},
            onApproveAndCreate = {},
            onNameChanged = {},
        )
    }
}
