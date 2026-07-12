package com.cbgm.securechat.feature.identity.startup

class DefaultIdentityStartupManager(
    private val identityExists:
    suspend () -> Result<Boolean>,

    private val createIdentity:
    suspend () -> Result<Unit>
) : IdentityStartupManager {

    override suspend fun ensureIdentityExists():
            Result<IdentityStartupResult> {

        return runCatching {
            val exists =
                identityExists()
                    .getOrThrow()

            if (exists) {
                return@runCatching IdentityStartupResult.ALREADY_EXISTS
            }

            createIdentity()
                .getOrThrow()

            require(
                identityExists()
                    .getOrThrow()
            ) {
                "Identity creation completed but no stored identity was found"
            }

            IdentityStartupResult.CREATED
        }
    }
}