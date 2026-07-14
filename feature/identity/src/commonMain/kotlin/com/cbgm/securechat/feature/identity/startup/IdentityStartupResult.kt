package com.cbgm.securechat.feature.identity.startup

enum class IdentityStartupResult {

    /**
     * A complete local identity was already stored.
     */
    ALREADY_EXISTS,

    /**
     * No identity currently exists.
     *
     * Identity creation must be performed through onboarding after
     * the local phone number has been entered and saved.
     */
    NOT_CREATED
}