# SecureChat Build Logic

This document explains how the SecureChat Gradle build logic works, what each convention plugin configures, what remains explicit in module build files, and how dependency bundles are intended to be used.

The final setup follows one rule:

> Convention plugins configure Gradle infrastructure.  
> Version-catalog bundles group related dependencies.  
> Module build files keep their actual dependencies explicit.

This avoids hiding too much inside convention plugins while still removing repeated Kotlin Multiplatform, Android, Compose, testing, serialization, and Room configuration.

---

# 1. Project structure

```text
SecureChat/
├── build-logic/
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   └── src/main/kotlin/com/cbgm/securechat/buildlogic/
│       ├── SecureChatKmpLibraryPlugin.kt
│       ├── SecureChatKmpComposePlugin.kt
│       ├── SecureChatKmpComposeFeaturePlugin.kt
│       ├── SecureChatKmpTestingPlugin.kt
│       ├── SecureChatKmpSerializationPlugin.kt
│       ├── SecureChatKmpRoomPlugin.kt
│       └── VersionCatalogExtensions.kt
├── gradle/
│   └── libs.versions.toml
├── settings.gradle.kts
└── ...
```

The build logic is an included Gradle build.

The root `settings.gradle.kts` must contain:

```kotlin
pluginManagement {
    includeBuild("build-logic")

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
```

The SecureChat plugin aliases in `libs.versions.toml` must not declare a version:

```toml
[plugins]
securechat-kmp-library = { id = "securechat.kmp.library" }
securechat-kmp-compose = { id = "securechat.kmp.compose" }
securechat-kmp-compose-feature = { id = "securechat.kmp.compose.feature" }
securechat-kmp-testing = { id = "securechat.kmp.testing" }
securechat-kmp-serialization = { id = "securechat.kmp.serialization" }
securechat-kmp-room = { id = "securechat.kmp.room" }
```

Do not add `version = "1.0.0"` to these local included-build plugins.

---

# 2. Main design rule

Convention plugins do not add normal runtime dependencies.

They configure:

- Gradle plugins
- Kotlin Multiplatform targets
- Android SDK values
- JVM target
- Android host/device test components
- Compose compiler/plugin setup
- Kotlin serialization plugin setup
- Room and KSP infrastructure
- Room schema output
- Room compiler KSP configurations

Module build files still explicitly add:

- Compose libraries
- Coroutines
- Koin
- Lifecycle
- Serialization runtime
- Room runtime
- CameraX
- Ktor
- Project dependencies
- Test dependencies

This keeps module ownership visible.

---

# 3. Plugin hierarchy

```text
securechat.kmp.library
├── securechat.kmp.compose
│   └── securechat.kmp.compose.feature
├── securechat.kmp.testing
├── securechat.kmp.serialization
└── securechat.kmp.room
```

More precisely:

```text
securechat.kmp.compose.feature
├── securechat.kmp.compose
│   └── securechat.kmp.library
└── securechat.kmp.testing
    └── securechat.kmp.library
```

Gradle applies the same plugin only once, even when it is reached through multiple convention plugins.

---

# 4. `securechat.kmp.library`

Apply with:

```kotlin
plugins {
    alias(libs.plugins.securechat.kmp.library)
}
```

Use it for plain Kotlin Multiplatform modules such as domain, core, repository, crypto, or other non-Compose modules.

## What it configures

It applies:

```text
org.jetbrains.kotlin.multiplatform
com.android.kotlin.multiplatform.library
```

It also configures:

- Android `compileSdk`
- Android `minSdk`
- Android JVM target 17
- `iosArm64()` on macOS
- `iosSimulatorArm64()` on macOS

## What it replaces

Remove:

```kotlin
alias(libs.plugins.kotlinMultiplatform)
alias(libs.plugins.androidMultiplatformLibrary)
```

Remove:

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
```

Remove repeated Android configuration:

```kotlin
compileSdk = libs.versions.android.compileSdk.get().toInt()
minSdk = libs.versions.android.minSdk.get().toInt()

