# Build Infrastructure

SecureChat uses the included Gradle build `build-logic` to centralize reusable build configuration, quality automation, architecture validation, and generated documentation.

## Build architecture

```text
Version Catalog
      ↓
Convention Plugins
      ↓
Project Modules
      ↓
Quality Automation
      ↓
Architecture Validation
      ↓
Generated Documentation
```

## Responsibilities

| Area | Responsibility |
|---|---|
| `build-logic` | Convention plugins, architecture discovery, renderers, and custom Gradle tasks |
| `gradle/libs.versions.toml` | Dependency versions, aliases, bundles, and plugin aliases |
| `quality/detekt-rules` | SecureChat-specific static-analysis rules |
| `.githooks` | Tracked pre-commit and pre-push automation |
| `docs` | Handwritten MkDocs source documentation |
| `docs/generated` | Architecture reports generated from the Gradle project |

## Build documentation

- [Build overview](overview.md)
- [Convention plugins](convention-plugins.md)
- [Quality pipeline](quality.md)
- [Architecture plugin](architecture-plugin.md)
- [Git hooks](git-hooks.md)
- [Gradle tasks](tasks.md)
- [Version catalog](version-catalog.md)
- [Dependency management](dependency-management.md)
- [Configuration cache](configuration-cache.md)
- [Documentation generation](documentation-generation.md)
- [Continuous integration](ci.md)
- [Troubleshooting](troubleshooting.md)
- [Extending the build](extending.md)
- [Custom Detekt rules](custom-detekt-rules.md)

## Common commands

Initial repository setup:

```powershell
.\gradlew setup
```

Format and verify:

```powershell
.\gradlew quality
```

Verify without modifying source files:

```powershell
.\gradlew qualityCheck
```

Update tracked architecture documentation:

```powershell
.\gradlew architectureReport
```

Verify that committed generated documentation is current:

```powershell
.\gradlew verifyArchitectureReport
```

## Design rules

- Shared Gradle configuration belongs in convention plugins.
- Runtime dependencies remain explicit in module build files.
- Formatting and verification are separate lifecycles.
- Pre-push and CI tasks must not modify tracked files.
- Modules and dependencies are discovered automatically.
- All generated reports use one immutable architecture model.
- Architecture diagrams and module references are generated, not maintained manually.
