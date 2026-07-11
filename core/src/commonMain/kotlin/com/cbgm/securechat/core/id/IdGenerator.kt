package com.cbgm.securechat.core.id

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object IdGenerator {

    @OptIn(ExperimentalUuidApi::class)
    fun generate(): String {
        return Uuid.random().toString()
    }
}