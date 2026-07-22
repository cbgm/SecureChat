package com.cbgm.securechat.feature.settings.data.repository

import com.cbgm.securechat.feature.settings.domain.repository.LicensesRepository
import com.cbgm.securechat.feature.settings.resources.Res

class LicencesRepositoryImpl(): LicensesRepository {
    override suspend fun getLibraries(): String {
        return Res.readBytes(path = "files/aboutlibraries.json").decodeToString()
    }

}