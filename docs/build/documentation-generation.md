# Documentation Generation

## Overview

SecureChat automatically generates a large portion of its technical documentation directly from the Gradle project.

Rather than maintaining architecture diagrams and module lists manually, the build derives this information from the actual project structure.

This ensures that the documentation always reflects the current state of the repository.

---

# Design Goals

Documentation generation has several objectives.

- Eliminate outdated documentation
- Generate architecture automatically
- Produce machine-readable metadata
- Support MkDocs
- Support Continuous Integration
- Require minimal developer effort

---

# Source of Truth

The Gradle project itself is the source of truth.

```
Gradle Project

↓

Architecture Discovery

↓

Project Model

↓

Documentation Generators

↓

Markdown

JSON

Mermaid
```

Nothing is generated from manually maintained configuration files.

---

# Generated Output

Executing

```bash
./gradlew architectureReport
```

generates

```
docs/generated/
```

Typical output includes

```
architecture.md

architecture.mmd

dependency-matrix.md

statistics.md

modules/

modules.json

dependencies.json

statistics.json
```

---

# Markdown Reports

Markdown reports are intended for humans.

Examples include

- architecture overview
- module pages
- dependency matrix
- project statistics

These reports are rendered by MkDocs.

---

# Mermaid Diagrams

The Architecture Plugin also generates

```
architecture.mmd
```

This diagram visualizes

- modules
- dependencies
- architecture layers

Because it is generated automatically it always matches the repository.

---

# JSON Reports

JSON reports are intended for tooling.

Examples

```
modules.json

dependencies.json

statistics.json
```

Possible future consumers include

- dashboards
- CI
- IDE plugins
- architectural analysis tools

---

# Module Pages

Every Gradle module receives its own generated page.

Typical information includes

- module path
- module type
- dependencies
- dependents
- source sets
- statistics

Developers should never edit these pages manually.

---

# Dependency Matrix

The dependency matrix provides a project-wide overview.

```
Module A

↓

Module B
```

The generated matrix makes architectural coupling easy to identify.

---

# Statistics

Project statistics summarize

- module count
- Kotlin files
- source sets
- dependencies
- resources
- tests

Statistics are regenerated automatically whenever the architecture changes.

---

# Regeneration

Regenerate documentation whenever

- modules are added
- modules are removed
- project dependencies change
- architecture changes

Execute

```bash
./gradlew architectureReport
```

---

# Verification

Generated documentation is verified using

```bash
./gradlew verifyArchitectureReport
```

This task

- regenerates reports in memory
- compares them with committed files
- fails if differences exist

It never modifies files.

---

# Continuous Integration

CI executes

```
verifyArchitectureReport
```

to ensure committed documentation matches the current project.

This prevents outdated generated documentation from entering the repository.

---

# Manual Documentation

Only the engineering handbook should be edited manually.

Examples

```
docs/

architecture/

security/

development/

build/

features/

api/
```

Generated documentation should remain read-only.

---

# MkDocs Integration

MkDocs renders both

- handwritten documentation
- generated documentation

The navigation combines both into a single documentation site.

Example

```
Architecture

↓

Generated

↓

Modules

↓

Statistics
```

Users do not need to distinguish between manual and generated pages.

---

# Build Integration

Documentation generation is integrated into the build infrastructure.

```
Architecture Discovery

↓

Validation

↓

Generation

↓

Verification
```

The same architecture model is reused for every generated artifact.

---

# Extending Documentation

New generators should consume the existing architecture model.

Avoid implementing additional project discovery.

Examples of future generators

- HTML
- PlantUML
- GraphViz
- CSV
- Interactive dashboards

Only rendering should change.

---

# Best Practices

- Never edit generated files manually.
- Commit regenerated documentation with architecture changes.
- Keep generated reports deterministic.
- Reuse the shared architecture model.
- Verify documentation in CI.

---

# Summary

SecureChat treats generated documentation as a first-class build artifact.

By deriving architecture documentation directly from the Gradle project, the repository remains self-documenting and avoids the common problem of diagrams and module documentation becoming outdated over time.
