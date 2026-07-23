# Dependency Management

## Overview

SecureChat manages dependencies using a combination of

- Gradle Version Catalog
- Convention Plugins
- Explicit module dependencies

This approach keeps dependency management predictable, centralized and easy to maintain.

The project intentionally avoids hidden or implicit runtime dependencies.

---

# Design Goals

Dependency management follows several principles.

- Single source of truth
- Explicit runtime dependencies
- Centralized versions
- Minimal duplication
- Reproducible builds

Every dependency should exist for a clear reason.

---

# Dependency Types

SecureChat uses several kinds of dependencies.

```
External Libraries

↓

Project Modules

↓

Convention Plugins

↓

Generated Code
```

Each type serves a different purpose.

---

# External Libraries

External dependencies are managed through

```
gradle/libs.versions.toml
```

Examples include

- Kotlin
- Compose
- Coroutines
- Ktor
- Room
- Koin

Versions are defined once and reused throughout the project.

---

# Project Dependencies

Modules depend explicitly on one another.

Example

```kotlin
dependencies {

    implementation(projects.core.crypto)

    implementation(projects.data.database)

}
```

Project dependencies should always be visible in the module build file.

---

# Convention Plugins

Convention plugins configure infrastructure.

They should **not** silently introduce unrelated runtime dependencies.

Good examples include

- compiler configuration
- Android configuration
- Kotlin Multiplatform setup
- Compose setup

Runtime libraries should remain explicit.

---

# Dependency Direction

Dependencies generally follow this direction.

```
Android App

↓

Shared

↓

Navigation

↓

Feature

↓

Data

↓

Core
```

Lower layers should never depend on higher layers.

---

# Core Modules

Core modules should remain lightweight and reusable.

They may depend on

- Kotlin libraries
- other Core modules

They should never depend on

- Features
- Android application
- Presentation code

---

# Feature Modules

Feature modules may depend on

- Core
- Data
- Shared

Feature-to-feature dependencies should be minimized.

When required they should use stable public APIs.

---

# Data Modules

Data modules implement infrastructure.

They may depend on

- Core
- Repository interfaces

They should not depend on

- Compose
- Screens
- ViewModels

---

# Avoiding Dependency Duplication

Do not declare the same dependency repeatedly with different versions.

The Version Catalog guarantees that every module uses identical versions.

---

# Adding a Dependency

Before introducing a dependency ask

1. Is it already available?
2. Can existing code solve the problem?
3. Is it multiplatform?
4. Is it actively maintained?
5. Does it justify the maintenance cost?

Every dependency increases long-term maintenance effort.

---

# Updating Dependencies

Dependency updates should occur centrally.

Typical workflow

```
Update Version Catalog

↓

Build

↓

quality

↓

Tests

↓

Commit
```

Avoid updating multiple unrelated libraries simultaneously unless necessary.

---

# Removing Dependencies

Unused dependencies should be removed promptly.

Steps

1. Remove module dependency.
2. Remove Version Catalog entry if unused.
3. Execute

```bash
./gradlew build
```

4. Verify the project still compiles.

Keeping dependencies clean improves build performance and maintainability.

---

# Version Alignment

Related libraries should generally remain on compatible versions.

Examples

- Kotlin + Compose Compiler
- Room Runtime + Room Compiler
- Ktor modules
- Coroutines libraries

Updating only part of a library ecosystem may introduce incompatibilities.

---

# Transitive Dependencies

Do not rely on transitive dependencies.

If a module directly uses a library, declare it explicitly.

This makes dependency graphs easier to understand and reduces surprises during upgrades.

---

# Dependency Audits

Periodically review

- obsolete libraries
- duplicate functionality
- security updates
- abandoned projects

Removing unnecessary dependencies is as valuable as adding new ones.

---

# Best Practices

- Use the Version Catalog.
- Keep runtime dependencies explicit.
- Avoid hidden dependencies inside convention plugins.
- Minimize feature-to-feature coupling.
- Remove unused libraries promptly.
- Review new dependencies carefully.

---

# Summary

SecureChat treats dependency management as part of its architecture.

By centralizing versions, keeping runtime dependencies explicit and validating module relationships automatically, the project remains predictable, maintainable and easy to evolve.
