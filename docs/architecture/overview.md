# Architecture Overview

## Introduction

SecureChat is designed as a modular Kotlin Multiplatform application following the principles of Clean Architecture.

The primary goals of the architecture are:

- separation of responsibilities
- long-term maintainability
- high testability
- scalability
- platform independence
- explicit dependencies

The project intentionally avoids monolithic modules by separating reusable infrastructure from application features.

---

# High-Level Architecture

The project is organized into several architectural layers.

```
                 Android App
                      │
                      ▼
                  Shared Modules
                      │
                      ▼
                  Navigation
                      │
                      ▼
                 Feature Modules
                      │
                      ▼
                  Data Modules
                      │
                      ▼
                  Core Modules
```

Dependencies generally point downward.

Lower layers never depend on higher layers.

---

# Project Modules

The project consists of several top-level module groups.

```
androidApp

build-logic

core

data

feature

navigation

shared

startup

relay

quality
```

Each group has a single responsibility.

---

# Android Application

The Android application is intentionally small.

Responsibilities include

- Application startup
- Activity
- Android Manifest
- Dependency Injection bootstrap

Business logic must never be implemented here.

---

# Core

Core modules provide reusable libraries.

Examples include

- cryptography
- protocol
- identifiers
- shared UI
- utility classes

Core modules are completely reusable.

They must never depend on application features.

---

# Data

The data layer provides infrastructure required by the application.

Examples

- Room
- repositories
- persistence
- remote communication
- local storage

Business rules remain outside this layer.

---

# Feature Modules

Every user-visible feature is implemented as its own Gradle module.

Examples

```
contacts

identity

chats

transport

onboarding
```

Each feature owns its presentation, domain and feature-specific data code.

---

# Navigation

Navigation is isolated from features.

Responsibilities include

- routes
- navigation graph
- navigation helpers

Navigation should coordinate features rather than contain feature logic.

---

# Shared

Shared modules contain functionality used by multiple features.

Examples

- reusable Compose components
- application-wide services
- shared ViewModels
- common utilities

Shared code should avoid feature-specific behaviour.

---

# Startup

Startup is responsible for application initialization.

Typical responsibilities

- dependency initialization
- startup verification
- application boot sequence

Separating startup logic keeps the Android application module lightweight.

---

# Relay

The relay is an independent server project.

Responsibilities

- WebSocket connections
- client registration
- message forwarding
- transport infrastructure

The relay is intentionally separated from the Android application.

---

# Quality

The quality module contains project-specific static analysis.

Examples

- SecureChat Detekt rules
- rule providers
- rule tests

General application code should never be placed here.

---

# Build Logic

The build infrastructure is implemented as an included Gradle build.

```
build-logic/
```

Responsibilities

- convention plugins
- architecture validation
- documentation generation
- quality automation
- Gradle utilities

This infrastructure configures every project module automatically.

---

# Clean Architecture

Within each feature the project follows Clean Architecture.

```
Presentation

↓

Domain

↓

Data
```

Presentation depends on Domain.

Data depends on Domain.

Presentation never depends directly on Data implementations.

---

# Dependency Direction

Dependencies should always point toward more stable layers.

Good

```
Feature

↓

Core
```

Good

```
Feature

↓

Data

↓

Core
```

Bad

```
Core

↓

Feature
```

Bad

```
Data

↓

Presentation
```

The architecture validators and custom Detekt rules enforce these boundaries.

---

# Kotlin Multiplatform

Business logic is implemented inside

```
commonMain
```

Platform-specific implementations belong inside

```
androidMain
```

Future platforms can therefore reuse the same domain and business logic.

---

# Dependency Injection

Dependency Injection is handled through Koin.

Responsibilities are split into

- feature modules
- core modules
- shared modules
- Android bootstrap

Each module contributes only the definitions it owns.

---

# Build Automation

The build infrastructure automatically provides

- convention plugins
- Version Catalog
- formatting
- static analysis
- architecture validation
- documentation generation

Developers should rarely need custom Gradle configuration.

---

# Generated Documentation

The architecture plugin automatically generates

- architecture overview
- module documentation
- dependency matrix
- project statistics
- Mermaid diagrams

These files are committed and should never be edited manually.

Instead execute

```bash
./gradlew architectureReport
```

to regenerate them.

---

# Architectural Principles

SecureChat follows several core principles.

## Explicit Dependencies

Dependencies should always be visible.

Modules declare the libraries they require.

Convention plugins configure infrastructure rather than hiding runtime dependencies.

---

## Single Responsibility

Every module should have one primary responsibility.

Examples

```
core:crypto

feature:contacts

data:database
```

Module names should describe exactly what they provide.

---

## Composition

Large reusable modules are preferred over duplicated implementations.

Shared functionality belongs inside Core or Shared modules.

---

## Platform Independence

Whenever possible business logic belongs inside

```
commonMain
```

Only platform-specific code should use Android APIs.

---

## Automation

The build infrastructure automates

- formatting
- static analysis
- documentation
- architecture validation

Developers should focus on implementing SecureChat rather than maintaining the build.

---

# Summary

SecureChat is organized around modularity, explicit dependencies and Clean Architecture.

Each layer has a clearly defined responsibility.

The architecture plugin continuously validates these boundaries and generates documentation directly from the Gradle project, ensuring that the documented architecture always reflects the actual codebase.
