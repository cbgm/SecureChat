package com.cbgm.securechat.server.security

import java.security.MessageDigest

object InternalApiAuthentication {
    const val TOKEN_HEADER = "X-SecureChat-Internal-Token"

    fun isAuthorized(
        expectedToken: String?,
        presentedToken: String?
    ): Boolean {
        if (expectedToken == null) {
            return true
        }
        if (presentedToken == null) {
            return false
        }
        return MessageDigest.isEqual(
            expectedToken.encodeToByteArray(),
            presentedToken.encodeToByteArray()
        )
    }
}
