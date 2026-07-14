package com.cbgm.securechat.feature.onboarding.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
actual fun OnboardingPermissionRequester(
    requestId: Int,
    onResult: (PermissionRequestResult) -> Unit
) {
    val context = LocalContext.current
    val permissions = remember {
        buildList {
            add(Manifest.permission.READ_CONTACTS)
            add(Manifest.permission.CAMERA)
            if (Build.VERSION.SDK_INT >= 26) {
                add(Manifest.permission.READ_PHONE_NUMBERS)
                add(Manifest.permission.READ_PHONE_STATE)
            }
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        onResult(
            PermissionRequestResult(
                contactsGranted = result[Manifest.permission.READ_CONTACTS] == true || context.isGranted(Manifest.permission.READ_CONTACTS),
                cameraGranted = result[Manifest.permission.CAMERA] == true || context.isGranted(Manifest.permission.CAMERA),
                notificationsGranted = Build.VERSION.SDK_INT < 33 || result[Manifest.permission.POST_NOTIFICATIONS] == true || context.isGranted(Manifest.permission.POST_NOTIFICATIONS),
                phoneNumberGranted = Build.VERSION.SDK_INT >= 26 &&
                    (result[Manifest.permission.READ_PHONE_NUMBERS] == true || context.isGranted(Manifest.permission.READ_PHONE_NUMBERS)) &&
                    (result[Manifest.permission.READ_PHONE_STATE] == true || context.isGranted(Manifest.permission.READ_PHONE_STATE))
            )
        )
    }
    LaunchedEffect(requestId) {
        if (requestId > 0) launcher.launch(permissions)
    }
}

@Composable
actual fun AutomaticPhoneNumberReader(
    requestId: Int,
    enabled: Boolean,
    onResult: (AutomaticPhoneNumberResult) -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(requestId, enabled) {
        if (!enabled || requestId <= 0) return@LaunchedEffect
        if (Build.VERSION.SDK_INT < 26 ||
            !context.isGranted(Manifest.permission.READ_PHONE_NUMBERS) ||
            !context.isGranted(Manifest.permission.READ_PHONE_STATE)
        ) {
            onResult(AutomaticPhoneNumberResult.Unavailable)
            return@LaunchedEffect
        }
        val number = runCatching {
            val manager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val subscriptions = manager.activeSubscriptionInfoList.orEmpty()
            subscriptions.firstNotNullOfOrNull { info ->
                val value = if (Build.VERSION.SDK_INT >= 33) {
                    manager.getPhoneNumber(info.subscriptionId)
                } else {
                    @Suppress("DEPRECATION")
                    (context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager)
                        .createForSubscriptionId(info.subscriptionId)
                        .line1Number
                }
                value?.trim()?.takeIf { it.isNotBlank() }
            }
        }.getOrElse {
            onResult(AutomaticPhoneNumberResult.Failed(it.message ?: "SIM phone number could not be read"))
            return@LaunchedEffect
        }
        if (number == null) onResult(AutomaticPhoneNumberResult.Unavailable)
        else onResult(AutomaticPhoneNumberResult.Found(number))
    }
}

private fun Context.isGranted(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
