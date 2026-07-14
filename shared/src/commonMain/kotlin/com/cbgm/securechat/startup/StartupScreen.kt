package com.cbgm.securechat.startup

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun StartupScreen(
    uiState: StartupUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    var animationStarted by
    remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        animationStarted = true
    }

    val entranceScale by
    animateFloatAsState(
        targetValue =
            if (animationStarted) {
                1f
            } else {
                0.82f
            },
        animationSpec =
            tween(
                durationMillis = 700,
                easing = FastOutSlowInEasing
            ),
        label = "startupEntranceScale"
    )

    val entranceAlpha by
    animateFloatAsState(
        targetValue =
            if (animationStarted) {
                1f
            } else {
                0f
            },
        animationSpec =
            tween(
                durationMillis = 650
            ),
        label = "startupEntranceAlpha"
    )

    val infiniteTransition =
        rememberInfiniteTransition(
            label = "startupPulse"
        )

    val pulseScale by
    infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.025f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis = 1_800,
                        easing =
                            FastOutSlowInEasing
                    ),
                repeatMode =
                    RepeatMode.Reverse
            ),
        label = "startupPulseScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush =
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                Color(0xFF020817),
                                Color(0xFF071A2E),
                                Color(0xFF020817)
                            )
                    )
            )
            .padding(
                horizontal = 24.dp,
                vertical = 32.dp
            )
    ) {
        Column(
            modifier =
                Modifier.fillMaxSize(),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center
        ) {
            StartupArtwork(
                modifier =
                    Modifier
                        .size(280.dp)
                        .graphicsLayer {
                            alpha =
                                entranceAlpha

                            scaleX =
                                entranceScale *
                                        pulseScale

                            scaleY =
                                entranceScale *
                                        pulseScale
                        }
            )

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            Text(
                text = "SecureChat",
                style =
                    MaterialTheme.typography
                        .headlineLarge,
                color = Color.White
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "Private. Encrypted. Yours.",
                style =
                    MaterialTheme.typography
                        .bodyLarge,
                color =
                    Color.White.copy(
                        alpha = 0.72f
                    )
            )

            Spacer(
                modifier = Modifier.height(40.dp)
            )

            AnimatedContent(
                targetState = uiState,
                transitionSpec = {
                    fadeIn(
                        animationSpec =
                            tween(300)
                    ) togetherWith
                            fadeOut(
                                animationSpec =
                                    tween(200)
                            )
                },
                label = "startupState"
            ) { state ->
                StartupStateContent(
                    uiState = state,
                    onRetry = onRetry
                )
            }
        }
    }
}

@Composable
private fun StartupStateContent(
    uiState: StartupUiState,
    onRetry: () -> Unit
) {
    when (uiState) {
        StartupUiState.Loading -> {
            StartupProgress(
                message = "Preparing SecureChat…"
            )
        }

        StartupUiState.Ready -> {
            StartupProgress(
                message = "Opening SecureChat…"
            )
        }

        StartupUiState.IdentityRequired -> {
            StartupProgress(
                message = "Opening identity setup…"
            )
        }

        is StartupUiState.Error -> {
            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {
                Text(
                    text =
                        "SecureChat could not finish setup.",
                    style =
                        MaterialTheme.typography
                            .titleMedium,
                    color = Color.White,
                    textAlign =
                        TextAlign.Center
                )

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                Text(
                    text = uiState.message,
                    style =
                        MaterialTheme.typography
                            .bodyMedium,
                    color =
                        MaterialTheme.colorScheme
                            .error,
                    textAlign =
                        TextAlign.Center
                )

                Spacer(
                    modifier =
                        Modifier.height(24.dp)
                )

                Button(
                    onClick = onRetry,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
private fun StartupProgress(
    message: String
) {
    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = Color(0xFF35E6FF),
            trackColor =
                Color.White.copy(
                    alpha = 0.12f
                )
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Text(
            text = message,
            style =
                MaterialTheme.typography
                    .bodyMedium,
            color =
                Color.White.copy(
                    alpha = 0.78f
                ),
            textAlign = TextAlign.Center
        )
    }
}