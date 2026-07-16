package com.cbgm.securechat.core.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import securechat.core.ui.generated.resources.Res
import securechat.core.ui.generated.resources.startup


@Composable
fun StartupArtwork(
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(resource = Res.drawable.startup),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

@Composable
fun PulsingLogo(modifier: Modifier = Modifier) {
    var animationStarted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animationStarted = true
    }

    val entranceScale by animateFloatAsState(
        targetValue = if (animationStarted) { 1f } else { 0.88f },
        animationSpec = tween(
            durationMillis = 700,
            easing = FastOutSlowInEasing
        ),
        label = "startupEntranceScale"
    )

    val entranceAlpha by animateFloatAsState(
        targetValue = if (animationStarted) { 1f } else { 0f },
        animationSpec = tween(durationMillis = 600),
        label = "startupEntranceAlpha"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "startupPulse")

    val pulseScale by
    infiniteTransition.animateFloat(
        initialValue = 0.992f,
        targetValue = 1.012f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2_300,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "startupPulseScale"
    )

    StartupArtwork(
        modifier = modifier
            .graphicsLayer {
                alpha = entranceAlpha
                scaleX = entranceScale * pulseScale
                scaleY = entranceScale * pulseScale
            }
    )
}