compilerOptions {
    jvmTarget = JvmTarget.JVM_17
}
```

Remove repeated iOS target creation:

```kotlin
val isMacOs = System
    .getProperty("os.name")
    .startsWith("Mac", ignoreCase = true)

if (isMacOs) {
    iosArm64()
    iosSimulatorArm64()
}
```

## What remains explicit

Keep the namespace:

```kotlin
kotlin {
    android {
        namespace = "com.cbgm.securechat.core.example"
    }
}
```

Keep actual dependencies:

```kotlin
implementation(libs.bundles.coroutines)
implementation(libs.bundles.koin.core)
implementation(projects.core.protocol)
```

## Example

```kotlin
plugins {
    alias(libs.plugins.securechat.kmp.library)
}

kotlin {
    android {
        namespace = "com.cbgm.securechat.core"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.bundles.coroutines)
        }
    }
}
```

---

# 5. `securechat.kmp.compose`

Apply with:

```kotlin
plugins {
    alias(libs.plugins.securechat.kmp.compose)
}
```

Use it for reusable Compose KMP modules such as `core:ui`.

## What it configures

It applies:

```text
securechat.kmp.library
org.jetbrains.compose
org.jetbrains.kotlin.plugin.compose
```

## What it replaces

Remove:

```kotlin
alias(libs.plugins.kotlinMultiplatform)
alias(libs.plugins.androidMultiplatformLibrary)
alias(libs.plugins.composeMultiplatform)
alias(libs.plugins.composeCompiler)
```

Also remove the base KMP configuration already handled by `securechat.kmp.library`.

## What remains explicit

The plugin does not add Compose dependencies. Keep:

```kotlin
implementation(libs.bundles.compose)
```

Add only the other capabilities actually used:

```kotlin
implementation(libs.bundles.lifecycle.compose)
implementation(libs.bundles.coroutines)
implementation(libs.bundles.koin.compose)
```

## Example

```kotlin
plugins {
    alias(libs.plugins.securechat.kmp.compose)
}

