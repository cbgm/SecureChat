package com.cbgm.securechat.core.crypto.di

import com.cbgm.securechat.core.crypto.hash.CryptoHash
import com.cbgm.securechat.core.crypto.hash.DefaultCryptoHash
import com.cbgm.securechat.core.crypto.identity.IdentityKeyGenerator
import com.cbgm.securechat.core.crypto.identity.SodiumIdentityKeyGenerator
import com.cbgm.securechat.core.crypto.safety.SafetyNumberGenerator
import com.cbgm.securechat.core.crypto.transport.DefaultTransportPayloadCodec
import com.cbgm.securechat.core.crypto.transport.SodiumTransportMessageCipher
import com.cbgm.securechat.core.crypto.transport.TransportMessageCipher
import com.cbgm.securechat.core.crypto.transport.TransportPayloadCodec
import org.koin.dsl.module

val cryptoModule =
    module {

        single<CryptoHash> {
            DefaultCryptoHash()
        }

        single {
            SafetyNumberGenerator(
                cryptoHash = get()
            )
        }

        single<IdentityKeyGenerator> {
            SodiumIdentityKeyGenerator()
        }

        single<TransportMessageCipher> {
            SodiumTransportMessageCipher()
        }

        single<TransportPayloadCodec> {
            DefaultTransportPayloadCodec()
        }
    }