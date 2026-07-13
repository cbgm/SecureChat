package com.cbgm.securechat.core.crypto

import com.cbgm.securechat.core.crypto.hash.CryptoHash
import com.cbgm.securechat.core.crypto.hash.DefaultCryptoHash
import com.cbgm.securechat.core.crypto.safety.SafetyNumberGenerator

object CryptoProvider {

    fun createCryptoHash():
            CryptoHash {

        return DefaultCryptoHash()
    }

    fun createSafetyNumberGenerator(
        cryptoHash: CryptoHash
    ): SafetyNumberGenerator {

        return SafetyNumberGenerator(
            cryptoHash = cryptoHash
        )
    }
}