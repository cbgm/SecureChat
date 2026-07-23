# Testing

## Overview

Testing is an integral part of SecureChat's development process.

The project follows a layered testing strategy in which each architectural layer is tested independently whenever possible.

The primary objective is to verify business behaviour rather than implementation details.

---

# Testing Goals

SecureChat aims to

- verify business logic
- prevent regressions
- simplify refactoring
- document expected behaviour
- keep tests deterministic

Tests should be fast, reliable and independent.

---

# Testing Pyramid

SecureChat follows the traditional testing pyramid.

```
            UI Tests

         Integration Tests

             Unit Tests
```

Most tests should be unit tests.

---

# Unit Tests

Unit tests verify a single class or business rule.

Typical candidates include

- UseCases
- ViewModels
- Mappers
- Validators
- Repository implementations
- Utility classes

Unit tests should not require Android.

---

# Integration Tests

Integration tests verify interaction between multiple components.

Examples include

- Room database
- Repository + DAO
- Repository + Network
- Serialization
- Protocol compatibility

Integration tests should remain focused.

---

# UI Tests

UI tests verify presentation behaviour.

Typical scenarios

- screen rendering
- navigation
- user interaction
- scrolling
- dialogs

Business rules should not be duplicated inside UI tests.

---

# Module Testing

Every module should own its own tests.

Example

```
feature:contacts

commonTest/

androidUnitTest/
```

Tests should live beside the code they verify.

---

# Clean Architecture

Each layer should be tested independently.

Presentation

- ViewModels
- UI State

Domain

- UseCases
- Validation
- Business Rules

Data

- Repository
- Room
- Transport

This separation keeps failures easy to understand.

---

# ViewModel Tests

ViewModel tests verify

- UI state
- user actions
- Flow updates
- error handling

ViewModels should be tested using fake repositories rather than real infrastructure.

---

# UseCase Tests

UseCases represent business behaviour.

Typical tests include

- successful execution
- validation failures
- error propagation
- edge cases

Every significant business rule should have at least one unit test.

---

# Repository Tests

Repository tests verify

- mapping
- persistence
- synchronization
- caching

Repositories should be tested independently from the UI.

---

# Cryptography Tests

Cryptographic functionality should verify

- key generation
- encryption
- decryption
- signatures
- invalid input
- protocol compatibility

Security-sensitive code should receive particularly thorough testing.

---

# Transport Tests

Transport tests verify

- connection lifecycle
- retries
- queue behaviour
- malformed packets
- delivery acknowledgements

Networking behaviour should be deterministic.

---

# Architecture Tests

The build infrastructure validates

- module graph
- dependency rules
- circular dependencies
- generated documentation

These checks are part of the quality pipeline.

---

# Build Logic Tests

The included build should also be tested.

Examples include

- convention plugins
- architecture discovery
- report generation
- validation
- serialization

Build infrastructure is production code and deserves the same level of testing as application code.

---

# Test Naming

Test names should describe behaviour.

Good examples

```
createsIdentity()

rejectsInvalidPacket()

queuesMessageWhileOffline()
```

Avoid vague names such as

```
test1()

works()

check()
```

---

# Test Independence

Tests should

- be deterministic
- avoid shared state
- avoid ordering dependencies

Running tests individually or as a complete suite should produce identical results.

---

# Mocking

Prefer simple fakes whenever practical.

Use mocking only when replacing external dependencies or infrastructure.

Business behaviour should remain easy to understand from the test itself.

---

# Continuous Integration

Every Pull Request should execute the complete test suite.

A change should not be merged unless

- compilation succeeds
- tests pass
- quality checks pass
- architecture validation succeeds

---

# Regression Testing

Whenever a bug is fixed

1. create a failing test
2. implement the fix
3. verify the test passes

This prevents the same defect from reappearing later.

---

# Code Coverage

Coverage is a useful indicator but should not be treated as a goal in itself.

High-quality tests that verify meaningful behaviour are more valuable than high coverage achieved through trivial assertions.

---

# Summary

SecureChat emphasizes small, deterministic and behaviour-focused tests.

Business rules, infrastructure and presentation are tested independently, allowing the project to evolve confidently while maintaining a reliable and maintainable codebase.
