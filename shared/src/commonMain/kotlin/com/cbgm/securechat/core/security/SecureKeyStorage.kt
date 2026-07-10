package com.cbgm.securechat.core.security

interface SecureKeyStorage {

    fun saveKeyReference(
        reference: String
    )

    fun getKeyReference(): String?
}