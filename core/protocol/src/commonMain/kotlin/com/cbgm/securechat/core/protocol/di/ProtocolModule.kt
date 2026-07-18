package com.cbgm.securechat.core.protocol.di

import com.cbgm.securechat.core.protocol.codec.KotlinxPacketCodec
import com.cbgm.securechat.core.protocol.codec.PacketCodec
import com.cbgm.securechat.core.protocol.codec.createProtocolJson
import com.cbgm.securechat.core.protocol.handler.DefaultProtocolPacketHandler
import com.cbgm.securechat.core.protocol.handler.ProtocolPacketHandler
import com.cbgm.securechat.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.securechat.core.protocol.phone.DefaultPhoneNumberNormalizer
import com.cbgm.securechat.core.protocol.phone.PhoneNumberNormalizer
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val protocolModule = module {

    single<Json> {
        createProtocolJson()
    }

    single<PacketCodec> {
        KotlinxPacketCodec(json = get())
    }

    single<ProtocolPacketHandler> {
        DefaultProtocolPacketHandler(handlers = getAll<TypedProtocolPacketHandler>())
    }

    single<PhoneNumberNormalizer> {
        DefaultPhoneNumberNormalizer()
    }
}