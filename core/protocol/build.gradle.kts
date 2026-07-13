import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlin.serialization)
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
                baseName = "SecureChatProtocol"
                isStatic = true
            }
        }
    }

    android {
        namespace =
            "com.cbgm.securechat.core.protocol"

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
    }

    sourceSets {
        commonMain.dependencies {
            implementation(
                libs.kotlinx.serialization.core
            )

            implementation(
                libs.kotlinx.serialization.json
            )

            implementation(
                libs.koin.core
            )
            implementation(libs.kotlinx.coroutines.core)
        }

        commonTest.dependencies {
            implementation(
                libs.kotlin.test
            )
        }
    }
}