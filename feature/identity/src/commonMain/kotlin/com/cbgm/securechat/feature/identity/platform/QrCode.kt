package com.cbgm.securechat.feature.identity.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Displays a QR code.
 *
 * Platform implementations choose how the QR code is rendered.
 */
@Composable
expect fun QrCode(
    content: String,
    modifier: Modifier = Modifier
)
