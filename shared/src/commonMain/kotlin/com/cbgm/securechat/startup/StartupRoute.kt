package com.cbgm.securechat.startup

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
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun StartupRoute(
    onStartupComplete: () -> Unit,
    startupViewModel: StartupViewModel =
        koinViewModel(),
    identityViewModel: IdentityViewModel =
        koinViewModel()
) {
    val startupUiState by
    startupViewModel
        .uiState
        .collectAsStateWithLifecycle()

    val identityUiState by
    identityViewModel
        .uiState
        .collectAsStateWithLifecycle()

    var phoneNumberHintRequestId by
    remember {
        mutableIntStateOf(0)
    }

    val identityRequired =
        startupUiState ==
                StartupUiState.IdentityRequired

    val canRequestPhoneNumber =
        identityRequired &&
                identityUiState is
                        IdentityUiState.NoIdentity

    PhoneNumberHintLauncher(
        requestId =
            phoneNumberHintRequestId,

        enabled =
            canRequestPhoneNumber,

        onResult = { result ->
            when (result) {
                is PhoneNumberHintResult.Selected -> {
                    identityViewModel
                        .onSuggestedPhoneNumber(
                            phoneNumber =
                                result.phoneNumber
                        )
                }

                PhoneNumberHintResult.Unavailable -> {
                    identityViewModel
                        .onPhoneNumberHintUnavailable()
                }

                PhoneNumberHintResult.Cancelled -> {
                    Unit
                }

                is PhoneNumberHintResult.Failed -> {
                    identityViewModel
                        .onPhoneNumberHintFailed(
                            message =
                                result.message
                        )
                }
            }
        }
    )

    LaunchedEffect(
        startupUiState
    ) {
        when (startupUiState) {
            StartupUiState.Ready -> {
                onStartupComplete()
            }

            StartupUiState.IdentityRequired -> {
                identityViewModel
                    .loadIdentityState()
            }

            StartupUiState.Loading,
            is StartupUiState.Error -> {
                Unit
            }
        }
    }

    LaunchedEffect(
        startupUiState,
        identityUiState
    ) {
        if (
            startupUiState ==
            StartupUiState.IdentityRequired &&
            identityUiState is
                    IdentityUiState.Ready
        ) {
            onStartupComplete()
        }
    }


    StartupScreen(
        uiState =
            startupUiState,

        identityUiState =
            identityUiState,

        onRequestPhoneNumberHint = {
            phoneNumberHintRequestId += 1
        },

        onPhoneNumberChanged =
            identityViewModel::onPhoneNumberChanged,

        onCreateIdentity =
            identityViewModel::createNewIdentity,

        onRetry = {
            when (startupUiState) {
                is StartupUiState.Error -> {
                    startupViewModel.retry()
                }

                StartupUiState.IdentityRequired -> {
                    identityViewModel
                        .loadIdentityState()
                }

                StartupUiState.Loading,
                StartupUiState.Ready -> {
                    Unit
                }
            }
        }
    )
}
