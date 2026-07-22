package com.cbgm.securechat.startup

import com.cbgm.securechat.feature.identity.startup.IdentityStartupManager
import com.cbgm.securechat.feature.identity.startup.IdentityStartupResult

class AppInitializer(
    private val identityStartupManager: IdentityStartupManager
) {

    suspend fun initialize():
            Result<AppInitializationResult> {

        return runCatching {
            val identityResult = identityStartupManager.ensureIdentityExists().getOrThrow()

            AppInitializationResult(
                identityReady = identityResult == IdentityStartupResult.ALREADY_EXISTS
            )
        }
    }
}
