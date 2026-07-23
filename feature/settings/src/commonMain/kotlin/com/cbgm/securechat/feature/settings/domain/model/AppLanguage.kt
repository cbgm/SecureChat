package com.cbgm.securechat.feature.settings.domain.model

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
) {
    ENGLISH("en", "English", "English"),
    GERMAN("de", "German", "Deutsch"),
    SPANISH("es", "Spanish", "Español"),
    FRENCH("fr", "French", "Français"),
    ;

    companion object {
        fun fromCode(code: String): AppLanguage = entries.find { it.code == code } ?: ENGLISH
    }
}
