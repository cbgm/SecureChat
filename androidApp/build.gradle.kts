import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.securechat.lint)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

android {
    namespace = "com.cbgm.securechat"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = "com.cbgm.securechat"
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.android.targetSdk
                .get()
                .toInt()
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(projects.shared)
    implementation(projects.startup)

    implementation(projects.core)
    implementation(projects.core.crypto)
    implementation(projects.core.protocol)

    implementation(projects.data.database)
    implementation(projects.feature.messaging)

    implementation(projects.feature.chats)
    implementation(projects.feature.contactimport)
    implementation(projects.feature.contacts)
    implementation(projects.feature.identity)
    implementation(projects.feature.onboarding)
    implementation(projects.feature.settings)
    implementation(projects.feature.transport)

    implementation(libs.bundles.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.koin.android)

    debugImplementation(libs.compose.uiTooling)
}
