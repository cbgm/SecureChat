package com.cbgm.securechat.feature.settings.presentation.screen

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.settings.domain.model.BuildInfo

private val CardColor = Color(0xFF102A46)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperMenuScreen(
    buildInfo: BuildInfo,
    isClearingLocalData: Boolean,
    onBack: () -> Unit,
    onClearLocalData: () -> Unit,
    onDisableDeveloperMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = {
                    Text(
                        text = "Developer menu",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(MaterialTheme.spacing.small),
                color = CardColor
            ) {
                Column(modifier = Modifier.padding(MaterialTheme.spacing.small)) {
                    Text(
                        text = "Build info",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.padding(top = MaterialTheme.spacing.base))

                    BuildInfoRow(label = "Version name", value = buildInfo.versionName)
                    BuildInfoRow(label = "Version code", value = buildInfo.versionCode.toString())
                    BuildInfoRow(label = "Build type", value = buildInfo.buildType)
                    BuildInfoRow(label = "Git SHA", value = buildInfo.gitSha ?: "unknown")
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(MaterialTheme.spacing.small),
                color = CardColor
            ) {
                Column(modifier = Modifier.padding(MaterialTheme.spacing.small)) {
                    Text(
                        text = "Danger zone",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )

                    Text(
                        text = "These actions are destructive and only intended for local debugging.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.padding(
                            top = MaterialTheme.spacing.base.div(2),
                            bottom = MaterialTheme.spacing.small
                        )
                    )

                    Button(
                        onClick = onClearLocalData,
                        enabled = !isClearingLocalData,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        if (isClearingLocalData) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text(text = "Clear local data", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.padding(top = MaterialTheme.spacing.base))

                    OutlinedButton(
                        onClick = onDisableDeveloperMode,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Disable developer mode")
                    }
                }
            }
        }
    }
}

@Composable
private fun BuildInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.spacing.base.div(2)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}