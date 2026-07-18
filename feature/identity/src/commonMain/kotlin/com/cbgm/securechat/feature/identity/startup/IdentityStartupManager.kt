package com.cbgm.securechat.feature.identity.startup


/**
 * Ensures that this installation has a complete local identity.
 *
 * The implementation must create and persist both the private and
 * public identity material when no identity exists.
 */
interface IdentityStartupManager {

    suspend fun ensureIdentityExists(): Result<IdentityStartupResult>
}