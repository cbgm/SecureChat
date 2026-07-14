package com.cbgm.securechat.feature.identity.presentation

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
import com.cbgm.securechat.feature.identity.presentation.screen.IdentityScreen
import com.cbgm.securechat.feature.identity.presentation.screen.IdentityViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun IdentityRoute(
    onIdentityReady: () -> Unit = {},
    onShareIdentity: () -> Unit,
    onImportContact: () -> Unit,
    onContacts: () -> Unit,
    viewModel: IdentityViewModel =
        koinViewModel()
) {
    val uiState by
    viewModel
        .uiState
        .collectAsStateWithLifecycle()

    var phoneNumberHintRequestId by
    remember {
        mutableIntStateOf(0)
    }

    val canRequestPhoneNumber =
        uiState is
                IdentityUiState.NoIdentity

    PhoneNumberHintLauncher(
        requestId =
            phoneNumberHintRequestId,

        enabled =
            canRequestPhoneNumber,

        onResult = { result ->
            when (result) {
                is PhoneNumberHintResult.Selected -> {
                    viewModel.onSuggestedPhoneNumber(
                        phoneNumber =
                            result.phoneNumber
                    )
                }

                PhoneNumberHintResult.Unavailable -> {
                    viewModel
                        .onPhoneNumberHintUnavailable()
                }

                PhoneNumberHintResult.Cancelled -> {
                    /*
                     * Manual entry remains visible. Cancellation is
                     * therefore not treated as an error.
                     */
                }

                is PhoneNumberHintResult.Failed -> {
                    viewModel.onPhoneNumberHintFailed(
                        message = result.message
                    )
                }
            }
        }
    )

    LaunchedEffect(uiState) {
        if (uiState is IdentityUiState.Ready) {
            onIdentityReady()
        }
    }


    IdentityScreen(
        uiState =
            uiState,

        onRequestPhoneNumberHint = {
            phoneNumberHintRequestId += 1
        },

        onPhoneNumberChanged =
            viewModel::onPhoneNumberChanged,

        onCreateIdentity =
            viewModel::createNewIdentity,

        onRetry =
            viewModel::loadIdentityState,

        onShareIdentity =
            onShareIdentity,

        onImportContact =
            onImportContact,

        onContacts =
            onContacts
    )
}
