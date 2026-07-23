package com.cbgm.securechat.feature.transport.relay.identity

import com.cbgm.securechat.core.protocol.phone.PhoneNumberNormalizer
import okio.ByteString.Companion.toByteString

class Sha256RelayIdGenerator(
    private val phoneNumberNormalizer: PhoneNumberNormalizer,
) : RelayIdGenerator {
    override fun deriveFromPhoneNumber(phoneNumber: String): Result<String> =
        runCatching {
            val normalizedPhoneNumber =
                phoneNumberNormalizer.normalize(phoneNumber = phoneNumber).getOrThrow()

            val digest =
                normalizedPhoneNumber
                    .encodeToByteArray()
                    .toByteString()
                    .sha256()
                    .base64Url()
                    .trimEnd('=')

            "$RELAY_ID_PREFIX$digest"
        }

    private companion object {
        const val RELAY_ID_PREFIX = "scphone1_"
    }
}
