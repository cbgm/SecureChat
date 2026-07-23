# Git Hooks

## Overview

SecureChat uses repository-managed Git hooks to automate quality checks before code reaches the repository.

Unlike traditional Git hooks that are installed manually into a user's local `.git/hooks` directory, SecureChat stores its hooks inside the repository.

This guarantees that every developer uses the same hook implementation.

---

# Design Goals

The Git hook system has several objectives.

- Automatic installation
- Version-controlled hooks
- Identical developer environments
- Early quality feedback
- No manual setup after cloning

---

# Repository Layout

Git hooks are stored inside

```
.githooks/
```

Typical structure

```
.githooks/

pre-commit

pre-push
```

These files are committed to the repository.

---

# Installation

Hooks are installed automatically by

```bash
./gradlew setup
```

The setup task configures Git using

```bash
git config core.hooksPath .githooks
```

Developers normally execute this only once after cloning the repository.

---

# Verification

Verify the installation

```bash
git config --get core.hooksPath
```

Expected output

```
.githooks
```

If another value is returned

```bash
./gradlew setup
```

should be executed again.

---

# Hook Lifecycle

```
Clone Repository

↓

Run setup

↓

Git configured

↓

Hooks Active
```

No manual copying of scripts is required.

---

# Pre-Commit Hook

The pre-commit hook executes

```bash
./gradlew qualityFix
```

Responsibilities

- format Kotlin code
- organize imports
- fix whitespace
- apply automatic formatting

If files are modified

```
Format

↓

Files Changed

↓

Commit Aborted
```

The developer should review the changes and commit again.

This prevents automatically modified code from being committed without inspection.

---

# Pre-Push Hook

The pre-push hook executes

```bash
./gradlew qualityCheck
```

This performs

- formatting verification
- Detekt
- architecture validation
- generated documentation verification

Unlike the pre-commit hook, it **never modifies files**.

---

# Why Two Hooks?

The responsibilities are intentionally separated.

### Pre-Commit

```
Fix
```

### Pre-Push

```
Verify
```

This keeps behaviour predictable.

The pre-push hook should never rewrite committed files.

---

# Generated Documentation

Generated documentation is verified during pre-push.

If documentation is outdated

```
verifyArchitectureReport
```

fails.

Developers should regenerate documentation using

```bash
./gradlew architectureReport
```

commit the changes

and push again.

---

# Failure Behaviour

## Formatting Failure

```
Commit

↓

Formatting

↓

Files Modified

↓

Abort Commit
```

Review changes.

Commit again.

---

## Verification Failure

```
Push

↓

qualityCheck

↓

Failure

↓

Push Rejected
```

Resolve the reported issue before pushing.

---

# Continuous Integration

CI performs the same verification as the pre-push hook.

```
Developer

↓

qualityCheck

↓

Push

↓

CI

↓

qualityCheck
```

Keeping local verification identical to CI minimizes failed pipelines.

---

# Configuration Cache

The hook tasks have been designed to remain compatible with Gradle Configuration Cache.

They

- avoid accessing `Project` during execution
- declare inputs and outputs
- use lazy task configuration

This keeps repeated executions fast.

---

# Updating Hooks

Because hooks are stored in the repository, updates are received automatically through Git.

After pulling changes, rerun

```bash
./gradlew setup
```

if the hook configuration itself has changed.

---

# Troubleshooting

## Hooks Do Not Execute

Verify

```bash
git config --get core.hooksPath
```

If incorrect

```bash
./gradlew setup
```

---

## Hook Permission Problems

On Unix-like systems

```bash
chmod +x .githooks/pre-commit
chmod +x .githooks/pre-push
```

Windows users normally do not need to perform this step.

---

## Push Rejected

Execute

```bash
./gradlew qualityCheck
```

Resolve the reported issue.

Do **not** bypass the hook.

---

# Best Practices

- Always run `./gradlew setup` after cloning.
- Do not disable Git hooks.
- Treat hook failures as normal development feedback.
- Commit regenerated documentation together with architecture changes.
- Keep hooks lightweight and deterministic.

---

# Summary

SecureChat uses repository-managed Git hooks to automate formatting and verification before code reaches the repository.

By separating automatic fixes (pre-commit) from verification (pre-push), the workflow remains predictable while ensuring that every pushed change satisfies the project's quality standards.
