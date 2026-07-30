# Dependency Matrix

A check mark means the row module directly depends on the column module.

| Module | `androidApp` | `core` | `core/crypto` | `core/protocol` | `core/ui` | `data` | `data/database` | `feature` | `feature/chats` | `feature/contactimport` | `feature/contacts` | `feature/identity` | `feature/onboarding` | `feature/settings` | `feature/transport` | `navigation` | `quality` | `quality/detekt-rules` | `relay` | `shared` | `startup` |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| `:androidApp` |  | ✓ | ✓ | ✓ |  |  | ✓ |  | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |  |  |  |  | ✓ | ✓ |
| `:core` |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| `:core:crypto` |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| `:core:protocol` |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| `:core:ui` |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| `:data` |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| `:data:database` |  | ✓ |  | ✓ |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| `:feature` |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| `:feature:chats` |  | ✓ | ✓ | ✓ | ✓ |  | ✓ |  |  |  | ✓ |  |  |  |  |  |  |  |  |  |  |
| `:feature:contactimport` |  | ✓ |  |  | ✓ |  |  |  |  |  | ✓ | ✓ |  |  |  |  |  |  |  |  |  |
| `:feature:contacts` |  | ✓ | ✓ | ✓ | ✓ |  | ✓ |  |  |  |  | ✓ |  |  |  |  |  |  |  |  |  |
| `:feature:identity` |  | ✓ | ✓ | ✓ | ✓ |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| `:feature:onboarding` |  |  |  |  | ✓ |  |  |  |  |  |  | ✓ |  |  |  |  |  |  |  |  |  |
| `:feature:settings` |  |  |  |  | ✓ |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| `:feature:transport` |  | ✓ | ✓ | ✓ |  |  | ✓ |  | ✓ |  | ✓ |  |  |  |  |  |  |  |  |  |  |
| `:navigation` |  | ✓ |  |  | ✓ |  |  |  | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |  |  |  |  |  |  | ✓ |
| `:quality` |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| `:quality:detekt-rules` |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| `:relay` |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| `:shared` |  | ✓ |  |  | ✓ |  |  |  |  |  |  |  |  | ✓ |  | ✓ |  |  |  |  |  |
| `:startup` |  |  |  |  | ✓ |  |  |  |  |  |  | ✓ | ✓ |  |  |  |  |  |  |  |  |
