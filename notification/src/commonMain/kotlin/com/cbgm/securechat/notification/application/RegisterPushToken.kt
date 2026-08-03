package com.cbgm.securechat.notification.application

import com.cbgm.securechat.feature.transport.push.PushPlatform
import com.cbgm.securechat.feature.transport.push.PushTokenRegistrationGateway

class RegisterPushToken(
    private val pushTokenRegistrationGateway: PushTokenRegistrationGateway
) {
    suspend operator fun invoke(
        token: String,
        platform: PushPlatform
    ): Result<Unit> {
        require(token.isNotBlank()) {
            "Push token must not be blank"
        }

        return pushTokenRegistrationGateway.register(
            token = token,
            platform = platform
        )
    }
}
