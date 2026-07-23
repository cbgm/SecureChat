package com.cbgm.securechat.feature.settings.domain.repository

interface LicensesRepository {
    suspend fun getLibraries(): String
}
