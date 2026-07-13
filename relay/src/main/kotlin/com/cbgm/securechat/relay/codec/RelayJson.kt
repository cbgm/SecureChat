package com.cbgm.securechat.relay.codec

import kotlinx.serialization.json.Json

fun createRelayJson(): Json {
    return Json {
        classDiscriminator = "type"
        encodeDefaults = true
        ignoreUnknownKeys = false
        isLenient = false
        explicitNulls = false
        prettyPrint = false
    }
}