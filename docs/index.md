# SecureChat Documentation

SecureChat is a Kotlin Multiplatform end-to-end encrypted messaging project built around modular architecture, explicit dependencies, automated quality checks, and generated architecture documentation.

## Start here

For a newly cloned repository:

```powershell
.\gradlew setup
```

For normal local formatting and verification:

```powershell
.\gradlew quality
```

For read-only verification used by Git hooks and CI:

```powershell
.\gradlew qualityCheck
```

## Handbook

- [Getting started](getting-started/introduction.md)
- [Architecture](architecture/overview.md)
- [Security](security/overview.md)
- [Features](features/chats.md)
- [Development](development/coding-style.md)
- [Build infrastructure](build/index.md)
- [Protocol and relay APIs](api/protocol.md)

## Generated architecture reference

The architecture plugin generates the following from the real Gradle project:

- [Generated overview](generated/index.md)
- [Module architecture](generated/architecture.md)
- [Module catalog](generated/modules.md)
- [Dependency matrix](generated/dependency-matrix.md)
- [Project statistics](generated/statistics.md)

It also generates one page per Gradle module and machine-readable JSON exports.

When modules, dependencies, source sets, or module contents change:

```powershell
.\gradlew architectureReport
```

Commit the updated files under `docs/generated/`.

Do not edit generated files manually.

## Documentation workflow

Local Python, MkDocs, and Docker installations are not required. GitHub Actions builds and publishes the MkDocs site.

## Core principles

> Convention plugins configure infrastructure.
> Modules declare their actual dependencies.
> Architecture and quality rules are automated.
> Generated documentation is derived from the project itself.

## Detailed implementation guides
> Message sending and transport flow
