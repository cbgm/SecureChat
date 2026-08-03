plugins {
    alias(libs.plugins.securechat.kmp.compose)
}

kotlin {
    android {
        namespace = "com.cbgm.securechat.resources"

        androidResources {
            enable = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.compose.components.resources)
        }
    }
}

compose.resources {
    publicResClass = true
    generateResClass = always
    packageOfResClass = "com.cbgm.securechat.resources"
}
