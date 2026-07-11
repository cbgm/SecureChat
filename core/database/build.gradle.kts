import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

val isMacOs = System
    .getProperty("os.name")
    .startsWith(
        prefix = "Mac",
        ignoreCase = true
    )

kotlin {

    if (isMacOs) {
        listOf(
            iosArm64(),
            iosSimulatorArm64()
        ).forEach { iosTarget ->

            iosTarget.binaries.framework {
                baseName = "SecureChatCoreDatabase"
                isStatic = true
            }
        }
    }

    androidLibrary {
        namespace = "com.cbgm.securechat.core.database"

        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }

    sourceSets {

        commonMain.dependencies {
            implementation(
                libs.androidx.room.runtime
            )

            implementation(
                libs.androidx.sqlite.bundled
            )

            implementation(
                libs.kotlinx.coroutines.core
            )
        }

        commonTest.dependencies {
            implementation(
                libs.kotlin.test
            )
        }
    }
}