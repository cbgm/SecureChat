package com.cbgm.securechat.feature.identity.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.securechat.feature.identity.presentation.screen.ShareIdentityScreen
import com.cbgm.securechat.feature.identity.presentation.screen.ShareIdentityViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ShareIdentityRoute(
    onBack: () -> Unit,
    viewModel: ShareIdentityViewModel = koinViewModel()
) {
    val uiState by viewModel
        .uiState
        .collectAsStateWithLifecycle()

    val clipboardManager = LocalClipboardManager.current

    val snackbarHostState = remember { SnackbarHostState() }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState
            )
        }
    ) { innerPadding ->

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

                uiState.encodedIdentity?.let {
                    clipboardManager.setText(
                        AnnotatedString(
                            it
                        )
                    )

                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Identity copied to clipboard."
                        )
                    }
                }
            },

            modifier = Modifier.padding(innerPadding)
        )
    }
}