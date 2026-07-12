package com.cbgm.securechat.feature.chats.domain.model

enum class ContactSecurityState {

    /**
     * The contact was imported from the phone book or otherwise
     * has no SecureChat public identity.
     *
     * Messages can still be sent, but they cannot be protected
     * with end-to-end encryption.
     */
    NO_PUBLIC_KEY,

    /**
     * The contact has encryption and signing public keys, but the
     * user has not verified that they belong to the expected person.
     */
    PUBLIC_KEY_UNVERIFIED,

    /**
     * The contact has a public identity and the user has verified it.
     */
    PUBLIC_KEY_VERIFIED
}