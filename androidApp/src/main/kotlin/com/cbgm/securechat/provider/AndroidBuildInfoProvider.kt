package com.cbgm.securechat.provider

import com.cbgm.securechat.BuildConfig
import com.cbgm.securechat.feature.settings.domain.model.BuildInfo
import com.cbgm.securechat.feature.settings.domain.repository.BuildInfoProvider

class AndroidBuildInfoProvider : BuildInfoProvider {
    override val build: BuildInfo
        get() {

            return BuildInfo(
                versionName = BuildConfig.VERSION_NAME,
                versionCode = BuildConfig.VERSION_CODE,
                buildType = BuildConfig.BUILD_TYPE,
                gitSha = "",
            )
        }
}
