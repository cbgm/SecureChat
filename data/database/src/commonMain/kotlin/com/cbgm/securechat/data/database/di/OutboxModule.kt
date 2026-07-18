package com.cbgm.securechat.data.database.di

import com.cbgm.securechat.core.protocol.outbox.ProtocolOutbox
import com.cbgm.securechat.data.database.SecureChatDatabase
import com.cbgm.securechat.data.database.outbox.DefaultProtocolOutbox
import org.koin.dsl.module

val outboxModule =
    module {

        single {
            get<SecureChatDatabase>().protocolOutboxDao()
        }

        single<ProtocolOutbox> {
            DefaultProtocolOutbox(
                outboxDao = get(),
                packetCodec = get()
            )
        }
    }