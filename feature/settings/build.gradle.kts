import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.about.libs)
}

val isMacOs = System.getProperty("os.name").startsWith("Mac", ignoreCase = true)

kotlin {
    if (isMacOs) {
        iosArm64()
        iosSimulatorArm64()
    }

    android {
        namespace = "com.cbgm.securechat.feature.settings"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions { jvmTarget = JvmTarget.JVM_17 }
        androidResources { enable = true }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(projects.core.ui)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.compose.uiTooling)
            implementation(compose.materialIconsExtended)
            implementation(libs.about.libs.compose)
            implementation(libs.about.libs.render)
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
    packageOfResClass = "com.cbgm.securechat.feature.settings.resources"
}

/**
 * For about libs
 * .\gradlew.bat :feature:settings:exportLibraryDefinitions '-PaboutLibraries.exportPath=src/commonMain/composeResources/files/'
 *
 **/
