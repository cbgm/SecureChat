package com.cbgm.securechat.feature.settings.presentation.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.cbgm.securechat.feature.settings.presentation.model.LicensesUiState
import com.mikepenz.aboutlibraries.ui.compose.DefaultChipColors
import com.mikepenz.aboutlibraries.ui.compose.DefaultLibraryColors
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.produceLibraries

private val CardColor = Color(0xFF102A46)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(
    uiState: LicensesUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val libraries by produceLibraries {
        uiState.libraries
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor =  MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = {
                    Text(
                        text = "Open source licenses",
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
        LibrariesContainer(
            libraries = libraries,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            colors = DefaultLibraryColors(
                // Card itself sits one shade lighter than the screen
                // background, same as every other card in the app, with
                // white text on top of it instead of white-on-white.
                libraryBackgroundColor = CardColor,
                libraryContentColor = MaterialTheme.colorScheme.onBackground,

                // Chips use accent cyan text on a translucent cyan pill —
                // matches the "Preferred"/"Secure"/status badge pattern
                // used across contacts and chats screens.
                versionChipColors = DefaultChipColors(
                    containerColor =  MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                    contentColor =  MaterialTheme.colorScheme.secondary
                ),
                licenseChipColors = DefaultChipColors(
                    containerColor =  MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                    contentColor =  MaterialTheme.colorScheme.secondary
                ),
                fundingChipColors = DefaultChipColors(
                    containerColor =  MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                    contentColor =  MaterialTheme.colorScheme.secondary
                ),

                // Detail dialog (tapping a library) matches the app's
                // navy card color, not a stray white sheet.
                dialogBackgroundColor = CardColor,
                dialogContentColor =  MaterialTheme.colorScheme.surface,
                dialogConfirmButtonColor =  MaterialTheme.colorScheme.secondary
            )
        )
    }
}