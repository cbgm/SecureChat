package com.cbgm.securechat.feature.contactimport.presentation.verification

data class ScannedIdentityPreview(
    val encodedIdentity: String,
    val displayName: String?,
    val phoneNumber: String?,
    val signingKeyFingerprint: String,
    val encryptionKeyFingerprint: String
)