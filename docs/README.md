# SecureChat Documentation

Welcome to the SecureChat engineering handbook.

This documentation serves as the primary reference for developers working on SecureChat. It combines manually written engineering guides with automatically generated architecture documentation produced directly from the Gradle project.

The documentation is organized around the principle that architecture, build infrastructure and security are just as important as application code.

---

# Documentation Structure

```
docs/

README.md

getting-started/
    introduction.md
    project-structure.md
    local-development.md

architecture/
    overview.md
    clean-architecture.md
    dependency-rules.md
    module-types.md

security/
    overview.md
    identity.md
    encryption.md
    transport.md
    safety-numbers.md
    threat-model.md

features/
    chats.md
    contacts.md
    identity.md
    onboarding.md
    transport.md

development/
    coding-style.md
    testing.md
    contributing.md
    release-process.md

build/
    overview.md
    convention-plugins.md
    quality.md
    architecture-plugin.md
    git-hooks.md
    tasks.md
    version-catalog.md
    dependency-management.md
    configuration-cache.md
    documentation-generation.md
    ci.md
    troubleshooting.md
    extending.md

api/
    relay.md
    websocket.md
    protocol.md

generated/
    architecture.md
    architecture.mmd
    dependency-matrix.md
    statistics.md
    modules/
```

---

# Two Types of Documentation

SecureChat documentation consists of two categories.

## Manual Documentation

Written by developers.

Examples

- Architecture
- Security
- Features
- Development
- Build
- API

These files explain design decisions and implementation concepts.

---

## Generated Documentation

Generated automatically from the Gradle project.

Examples

- Module pages
- Dependency matrix
- Architecture diagrams
- Project statistics

Never edit generated documentation manually.

Instead execute

```bash
./gradlew architectureReport
```

---

# Development Workflow

Recommended workflow

```
Implement Feature

↓

Run quality

↓

Run tests

↓

Generate architecture documentation

↓

Commit
```

Commands

```bash
./gradlew quality
```

```bash
./gradlew architectureReport
```

---

# Documentation Principles

Documentation should be

- accurate
- concise
- version controlled
- architecture focused
- automatically verifiable

Whenever the architecture changes, documentation should evolve together with the code.

---

# Engineering Philosophy

SecureChat emphasizes

- modular architecture
- Clean Architecture
- Kotlin Multiplatform
- end-to-end encryption
- explicit dependencies
- reproducible builds
- automated verification

Every document in this handbook reflects one or more of these principles.

---

# Generated Site

The complete handbook can be viewed locally using MkDocs.

Generate documentation

```bash
./gradlew architectureReport
```

Run the documentation server

```bash
./gradlew docsServe
```

or

```bash
mkdocs serve
```

if MkDocs is installed locally.

---

# Contributing

When contributing

- keep documentation current
- regenerate generated reports
- avoid editing generated files
- follow the coding standards
- keep architectural decisions documented

Documentation is considered part of the source code.

---

# Summary

The SecureChat handbook is intended to remain the single source of truth for the project's architecture, build infrastructure and engineering practices.

By combining handwritten guides with automatically generated documentation, the handbook stays accurate while minimizing manual maintenance.
