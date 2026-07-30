# SecureChat Architecture

Generated automatically by `./gradlew architectureReport`.

## Overview

| Metric | Count |
|---|---:|
| Modules | 22 |
| Module groups | 9 |
| Project dependencies | 67 |
| Kotlin files | 602 |
| Test Kotlin files | 41 |
| Resource files | 54 |

## Module groups

### androidApp

- [**androidApp** (`:androidApp`)](modules/androidApp.md)

### core

- [**core** (`:core`)](modules/core.md)
- [**crypto** (`:core:crypto`)](modules/core-crypto.md)
- [**protocol** (`:core:protocol`)](modules/core-protocol.md)
- [**ui** (`:core:ui`)](modules/core-ui.md)

### data

- [**data** (`:data`)](modules/data.md)
- [**database** (`:data:database`)](modules/data-database.md)

### feature

- [**feature** (`:feature`)](modules/feature.md)
- [**chats** (`:feature:chats`)](modules/feature-chats.md)
- [**contactimport** (`:feature:contactimport`)](modules/feature-contactimport.md)
- [**contacts** (`:feature:contacts`)](modules/feature-contacts.md)
- [**identity** (`:feature:identity`)](modules/feature-identity.md)
- [**messaging** (`:feature:messaging`)](modules/feature-messaging.md)
- [**onboarding** (`:feature:onboarding`)](modules/feature-onboarding.md)
- [**settings** (`:feature:settings`)](modules/feature-settings.md)
- [**transport** (`:feature:transport`)](modules/feature-transport.md)

### navigation

- [**navigation** (`:navigation`)](modules/navigation.md)

### quality

- [**quality** (`:quality`)](modules/quality.md)
- [**detekt-rules** (`:quality:detekt-rules`)](modules/quality-detekt-rules.md)

### relay

- [**relay** (`:relay`)](modules/relay.md)

### shared

- [**shared** (`:shared`)](modules/shared.md)

### startup

- [**startup** (`:startup`)](modules/startup.md)

## Module graph

```mermaid
graph TD

    subgraph group_androidApp["androidApp"]
        module_androidApp[":androidApp"]
    end

    subgraph group_core["core"]
        module_core[":core"]
        module_core_crypto[":core:crypto"]
        module_core_protocol[":core:protocol"]
        module_core_ui[":core:ui"]
    end

    subgraph group_data["data"]
        module_data[":data"]
        module_data_database[":data:database"]
    end

    subgraph group_feature["feature"]
        module_feature[":feature"]
        module_feature_chats[":feature:chats"]
        module_feature_contactimport[":feature:contactimport"]
        module_feature_contacts[":feature:contacts"]
        module_feature_identity[":feature:identity"]
        module_feature_messaging[":feature:messaging"]
        module_feature_onboarding[":feature:onboarding"]
        module_feature_settings[":feature:settings"]
        module_feature_transport[":feature:transport"]
    end

    subgraph group_navigation["navigation"]
        module_navigation[":navigation"]
    end

    subgraph group_quality["quality"]
        module_quality[":quality"]
        module_quality_detekt_rules[":quality:detekt-rules"]
    end

    subgraph group_relay["relay"]
        module_relay[":relay"]
    end

    subgraph group_shared["shared"]
        module_shared[":shared"]
    end

    subgraph group_startup["startup"]
        module_startup[":startup"]
    end

    module_androidApp --> module_core
    module_androidApp --> module_core_crypto
    module_androidApp --> module_core_protocol
    module_androidApp --> module_data_database
    module_androidApp --> module_feature_chats
    module_androidApp --> module_feature_contactimport
    module_androidApp --> module_feature_contacts
    module_androidApp --> module_feature_identity
    module_androidApp --> module_feature_messaging
    module_androidApp --> module_feature_onboarding
    module_androidApp --> module_feature_settings
    module_androidApp --> module_feature_transport
    module_androidApp --> module_shared
    module_androidApp --> module_startup
    module_core_protocol --> module_core
    module_data_database --> module_core
    module_data_database --> module_core_protocol
    module_feature_chats --> module_core
    module_feature_chats --> module_core_crypto
    module_feature_chats --> module_core_protocol
    module_feature_chats --> module_core_ui
    module_feature_chats --> module_data_database
    module_feature_chats --> module_feature_contactimport
    module_feature_chats --> module_feature_contacts
    module_feature_chats --> module_feature_identity
    module_feature_contactimport --> module_core
    module_feature_contactimport --> module_core_ui
    module_feature_contactimport --> module_feature_contacts
    module_feature_contactimport --> module_feature_identity
    module_feature_contacts --> module_core
    module_feature_contacts --> module_core_crypto
    module_feature_contacts --> module_core_protocol
    module_feature_contacts --> module_core_ui
    module_feature_contacts --> module_data_database
    module_feature_identity --> module_core
    module_feature_identity --> module_core_crypto
    module_feature_identity --> module_core_protocol
    module_feature_identity --> module_core_ui
    module_feature_messaging --> module_core
    module_feature_messaging --> module_core_crypto
    module_feature_messaging --> module_core_protocol
    module_feature_messaging --> module_data_database
    module_feature_messaging --> module_feature_chats
    module_feature_messaging --> module_feature_contacts
    module_feature_messaging --> module_feature_transport
    module_feature_onboarding --> module_core_ui
    module_feature_onboarding --> module_feature_identity
    module_feature_settings --> module_core
    module_feature_settings --> module_core_ui
    module_feature_transport --> module_core
    module_feature_transport --> module_core_protocol
    module_navigation --> module_core
    module_navigation --> module_core_ui
    module_navigation --> module_feature_chats
    module_navigation --> module_feature_contactimport
    module_navigation --> module_feature_contacts
    module_navigation --> module_feature_identity
    module_navigation --> module_feature_onboarding
    module_navigation --> module_feature_settings
    module_navigation --> module_startup
    module_shared --> module_core
    module_shared --> module_core_ui
    module_shared --> module_feature_settings
    module_shared --> module_navigation
    module_startup --> module_core_ui
    module_startup --> module_feature_identity
    module_startup --> module_feature_onboarding
```
