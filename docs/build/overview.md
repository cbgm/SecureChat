# Build System Overview

## Introduction

SecureChat uses a modern Gradle-based build infrastructure designed around automation, consistency and maintainability.

Unlike traditional Android projects where each module maintains its own build configuration, SecureChat centralizes nearly all configuration inside reusable convention plugins.

This results in

- smaller build scripts
- consistent configuration
- faster onboarding
- easier dependency updates
- less duplication

---

# Build Philosophy

The build system follows several core principles.

- Convention over configuration
- One source of truth
- Automation before documentation
- Fail fast
- Reproducible builds

Developers should rarely need to modify individual module build files.

---

# Repository Structure

```
SecureChat/

build-logic/
gradle/
config/
docs/
```

The build infrastructure lives almost entirely inside **build-logic**.

---

# Included Build

SecureChat uses an Included Build.

```
build-logic/
```

This project compiles first.

It provides

- Convention Plugins
- Architecture Plugin
- Quality Plugin
- Shared Gradle Utilities

The application modules consume these plugins.

---

# Version Catalog

All dependency versions are managed centrally.

```
gradle/libs.versions.toml
```

This file defines

- libraries
- plugin versions
- bundles
- version aliases

Individual modules should never hardcode dependency versions.

---

# Convention Plugins

Convention Plugins replace duplicated Gradle configuration.

Instead of every module defining

- compileSdk
- Kotlin
- Compose
- Android configuration
- testing

they simply apply the appropriate convention plugin.

Example

```kotlin
plugins {
    alias(libs.plugins.securechat.feature)
}
```

The convention plugin performs the remaining configuration.

---

# Architecture Plugin

The Architecture Plugin discovers the project automatically.

It validates

- project dependencies
- dependency cycles
- module graph

It also generates

```
docs/generated/
```

including

- architecture.md
- architecture.mmd
- module pages
- dependency matrix
- statistics

---

# Quality Plugin

The Quality Plugin centralizes quality automation.

Responsibilities include

- KtLint
- Detekt
- Architecture Verification
- Documentation Verification

Developers typically execute

```bash
./gradlew quality
```

instead of individual verification tasks.

---

# Git Hooks

Git hooks are tracked inside the repository.

```
.githooks/
```

Running

```bash
./gradlew setup
```

installs them automatically.

Current hooks include

- pre-commit
- pre-push

The hooks ensure that quality checks execute before code reaches the repository.

---

# Configuration Cache

The build is designed to support Gradle Configuration Cache.

Custom tasks should

- avoid accessing Project during execution
- use lazy Providers
- declare inputs and outputs correctly

Maintaining cache compatibility significantly improves build performance.

---

# Build Lifecycle

```
Gradle

↓

Included Build

↓

Convention Plugins

↓

Module Configuration

↓

Compilation

↓

Quality

↓

Architecture

↓

Verification

↓

Success
```

---

# Generated Documentation

Generated documentation is stored in

```
docs/generated/
```

Developers should never edit these files manually.

Instead run

```bash
./gradlew architectureReport
```

to regenerate them.

---

# Common Commands

Build

```bash
./gradlew build
```

Quality

```bash
./gradlew quality
```

Auto-fix formatting

```bash
./gradlew qualityFix
```

Generate documentation

```bash
./gradlew architectureReport
```

Validate architecture

```bash
./gradlew validateArchitecture
```

---

# Continuous Integration

CI executes the same verification pipeline used locally.

No separate CI-only build configuration exists.

This minimizes "works on my machine" issues.

---

# Extending the Build

When extending the build

- prefer convention plugins
- reuse existing infrastructure
- avoid module-specific Gradle logic
- keep tasks configuration-cache compatible

Build infrastructure should remain modular just like application code.

---

# Summary

The SecureChat build system is designed to remove repetitive configuration from individual modules and replace it with centralized, reusable infrastructure.

Convention plugins, automated quality checks and generated architecture documentation ensure that the project remains maintainable as it continues to grow.
