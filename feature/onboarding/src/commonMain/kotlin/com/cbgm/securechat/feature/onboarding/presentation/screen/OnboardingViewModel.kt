package com.cbgm.securechat.feature.onboarding.presentation.screen

import androidx.lifecycle.ViewModel
import com.cbgm.securechat.feature.onboarding.platform.PermissionRequestResult
import com.cbgm.securechat.feature.onboarding.presentation.model.OnboardingPage
import com.cbgm.securechat.feature.onboarding.presentation.model.OnboardingUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class OnboardingViewModel : ViewModel() {
    private val mutableState = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = mutableState.asStateFlow()

    fun next() {
        mutableState.value =
            when (mutableState.value.page) {
                OnboardingPage.WELCOME -> mutableState.value.copy(page = OnboardingPage.PRIVACY)
                OnboardingPage.PRIVACY -> mutableState.value.copy(page = OnboardingPage.PERMISSIONS)
                OnboardingPage.PERMISSIONS -> mutableState.value.copy(page = OnboardingPage.PHONE)
                OnboardingPage.PHONE -> mutableState.value
            }
    }

    fun requestPermissions() {
        mutableState.value =
            mutableState.value.copy(
                permissionRequestId = mutableState.value.permissionRequestId + 1,
            )
    }

    fun onPermissionsResult(result: PermissionRequestResult) {
        mutableState.value =
            mutableState.value.copy(
                permissionsRequested = true,
                phonePermissionGranted = result.phoneNumberGranted,
                page = OnboardingPage.PHONE,
                automaticPhoneRequestId =
                    if (result.phoneNumberGranted) {
                        mutableState.value.automaticPhoneRequestId + 1
                    } else {
                        mutableState.value.automaticPhoneRequestId
                    },
            )
    }

    fun retryAutomaticPhoneNumber() {
        if (!mutableState.value.phonePermissionGranted) return
        mutableState.value =
            mutableState.value.copy(
                automaticPhoneRequestId = mutableState.value.automaticPhoneRequestId + 1,
            )
    }

    fun setCreatingIdentity(value: Boolean) {
        mutableState.value = mutableState.value.copy(isCreatingIdentity = value)
    }
}
