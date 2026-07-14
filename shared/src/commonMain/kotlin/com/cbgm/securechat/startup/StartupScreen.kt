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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.feature.identity.presentation.model.IdentityUiState

@Composable
fun StartupScreen(
    uiState: StartupUiState,
    identityUiState: IdentityUiState,
    onRequestPhoneNumberHint: () -> Unit,
    onPhoneNumberChanged: (String) -> Unit,
    onCreateIdentity: () -> Unit,
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
                0.88f
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
                durationMillis = 600
            ),
        label = "startupEntranceAlpha"
    )

    val cardAlpha by
    animateFloatAsState(
        targetValue =
            if (animationStarted) {
                1f
            } else {
                0f
            },
        animationSpec =
            tween(
                durationMillis = 500,
                delayMillis = 260
            ),
        label = "startupCardAlpha"
    )

    val cardTranslation by
    animateFloatAsState(
        targetValue =
            if (animationStarted) {
                0f
            } else {
                42f
            },
        animationSpec =
            tween(
                durationMillis = 650,
                delayMillis = 180,
                easing = FastOutSlowInEasing
            ),
        label = "startupCardTranslation"
    )

    val infiniteTransition =
        rememberInfiniteTransition(
            label = "startupPulse"
        )

    val pulseScale by
    infiniteTransition.animateFloat(
        initialValue = 0.992f,
        targetValue = 1.012f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis = 2_300,
                        easing = FastOutSlowInEasing
                    ),
                repeatMode =
                    RepeatMode.Reverse
            ),
        label = "startupPulseScale"
    )

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(
                    color = StartupBackground
                )
                .padding(
                    horizontal = 24.dp,
                    vertical = 24.dp
                )
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(
                        rememberScrollState()
                    ),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center
        ) {
            StartupArtwork(
                modifier =
                    Modifier
                        .size(300.dp)
                        .graphicsLayer {
                            alpha = entranceAlpha
                            scaleX =
                                entranceScale *
                                        pulseScale
                            scaleY =
                                entranceScale *
                                        pulseScale
                        }
            )

            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )

            Text(
                text = "SecureChat",
                style =
                    MaterialTheme
                        .typography
                        .headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )

            Text(
                text = "Private. Encrypted. Yours.",
                style =
                    MaterialTheme
                        .typography
                        .bodyLarge,
                color =
                    Color.White.copy(
                        alpha = 0.72f
                    )
            )

            Spacer(
                modifier =
                    Modifier.height(28.dp)
            )

            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .widthIn(
                            max = 520.dp
                        )
                        .graphicsLayer {
                            alpha = cardAlpha
                            translationY =
                                cardTranslation
                        },
                shape =
                    MaterialTheme.shapes.extraLarge,
                color =
                    StartupCardBackground,
                tonalElevation = 8.dp,
                shadowElevation = 12.dp
            ) {
                AnimatedContent(
                    targetState = uiState,
                    transitionSpec = {
                        fadeIn(
                            animationSpec =
                                tween(300)
                        ) togetherWith
                                fadeOut(
                                    animationSpec =
                                        tween(180)
                                )
                    },
                    label = "startupState"
                ) { state ->
                    Box(
                        modifier =
                            Modifier.padding(
                                horizontal = 22.dp,
                                vertical = 22.dp
                            )
                    ) {
                        StartupStateContent(
                            uiState = state,
                            identityUiState =
                                identityUiState,
                            onRequestPhoneNumberHint =
                                onRequestPhoneNumberHint,
                            onPhoneNumberChanged =
                                onPhoneNumberChanged,
                            onCreateIdentity =
                                onCreateIdentity,
                            onRetry = onRetry
                        )
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(24.dp)
            )
        }
    }
}

@Composable
private fun StartupStateContent(
    uiState: StartupUiState,
    identityUiState: IdentityUiState,
    onRequestPhoneNumberHint: () -> Unit,
    onPhoneNumberChanged: (String) -> Unit,
    onCreateIdentity: () -> Unit,
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
            StartupIdentityContent(
                identityUiState =
                    identityUiState,
                onRequestPhoneNumberHint =
                    onRequestPhoneNumberHint,
                onPhoneNumberChanged =
                    onPhoneNumberChanged,
                onCreateIdentity =
                    onCreateIdentity,
                onRetry = onRetry
            )
        }

        is StartupUiState.Error -> {
            StartupErrorContent(
                message = uiState.message,
                onRetry = onRetry
            )
        }
    }
}

