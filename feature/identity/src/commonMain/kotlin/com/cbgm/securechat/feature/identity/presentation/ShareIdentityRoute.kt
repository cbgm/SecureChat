package com.cbgm.securechat.feature.identity.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.securechat.feature.identity.platform.rememberIdentityShareLauncher
import com.cbgm.securechat.feature.identity.presentation.screen.ShareIdentityScreen
import com.cbgm.securechat.feature.identity.presentation.screen.ShareIdentityViewModel
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.feature_identity_share_identity
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ShareIdentityRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = true,
    viewModel: ShareIdentityViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val shareIdentity =
        rememberIdentityShareLauncher(
            encodedIdentity = uiState.encodedIdentity.orEmpty(),
            shareTitle = stringResource(Res.string.feature_identity_share_identity),
        )

    ShareIdentityScreen(
        uiState = uiState,
        onGenerateClick = viewModel::generateSharedIdentity,
        onBack = onBack,
        showBackButton = showBackButton,
        modifier = modifier,
        onShareIdentity = shareIdentity,
    )
}
