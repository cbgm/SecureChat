# Version Catalog

## Overview

SecureChat uses the Gradle Version Catalog as the single source of truth for dependency and plugin versions.

Instead of declaring dependency versions throughout the project, every version is defined centrally.

This approach provides

- consistent dependency versions
- simpler upgrades
- reduced duplication
- easier dependency auditing

---

# Location

The Version Catalog is located at

```
gradle/libs.versions.toml
```

Every dependency and plugin should be declared here.

Individual modules should never hardcode versions.

---

# Catalog Structure

The catalog is divided into several sections.

```
[versions]

[libraries]

[bundles]

[plugins]
```

Each section has a specific purpose.

---

# Versions

The

```
[versions]
```

section stores reusable version numbers.

Example

```toml
[versions]

kotlin = "2.4.0"

coroutines = "1.11.0"

compose = "1.11.1"
```

Libraries should reference these values instead of repeating version numbers.

---

# Libraries

The

```
[libraries]
```

section defines reusable library aliases.

Example

```toml
kotlinx-coroutines-core = {
    module = "org.jetbrains.kotlinx:kotlinx-coroutines-core",
    version.ref = "coroutines"
}
```

Modules consume aliases instead of raw Maven coordinates.

---

# Plugins

Gradle plugins are also defined centrally.

Example

```toml
[plugins]

kotlinMultiplatform = {
    id = "org.jetbrains.kotlin.multiplatform",
    version.ref = "kotlin"
}
```

Application modules simply reference the alias.

---

# Bundles

Bundles group related libraries.

Example

```
Compose

↓

Runtime

Foundation

Material3
```

A bundle simplifies dependency declarations while keeping versions centralized.

---

# Using Libraries

Instead of

```kotlin
implementation(
    "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0"
)
```

use

```kotlin
implementation(
    libs.kotlinx.coroutines.core
)
```

The version is resolved automatically.

---

# Using Plugins

Instead of

```kotlin
id("org.jetbrains.kotlin.multiplatform")
```

prefer

```kotlin
alias(libs.plugins.kotlinMultiplatform)
```

This keeps plugin versions centralized.

---

# Updating Dependencies

Updating a library normally requires changing only one value.

Example

```
Coroutines

↓

versions.coroutines

↓

Every Module Updated
```

No module build scripts need modification.

---

# Dependency Consistency

Centralized versions ensure

- identical dependency versions
- reproducible builds
- simpler upgrades
- reduced dependency conflicts

The Version Catalog prevents accidental version drift across modules.

---

# Adding Libraries

When introducing a new dependency

1. Add its version (if necessary) to

```
[versions]
```

2. Add the library alias to

```
[libraries]
```

3. Reference the alias from module build files.

Avoid inline Maven coordinates.

---

# Naming Conventions

Aliases should be

- descriptive
- consistent
- grouped by ecosystem

Examples

```
kotlinx.coroutines.core

androidx.lifecycle.viewmodel

ktor.client.core

koin.core
```

Avoid abbreviations unless they are universally understood.

---

# Removing Dependencies

Unused libraries should be removed from

- module build files
- bundles
- Version Catalog

Keeping the catalog clean makes dependency audits significantly easier.

---

# Best Practices

- Never hardcode versions.
- Always use aliases.
- Keep related libraries grouped.
- Reuse version references whenever possible.
- Remove obsolete entries promptly.

---

# Summary

The Gradle Version Catalog is the central dependency registry for SecureChat.

By defining every dependency and plugin in a single location, the project achieves consistent builds, simpler maintenance and significantly easier dependency management as the codebase grows.
