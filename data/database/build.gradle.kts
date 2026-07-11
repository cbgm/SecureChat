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

    /*
     * iOS targets are configured only on macOS.
     *
     * Windows can still develop and build the Android target.
     */
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

        /*
         * Enable Android instrumented tests later.
         */
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

            implementation(
                project(":core")
            )

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

/*
 * Run Room's compiler for every configured platform target.
 */
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

/*
 * Export Room schema history.
 */
room {
    schemaDirectory(
        "$projectDir/schemas"
    )
}