package com.cbgm.securechat.startup

import com.cbgm.securechat.feature.identity.domain.model.IdentityStatus
import com.cbgm.securechat.feature.identity.domain.usecase.GetIdentityStatus

class AppInitializer(
    private val getIdentityStatus: GetIdentityStatus
) {
    suspend fun initialize(): Result<AppInitializationResult> =
        runCatching {
            val identityStatus = getIdentityStatus().getOrThrow()

            AppInitializationResult(
                identityReady = identityStatus == IdentityStatus.READY
            )
        }
}
