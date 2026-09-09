plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.serialization.classpath)
    alias(libs.plugins.sparrow.lint)
    application
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.cbgm.sparrow.server.linkpreview.ApplicationKt")
}

dependencies {
    implementation(projects.server.observability)
    implementation(projects.server.protocol)
    implementation(libs.bundles.ktor.server)
    implementation(libs.bundles.ktor.client)
    implementation(libs.ktor.client.cio)
    implementation(libs.logback.classic)

    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test.host)
}
