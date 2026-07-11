plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}


kotlin {
    val isMacOs = System
        .getProperty("os.name")
        .startsWith(
            prefix = "Mac",
            ignoreCase = true
        )

    if (isMacOs) {
        listOf(
            iosArm64(),
            iosSimulatorArm64()
        ).forEach { iosTarget ->
            iosTarget.binaries.framework {
                baseName = "SecureChatContactImport"
                isStatic = true
            }
        }
    }

    android {
        namespace =
            "com.cbgm.securechat.feature.contactimport"

        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()

        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()

        /*
         * Required when a module uses Compose Multiplatform together
         * with the Android KMP library plugin.
         *
         * It also gives Compose's Android device-test resource tasks
         * a configured Android assets/output directory.
         */
        androidResources {
            enable = true
        }

        withDeviceTest {
            instrumentationRunner =
                "androidx.test.runner.AndroidJUnitRunner"

            execution = "HOST"
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core"))
            implementation(project(":feature:identity"))
            implementation(project(":feature:contacts"))

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)

            implementation(
                libs.androidx.lifecycle.viewmodelCompose
            )

            implementation(
                libs.androidx.lifecycle.runtimeCompose
            )

            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)

            implementation(libs.androidx.camera.core)
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.androidx.camera.view)

            implementation(libs.zxing.core)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        getByName("androidDeviceTest").dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.androidx.test.core)
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            implementation(project(":data:database"))
        }
    }
}