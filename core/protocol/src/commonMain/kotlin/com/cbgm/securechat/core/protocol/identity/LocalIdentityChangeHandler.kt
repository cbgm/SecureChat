package com.cbgm.securechat.core.protocol.identity

interface LocalIdentityChangeHandler {
    suspend fun onLocalIdentityChanged(): Result<Unit>
}
