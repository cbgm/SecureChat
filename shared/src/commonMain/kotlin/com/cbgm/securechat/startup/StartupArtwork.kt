package com.cbgm.securechat.startup

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import securechat.shared.generated.resources.Res
import securechat.shared.generated.resources.startup


@Composable
fun StartupArtwork(
    modifier: Modifier = Modifier
) {
    Image(
        painter =
            painterResource(
                resource = Res.drawable.startup
            ),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}