plugins {
    alias(libs.plugins.securechat.kmp.library)
    alias(libs.plugins.securechat.kmp.testing)
}

kotlin {
    android {
        namespace = "com.cbgm.securechat.core.crypto"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.bundles.coroutines)
            implementation(libs.bundles.koin.core)

            implementation(libs.libsodium)
            implementation(libs.kotlincrypto.sha2)
        }

        commonTest.dependencies {
            implementation(libs.bundles.kmp.testing)
        }

        androidDeviceTest.dependencies {
            implementation(libs.bundles.android.device.testing)
            implementation(libs.bundles.coroutines.test)
        }
    }
}