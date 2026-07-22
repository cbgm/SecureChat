package com.cbgm.securechat.feature.identity.startup

class IdentityStartupManager(
    private val identityExists:
    suspend () -> Result<Boolean>
) {

    suspend fun ensureIdentityExists():
            Result<IdentityStartupResult> {

        return runCatching {
            val exists = identityExists().getOrThrow()

            if (exists) {
                IdentityStartupResult.ALREADY_EXISTS
            } else {
                /*
                 * Do not automatically create an identity here.
                 *
                 * The user must first enter and persist their local
                 * phone number on the identity screen. The screen then
                 * invokes CreateIdentity explicitly.
                 */
                IdentityStartupResult.NOT_CREATED
            }
        }
    }
}