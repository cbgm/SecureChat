package com.cbgm.securechat.feature.contactimport.presentation.model

import com.cbgm.securechat.feature.contacts.domain.model.IdentityImportTrust

sealed interface ImportIdentityEvent {
    data class EncodedIdentityChanged(
        val value: String
    ) : ImportIdentityEvent

    data class ImportClicked(
        val contactId: String?,
        val identityImportTrust: IdentityImportTrust = IdentityImportTrust.UNVERIFIED
    ) : ImportIdentityEvent
}
