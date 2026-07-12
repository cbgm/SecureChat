package com.cbgm.securechat.startup

import com.cbgm.securechat.feature.identity.startup.IdentityStartupManager
import com.cbgm.securechat.feature.identity.startup.IdentityStartupResult

/**
 * Performs application-wide startup work.
 *
 * More startup operations can be added here later, such as:
 *
 * - key-format upgrades
 * - preference migrations
 * - database integrity checks
 * - backup reminders
 */
class DefaultAppInitializer(
    private val identityStartupManager:
    IdentityStartupManager
) : AppInitializer {

    override suspend fun initialize():
            Result<AppInitializationResult> {

        return runCatching {
            val identityResult =
                identityStartupManager
                    .ensureIdentityExists()
                    .getOrThrow()

            AppInitializationResult(
                identityCreated =
                    identityResult ==
                            IdentityStartupResult.CREATED
            )
        }
    }
}