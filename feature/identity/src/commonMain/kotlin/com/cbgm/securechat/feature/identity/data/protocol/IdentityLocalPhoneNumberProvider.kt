package com.cbgm.securechat.feature.identity.data.protocol

import com.cbgm.securechat.core.protocol.phone.LocalPhoneNumberProvider
import com.cbgm.securechat.core.protocol.phone.PhoneNumberNormalizer
import com.cbgm.securechat.feature.identity.core.LocalPhoneNameStorage

class IdentityLocalPhoneNumberProvider(
    private val localPhoneNameStorage: LocalPhoneNameStorage,
    private val phoneNumberNormalizer: PhoneNumberNormalizer,
) : LocalPhoneNumberProvider {
    override suspend fun getLocalPhoneNumber(): Result<String> =
        runCatching {
            val storedPhoneNumber =
                localPhoneNameStorage
                    .loadPhoneNumber()
                    .getOrThrow()
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: error(
                        "Local phone number has not been configured",
                    )

            phoneNumberNormalizer.normalize(phoneNumber = storedPhoneNumber).getOrThrow()
        }
}
