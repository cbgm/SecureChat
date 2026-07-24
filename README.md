<div align="center">

# 🔐 SecureChat

**Modern end-to-end encrypted messaging built with Kotlin Multiplatform**

![Kotlin](https://img.shields.io/badge/Kotlin-2.4.0-7F52FF?logo=kotlin&logoColor=white)
![Compose Multiplatform](https://img.shields.io/badge/Compose-Multiplatform-4285F4)
![Android](https://img.shields.io/badge/Android-API%2029+-3DDC84?logo=android&logoColor=white)
![iOS](https://img.shields.io/badge/iOS-Supported-black?logo=apple)
![Material 3](https://img.shields.io/badge/Material-3-6750A4)
![Architecture](https://img.shields.io/badge/Architecture-Clean-success)
![Compose UI](https://img.shields.io/badge/UI-Compose_Multiplatform-blue)
![Detekt](https://img.shields.io/badge/Quality-Detekt-success)
![License](https://img.shields.io/badge/License-Apache%202.0-blue)

</div>

---

# Overview

SecureChat is a modular **Kotlin Multiplatform** secure messaging application built with **Compose Multiplatform**, **Material 3**, **Koin**, **Room**, **Ktor**, and **LibSodium**.

The project follows a feature-based **Clean Architecture** with centralized Gradle convention plugins, automated quality verification, generated architecture documentation, and custom static analysis rules.

---

# Project Structure

```text
androidApp/        Android application
iosApp/            iOS application

core/              Shared reusable libraries
feature/           Feature modules
navigation/        Navigation layer
data/              Database & repositories
relay/             Relay server

build-logic/       Convention plugins
quality/           Custom Detekt rules
docs/              Project documentation
```

---

# Documentation

## Main Documentation

- 📘 [Documentation Index](docs/index.md)

## Generated Documentation

Generated automatically by the architecture tooling.

- 🏗️ [Architecture Overview](docs/generated/architecture.md)
- 📦 [Module Documentation](docs/generated/modules.md)
- 🔗 [Dependency Matrix](docs/generated/dependencies.md)
- 📊 [Project Statistics](docs/generated/statistics.md)
- 📈 `docs/generated/module-graph.mmd`
- 🗂️ `docs/generated/architecture.json`

---

# Getting Started

Run once after cloning:

```bash
./gradlew setup
```

---

# Build

```bash
./gradlew build
```

---

# Code Quality

Automatically formats source code and runs all quality tools.

```bash
./gradlew quality
```

Verification only (CI-safe):

```bash
./gradlew qualityCheck
```

Included checks:

- ktlint
- Detekt
- Custom Detekt Rules
- Architecture Verification

---

# Architecture Documentation

Generate documentation whenever module dependencies change:

```bash
./gradlew architectureReport
```

Verify generated documentation:

```bash
./gradlew verifyArchitectureReport
```

Generated files are written to:

```text
docs/generated/
```

---

# Android

Build:

```bash
./gradlew :androidApp:assembleDebug
```

Run using Android Studio.

---

# iOS

Open

```text
iosApp/
```

in Xcode and run the application.

---

# Technology Stack

- Kotlin Multiplatform
- Compose Multiplatform
- Material 3
- Kotlin Coroutines
- Kotlin Serialization
- Koin
- Room
- Ktor
- LibSodium
- Gradle Convention Plugins
- Detekt
- Ktlint
- MkDocs

---

# Architecture

SecureChat follows a modular architecture consisting of:

- Feature modules
- Shared core libraries
- Convention plugins
- Automated dependency verification
- Generated architecture documentation
- Custom Detekt rules
- Feature-based Clean Architecture

The generated architecture documentation is considered the source of truth for the project's dependency graph.

---

# License

Licensed under the Apache 2.0 License.
