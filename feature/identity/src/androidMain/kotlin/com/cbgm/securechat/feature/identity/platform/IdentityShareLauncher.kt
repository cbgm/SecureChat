package com.cbgm.securechat.feature.identity.platform

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberIdentityShareLauncher(
    encodedIdentity: String,
    shareTitle: String,
): () -> Unit {
    val context = LocalContext.current

    val currentEncodedIdentity = rememberUpdatedState(newValue = encodedIdentity)

    val currentShareTitle = rememberUpdatedState(newValue = shareTitle)

    return remember(context) {
        {
            val payload = currentEncodedIdentity.value.trim()

            if (payload.isNotEmpty()) {
                val sendIntent =
                    Intent(Intent.ACTION_SEND)
                        .apply {
                            type = "text/plain"

                            putExtra(
                                Intent.EXTRA_SUBJECT,
                                "SecureChat identity",
                            )

                            putExtra(
                                Intent.EXTRA_TEXT,
                                buildShareText(
                                    encodedIdentity = payload,
                                ),
                            )
                        }

                val chooserIntent =
                    Intent
                        .createChooser(
                            sendIntent,
                            currentShareTitle.value,
                        ).apply {
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

private fun buildShareText(encodedIdentity: String): String =
    buildString {
        /*appendLine("Add me on SecureChat.")

        appendLine()

        appendLine("Open SecureChat and import this identity:")

        appendLine()*/

        append(encodedIdentity)
    }
