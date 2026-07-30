plugins {
    alias(libs.plugins.securechat.kmp.library)
    alias(libs.plugins.securechat.kmp.testing)
}

kotlin {
    android {
        namespace = "com.cbgm.securechat.core"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.bundles.coroutines)
            implementation(libs.kermit)
        }

        commonTest.dependencies {
            implementation(libs.bundles.kmp.testing)
        }

        androidDeviceTest.dependencies {
            implementation(libs.bundles.android.device.testing)
        }
    }
}
