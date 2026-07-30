# Project Structure

## Overview

SecureChat is organized as a modular Kotlin Multiplatform project.

Every directory has a single responsibility.

The project intentionally avoids large monolithic modules by separating functionality into reusable libraries and feature modules.

The overall structure follows Clean Architecture while taking advantage of Gradle convention plugins and Kotlin Multiplatform.

---

# Repository Layout

The repository is organized as follows.

```
SecureChat/

androidApp/

build-logic/

config/

core/

data/

docs/

feature/

navigation/

quality/

relay/

shared/

startup/

gradle/

.github/
```

Each directory has a clearly defined purpose.

---

# androidApp

```
androidApp/
```

This module contains the Android application.

Responsibilities include

- Application class
- Android Manifest
- Android entry point
- Koin initialization
- Activity
- Android-only resources

The Android application should remain as small as possible.

Business logic belongs elsewhere.

---

# build-logic

```
build-logic/
```

This is an included Gradle build.

It contains

- Convention Plugins
- Quality Plugin
- Architecture Plugin
- Gradle Utilities
- Custom Tasks
- Documentation Generator

This project configures the entire build.

Individual modules should contain very little Gradle configuration.

---

# config

```
config/
```

Contains configuration for development tools.

Typical contents include

- Detekt
- KtLint
- Gradle
- IDE configuration

Keeping configuration centralized ensures consistent behaviour across the project.

---

# core

```
core/
```

Contains reusable libraries shared by multiple features.

Examples include

- crypto
- protocol
- ui
- id
- recommendations

Core modules should remain independent.

They must never depend on feature modules.

---

# data

```
data/
```

Contains the data layer.

Typical responsibilities include

- Room
- Database
- Repository implementations
- Local storage
- Remote APIs

Business rules should remain inside the domain layer.

---

# feature

```
feature/
```

Contains independent application features.

Examples

```
contacts

identity

chats

transport

onboarding
```

Each feature should encapsulate

- presentation
- domain
- data (if applicable)
- navigation entry point

Features communicate through public APIs rather than implementation details.

---

# navigation

```
navigation/
```

Contains application navigation.

Responsibilities

- Navigation Graph
- Routes
- Navigation APIs

Business logic should never be implemented here.

---

# quality

```
quality/
```

Contains project-specific static analysis.

Currently this includes

- custom Detekt rules
- Detekt registration
- rule tests

Only SecureChat-specific rules belong here.

General-purpose rules should use the standard Detekt rule set.

---

# relay

```
relay/
```

Contains the SecureChat relay server.

Responsibilities include

- WebSocket handling
- Client registration
- Message forwarding
- Connection management

The relay intentionally remains lightweight and stores no persistent message history.

---

# shared

```
shared/
```

Contains modules shared by multiple application features.

Examples include

- shared UI
- reusable navigation helpers
- application-wide services

Shared modules should avoid depending on individual features whenever possible.

---

# startup

```
startup/
```

Contains startup and initialization logic.

Typical responsibilities include

- application initialization
- startup checks
- dependency initialization

Keeping startup separate improves maintainability and testability.

---

# docs

```
docs/
```

Contains the engineering handbook.

Documentation is divided into

```
getting-started/

architecture/

build/

security/

development/

features/

api/

generated/
```

Generated documentation should never be edited manually.

---

# gradle

```
gradle/
```

Contains

- Version Catalog
- Gradle Wrapper

All dependency versions are managed centrally.

Individual modules should never hardcode library versions.

---

# .github

```
.github/
```

Contains GitHub configuration.

Typical contents

- GitHub Actions
- Issue Templates
- Pull Request Templates

CI configuration belongs here.

---

# Layered Architecture

The project follows a layered architecture.

```
androidApp

↓

shared

↓

navigation

↓

feature

↓

data

↓

core
```

Dependencies should generally point downward.

Lower layers must remain reusable.

---

# Feature Structure

A typical feature module is organized as

```
feature/

contacts/

commonMain/

androidMain/

commonTest/
```

Inside

```
commonMain/
```

the feature usually contains

```
presentation/

domain/

data/
```

This structure keeps responsibilities clearly separated.

---

# Naming Conventions

Modules should follow consistent naming.

Examples

```
core:crypto

core:ui

feature:contacts

feature:identity

data:database
```

Avoid abbreviations.

Module names should describe their primary responsibility.

---

# Dependencies

Every module declares only the dependencies it actually requires.

Convention plugins configure infrastructure.

Runtime libraries remain explicit.

Example

```
plugins

↓

configuration

dependencies

↓

runtime
```

This makes dependencies easy to understand during code reviews.

---

# Generated Files

Generated files are located in

```
build/
```

or

```
docs/generated/
```

Developers should not manually edit generated documentation.

Instead regenerate it using

```bash
./gradlew architectureReport
```

---

# Build Infrastructure

The repository contains an extensive build infrastructure.

Major components include

- Convention Plugins
- Version Catalog
- Quality Plugin
- Architecture Plugin
- Git Hooks
- Generated Documentation

These systems automate repetitive tasks and keep module build files concise.

---

# Summary

SecureChat's directory structure reflects its architecture.

Each directory has a clearly defined purpose and should only contain code related to that responsibility.

Maintaining these boundaries keeps the project scalable as new features and modules are added.
