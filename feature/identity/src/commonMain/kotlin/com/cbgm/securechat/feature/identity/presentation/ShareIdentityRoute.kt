package com.cbgm.securechat.feature.identity.presentation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.securechat.feature.identity.presentation.screen.ShareIdentityScreen
import com.cbgm.securechat.feature.identity.presentation.screen.ShareIdentityViewModel
import com.cbgm.securechat.feature.identity.sharing.rememberIdentityShareLauncher
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ShareIdentityRoute(
    onBack: () -> Unit,
    viewModel: ShareIdentityViewModel =
        koinViewModel()
) {
    val uiState by
    viewModel.uiState
        .collectAsStateWithLifecycle()

    val shareIdentity =
        rememberIdentityShareLauncher(
            encodedIdentity =
                uiState.encodedIdentity.orEmpty()
        )

    val clipboardManager =
        LocalClipboardManager.current

    val snackbarHostState =
        remember {
            SnackbarHostState()
        }

    val coroutineScope =
        rememberCoroutineScope()

    ShareIdentityScreen(
        uiState = uiState,

        onIncludeContactDetailsChanged =
            viewModel::onIncludeContactDetailsChanged,

        onDisplayNameChanged =
            viewModel::onDisplayNameChanged,

        onPhoneNumberChanged =
            viewModel::onPhoneNumberChanged,

        onGenerateClick =
            viewModel::generateSharedIdentity,

        onBack = onBack,

        onCopyIdentity = {
            val encodedIdentity =
                uiState.encodedIdentity
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: return@ShareIdentityScreen

            clipboardManager.setText(
                AnnotatedString(
                    encodedIdentity
                )
            )

            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message =
                        "Identity copied to clipboard."
                )
            }
        },

        onShareIdentity =
            shareIdentity,

        snackbarHostState =
            snackbarHostState
    )
}
