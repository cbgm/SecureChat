package com.cbgm.securechat.feature.transport.relay.codec

import kotlinx.serialization.json.Json

fun createRelayJson(): Json =
    Json {
        classDiscriminator = "type"
        encodeDefaults = true
        ignoreUnknownKeys = false
        isLenient = false
        explicitNulls = false
        prettyPrint = false
    }
