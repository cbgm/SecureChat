package com.cbgm.securechat.feature.settings.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cbgm.securechat.feature.settings.domain.model.DisclaimerContent
import com.cbgm.securechat.feature.settings.presentation.model.DisclaimerType
import com.cbgm.securechat.feature.settings.presentation.screen.MarkdownDisclaimerScreen

@Composable
fun DisclaimerRoute(
    type: DisclaimerType,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
) {
    MarkdownDisclaimerScreen(
        title =
            when (type) {
                DisclaimerType.PRIVACY_POLICY -> "Privacy policy"
                DisclaimerType.DATA_DISCLAIMER -> "Data disclaimer"
            },
        markdownContent =
            when (type) {
                DisclaimerType.PRIVACY_POLICY -> DisclaimerContent.privacyPolicy
                DisclaimerType.DATA_DISCLAIMER -> DisclaimerContent.dataDisclaimer
            },
        onBack = onBack,
        modifier = modifier,
    )
}
