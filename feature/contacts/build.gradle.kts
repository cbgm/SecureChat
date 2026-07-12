import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
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
                baseName = "SecureChatDatabase"
                isStatic = true
            }
        }
    }

    android {
        namespace = "com.cbgm.securechat.feature.contacts"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }

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
            implementation(project(":data:database"))

            implementation(libs.kotlinx.coroutines.core)

            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(compose.materialIconsExtended)
            implementation(libs.compose.uiToolingPreview)

            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.koin.core)
            implementation(libs.koin.core.viewmodel)
            implementation(libs.koin.compose.viewmodel)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        getByName("androidDeviceTest").dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.androidx.test.core)
            implementation(libs.androidx.test.runner)
            implementation(libs.kotlinx.coroutines.core)

            implementation(libs.androidx.room.runtime)
            implementation(projects.data.database)
            implementation(libs.androidx.sqlite.bundled)
        }
    }
}
