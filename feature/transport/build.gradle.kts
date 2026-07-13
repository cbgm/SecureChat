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
                baseName = "SecureChatTransport"
                isStatic = true
            }
        }
    }

    android {
        namespace =
            "com.cbgm.securechat.feature.transport"

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
                project(":core")
            )

            implementation(
                project(":core:crypto")
            )

            implementation(
                project(":core:protocol")
            )

            implementation(
                project(":feature:contacts")
            )

            implementation(
                project(":data:database")
            )

            implementation(
                project(":feature:chats")
            )

            implementation(
                libs.kotlinx.coroutines.core
            )

            implementation(
                libs.koin.core
            )

            implementation(
                libs.kotlinx.serialization.json
            )

            implementation(
                libs.ktor.client.core
            )

            implementation(
                libs.ktor.client.websockets
            )

            implementation(
                libs.koin.core
            )

            implementation(libs.okio)

        }

        androidMain.dependencies {
            implementation(
                libs.ktor.client.okhttp
            )
        }

        if (isMacOs) {
            iosMain.dependencies {
                implementation(
                    libs.ktor.client.darwin
                )
            }
        }

        commonTest.dependencies {
            implementation(
                libs.kotlin.test
            )

            implementation(
                libs.kotlinx.coroutines.test
            )
        }
    }
}