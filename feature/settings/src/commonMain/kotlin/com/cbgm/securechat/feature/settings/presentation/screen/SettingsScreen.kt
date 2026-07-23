package com.cbgm.securechat.feature.settings.presentation.screen

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.settings.domain.model.AppLanguage
import com.cbgm.securechat.feature.settings.presentation.model.SettingsUiState
import com.cbgm.securechat.feature.settings.presentation.screen.components.LanguagePickerDialog

private val CardColor = Color(0xFF102A46)

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    snackbarHostState: SnackbarHostState,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenDataDisclaimer: () -> Unit,
    onOpenLicenses: () -> Unit,
    onOpenDeveloperMenu: () -> Unit,
    onOpenLanguagePicker: () -> Unit,
    onDismissLanguagePicker: () -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    onVersionRowTapped: () -> Unit,
    scrollState: ScrollState,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding(),
                ).verticalScroll(scrollState)
                .padding(MaterialTheme.spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        SettingsSection(title = "General") {
            SettingsRow(
                icon = Icons.Default.Language,
                title = "Language",
                subtitle = uiState.currentLanguage.nativeName,
                onClick = onOpenLanguagePicker,
            )
        }

        SettingsSection(title = "Privacy & data") {
            SettingsRow(
                icon = Icons.Default.PrivacyTip,
                title = "Privacy policy",
                subtitle = "How your data is handled",
                onClick = onOpenPrivacyPolicy,
            )

            SettingsDivider()

            SettingsRow(
                icon = Icons.Default.Lock,
                title = "Data disclaimer",
                subtitle = "What SecureChat stores locally and on the relay",
                onClick = onOpenDataDisclaimer,
            )
        }

        SettingsSection(title = "About") {
            SettingsRow(
                icon = Icons.Default.Code,
                title = "Open source licenses",
                subtitle = "Libraries used in this app",
                onClick = onOpenLicenses,
            )

            SettingsDivider()

            SettingsRow(
                icon = Icons.Default.Description,
                title = "Version",
                subtitle = "${uiState.buildInfo.versionName} (${uiState.buildInfo.versionCode})",
                showChevron = false,
                onClick = onVersionRowTapped,
            )
        }

        if (uiState.isDeveloperModeEnabled) {
            SettingsSection(title = "Developer") {
                SettingsRow(
                    icon = Icons.Default.BugReport,
                    title = "Developer menu",
                    subtitle = "Build info, feature flags, diagnostics",
                    onClick = onOpenDeveloperMenu,
                    iconTint = MaterialTheme.colorScheme.secondary,
                )
            }
        }

        Spacer(modifier = Modifier.size(MaterialTheme.spacing.base))
    }

    if (uiState.showLanguagePicker) {
        LanguagePickerDialog(
            currentLanguage = uiState.currentLanguage,
            onLanguageSelected = onLanguageSelected,
            onDismiss = onDismissLanguagePicker,
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = MaterialTheme.spacing.base.div(2), bottom = MaterialTheme.spacing.base),
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = CardColor,
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showChevron: Boolean = true,
    iconTint: Color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = MaterialTheme.spacing.small, vertical = MaterialTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))

        Spacer(modifier = Modifier.size(MaterialTheme.spacing.small))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
        }

        if (showChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 54.dp),
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f),
    )
}
