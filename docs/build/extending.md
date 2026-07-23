# Extending the Build

## Overview

The SecureChat build infrastructure has been designed to evolve alongside the project.

New functionality should be added by extending the existing architecture rather than introducing isolated Gradle scripts or duplicated configuration.

Whenever possible, build features should be implemented once and reused everywhere.

---

# Design Principles

When extending the build, follow these principles.

- Convention over configuration
- Reuse existing infrastructure
- Keep tasks deterministic
- Support Configuration Cache
- Prefer composition over duplication

The build should scale with the project without becoming increasingly complex.

---

# Build Architecture

```
Gradle

↓

Included Build

↓

Convention Plugins

↓

Tasks

↓

Reports

↓

Verification
```

Every new component should fit naturally into this structure.

---

# Adding a Convention Plugin

Create a new plugin only when existing plugins cannot reasonably be extended.

Typical workflow

```
Plugin Class

↓

Plugin Registration

↓

Version Catalog

↓

Apply Plugin
```

Convention plugins should configure reusable behaviour rather than project-specific business logic.

---

# Adding a Task

A custom task should

- have a single responsibility
- declare inputs
- declare outputs
- support Configuration Cache
- remain deterministic

Prefer

```kotlin
tasks.register(...)
```

instead of eager task creation.

---

# Task Design

Good task lifecycle

```
Configuration

↓

Inputs

↓

Execution

↓

Outputs
```

Avoid reading undeclared files or generating undeclared outputs.

---

# Shared Utilities

Reusable Gradle functionality should live inside

```
build-logic/
```

Examples include

- extension functions
- helper classes
- shared task infrastructure
- common Gradle utilities

Avoid duplicating helper code across plugins.

---

# Documentation Generators

New documentation generators should reuse the existing architecture model.

```
Architecture Model

↓

Renderer

↓

Output Format
```

Examples of output formats

- Markdown
- Mermaid
- JSON
- HTML
- CSV

Discovery logic should never be duplicated.

---

# Quality Checks

New quality rules should integrate into

```
qualityCheck
```

rather than introducing independent verification workflows.

The developer experience should remain simple.

---

# Custom Detekt Rules

Project-specific coding rules belong inside the Quality module.

Examples include

- architectural boundaries
- dependency restrictions
- platform rules
- Compose conventions

Avoid placing project-specific checks inside application modules.

---

# Architecture Validation

When adding architectural validation

- reuse the existing project model
- keep rules independent
- produce actionable error messages

Validation should explain **why** a rule failed, not only that it failed.

---

# Version Catalog

New dependencies should always be added through

```
gradle/libs.versions.toml
```

Avoid introducing hardcoded versions inside plugins.

---

# Configuration Cache

Every new task should be compatible with Configuration Cache.

Requirements include

- immutable task state
- lazy Providers
- declared inputs
- declared outputs

Avoid execution-time Gradle model access.

---

# Testing

Build infrastructure should include automated tests.

Examples

- convention plugin tests
- task tests
- architecture validation tests
- documentation generator tests

The build is production code and deserves the same quality standards.

---

# Backwards Compatibility

When changing convention plugins

- preserve existing behaviour where practical
- document breaking changes
- avoid unnecessary plugin renaming

Existing modules should require minimal migration.

---

# Future Extensions

The build architecture is intended to support future additions such as

- dependency dashboards
- architecture trend reports
- build performance reports
- license verification
- security scanning
- API compatibility reports

These features should integrate with the existing build infrastructure rather than introducing parallel systems.

---

# Best Practices

- Extend before replacing.
- Keep plugins focused.
- Prefer composition.
- Reuse the architecture model.
- Keep build logic modular.
- Test every custom task.
- Document new build features.

---

# Summary

The SecureChat build infrastructure is intentionally modular.

By extending the existing convention plugins, architecture model and quality pipeline, new functionality can be added without increasing complexity or duplicating build logic.

Treat the build system with the same engineering discipline as the application itself to ensure it remains maintainable as the project grows.
