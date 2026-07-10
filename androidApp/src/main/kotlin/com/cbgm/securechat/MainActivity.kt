package com.cbgm.securechat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.securechat.presentation.identity.IdentityUiState

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            App()
        }
    }
}

/**
 * Preview the stateless screen rather than App().
 *
 * App() requires Koin and a real IdentityViewModel.
 */
@Preview(
    showBackground = true
)
@Composable
private fun NoIdentityPreview() {
    IdentityScreen(
        uiState = IdentityUiState.NoIdentity,
        onCreateIdentity = {},
        onRetry = {}
    )
}

@Preview(
    showBackground = true
)
@Composable
private fun IncompleteIdentityPreview() {
    IdentityScreen(
        uiState = IdentityUiState.IncompleteIdentity,
        onCreateIdentity = {},
        onRetry = {}
    )
}