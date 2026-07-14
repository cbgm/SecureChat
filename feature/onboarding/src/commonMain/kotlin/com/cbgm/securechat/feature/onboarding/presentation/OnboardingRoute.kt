package com.cbgm.securechat.feature.onboarding.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.securechat.feature.identity.phone.PhoneNumberHintLauncher
import com.cbgm.securechat.feature.identity.phone.PhoneNumberHintResult
import com.cbgm.securechat.feature.identity.presentation.model.IdentityUiState
import com.cbgm.securechat.feature.identity.presentation.screen.IdentityViewModel
import com.cbgm.securechat.feature.onboarding.platform.AutomaticPhoneNumberReader
import com.cbgm.securechat.feature.onboarding.platform.AutomaticPhoneNumberResult
import com.cbgm.securechat.feature.onboarding.platform.OnboardingPermissionRequester
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnboardingRoute(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel(),
    identityViewModel: IdentityViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val identityState by identityViewModel.uiState.collectAsStateWithLifecycle()
    var hintRequestId by remember { mutableIntStateOf(0) }

    OnboardingPermissionRequester(
        requestId = state.permissionRequestId,
        onResult = viewModel::onPermissionsResult
    )

    AutomaticPhoneNumberReader(
        requestId = state.automaticPhoneRequestId,
        enabled = state.page == OnboardingPage.PHONE && state.phonePermissionGranted,
        onResult = { result ->
            when (result) {
                is AutomaticPhoneNumberResult.Found ->
                    identityViewModel.onSuggestedPhoneNumber(result.phoneNumber)
                AutomaticPhoneNumberResult.Unavailable -> Unit
                is AutomaticPhoneNumberResult.Failed ->
                    identityViewModel.onPhoneNumberHintFailed(result.message)
            }
        }
    )

    PhoneNumberHintLauncher(
        requestId = hintRequestId,
        enabled = state.page == OnboardingPage.PHONE,
        onResult = { result ->
            when (result) {
                is PhoneNumberHintResult.Selected ->
                    identityViewModel.onSuggestedPhoneNumber(result.phoneNumber)
                PhoneNumberHintResult.Unavailable ->
                    identityViewModel.onPhoneNumberHintUnavailable()
                PhoneNumberHintResult.Cancelled -> Unit
                is PhoneNumberHintResult.Failed ->
                    identityViewModel.onPhoneNumberHintFailed(result.message)
            }
        }
    )

    LaunchedEffect(identityState) {
        when (identityState) {
            is IdentityUiState.Ready -> onComplete()
            IdentityUiState.Loading -> viewModel.setCreatingIdentity(true)
            else -> viewModel.setCreatingIdentity(false)
        }
    }

    OnboardingScreen(
        state = state,
        identityState = identityState,
        onNext = viewModel::next,
        onRequestPermissions = viewModel::requestPermissions,
        onChooseAnotherNumber = { hintRequestId += 1 },
        onRetryAutomaticNumber = viewModel::retryAutomaticPhoneNumber,
        onPhoneNumberChanged = identityViewModel::onPhoneNumberChanged,
        onApproveAndCreate = identityViewModel::createNewIdentity
    )
}
