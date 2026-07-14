package com.cbgm.securechat.feature.identity.phone

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.identity.GetPhoneNumberHintIntentRequest
import com.google.android.gms.auth.api.identity.Identity

@Composable
actual fun PhoneNumberHintLauncher(
    requestId: Int,
    enabled: Boolean,
    onResult: (PhoneNumberHintResult) -> Unit
) {
    val context =
        LocalContext.current

    val activity =
        remember(context) {
            context.findActivity()
        }

    val signInClient =
        remember(activity) {
            activity?.let {
                Identity.getSignInClient(it)
            }
        }

    val resultLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .StartIntentSenderForResult()
        ) { result ->
            if (
                result.resultCode !=
                Activity.RESULT_OK
            ) {
                onResult(
                    PhoneNumberHintResult.Cancelled
                )

                return@rememberLauncherForActivityResult
            }

            val selectedPhoneNumber =
                runCatching {
                    signInClient
                        ?.getPhoneNumberFromIntent(
                            result.data
                        )
                }
                    .getOrNull()
                    ?.trim()
                    .orEmpty()

            if (selectedPhoneNumber.isBlank()) {
                onResult(
                    PhoneNumberHintResult.Unavailable
                )
            } else {
                onResult(
                    PhoneNumberHintResult.Selected(
                        phoneNumber =
                            selectedPhoneNumber
                    )
                )
            }
        }

    LaunchedEffect(
        requestId,
        enabled,
        signInClient
    ) {
        if (!enabled || requestId <= 0) {
            return@LaunchedEffect
        }

        val client =
            signInClient
                ?: run {
                    onResult(
                        PhoneNumberHintResult.Failed(
                            message =
                                "Phone number picker requires an Android activity"
                        )
                    )

                    return@LaunchedEffect
                }

        val request =
            GetPhoneNumberHintIntentRequest
                .builder()
                .build()

        client
            .getPhoneNumberHintIntent(
                request
            )
            .addOnSuccessListener { pendingIntent ->
                resultLauncher.launch(
                    IntentSenderRequest
                        .Builder(
                            pendingIntent.intentSender
                        )
                        .build()
                )
            }
            .addOnFailureListener { error ->
                onResult(
                    PhoneNumberHintResult.Failed(
                        message =
                            error.message
                                ?: "Phone number picker is unavailable"
                    )
                )
            }
    }
}

private tailrec fun Context.findActivity():
        Activity? {

    return when (this) {
        is Activity -> {
            this
        }

        is ContextWrapper -> {
            baseContext.findActivity()
        }

        else -> {
            null
        }
    }
}
