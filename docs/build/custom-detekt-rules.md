# Custom Detekt Rules

SecureChat-specific rules live in:

```text
quality/detekt-rules/
```

The module is a JVM library that depends on the Detekt API and registers its rule set through Java `ServiceLoader`.

## Current rule areas

The project has introduced rules for concerns such as:

- platform-specific imports in `commonMain`;
- test imports in production source sets;
- non-null assertions;
- layer dependency boundaries;
- ViewModel direct infrastructure dependencies;
- UseCase dependencies;
- Repository dependencies;
- DAO usage;
- weak hash algorithms.

## Provider registration

The rule-set provider is listed in:

```text
src/main/resources/
└── META-INF/services/dev.detekt.api.RuleSetProvider
```

## Configuration

Rules are enabled under the `SecureChat` rule set in `config/detekt/detekt.yml`.

## Rule design principles

Custom rules should:

- enforce a real project decision;
- avoid duplicating built-in Detekt rules;
- produce actionable messages;
- minimize false positives;
- remain independent;
- include tests for valid and invalid code.

## Built-in rules first

Use built-in Detekt rules for concerns already supported, such as forbidden calls, forbidden comments, and global coroutine usage. Add custom rules only where project-specific logic is required.
