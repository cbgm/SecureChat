package com.cbgm.securechat.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

class SecureChatKmpComposeFeaturePlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("securechat.kmp.compose")
        pluginManager.apply("securechat.kmp.testing")
    }
}
