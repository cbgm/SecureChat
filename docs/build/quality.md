# Quality Pipeline

## Overview

SecureChat uses an automated quality pipeline to ensure every change satisfies the project's engineering standards before it reaches the repository.

The quality pipeline combines

- automatic formatting
- static analysis
- architecture validation
- documentation verification

into a single reproducible workflow.

Developers should run the quality pipeline frequently during development.

---

# Philosophy

The quality pipeline follows several principles.

- Automate everything possible
- Fail early
- Keep local and CI identical
- Enforce architecture automatically
- Keep formatting deterministic

Developers should spend time implementing features rather than manually checking formatting or architectural rules.

---

# Pipeline Overview

```
Source Code

↓

KtLint

↓

Detekt

↓

Architecture Validation

↓

Generated Documentation Verification

↓

Success
```

Every stage must succeed.

---

# Primary Tasks

The build exposes three primary quality tasks.

## quality

```bash
./gradlew quality
```

Runs the complete developer workflow.

Typical execution

```
qualityFix

↓

qualityCheck
```

This is the recommended task before committing.

---

## qualityFix

```bash
./gradlew qualityFix
```

Automatically fixes issues that can be corrected safely.

Examples include

- Kotlin formatting
- import ordering
- whitespace
- indentation

Not every issue can be fixed automatically.

---

## qualityCheck

```bash
./gradlew qualityCheck
```

Performs verification only.

It never modifies source files.

Typical checks include

- formatting verification
- Detekt
- architecture validation
- documentation verification

This task is executed by the pre-push Git hook.

---

# Formatting

Formatting is performed using KtLint.

Developers should never manually format the entire project.

Instead execute

```bash
./gradlew qualityFix
```

Formatting should always remain deterministic.

---

# Static Analysis

SecureChat uses Detekt for static analysis.

The project combines

- standard Detekt rules
- SecureChat-specific rules

Custom rules enforce architectural boundaries that cannot be expressed through formatting alone.

Examples include

- ViewModel dependency restrictions
- Repository boundaries
- DAO usage
- commonMain platform restrictions

---

# Architecture Validation

Architecture validation verifies the Gradle project structure.

Checks include

- dependency graph validation
- self-dependencies
- circular project dependencies
- discovered module graph consistency

Architecture violations fail the build immediately.

---

# Documentation Verification

Generated documentation is treated as part of the source code.

The verification task ensures that committed documentation matches the current project structure.

```
Project

↓

Generate

↓

Compare

↓

Match

↓

Success
```

If documentation is outdated

```
./gradlew architectureReport
```

should be executed before committing.

---

# Generated Documentation

Generated files include

```
docs/generated/

architecture.md

architecture.mmd

modules/

dependency-matrix.md

statistics.md

*.json
```

These files should never be edited manually.

---

# Git Hooks

The quality pipeline integrates with Git.

## Pre-Commit

Runs

```
qualityFix
```

If formatting changes are required

- files are modified
- commit stops
- developer reviews changes
- commit is repeated

---

## Pre-Push

Runs

```
qualityCheck
```

A push is rejected if

- formatting verification fails
- Detekt reports issues
- architecture validation fails
- generated documentation is outdated

---

# Continuous Integration

CI executes the same verification tasks.

```
Checkout

↓

Build

↓

qualityCheck

↓

Tests

↓

Success
```

There are no separate CI-only quality rules.

Passing locally should generally mean passing in CI.

---

# Failure Strategy

Always fix the **first** reported error.

Example

```
Compilation Error

↓

Fix

↓

Detekt

↓

Fix

↓

Architecture

↓

Fix

↓

Documentation

↓

Success
```

Later failures are often consequences of earlier ones.

---

# Common Commands

Run everything

```bash
./gradlew quality
```

Format automatically

```bash
./gradlew qualityFix
```

Verification only

```bash
./gradlew qualityCheck
```

Regenerate documentation

```bash
./gradlew architectureReport
```

---

# Best Practices

- Run `quality` before every commit.
- Commit generated documentation together with architecture changes.
- Never suppress Detekt rules without justification.
- Keep architecture violations at zero.
- Let automation perform formatting.

---

# Benefits

The quality pipeline provides

- consistent formatting
- reproducible builds
- enforced architecture
- deterministic documentation
- reduced review effort

Developers can focus on application logic rather than repetitive maintenance tasks.

---

# Summary

The SecureChat quality pipeline is the central verification mechanism for the project.

By combining formatting, static analysis, architecture validation and documentation verification into a single automated workflow, it ensures that every change entering the repository satisfies the same engineering standards.
