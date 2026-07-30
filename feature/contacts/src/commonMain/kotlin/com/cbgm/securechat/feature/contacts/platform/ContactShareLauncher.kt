package com.cbgm.securechat.feature.contacts.platform

import androidx.compose.runtime.Composable
import com.cbgm.securechat.feature.contacts.domain.model.Contact

/**
 * Returns a callback that opens the platform share UI with the
 * supplied SecureChat contact payload.
 */
@Composable
expect fun rememberContactShareLauncher(
    encodedIdentity: String,
    shareTitle: String = "Share SecureChat identity",
): (Contact) -> Unit
