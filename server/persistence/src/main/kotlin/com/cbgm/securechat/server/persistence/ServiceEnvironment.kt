package com.cbgm.securechat.server.persistence

import java.nio.file.Files
import java.nio.file.Path

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

    fun secret(name: String): String? =
        resolveSecret(
            name = name,
            environment = System::getenv,
            readFile = { path -> Files.readString(Path.of(path)) }
        )

    internal fun resolveSecret(
        name: String,
        environment: (String) -> String?,
        readFile: (String) -> String
    ): String? {
        val fileVariable = "${name}_FILE"
        val secretFile = environment(fileVariable)?.takeIf(String::isNotBlank)
        if (secretFile != null) {
            return readFile(secretFile)
                .trimEnd('\r', '\n')
                .takeIf(String::isNotBlank)
                ?: error("Secret file configured by $fileVariable is empty")
        }
        return environment(name)?.takeIf(String::isNotBlank)
    }
}
