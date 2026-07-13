package com.cbgm.securechat.core.protocol.version

object ProtocolVersion {

    const val CURRENT: Int = 1

    fun isSupported(
        version: Int
    ): Boolean {
        return version == CURRENT
    }
}