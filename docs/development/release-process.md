# Release Process

## Overview

This document describes the recommended release workflow for SecureChat.

The objective is to produce releases that are

- reproducible
- well tested
- fully documented
- easy to audit
- easy to roll back if necessary

The release process intentionally follows the same engineering principles used during normal development.

---

# Release Goals

Every release should satisfy the following requirements.

- Successful build
- Passing test suite
- Passing quality verification
- Updated generated documentation
- Version information updated
- Tagged source code

No release should bypass the normal quality pipeline.

---

# Release Workflow

The complete release process is

```
Development

↓

Feature Complete

↓

Code Freeze

↓

Quality

↓

Tests

↓

Architecture Report

↓

Release Build

↓

Tag

↓

Publish
```

Each step should complete successfully before continuing.

---

# Code Freeze

Before creating a release

- stop feature development
- stabilize the branch
- review open issues
- resolve known blockers

Only release-critical fixes should be merged after the freeze begins.

---

# Version Update

Update the application version.

Typical versioning follows

```
Major.Minor.Patch
```

Examples

```
1.0.0

1.1.0

1.1.1

2.0.0
```

Version changes should be committed separately from feature work whenever practical.

---

# Quality Verification

Execute

```bash
./gradlew quality
```

This performs

- formatting
- static analysis
- architecture validation
- generated documentation verification

A release should never proceed while quality checks fail.

---

# Test Execution

Run the complete test suite.

```bash
./gradlew test
```

Verify

- unit tests
- integration tests
- build-logic tests

Any failing test blocks the release.

---

# Architecture Documentation

Generate fresh documentation.

```bash
./gradlew architectureReport
```

Review generated files before committing them.

Commit documentation changes if the project structure has changed.

---

# Dependency Review

Review recently added dependencies.

Verify

- actively maintained
- compatible licenses
- required versions
- security updates

Remove unused dependencies before releasing.

---

# Build

Create a clean release build.

```bash
./gradlew clean build
```

A release build should always start from a clean state.

---

# Release Checklist

Before publishing verify

- Build successful
- Tests passing
- Quality passing
- Documentation updated
- Version updated
- Changelog updated
- Generated documentation committed

---

# Tagging

After verification create a Git tag.

Example

```bash
git tag v1.0.0
```

Push the tag.

```bash
git push origin v1.0.0
```

Tags provide an immutable reference for every released version.

---

# Changelog

Every release should include a changelog.

Typical sections

- Added
- Changed
- Fixed
- Removed
- Security

The changelog should describe user-visible changes rather than implementation details.

---

# Continuous Integration

CI should execute the same verification as local development.

Typical pipeline

```
Checkout

↓

Build

↓

Tests

↓

Quality

↓

Architecture Verification

↓

Package
```

A release should never rely on checks that are not executed locally.

---

# Rollback

If a release issue is discovered

1. identify the affected version
2. fix the issue on a dedicated branch
3. create a patch release
4. publish a new version

Avoid modifying existing release tags.

---

# Hotfix Releases

Critical production issues may require a hotfix.

Recommended flow

```
Release Tag

↓

Hotfix Branch

↓

Fix

↓

Quality

↓

Tests

↓

Patch Release
```

Hotfixes should remain as small as possible.

---

# Documentation

Documentation should always match the released source code.

If architecture changes occur during a release cycle

```bash
./gradlew architectureReport
```

should be executed before publishing.

Generated documentation is considered part of the release artifact.

---

# Security Review

Before major releases review

- cryptographic changes
- protocol modifications
- identity management
- dependency updates
- transport implementation

Security-sensitive changes deserve additional attention.

---

# Post Release

After publishing

- verify CI artifacts
- verify release tag
- update project roadmap
- reopen feature development
- create the next development version

---

# Summary

The SecureChat release process emphasizes repeatability and verification.

Every release should pass the complete quality pipeline, include current generated documentation and be reproducible from the tagged source code.

Following this workflow helps ensure that releases remain stable, traceable and easy to maintain.
