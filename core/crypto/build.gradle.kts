import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

val isMacOs =
    System.getProperty("os.name")
        .startsWith(
            prefix = "Mac",
            ignoreCase = true
        )

kotlin {
    if (isMacOs) {
        listOf(
            iosArm64(),
            iosSimulatorArm64()
        ).forEach { target ->
            target.binaries.framework {
                baseName =
                    "SecureChatCrypto"

                isStatic = true
            }
        }
    }

    android {
        namespace =
            "com.cbgm.securechat.core.crypto"

        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()

        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()

        compilerOptions {
            jvmTarget =
                JvmTarget.JVM_17
        }

        withHostTest {}

        withDeviceTest {
            instrumentationRunner =
                "androidx.test.runner.AndroidJUnitRunner"

            execution =
                "HOST"
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(
                libs.kotlinx.coroutines.core
            )

            implementation(
                libs.libsodium
            )

            implementation(
                libs.kotlincrypto.sha2
            )
            implementation(libs.koin.core)
        }

        commonTest.dependencies {
            implementation(
                libs.kotlin.test
            )

            implementation(
                libs.kotlinx.coroutines.test
            )
        }

        getByName(
            "androidDeviceTest"
        ).dependencies {
            implementation(
                libs.kotlin.test
            )

            implementation(
                libs.androidx.test.runner
            )

            implementation(
                libs.androidx.test.core
            )

            implementation(
                libs.kotlinx.coroutines.test
            )
        }
    }
}