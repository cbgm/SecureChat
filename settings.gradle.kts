rootProject.name = "SecureChat"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")

    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":androidApp")
include(":shared")
include(":core")
include(":feature:identity")
include(":feature:contacts")
include(":data:database")
include(":feature:contactimport")
include(":feature:chats")
include(":core:crypto")
include(":core:protocol")
include(":feature:messaging")
include(":feature:transport")
include(":relay")
include(":feature:onboarding")
include(":startup")
include(":navigation")
include(":notification")
include(":core:ui")
include(":feature:settings")
include(":quality:detekt-rules")
include(":resources")
