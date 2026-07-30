package com.cbgm.securechat.startup.presentation.screen.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.ui.component.StartupArtwork

const val BACKGROUND_TINT_ALPHA =
    0.09f

const val LOGO_WATERMARK_ALPHA =
    0.10f

@Composable
fun SecureChatAppBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.background.copy(alpha = BACKGROUND_TINT_ALPHA))
    ) {
        StartupArtwork(
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .size(300.dp)
                    .graphicsLayer {
                        alpha = LOGO_WATERMARK_ALPHA
                    }
        )

        content()
    }
}
