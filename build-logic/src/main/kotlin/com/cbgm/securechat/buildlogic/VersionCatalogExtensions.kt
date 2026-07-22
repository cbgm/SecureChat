package com.cbgm.securechat.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

internal val Project.libs: VersionCatalog
    get() = extensions
        .getByType<VersionCatalogsExtension>()
        .named("libs")

internal fun VersionCatalog.intVersion(alias: String): Int {
    return findVersion(alias)
        .orElseThrow {
            IllegalArgumentException(
                "Version alias '$alias' was not found in libs.versions.toml"
            )
        }
        .requiredVersion
        .toInt()
}
