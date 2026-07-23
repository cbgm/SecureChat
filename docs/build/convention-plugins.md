# Convention Plugins

## Overview

Convention Plugins are the foundation of the SecureChat build system.

Instead of duplicating Gradle configuration across every module, common configuration is centralized into reusable plugins.

Every project module applies one or more SecureChat convention plugins.

This approach keeps module build files extremely small and guarantees consistent configuration throughout the project.

---

# Why Convention Plugins?

Without convention plugins every module would repeat

- Kotlin configuration
- Android configuration
- Compose configuration
- compiler options
- testing
- lint configuration

Example

```
Module A

compileSdk
minSdk
jvmTarget
Compose
Kotlin

Module B

compileSdk
minSdk
jvmTarget
Compose
Kotlin
```

The duplicated configuration quickly becomes difficult to maintain.

Convention plugins eliminate this duplication.

---

# Build Logic

Convention plugins live inside

```
build-logic/
```

This project is compiled before the main project.

Every application module consumes the compiled plugins.

---

# Advantages

Convention plugins provide

- centralized configuration
- consistent module setup
- easier upgrades
- less duplicated Gradle code
- simpler onboarding
- safer refactoring

Most module build scripts contain only

- plugins
- dependencies

Everything else is configured automatically.

---

# Typical Module

A typical feature module contains

```kotlin
plugins {
    alias(libs.plugins.securechat.feature)
}
```

and its dependencies.

There is almost no additional Gradle configuration.

---

# Plugin Responsibilities

A convention plugin typically configures

- Kotlin
- Android
- Compose
- compiler options
- source sets
- testing
- resources
- common dependencies

It should **not**

- configure unrelated modules
- contain business logic
- hardcode project-specific paths unnecessarily

---

# Plugin Types

SecureChat convention plugins generally fall into several categories.

## Android

Configures Android modules.

Typical configuration

- compileSdk
- minSdk
- namespace
- manifest
- resources

---

## Kotlin Multiplatform

Configures

- commonMain
- androidMain
- commonTest

along with compiler options shared by KMP modules.

---

## Compose

Provides

- Compose Compiler
- Compose Multiplatform
- Compose Resources

and common UI configuration.

---

## Feature

Feature plugins configure

```
feature/*
```

modules.

Typical responsibilities

- Compose
- KMP
- testing
- Android configuration

---

## Core

Core plugins configure reusable libraries.

Core plugins generally avoid

- Android-only APIs
- feature-specific dependencies

---

## Data

Data plugins configure

- Room
- serialization
- persistence
- testing

---

## Shared

Shared plugins configure reusable application modules.

---

## Relay

Relay plugins configure the standalone relay server.

---

# Dependency Management

Convention plugins configure infrastructure.

Application dependencies remain explicit.

Example

```kotlin
dependencies {

    implementation(projects.core.crypto)

    implementation(projects.data.database)

}
```

Developers can immediately see runtime dependencies.

---

# Version Catalog

Convention plugins should use the Version Catalog whenever possible.

Avoid

```kotlin
implementation("library:1.2.3")
```

Prefer

```kotlin
implementation(libs.kotlinx.coroutines)
```

This keeps versions centralized.

---

# Plugin Composition

Plugins may build on top of one another.

Example

```
Android

↓

Kotlin

↓

Compose

↓

Feature
```

Higher-level plugins compose lower-level plugins.

Avoid duplicating shared configuration.

---

# Configuration Cache

Convention plugins should remain compatible with Gradle Configuration Cache.

Avoid

- eager task lookup
- accessing Project during task execution
- mutable global state

Prefer

- Providers
- lazy configuration
- declared task inputs and outputs

---

# Testing

Convention plugins should be tested like application code.

Typical tests include

- plugin application
- configured extensions
- generated tasks
- expected compiler configuration

Build infrastructure deserves the same engineering standards as production code.

---

# Extending Convention Plugins

Before creating a new plugin ask

- Can an existing plugin be extended?
- Is this configuration reusable?
- Does another module already solve this problem?

Avoid plugin proliferation.

Every plugin should have a clear purpose.

---

# Best Practices

- Keep plugins focused.
- Keep configuration declarative.
- Prefer composition over duplication.
- Centralize common configuration.
- Avoid hidden runtime dependencies.
- Keep module build scripts small.

---

# Summary

Convention Plugins are one of the most important architectural decisions in the SecureChat build.

They centralize Gradle configuration, eliminate duplication and allow every module to remain small, consistent and easy to understand.

As the project grows, almost all reusable build behaviour should be implemented through convention plugins rather than duplicated Gradle scripts.
