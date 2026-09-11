package com.cbgm.sparrow.feature.autoreply.presentation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.feature.autoreply.presentation.model.AutoReplyEffect
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AutoReplySettingsRoute(
    viewModel: AutoReplyViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is AutoReplyEffect.ShowSnackbar ->
                    snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    AutoReplySettingsScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onUiEvent = viewModel::onUiEvent
    )
}
