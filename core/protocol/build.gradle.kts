plugins {
    alias(libs.plugins.securechat.kmp.serialization)
    alias(libs.plugins.securechat.kmp.testing)
}

kotlin {
    android {
        namespace = "com.cbgm.securechat.core.protocol"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.bundles.coroutines)
            implementation(libs.bundles.koin.core)
            implementation(libs.bundles.serialization)
        }

        commonTest.dependencies {
            implementation(libs.bundles.kmp.testing)
        }

        androidDeviceTest.dependencies {
            implementation(libs.bundles.android.device.testing)
        }
    }
}
