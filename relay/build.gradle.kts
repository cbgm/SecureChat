plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.serialization.classpath)
    alias(libs.plugins.securechat.lint)
    alias(libs.plugins.securechat.properties)
    application
}

val firebaseAdminCredentialsPath =
    localProperties.getOrNull(
        "securechat.firebase.adminCredentials"
    )

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
    implementation(libs.firebase.admin)

    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test.host)
}

tasks.named<JavaExec>("run") {
    if (!firebaseAdminCredentialsPath.isNullOrBlank()) {
        val credentialsFile =
            rootProject.file(firebaseAdminCredentialsPath)

        environment(
            "GOOGLE_APPLICATION_CREDENTIALS",
            credentialsFile.absolutePath
        )

        doFirst {
            require(credentialsFile.isFile) {
                "Firebase Admin credential file does not exist: " +
                    credentialsFile.absolutePath
            }
        }
    } else {
        doFirst {
            logger.warn(
                "securechat.firebase.adminCredentials is not configured " +
                    "in local.properties. FCM push delivery will be disabled."
            )
        }
    }
}