kotlin {
    android {
        namespace = "com.cbgm.securechat.core.ui"

        androidResources {
            enable = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.bundles.compose)
            implementation(libs.bundles.coroutines)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(compose.materialIconsExtended)
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

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
```

---

# 6. `securechat.kmp.testing`

Apply with:

```kotlin
plugins {
    alias(libs.plugins.securechat.kmp.testing)
}
```

## What it configures

It applies `securechat.kmp.library` and creates:

```kotlin
withHostTest {
    isIncludeAndroidResources = true
}

withDeviceTest {
    instrumentationRunner =
        "androidx.test.runner.AndroidJUnitRunner"

    execution = "HOST"
}
```

## Important rule

The test components must be created only once.

When this plugin is applied, remove all module-level `withHostTest` and `withDeviceTest` blocks. Otherwise Gradle reports that host or device tests have already been enabled.

## What remains explicit

The plugin does not add test dependencies.

```kotlin
commonTest.dependencies {
    implementation(libs.bundles.kmp.testing)
}
```

```kotlin
androidHostTest.dependencies {
    implementation(libs.bundles.android.host.testing)
}
```

```kotlin
androidDeviceTest.dependencies {
    implementation(libs.bundles.android.device.testing)
}
```

Add module-specific test dependencies separately:

```kotlin
androidDeviceTest.dependencies {
    implementation(libs.bundles.android.device.testing)
    implementation(libs.bundles.room.runtime)
    implementation(projects.data.database)
}
```

---

# 7. `securechat.kmp.compose.feature`

Apply with:

```kotlin
plugins {
    alias(libs.plugins.securechat.kmp.compose.feature)
}
```

Use it for Compose feature modules with screens, routes, ViewModels, and standard KMP/Android tests.

## What it configures

It applies:

```text
securechat.kmp.compose
securechat.kmp.testing
```

That includes:

- Kotlin Multiplatform
- Android KMP library
- Android SDK configuration
- JVM 17
- iOS targets on macOS
- Compose plugin/compiler
- Android host tests
- Android device tests

## What it replaces

Remove the manual Kotlin, Android KMP, Compose, and Compose compiler plugin aliases. Also remove repeated SDK, JVM, iOS, host-test, and device-test configuration.

## What remains explicit

A normal feature module still declares:

```kotlin
implementation(libs.bundles.compose)
implementation(libs.bundles.lifecycle.compose)
implementation(libs.bundles.coroutines)
implementation(libs.bundles.koin.compose)
```

It also keeps project dependencies:

```kotlin
implementation(projects.core)
implementation(projects.core.ui)
implementation(projects.feature.contacts)
```

## Example

```kotlin
plugins {
    alias(libs.plugins.securechat.kmp.compose.feature)
}

kotlin {
    android {
        namespace = "com.cbgm.securechat.feature.contacts"

        androidResources {
            enable = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core)
            implementation(projects.core.crypto)
            implementation(projects.core.protocol)
            implementation(projects.core.ui)
            implementation(projects.data.database)
            implementation(projects.feature.identity)

            implementation(libs.bundles.compose)
            implementation(libs.bundles.lifecycle.compose)
            implementation(libs.bundles.coroutines)
            implementation(libs.bundles.koin.compose)

            implementation(compose.materialIconsExtended)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
        }

        commonTest.dependencies {
            implementation(libs.bundles.kmp.testing)
        }

        androidDeviceTest.dependencies {
            implementation(libs.bundles.android.device.testing)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
```

---

# 8. `securechat.kmp.serialization`

Apply with:

```kotlin
plugins {
    alias(libs.plugins.securechat.kmp.serialization)
}
```

## What it configures

It applies:

```text
securechat.kmp.library
org.jetbrains.kotlin.plugin.serialization
```

## What it replaces

Remove:

```kotlin
alias(libs.plugins.kotlinMultiplatform)
alias(libs.plugins.androidMultiplatformLibrary)
alias(libs.plugins.kotlin.serialization)
```

Also remove base KMP configuration handled by `securechat.kmp.library`.

## What remains explicit

Keep the serialization runtime bundle:

```kotlin
implementation(libs.bundles.serialization)
```

## Example

```kotlin
plugins {
    alias(libs.plugins.securechat.kmp.serialization)
    alias(libs.plugins.securechat.kmp.testing)
}

kotlin {
    android {
        namespace = "com.cbgm.securechat.core.protocol"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.bundles.coroutines)
            implementation(libs.bundles.koin.core)
            implementation(libs.bundles.serialization)
        }

        commonTest.dependencies {
            implementation(libs.bundles.kmp.testing)
        }
    }
}
```

---

# 9. `securechat.kmp.room`

Apply with:

```kotlin
plugins {
    alias(libs.plugins.securechat.kmp.room)
}
```

## What it configures

It applies:

```text
securechat.kmp.library
com.google.devtools.ksp
androidx.room
```

It configures:

```kotlin
room {
    schemaDirectory("$projectDir/schemas")
}
```

It wires the Room compiler to:

```text
kspAndroid
kspIosArm64
kspIosSimulatorArm64
```

The iOS compiler configurations are added only on macOS.

## What it replaces

Remove the manual Kotlin Multiplatform, Android KMP, KSP, and Room plugin aliases. Remove manual Room schema setup and manual `ksp*` compiler declarations.

## What remains explicit

Keep Room runtime dependencies:

```kotlin
implementation(libs.bundles.room.runtime)
```

Testing is separate:

```kotlin
plugins {
    alias(libs.plugins.securechat.kmp.room)
    alias(libs.plugins.securechat.kmp.testing)
}
```

---

# 10. Dependency bundles

Bundles should represent one coherent library family or capability.

Avoid bundles that mix unrelated concerns, such as coroutines and Koin.

Recommended bundles:

```toml
[bundles]
compose = [
    "compose-runtime",
    "compose-foundation",
    "compose-material3",
    "compose-ui",
    "compose-components-resources",
    "compose-uiToolingPreview"
]

lifecycle-compose = [
    "androidx-lifecycle-viewmodelCompose",
    "androidx-lifecycle-runtimeCompose"
]

coroutines = [
    "kotlinx-coroutines-core"
]

coroutines-test = [
    "kotlinx-coroutines-test"
]

koin-core = [
    "koin-core"
]

koin-compose = [
    "koin-core",
    "koin-core-viewmodel",
    "koin-compose",
    "koin-compose-viewmodel"
]

kmp-testing = [
    "kotlin-test",
    "kotlinx-coroutines-test"
]

android-host-testing = [
    "junit",
    "kotlin-testJunit"
]

android-device-testing = [
    "kotlin-test",
    "androidx-test-core",
    "androidx-test-runner",
    "androidx-testExt-junit",
    "androidx-espresso-core"
]

serialization = [
    "kotlinx-serialization-core",
    "kotlinx-serialization-json"
]

room-runtime = [
    "androidx-room-runtime",
    "androidx-sqlite-bundled"
]

camera = [
    "androidx-camera-core",
    "androidx-camera-camera2",
    "androidx-camera-lifecycle",
    "androidx-camera-view",
    "zxing-core"
]

ktor-client = [
    "ktor-client-core",
    "ktor-client-websockets"
]

ktor-server = [
    "ktor-server-core",
    "ktor-server-netty",
    "ktor-server-websockets",
    "ktor-server-call-logging",
    "ktor-serialization-kotlinx-json"
]
```

Generated accessors:

```kotlin
libs.bundles.compose
libs.bundles.lifecycle.compose
libs.bundles.coroutines
libs.bundles.coroutines.test
libs.bundles.koin.core
libs.bundles.koin.compose
libs.bundles.kmp.testing
libs.bundles.android.host.testing
libs.bundles.android.device.testing
libs.bundles.serialization
libs.bundles.room.runtime
libs.bundles.camera
libs.bundles.ktor.client
libs.bundles.ktor.server
```

---

# 11. What remains in each module

Convention plugins intentionally do not hide module-specific decisions.

Keep:

## Namespace

```kotlin
android {
    namespace = "com.cbgm.securechat.feature.contacts"
}
```

## Android resources

```kotlin
androidResources {
    enable = true
}
```

Only keep this where resources are actually required.

## Compose resource package

```kotlin
compose.resources {
    publicResClass = true
    generateResClass = always
    packageOfResClass = "com.cbgm.securechat.resources"
}
```

## Project dependencies

```kotlin
implementation(projects.core)
implementation(projects.core.ui)
implementation(projects.feature.identity)
```

## Platform-specific dependencies

```kotlin
androidMain.dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
}
```

## Module-specific tests

```kotlin
androidDeviceTest.dependencies {
    implementation(libs.bundles.android.device.testing)
    implementation(libs.bundles.room.runtime)
    implementation(projects.data.database)
}
```

## Compose tooling

For Android KMP Compose modules:

```kotlin
dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
```

For the Android application:

```kotlin
debugImplementation(libs.compose.uiTooling)
```

Do not add `compose-ui-tooling` to `commonMain`.

---

# 12. iOS frameworks

The convention plugin creates iOS targets on macOS.

Feature, core, database, transport, and navigation modules normally do not need separate framework binaries.

Remove framework blocks from those modules unless they are exported directly to an iOS application.

Usually only `shared` needs:

```kotlin
val isMacOs = System
    .getProperty("os.name")
    .startsWith("Mac", ignoreCase = true)

kotlin {
    if (isMacOs) {
        listOf(
            iosArm64(),
            iosSimulatorArm64()
        ).forEach { target ->
            target.binaries.framework {
                baseName = "SecureChat"
                isStatic = true
            }
        }
    }
}
```

The convention plugin already creates the targets. This block only configures the exported framework binary.

---

# 13. Android application module

The current SecureChat convention plugins target KMP libraries.

The Android app should keep its application plugins explicit:

```kotlin
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}
```

A dedicated Android application convention plugin could be introduced later, but it is not necessary for a single app module.

---

# 14. JVM relay module

The relay server is a JVM application, not KMP.

Do not use the KMP convention plugins.

```kotlin
plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.serialization)
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
```

---

# 15. Choosing the correct plugin

| Module type | Plugin |
|---|---|
| Plain KMP module | `securechat.kmp.library` |
| Reusable Compose KMP module | `securechat.kmp.compose` |
| Compose feature module | `securechat.kmp.compose.feature` |
| Module with standard KMP/Android tests | Add `securechat.kmp.testing` |
| Serialization module | `securechat.kmp.serialization` |
| Room database module | `securechat.kmp.room` |
| Android application | Keep Android app plugins explicit |
| JVM server | Keep JVM/application plugins explicit |

---

# 16. Common mistakes

## Adding versions to local plugin aliases

Wrong:

```toml
securechat-kmp-library = {
    id = "securechat.kmp.library",
    version = "1.0.0"
}
```

Correct:

```toml
securechat-kmp-library = {
    id = "securechat.kmp.library"
}
```

## Creating Android tests twice

Wrong:

```kotlin
plugins {
    alias(libs.plugins.securechat.kmp.testing)
}

