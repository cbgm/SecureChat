package com.cbgm.securechat.feature.contacts.sharing

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.cbgm.securechat.feature.contacts.domain.model.Contact

@Composable
actual fun rememberContactShareLauncher(
    encodedIdentity: String,
    shareTitle: String
): (Contact) -> Unit {
    val context = LocalContext.current

    val currentEncodedIdentity = rememberUpdatedState(newValue = encodedIdentity)

    val currentShareTitle =rememberUpdatedState(newValue = shareTitle)

    return remember(context) {
        { contact ->
            val payload = currentEncodedIdentity.value.trim()

            if (payload.isNotEmpty()) {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_SUBJECT,
                            "SecureChat: ${contact.displayName ?: "Contact"}"
                        )
                        putExtra(
                            Intent.EXTRA_TEXT,
                            buildShareText(
                                displayName = contact.displayName,
                                encodedIdentity = payload
                            )
                        )
                    }

                val chooserIntent = Intent.createChooser(sendIntent, currentShareTitle.value).apply {
                        /*
                         * LocalContext may theoretically be backed by
                         * a non-Activity Context.
                         */
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                context.startActivity(chooserIntent)
            }
        }
    }
}

private fun buildShareText(
    displayName: String?,
    encodedIdentity: String
): String {
    return buildString {
        if (displayName != null) {
            appendLine("SecureChat identity for $displayName.")
        } else {
            appendLine("SecureChat identity.")
        }
        appendLine()
        appendLine("Open SecureChat and import this identity:")
        appendLine()
        append(encodedIdentity)
    }
}