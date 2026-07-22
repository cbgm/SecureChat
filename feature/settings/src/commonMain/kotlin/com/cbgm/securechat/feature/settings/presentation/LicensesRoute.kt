package com.cbgm.securechat.feature.settings.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cbgm.securechat.feature.settings.presentation.screen.LicensesScreen

@Composable
fun LicensesRoute(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    LicensesScreen(
        onBack = onBack,
        modifier = modifier
    )
}