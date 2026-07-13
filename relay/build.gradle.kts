import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.serialization)
    application
}

kotlin {
    compilerOptions {
        jvmTarget.set(
            JvmTarget.JVM_21
        )
    }
}

java {
    sourceCompatibility =
        JavaVersion.VERSION_21

    targetCompatibility =
        JavaVersion.VERSION_21
}

application {
    mainClass.set(
        "com.cbgm.securechat.relay.ApplicationKt"
    )
}

dependencies {
    implementation(
        libs.ktor.server.core
    )

    implementation(
        libs.ktor.server.netty
    )

    implementation(
        libs.ktor.server.websockets
    )

    implementation(
        libs.ktor.server.call.logging
    )

    implementation(
        libs.ktor.serialization.kotlinx.json
    )

    implementation(
        libs.kotlinx.serialization.json
    )

    implementation(
        libs.kotlinx.coroutines.core
    )

    implementation(
        libs.logback.classic
    )

    testImplementation(
        kotlin("test")
    )

    testImplementation(
        libs.ktor.server.test.host
    )
}