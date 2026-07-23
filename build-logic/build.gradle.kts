plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

group = "com.cbgm.securechat.buildlogic"
version = "1.0.0"

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(
        "org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}"
    )

    implementation(
        "org.jetbrains.kotlin.plugin.compose:" +
            "org.jetbrains.kotlin.plugin.compose.gradle.plugin:" +
            libs.versions.kotlin.get()
    )

    implementation(
        "org.jetbrains.kotlin.plugin.serialization:" +
            "org.jetbrains.kotlin.plugin.serialization.gradle.plugin:" +
            libs.versions.kotlin.get()
    )

    implementation(
        "org.jetbrains.compose:compose-gradle-plugin:" +
            libs.versions.composeMultiplatform.get()
    )

    implementation(
        "com.android.tools.build:gradle:${libs.versions.agp.get()}"
    )

    implementation(
        "com.google.devtools.ksp:symbol-processing-gradle-plugin:" +
            libs.versions.ksp.get()
    )

    implementation(
        "androidx.room:room-gradle-plugin:${libs.versions.room.get()}"
    )

    implementation(
        "dev.detekt:detekt-gradle-plugin:" +
            libs.versions.detekt.get()
    )

    implementation(
        "org.jlleitschuh.gradle.ktlint:" +
            "org.jlleitschuh.gradle.ktlint.gradle.plugin:" +
            libs.versions.ktlint.gradle.get()
    )

}

gradlePlugin {
    plugins {
        register("secureChatKmpLibrary") {
            id = "securechat.kmp.library"
            implementationClass =
                "com.cbgm.securechat.buildlogic.SecureChatKmpLibraryPlugin"
        }

        register("secureChatKmpCompose") {
            id = "securechat.kmp.compose"
            implementationClass =
                "com.cbgm.securechat.buildlogic.SecureChatKmpComposePlugin"
        }

        register("secureChatKmpTesting") {
            id = "securechat.kmp.testing"
            implementationClass =
                "com.cbgm.securechat.buildlogic.SecureChatKmpTestingPlugin"
        }

        register("secureChatKmpComposeFeature") {
            id = "securechat.kmp.compose.feature"
            implementationClass =
                "com.cbgm.securechat.buildlogic.SecureChatKmpComposeFeaturePlugin"
        }

        register("secureChatKmpSerialization") {
            id = "securechat.kmp.serialization"
            implementationClass =
                "com.cbgm.securechat.buildlogic.SecureChatKmpSerializationPlugin"
        }

        register("secureChatKmpRoom") {
            id = "securechat.kmp.room"
            implementationClass =
                "com.cbgm.securechat.buildlogic.SecureChatKmpRoomPlugin"
        }

        register("secureChatLint") {
            id = "securechat.lint"
            implementationClass =
                "com.cbgm.securechat.buildlogic.SecureChatLintPlugin"
        }

        register("secureChatArchitecture") {
            id = "securechat.architecture"
            implementationClass =
                "com.cbgm.securechat.buildlogic.SecureChatArchitecturePlugin"
        }

        register("secureChatQuality") {
            id = "securechat.quality"
            implementationClass =
                "com.cbgm.securechat.buildlogic.SecureChatQualityPlugin"
        }
    }
}