@Composable
private fun StartupIdentityContent(
    identityUiState: IdentityUiState,
    onRequestPhoneNumberHint: () -> Unit,
    onPhoneNumberChanged: (String) -> Unit,
    onCreateIdentity: () -> Unit,
    onRetry: () -> Unit
) {
    when (identityUiState) {
        IdentityUiState.Loading -> {
            StartupProgress(
                message =
                    "Generating secure identity…"
            )
        }

        is IdentityUiState.NoIdentity -> {
            Column(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Verify your phone number",
                    style =
                        MaterialTheme
                            .typography
                            .titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                Text(
                    text =
                        "Your contacts use your phone number to securely find you on SecureChat.",
                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,
                    color =
                        Color.White.copy(
                            alpha = 0.72f
                        ),
                    textAlign = TextAlign.Center
                )

                Spacer(
                    modifier =
                        Modifier.height(22.dp)
                )

                OutlinedTextField(
                    value =
                        identityUiState.phoneNumber,
                    onValueChange =
                        onPhoneNumberChanged,
                    modifier =
                        Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            text = "Phone number"
                        )
                    },
                    placeholder = {
                        Text(
                            text = "+491701234567"
                        )
                    },
                    supportingText = {
                        Text(
                            text =
                                identityUiState
                                    .phoneNumberError
                                    ?: if (
                                        identityUiState
                                            .phoneNumber
                                            .isBlank()
                                    ) {
                                        "Choose a number from your device or enter it manually."
                                    } else {
                                        "Detected automatically. You can edit it or choose another number."
                                    }
                        )
                    },
                    isError =
                        identityUiState
                            .phoneNumberError != null,
                    singleLine = true,
                    textStyle =
                        MaterialTheme
                            .typography
                            .titleMedium
                            .copy(
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            ),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            disabledTextColor =
                                Color.White.copy(
                                    alpha = 0.60f
                                ),
                            focusedContainerColor =
                                StartupPhoneFieldBackground,
                            unfocusedContainerColor =
                                StartupPhoneFieldBackground,
                            errorContainerColor =
                                StartupPhoneFieldBackground,
                            focusedBorderColor =
                                StartupAccent,
                            unfocusedBorderColor =
                                Color.White.copy(
                                    alpha = 0.18f
                                ),
                            errorBorderColor =
                                MaterialTheme
                                    .colorScheme
                                    .error,
                            focusedLabelColor =
                                StartupAccent,
                            unfocusedLabelColor =
                                Color.White.copy(
                                    alpha = 0.76f
                                ),
                            errorLabelColor =
                                MaterialTheme
                                    .colorScheme
                                    .error,
                            focusedPlaceholderColor =
                                Color.White.copy(
                                    alpha = 0.38f
                                ),
                            unfocusedPlaceholderColor =
                                Color.White.copy(
                                    alpha = 0.38f
                                ),
                            focusedSupportingTextColor =
                                Color.White.copy(
                                    alpha = 0.66f
                                ),
                            unfocusedSupportingTextColor =
                                Color.White.copy(
                                    alpha = 0.66f
                                ),
                            errorSupportingTextColor =
                                MaterialTheme
                                    .colorScheme
                                    .error,
                            cursorColor =
                                StartupAccent,
                            errorCursorColor =
                                MaterialTheme
                                    .colorScheme
                                    .error
                        ),
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType =
                                KeyboardType.Phone
                        )
                )

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                OutlinedButton(
                    onClick =
                        onRequestPhoneNumberHint,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text(
                        text =
                            if (
                                identityUiState
                                    .phoneNumber
                                    .isBlank()
                            ) {
                                "Choose phone number"
                            } else {
                                "Choose another number"
                            }
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(18.dp)
                )

                Button(
                    onClick = onCreateIdentity,
                    enabled =
                        identityUiState
                            .phoneNumber
                            .isNotBlank(),
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Continue"
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(14.dp)
                )

                Text(
                    text =
                        "Your encryption keys are generated only after you approve your number.",
                    style =
                        MaterialTheme
                            .typography
                            .labelMedium,
                    color = StartupAccent,
                    textAlign = TextAlign.Center
                )
            }
        }

        is IdentityUiState.Ready -> {
            StartupProgress(
                message =
                    "Identity ready. Opening SecureChat…"
            )
        }

        IdentityUiState.IncompleteIdentity -> {
            StartupErrorContent(
                message =
                    "Only part of the local identity is available. SecureChat will not generate replacement keys automatically.",
                onRetry = onRetry
            )
        }

        is IdentityUiState.Error -> {
            StartupErrorContent(
                message = identityUiState.message,
                onRetry = onRetry
            )
        }
    }
}

@Composable
private fun StartupErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {
        Text(
            text =
                "SecureChat could not finish setup.",
            style =
                MaterialTheme
                    .typography
                    .titleMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = message,
            style =
                MaterialTheme
                    .typography
                    .bodyMedium,
            color =
                MaterialTheme
                    .colorScheme
                    .error,
            textAlign = TextAlign.Center
        )

        Spacer(
            modifier = Modifier.height(22.dp)
        )

        OutlinedButton(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Retry")
        }
    }
}

@Composable
private fun StartupProgress(
    message: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = StartupAccent,
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
                MaterialTheme
                    .typography
                    .bodyMedium,
            color =
                Color.White.copy(
                    alpha = 0.78f
                ),
            textAlign = TextAlign.Center
        )
    }
}

private val StartupBackground =
    Color(0xFF071A2E)

private val StartupCardBackground =
    Color(0xFF10283D)

private val StartupAccent =
    Color(0xFF35E6FF)

private val StartupPhoneFieldBackground =
    Color(0xFF0B2035)
