package com.cbgm.securechat.feature.chats.domain.model

enum class ContactSecurityState {
    /**
     * Phone-book contact only.
     *
     * We do not possess this contact's public keys.
     */
    NO_REMOTE_PUBLIC_KEYS,

    /**
     * We possess their public keys, but they do not yet possess ours.
     */
    ONE_WAY_KEYS,

    /**
     * Both parties possess each other's public keys,
     * but the safety number is unverified.
     */
    MUTUAL_KEYS_UNVERIFIED,

    /**
     * Both parties possess each other's public keys,
     * and the safety number is verified.
     */
    MUTUAL_KEYS_VERIFIED
}
