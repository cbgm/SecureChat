package com.cbgm.securechat.feature.identity.presentation.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.core.protocol.phone.PhoneNumberNormalizer
import com.cbgm.securechat.feature.identity.core.LocalPhoneNumberStorage
import com.cbgm.securechat.feature.identity.domain.model.IdentityStatus
import com.cbgm.securechat.feature.identity.domain.usecase.CreateIdentity
import com.cbgm.securechat.feature.identity.domain.usecase.GetIdentityStatus
import com.cbgm.securechat.feature.identity.domain.usecase.GetPublicIdentity
import com.cbgm.securechat.feature.identity.presentation.model.IdentityUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class IdentityViewModel(
    private val getIdentityStatus:
    GetIdentityStatus,

    private val getPublicIdentity:
    GetPublicIdentity,

    private val createIdentity:
    CreateIdentity,

    private val localPhoneNumberStorage:
    LocalPhoneNumberStorage,

    private val phoneNumberNormalizer:
    PhoneNumberNormalizer
) : ViewModel() {

    private val mutableUiState =
        MutableStateFlow<IdentityUiState>(
            IdentityUiState.Loading
        )

    val uiState:
            StateFlow<IdentityUiState> =
        mutableUiState.asStateFlow()

    init {
        loadIdentityState()
    }

    fun loadIdentityState() {
        viewModelScope.launch {
            mutableUiState.value =
                IdentityUiState.Loading

            getIdentityStatus()
                .onSuccess { status ->
                    handleIdentityStatus(
                        status = status
                    )
                }
                .onFailure { error ->
                    mutableUiState.value =
                        IdentityUiState.Error(
                            message =
                                error.message
                                    ?: "Failed to load identity state"
                        )
                }
        }
    }

    fun onPhoneNumberChanged(
        value: String
    ) {
        val currentState =
            mutableUiState.value

        if (
            currentState is
                    IdentityUiState.NoIdentity
        ) {
            mutableUiState.value =
                currentState.copy(
                    phoneNumber =
                        value,

                    phoneNumberError =
                        null
                )
        }
    }

    fun createNewIdentity() {
        val currentState =
            mutableUiState.value

        if (
            currentState !is
                    IdentityUiState.NoIdentity
        ) {
            return
        }

        val normalizedPhoneNumber =
            phoneNumberNormalizer
                .normalize(
                    phoneNumber =
                        currentState.phoneNumber
                )
                .getOrElse { error ->
                    mutableUiState.value =
                        currentState.copy(
                            phoneNumberError =
                                error.message
                                    ?: "Invalid phone number"
                        )

                    return
                }

        viewModelScope.launch {
            mutableUiState.value =
                IdentityUiState.Loading

            localPhoneNumberStorage
                .savePhoneNumber(
                    phoneNumber =
                        normalizedPhoneNumber
                )
                .onFailure { error ->
                    mutableUiState.value =
                        IdentityUiState.NoIdentity(
                            phoneNumber =
                                normalizedPhoneNumber,

                            phoneNumberError =
                                error.message
                                    ?: "Phone number could not be saved"
                        )

                    return@launch
                }

            createIdentity()
                .onSuccess { publicIdentity ->
                    mutableUiState.value =
                        IdentityUiState.Ready(
                            publicIdentity =
                                publicIdentity,

                            localPhoneNumber =
                                normalizedPhoneNumber
                        )
                }
                .onFailure { error ->
                    mutableUiState.value =
                        IdentityUiState.Error(
                            message =
                                error.message
                                    ?: "Failed to create identity"
                        )
                }
        }
    }

    private suspend fun handleIdentityStatus(
        status: IdentityStatus
    ) {
        when (status) {
            IdentityStatus.NOT_CREATED -> {
                val storedPhoneNumber =
                    localPhoneNumberStorage
                        .loadPhoneNumber()
                        .getOrNull()
                        .orEmpty()

                mutableUiState.value =
                    IdentityUiState.NoIdentity(
                        phoneNumber =
                            storedPhoneNumber
                    )
            }

            IdentityStatus.INCOMPLETE -> {
                mutableUiState.value =
                    IdentityUiState
                        .IncompleteIdentity
            }

            IdentityStatus.READY -> {
                loadReadyIdentity()
            }
        }
    }

    private suspend fun loadReadyIdentity() {
        val localPhoneNumber =
            localPhoneNumberStorage
                .loadPhoneNumber()
                .getOrElse { error ->
                    mutableUiState.value =
                        IdentityUiState.Error(
                            message =
                                error.message
                                    ?: "Local phone number could not be loaded"
                        )

                    return
                }
                ?.takeIf {
                    it.isNotBlank()
                }

        if (localPhoneNumber == null) {
            mutableUiState.value =
                IdentityUiState.Error(
                    message =
                        "Identity exists, but the local phone number is missing. Clear the app data once and create the identity again with a phone number."
                )

            return
        }

        getPublicIdentity()
            .onSuccess { publicIdentity ->
                mutableUiState.value =
                    if (publicIdentity != null) {
                        IdentityUiState.Ready(
                            publicIdentity =
                                publicIdentity,

                            localPhoneNumber =
                                localPhoneNumber
                        )
                    } else {
                        IdentityUiState
                            .IncompleteIdentity
                    }
            }
            .onFailure { error ->
                mutableUiState.value =
                    IdentityUiState.Error(
                        message =
                            error.message
                                ?: "Failed to load public identity"
                    )
            }
    }
}