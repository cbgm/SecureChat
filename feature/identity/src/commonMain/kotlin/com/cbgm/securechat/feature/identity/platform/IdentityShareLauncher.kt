package com.cbgm.securechat.feature.identity.platform

import androidx.compose.runtime.Composable

/**
 * Returns a callback that opens the platform share UI with the
 * supplied SecureChat identity payload.
 */
@Composable
expect fun rememberIdentityShareLauncher(
    encodedIdentity: String,
    shareTitle: String = "Share SecureChat identity",
): () -> Unit
