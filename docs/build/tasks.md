# Gradle Tasks

## Overview

SecureChat exposes a small set of well-defined Gradle tasks for developers.

Most day-to-day work requires only a handful of commands.

The remaining tasks are intended for automation, Continuous Integration and project maintenance.

---

# Task Categories

Tasks are grouped into several categories.

```
Build

↓

Quality

↓

Architecture

↓

Documentation

↓

Testing

↓

Setup
```

Each category has a specific purpose.

---

# Build Tasks

## build

```bash
./gradlew build
```

Builds the complete project.

Responsibilities include

- compilation
- tests
- quality verification
- architecture validation

Recommended before opening a Pull Request.

---

## clean

```bash
./gradlew clean
```

Removes generated build artifacts.

Use when

- troubleshooting
- verifying clean builds
- switching branches with large build changes

---

## assemble

```bash
./gradlew assemble
```

Compiles build artifacts without executing the full verification pipeline.

Useful for quick local iterations.

---

# Quality Tasks

## quality

```bash
./gradlew quality
```

Primary developer task.

Executes

```
qualityFix

↓

qualityCheck
```

Recommended before every commit.

---

## qualityFix

```bash
./gradlew qualityFix
```

Automatically fixes issues that can safely be corrected.

Typical fixes include

- formatting
- imports
- whitespace

This task may modify source files.

---

## qualityCheck

```bash
./gradlew qualityCheck
```

Runs verification only.

No files are modified.

Executed by

- pre-push hook
- Continuous Integration

---

# Architecture Tasks

## validateArchitecture

```bash
./gradlew validateArchitecture
```

Validates the discovered project structure.

Checks include

- duplicate modules
- unknown dependencies
- circular dependencies
- self-dependencies

The task produces no documentation.

---

## architectureReport

```bash
./gradlew architectureReport
```

Generates architecture documentation.

Output

```
docs/generated/
```

Typical files include

- architecture.md
- architecture.mmd
- module pages
- dependency matrix
- statistics
- JSON reports

Generated files should be committed.

---

## verifyArchitectureReport

```bash
./gradlew verifyArchitectureReport
```

Verifies that committed generated documentation matches the current project.

Unlike

```
architectureReport
```

this task never modifies files.

It is safe for

- CI
- pre-push hooks
- verification pipelines

---

# Setup Tasks

## setup

```bash
./gradlew setup
```

Initializes the development environment.

Currently installs

- tracked Git hooks

Developers should execute this once after cloning the repository.

---

# Testing Tasks

## test

```bash
./gradlew test
```

Runs all configured unit tests.

Typical coverage includes

- domain
- repositories
- ViewModels
- build logic

---

## connectedAndroidTest

Runs Android instrumentation tests.

Typically used for

- UI verification
- Android integration testing

Availability depends on a connected emulator or device.

---

# Documentation Tasks

## architectureReport

Regenerates

```
docs/generated/
```

Execute whenever

- modules change
- dependencies change
- architecture changes

Never edit generated documentation manually.

---

# Common Workflows

## Daily Development

```bash
./gradlew quality
```

---

## Before Commit

```bash
./gradlew quality
```

---

## Before Push

```bash
./gradlew qualityCheck
```

---

## After Architecture Changes

```bash
./gradlew architectureReport
```

Commit the generated documentation.

---

## Full Verification

```bash
./gradlew clean build
```

Recommended before creating a release.

---

# Task Dependencies

Typical execution flow

```
quality

├── qualityFix

└── qualityCheck

        ├── Detekt

        ├── KtLint

        ├── validateArchitecture

        └── verifyArchitectureReport
```

This ensures formatting occurs before verification.

---

# Continuous Integration

The CI pipeline primarily executes

```bash
./gradlew clean build
```

which includes

- compilation
- testing
- quality verification
- architecture validation

No CI-specific Gradle tasks are required.

---

# Best Practices

- Use `quality` during normal development.
- Use `architectureReport` after structural changes.
- Avoid running individual formatting tasks manually.
- Prefer the aggregate tasks over individual verification tasks.
- Treat generated documentation as part of the source tree.

---

# Summary

SecureChat intentionally exposes a small, focused set of Gradle tasks.

Most developers will primarily use

- `setup`
- `build`
- `quality`
- `architectureReport`

while the remaining tasks support automation, verification and release workflows.
