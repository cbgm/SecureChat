package com.cbgm.securechat.presentation.identity

import com.cbgm.securechat.domain.model.PublicIdentity

/**
 * Everything the identity screen needs to render.
 *
 * Using a sealed interface means the UI must deal with a known,
 * explicit set of states.
 */
sealed interface IdentityUiState {

    /**
     * The ViewModel is currently loading the local identity state.
     */
    data object Loading : IdentityUiState

    /**
     * No public or private identity data exists.
     *
     * Creating a new identity is safe.
     */
    data object NoIdentity : IdentityUiState

    /**
     * Both public and private identity data exist.
     */
    data class Ready(
        val publicIdentity: PublicIdentity
    ) : IdentityUiState

    /**
     * Only part of the identity exists.
     *
     * We deliberately do not offer automatic identity creation here,
     * because replacing partial state could destroy access to old
     * encrypted conversations later.
     */
    data object IncompleteIdentity : IdentityUiState

    /**
     * Loading or identity creation failed.
     */
    data class Error(
        val message: String
    ) : IdentityUiState
}