kotlin {
    android {
        withDeviceTest {
            ...
        }
    }
}
```

Correct:

```kotlin
plugins {
    alias(libs.plugins.securechat.kmp.testing)
}
```

Then add only dependencies:

```kotlin
androidDeviceTest.dependencies {
    implementation(libs.bundles.android.device.testing)
}
```

## Adding Compose tooling to `commonMain`

Wrong:

```kotlin
commonMain.dependencies {
    implementation(libs.compose.uiTooling)
}
```

Correct:

```kotlin
dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
```

## Assuming plugins add dependencies

They do not. This remains required:

```kotlin
commonMain.dependencies {
    implementation(libs.bundles.compose)
    implementation(libs.bundles.coroutines)
}
```

## Producing frameworks from every module

Most modules should expose KMP targets only. Usually only `shared` should create the framework consumed by the iOS app.

---

# 17. Migration checklist

For each module:

1. Choose the correct convention plugin.
2. Replace manual Kotlin/Android/Compose plugin aliases.
3. Remove repeated `compileSdk`.
4. Remove repeated `minSdk`.
5. Remove repeated JVM target configuration.
6. Remove repeated iOS target creation.
7. Remove duplicate `withHostTest`.
8. Remove duplicate `withDeviceTest`.
9. Add required dependency bundles explicitly.
10. Keep project dependencies.
11. Keep the namespace.
12. Keep `androidResources` only where needed.
13. Keep module-specific platform and test dependencies.
14. Build the module before migrating the next one.

Example:

```powershell
.\gradlew --stop
.\gradlew :feature:contacts:build
.\gradlew :feature:identity:build
.\gradlew build
```

---

# 18. Cache errors

Errors such as:

```text
Unexpected lock protocol found in lock file
```

or:

```text
Could not add entry to cache file-access.bin
```

are corrupted Gradle cache errors, not convention-plugin errors.

Close Android Studio and delete:

```text
C:\Users\Chris\.gradle\caches\journal-1
C:\Users\Chris\.gradle\caches\jars-9
C:\Users\Chris\.gradle\daemon
```

Also delete project-local caches:

```text
.gradle
build-logic/.gradle
build-logic/build
```

Then run:

```powershell
.\gradlew --stop
.\gradlew help --refresh-dependencies
```

---

# 19. Summary

Convention plugins answer:

> How is this module built?

Dependency bundles answer:

> Which related libraries does this module use?

Module build files answer:

> What does this module actually depend on?

That separation keeps the build explicit, maintainable, and much less repetitive.
