import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
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
                baseName = "SecureChatDataDatabase"
                isStatic = true
            }
        }
    }

    androidLibrary {
        namespace = "com.cbgm.securechat.data.database"

        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }

        withDeviceTest {
            instrumentationRunner =
                "androidx.test.runner.AndroidJUnitRunner"

            execution = "HOST"
        }
    }

    sourceSets {

        commonMain.dependencies {
            implementation(
                project(":core:database")
            )

            implementation(projects.core)
            implementation(projects.core.protocol)

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

        androidMain.dependencies {
            implementation(
                libs.koin.android
            )
        }

        commonTest.dependencies {
            implementation(
                libs.kotlin.test
            )

            implementation(
                libs.kotlinx.coroutines.test
            )
        }

        getByName("androidDeviceTest").dependencies {
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
                libs.kotlinx.coroutines.core
            )
        }
    }
}

dependencies {

    add(
        "kspAndroid",
        libs.androidx.room.compiler
    )

    if (isMacOs) {

        add(
            "kspIosArm64",
            libs.androidx.room.compiler
        )

        add(
            "kspIosSimulatorArm64",
            libs.androidx.room.compiler
        )
    }
}

room {
    schemaDirectory(
        "$projectDir/schemas"
    )
}