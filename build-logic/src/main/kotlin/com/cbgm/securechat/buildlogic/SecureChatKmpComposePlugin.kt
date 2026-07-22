package com.cbgm.securechat.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

class SecureChatKmpComposePlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("securechat.kmp.library")
        pluginManager.apply("org.jetbrains.compose")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
    }
}
