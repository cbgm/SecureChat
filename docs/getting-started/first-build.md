# First Build

## Overview

This guide explains what happens during the first build of SecureChat and how to verify that the project has been configured correctly.

Unlike many Android projects, SecureChat performs considerably more work than simply compiling Kotlin code.

The build also verifies project quality, architecture and generated documentation.

---

# Build Pipeline

The first build follows this high-level flow.

```
Gradle

↓

Build Logic

↓

Convention Plugins

↓

Project Configuration

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

# Step 1 — Build Logic

Before any project module is compiled, Gradle builds the included build located at

```
build-logic/
```

This project provides

- convention plugins
- architecture validation
- quality automation
- shared Gradle utilities

Every other module depends on this build.

If build logic fails, the rest of the project cannot compile.

---

# Step 2 — Configure Modules

The convention plugins configure every module automatically.

Examples include

- Android configuration
- Kotlin Multiplatform
- Compose
- Room
- Serialization
- Testing

This eliminates duplicated Gradle configuration throughout the project.

---

# Step 3 — Resolve Dependencies

Gradle downloads

- Kotlin
- AndroidX
- Compose
- Coroutines
- Ktor
- Room
- Koin

along with any other declared dependencies.

The first build may therefore take several minutes.

Subsequent builds reuse the local Gradle cache.

---

# Step 4 — Compile Sources

Gradle compiles

- build logic
- commonMain
- androidMain
- test source sets

Generated code from tools such as Room is also compiled.

---

# Step 5 — Run Verification

During a normal build SecureChat executes

- KtLint
- Detekt
- Architecture Validation
- Documentation Verification

This ensures every successful build satisfies the project's engineering standards.

---

# Build Tasks

The most commonly used tasks are shown below.

| Task | Purpose |
|------|---------|
| `build` | Compile the complete project |
| `check` | Verification lifecycle |
| `quality` | Format then verify |
| `qualityFix` | Automatic formatting |
| `qualityCheck` | Verification only |
| `validateArchitecture` | Validate module graph |
| `architectureReport` | Generate documentation |

---

# Expected Output

A successful build should end with output similar to

```
BUILD SUCCESSFUL
```

No architecture violations should be reported.

No Detekt errors should be reported.

No KtLint errors should be reported.

---

# Build Outputs

Gradle generates build artifacts inside

```
build/
```

for each module.

Generated architecture documentation is written to

```
docs/generated/
```

These files are version controlled and should be committed whenever the project structure changes.

---

# Common Build Problems

## Build Logic Compilation

If

```
:build-logic:compileKotlin
```

fails, resolve those errors first.

Because every convention plugin lives inside the included build, consumer modules cannot be configured until build logic compiles successfully.

---

## Architecture Verification

If

```
verifyArchitectureReport
```

fails, regenerate the documentation.

```
./gradlew architectureReport
```

Review the generated files before committing them.

---

## Quality Verification

If

```
qualityCheck
```

fails, read the reported task carefully.

Typical causes include

- formatting
- static analysis
- architecture validation

Address the underlying issue rather than disabling the verification.

---

# Configuration Cache

SecureChat has been designed to support Gradle's Configuration Cache wherever practical.

The first build performs full configuration.

Subsequent builds are significantly faster when the cache can be reused.

Developers should avoid introducing custom Gradle tasks that invalidate the cache unnecessarily.

---

# Build Performance

Typical build performance improves after

- dependency downloads
- build logic compilation
- Gradle daemon startup
- configuration cache creation

The first build is therefore expected to be noticeably slower than later builds.

---

# Daily Workflow

Most developers rarely execute `build` directly.

Instead the recommended workflow is

```
Develop

↓

quality

↓

Commit

↓

Push

↓

CI

↓

Merge
```

This keeps the repository consistently formatted and verified.

---

# Continuous Integration

The CI pipeline performs the same verification as local development.

A change that passes locally should also pass in CI provided the generated documentation has been committed.

The build intentionally avoids maintaining separate local and CI quality rules.

---

# Next Steps

Continue with

- Development Workflow
- Project Structure

These guides explain how day-to-day development is performed within the SecureChat project.
