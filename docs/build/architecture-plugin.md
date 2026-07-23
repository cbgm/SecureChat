# Architecture Plugin

## Overview

The SecureChat Architecture Plugin automatically discovers the project structure, validates architectural constraints and generates living documentation directly from the Gradle project.

Unlike manually maintained architecture diagrams, the generated documentation always reflects the current repository.

The plugin is designed to become the single source of truth for the project's module graph.

---

# Goals

The Architecture Plugin has several objectives.

- Discover modules automatically
- Validate project dependencies
- Detect architectural violations
- Generate documentation
- Produce machine-readable output
- Eliminate manual architecture diagrams

---

# Plugin Responsibilities

The plugin is responsible for

- module discovery
- dependency discovery
- dependency validation
- circular dependency detection
- report generation
- documentation verification

The plugin is **not** responsible for

- application compilation
- static analysis
- formatting
- testing

These responsibilities belong to other parts of the build system.

---

# Discovery

Module discovery is performed automatically from the Gradle project.

```
Gradle

↓

Projects

↓

Architecture Model

↓

Reports
```

No module list is maintained manually.

---

# Project Model

The discovered project model contains

- modules
- module paths
- project dependencies
- source sets
- module statistics
- generated metadata

Every report is generated from this single model.

---

# Validation

The plugin validates

- self dependencies
- unknown dependencies
- duplicate modules
- circular project dependencies

Validation failures stop the build immediately.

---

# Generated Reports

Executing

```bash
./gradlew architectureReport
```

produces

```
docs/generated/

architecture.md

architecture.mmd

modules/

dependency-matrix.md

statistics.md

modules.json

dependencies.json

statistics.json
```

All reports are generated from the same project model.

---

# Mermaid Diagram

The plugin generates

```
architecture.mmd
```

This file can be rendered directly by MkDocs or Mermaid-compatible tools.

Because it is generated automatically it always reflects the current dependency graph.

---

# Module Pages

Every discovered Gradle module receives its own documentation page.

Typical information includes

- module path
- module type
- project dependencies
- dependents
- source sets
- file statistics

These pages should never be edited manually.

---

# Statistics

The statistics report summarizes the repository.

Examples include

- number of modules
- dependency counts
- source set counts
- Kotlin file counts
- resource counts
- test counts

The statistics are regenerated whenever the project structure changes.

---

# Dependency Matrix

The dependency matrix provides a project-wide overview of module relationships.

Rows represent source modules.

Columns represent target modules.

This makes architectural coupling easy to identify.

---

# JSON Output

Machine-readable JSON files are generated together with the Markdown reports.

These files allow future tooling to

- visualize dependencies
- generate dashboards
- integrate with CI
- perform custom analysis

without traversing the Gradle model again.

---

# Documentation Verification

The plugin also provides

```bash
./gradlew verifyArchitectureReport
```

This task regenerates the reports in memory and compares them with the committed versions.

It **does not modify files**.

If generated documentation is outdated the task fails.

This behaviour is suitable for

- CI
- pre-push hooks
- pull request validation

---

# Updating Documentation

Whenever

- modules are added
- modules are removed
- dependencies change

execute

```bash
./gradlew architectureReport
```

Review the generated changes before committing.

---

# Configuration Cache

The Architecture Plugin has been designed to remain compatible with Gradle's Configuration Cache.

Tasks

- declare inputs
- declare outputs
- avoid accessing `Project` during execution
- rely on lazy providers

Maintaining cache compatibility significantly improves repeated build performance.

---

# Architecture Evolution

The plugin is intentionally extensible.

Future renderers may generate

- GraphViz
- PlantUML
- HTML
- CSV
- interactive dashboards

without changing the discovery process.

Only new renderers need to be added.

---

# Best Practices

- Never edit generated documentation.
- Commit generated reports together with architectural changes.
- Keep dependency graphs acyclic.
- Prefer adding renderers instead of duplicating discovery logic.
- Treat generated documentation as part of the source tree.

---

# Summary

The Architecture Plugin transforms the Gradle project into a complete architectural model and generates documentation directly from that model.

By making the repository itself the source of truth, SecureChat avoids outdated diagrams and ensures that architectural documentation evolves automatically alongside the codebase.
