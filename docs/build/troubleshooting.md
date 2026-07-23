# Troubleshooting

## Overview

This document describes common build and development problems encountered while working on SecureChat together with their typical solutions.

The goal is to resolve issues quickly while preserving a clean and reproducible build environment.

---

# General Strategy

When a build fails, follow this order.

```
Read First Error

↓

Fix

↓

Rebuild

↓

Repeat
```

Avoid attempting to fix every reported error simultaneously.

Later failures are often consequences of the first one.

---

# Clean Build

If the build behaves unexpectedly

```bash
./gradlew clean build
```

This removes generated artifacts and performs a complete rebuild.

---

# Gradle Cache

If dependency resolution becomes inconsistent

```bash
./gradlew --refresh-dependencies
```

This forces Gradle to download dependencies again.

---

# Configuration Cache

If Configuration Cache reports an error

```bash
./gradlew build --configuration-cache
```

Read the generated report.

Typical causes include

- execution-time Project access
- undeclared task inputs
- undeclared outputs
- mutable task state

---

# Generated Documentation

## Problem

```
verifyArchitectureReport failed
```

### Solution

Regenerate documentation.

```bash
./gradlew architectureReport
```

Review the changes.

Commit the generated files.

---

# Architecture Validation

## Problem

```
validateArchitecture failed
```

Typical causes

- circular dependencies
- invalid module dependency
- self dependency

Review recent module dependency changes before modifying the validator.

---

# Detekt

## Problem

Detekt reports violations.

### Solution

Read the reported rule.

Fix the underlying issue instead of suppressing it whenever possible.

Suppression should remain exceptional.

---

# Formatting

## Problem

Formatting verification fails.

### Solution

Execute

```bash
./gradlew qualityFix
```

Review modified files.

Commit the formatting changes.

---

# Build Logic

## Problem

Convention plugin compilation fails.

### Solution

Remember that

```
build-logic/
```

is an Included Build.

Errors there must be resolved before the application modules can configure successfully.

---

# Version Catalog

## Problem

Dependency alias cannot be resolved.

### Solution

Verify

```
gradle/libs.versions.toml
```

Check

- alias spelling
- version reference
- library declaration

---

# Missing Plugin

## Problem

Plugin alias not found.

### Solution

Verify the plugin exists inside

```
[plugins]
```

of the Version Catalog.

---

# Circular Dependencies

## Problem

Architecture validation reports a dependency cycle.

### Solution

Review module responsibilities.

Typical solution

- move shared code into Core
- extract a Shared module
- invert dependencies through interfaces

Avoid introducing exceptions.

---

# Configuration Cache Disabled

## Problem

A custom task is incompatible with Configuration Cache.

### Solution

Verify that the task

- declares inputs
- declares outputs
- avoids execution-time Project access
- uses Providers

---

# Git Hooks

## Problem

Hooks are not executed.

### Solution

Verify

```bash
git config --get core.hooksPath
```

Expected

```
.githooks
```

If necessary

```bash
./gradlew setup
```

---

# Documentation Site

## Problem

MkDocs does not display updated documentation.

### Solution

Regenerate architecture documentation

```bash
./gradlew architectureReport
```

then restart

```bash
mkdocs serve
```

If using Docker

```bash
./gradlew docsServe
```

---

# Android Build

## Problem

Android compilation fails unexpectedly.

### Solution

Verify

- Android SDK installed
- correct compileSdk
- Gradle sync completed
- Version Catalog updated

A clean rebuild often resolves stale generated files.

---

# Dependency Conflicts

## Problem

Unexpected dependency resolution.

### Solution

Avoid hardcoded versions.

Verify every dependency uses

```
libs.*
```

from the Version Catalog.

---

# Build Performance

If builds become noticeably slower

verify

- Configuration Cache enabled
- Gradle cache available
- unnecessary dependencies removed
- custom tasks remain lazy

Performance regressions often originate from recently added build logic.

---

# Reporting Issues

When reporting build problems include

- first error message
- executed Gradle task
- Gradle version
- JDK version
- operating system

Avoid reporting only the final failure in a long stack trace.

---

# Best Practices

- Fix the first error first.
- Use aggregate Gradle tasks.
- Keep generated documentation current.
- Prefer deterministic builds.
- Avoid suppressing quality violations.

---

# Summary

Most SecureChat build issues originate from one of four areas:

- formatting
- architecture validation
- generated documentation
- dependency configuration

Following the troubleshooting steps in this document resolves the majority of build failures while preserving the consistency of the project's automated build infrastructure.
