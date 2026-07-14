package com.cbgm.securechat.feature.onboarding.presentation

enum class OnboardingPage { WELCOME, PRIVACY, PERMISSIONS, PHONE }

data class OnboardingUiState(
    val page: OnboardingPage = OnboardingPage.WELCOME,
    val permissionRequestId: Int = 0,
    val automaticPhoneRequestId: Int = 0,
    val permissionsRequested: Boolean = false,
    val phonePermissionGranted: Boolean = false,
    val isCreatingIdentity: Boolean = false
)
