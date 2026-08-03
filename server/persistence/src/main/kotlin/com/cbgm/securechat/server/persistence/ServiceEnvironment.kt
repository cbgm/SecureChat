package com.cbgm.securechat.server.persistence

object ServiceEnvironment {
    fun string(
        name: String,
        defaultValue: String
    ): String = System.getenv(name)?.takeIf(String::isNotBlank) ?: defaultValue

    fun int(
        name: String,
        defaultValue: Int
    ): Int = System.getenv(name)?.toIntOrNull() ?: defaultValue

    fun long(
        name: String,
        defaultValue: Long
    ): Long = System.getenv(name)?.toLongOrNull() ?: defaultValue
}
