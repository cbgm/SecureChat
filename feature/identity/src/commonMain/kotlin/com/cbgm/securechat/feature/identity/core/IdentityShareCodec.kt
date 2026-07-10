package com.cbgm.securechat.feature.identity.core

import com.cbgm.securechat.feature.identity.domain.model.SharedIdentityPayload

/**
 * Converts a shared identity between:
 *
 * - structured Kotlin data
 * - portable text suitable for QR codes and SMS
 */
interface IdentityShareCodec {

    fun encode(
        payload: SharedIdentityPayload
    ): Result<String>

    fun decode(
        encodedValue: String
    ): Result<SharedIdentityPayload>
}