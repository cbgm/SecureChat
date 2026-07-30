plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.serialization.classpath)
    alias(libs.plugins.securechat.lint)
    application
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set(
        "com.cbgm.securechat.relay.ApplicationKt"
    )
}

dependencies {
    implementation(libs.bundles.ktor.server)
    implementation(libs.bundles.serialization)
    implementation(libs.bundles.coroutines)
    implementation(libs.logback.classic)

    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test.host)
}
