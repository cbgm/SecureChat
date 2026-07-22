package com.cbgm.securechat.feature.settings.domain.repository

import com.cbgm.securechat.feature.settings.domain.model.BuildInfo

interface BuildInfoProvider {
    val build: BuildInfo
}