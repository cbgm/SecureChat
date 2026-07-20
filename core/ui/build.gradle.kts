import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

val isMacOs = System.getProperty("os.name").startsWith("Mac", ignoreCase = true)

kotlin {
    if (isMacOs) {
        iosArm64()
        iosSimulatorArm64()
    }

    android {
        namespace = "com.cbgm.securechat.core.ui"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions { jvmTarget = JvmTarget.JVM_17 }
        androidResources { enable = true }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.compose.components.resources)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
        }
    }
}

compose.resources {
    publicResClass = true
    generateResClass = always
    packageOfResClass = "com.cbgm.securechat.resources"
}