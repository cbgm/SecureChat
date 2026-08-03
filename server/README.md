# Federated SecureChat server

The `server/` directory contains the first runnable implementation of the federated node architecture.
The existing `:relay` service remains available during migration.

## Modules

| Module | Purpose | Default port |
|---|---|---:|
| `:server:protocol` | Shared serialized API models only | - |
| `:server:security` | Node signing, replay protection, and constant-time internal authentication | - |
| `:server:persistence` | Bounded idempotency and environment infrastructure | - |
| `:server:node-registry` | Signed descriptors, heartbeats, compatible node directory | 8090 |
| `:server:presence-directory` | Signed, expiring device routes with generation checks | 8091 |
| `:server:mailbox` | Capability-protected, expiring encrypted envelopes | 8092 |
| `:server:federation` | Presence lookup, authenticated forwarding, mailbox fallback | 8093 |
| `:server:gateway` | Client WebSockets and local connection delivery | 8094 |
| `:server:push` | Durable FCM tokens, wake-ups, and encrypted-envelope replay | 8095 |

No service application imports another service application's implementation. Communication crosses
the `server:protocol` contracts and HTTP interfaces.

## Run the local network

Create `server/.env` once. This file is ignored by Git:

```dotenv
FIREBASE_ADMIN_CREDENTIALS=C:/secure/chat-project-firebase-adminsdk.json
PUSH_DATABASE_PASSWORD=replace-for-non-local-deployments
MAILBOX_DATABASE_PASSWORD=replace-for-non-local-deployments
PRESENCE_REDIS_PASSWORD=replace-for-non-local-deployments
NODE_REGISTRY_DATABASE_PASSWORD=replace-for-non-local-deployments
FEDERATION_DATABASE_PASSWORD=replace-for-non-local-deployments
FEDERATION_INTERNAL_API_TOKEN=replace-with-a-different-random-token
GATEWAY_INTERNAL_API_TOKEN=replace-with-a-different-random-token
PUSH_INTERNAL_API_TOKEN=replace-with-a-different-random-token
# Used only by docker-compose.multinode.yml:
MAILBOX_B_DATABASE_PASSWORD=replace-for-non-local-deployments
FEDERATION_B_DATABASE_PASSWORD=replace-for-non-local-deployments
FEDERATION_B_INTERNAL_API_TOKEN=replace-with-a-different-random-token
GATEWAY_B_INTERNAL_API_TOKEN=replace-with-a-different-random-token
COMPOSE_PARALLEL_LIMIT=1
```

Start the network from the repository root:

```powershell
docker compose -f server/docker-compose.yml up --build
```

Configure the Android emulator to use:

```text
securechat.registry.baseUrl=http://10.0.2.2:8090
securechat.relay.httpBaseUrl=http://10.0.2.2:8095
```

The registry URL is used for signed WebSocket node discovery. The push URL remains separate. Leave
`securechat.registry.authorityNodeId` empty for local trust on first use, or pin the registry ID
returned by `/v1/nodes`. The push health response must contain `fcmEnabled=true`.

## Run the two-node federation test

The multi-node override adds a completely separate node B with its own identity, gateway,
federation service, mailbox, PostgreSQL databases, and persistent volumes. Both nodes share only
the central node registry, presence directory, and push service. Node A advertises port 8094 and
node B advertises port 8294. Clients obtain both direct endpoints from the signed registry response;
the former port-8194 Caddy failure-injection edge is no longer used.

Start both nodes from the repository root:

```powershell
docker compose `
    -f server/docker-compose.yml `
    -f server/docker-compose.multinode.yml `
    up -d --build --remove-orphans
```

Wait for both signed descriptors to be registered, then verify the topology:

```powershell
curl.exe http://localhost:8090/health
curl.exe http://localhost:8091/health
curl.exe http://localhost:8094/health
curl.exe http://localhost:8294/health
curl.exe http://localhost:8095/health
```

The registry must report `nodes=2`. Before opening the apps, both gateway health responses should
report `connections=0`.

Build the discovery-enabled app once:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

.\gradlew.bat :androidApp:assembleDebug `
    --no-configuration-cache

& $adb -s emulator-5554 install -r `
    ".\androidApp\build\outputs\apk\debug\androidApp-debug.apk"
```

Install the same APK on the second emulator. Each installation has a different relay ID and chooses
a stable starting node from the verified directory:

```powershell
& $adb -s emulator-5556 install -r `
    ".\androidApp\build\outputs\apk\debug\androidApp-debug.apk"
```

Open both apps and verify that the gateway connection counts add up to two while the shared presence
directory contains two signed routes:

```powershell
curl.exe http://localhost:8091/health
curl.exe http://localhost:8094/health
curl.exe http://localhost:8294/health
```

Node selection is derived from each installation's random relay ID, so the two gateway counts may
be `1 + 1` or `2 + 0`. With three emulators, a split is normally visible immediately. Send a message
between clients connected to different gateways. The compatibility `send_envelope` frame enters
federation whenever the recipient is not connected to the sender's gateway. Confirm the
node-to-node and destination-gateway requests:

```powershell
docker compose `
    -f server/docker-compose.yml `
    -f server/docker-compose.multinode.yml `
    logs --since=2m federation federation-b gateway gateway-b |
    Select-String -Pattern "/v1/federation/envelopes|/internal/v1/envelopes"
```

Typing events use a separate ephemeral federation path. They are signed between nodes but are not
stored in PostgreSQL, mailbox, or push. Type from a node A client to the node B client and verify:

```powershell
docker compose `
    -f server/docker-compose.yml `
    -f server/docker-compose.multinode.yml `
    logs --since=2m federation federation-b gateway gateway-b |
    Select-String -Pattern "federation/typing-events|internal/v1/typing-events"
```

For background push, close the receiving app normally without Android's Force stop action, then
send a message from the other node. The sender gateway stores the opaque envelope in the shared
push inbox before live federation. If no live route exists, FCM wakes the receiver after the normal
fallback delay. Duplicate storage attempts by the receiving gateway are accepted, while the local
federation queue is marked complete to prevent a later replay.

```powershell
docker compose `
    -f server/docker-compose.yml `
    -f server/docker-compose.multinode.yml `
    logs --since=2m push federation federation-b |
    Select-String -Pattern "internal/v1/envelopes|FCM wake-up|push/wake|/stored"
```

To test failover of node B's gateway and federation processes, keep the apps open and stop them:

```powershell
docker compose `
    -f server/docker-compose.yml `
    -f server/docker-compose.multinode.yml `
    stop gateway-b federation-b
```

Every client connected to node B temporarily blacklists the failed descriptor and selects node A
from the cached signed directory. Within the normal reconnect window, gateway A should own the
migrated connections and messages must continue in both directions. After the 90-second registry
heartbeat grace period, registry health reports `nodes=1`.

Restore node B with:

```powershell
docker compose `
    -f server/docker-compose.yml `
    -f server/docker-compose.multinode.yml `
    up -d federation-b gateway-b
```

The app refreshes the directory every minute, rejects invalid authority or node signatures, rejects
expired and incompatible descriptors, and keeps failed nodes out of selection for 30 seconds. If
the registry is briefly unavailable, the last verified directory has a five-minute grace period.
When `securechat.registry.baseUrl` is blank, the static `securechat.relay.websocketUrl` remains
available for legacy single-relay development.

The trusted registry ID is deliberately retained across app restarts. If local testing deletes the
`registry-identity` Docker volume, clear the app data once before trusting the newly generated local
authority. Production clients must never reset that trust automatically.

The gateway accepts the existing relay WebSocket frames. Current clients fetch `/v1/gateway`, create
a connection ID, and attach a signed, expiring presence route to the initial `register` frame. They
refresh that route every 30 seconds while the WebSocket remains connected. Older clients still work
locally through the compatibility registration, but they do not publish a cross-node presence route.
Current routing IDs begin with `scrouting1_` and are derived from the device signing public key, not
from its phone number. The presence service verifies that key-to-ID binding before accepting a route.
This is an addressing migration: update and open every test client once so each FCM token is moved to
its new routing ID. Cached contact mappings are replaced automatically from exchanged signing keys.
Legacy pending envelopes addressed to `scphone1_` IDs are not rewritten.

## Security behavior

- A node identity is generated once and persisted in the shared node identity volume.
- `nodeId` is the SHA-256 digest of the Ed25519 public key.
- Node descriptors and heartbeats are signed.
- Federation requests are signed over method, path, timestamp, nonce, and body hash.
- Request nonces are retained for a bounded window to reject replays.
- Gateway, federation, and push internal endpoints use separate credentials and constant-time checks.
- Presence routes are accepted only with a valid client signature and non-stale generation.
- A presence routing ID must match the SHA-256-derived ID of the signing public key in its proof.
- Mailbox IDs and capabilities are random. Only capability hashes are retained.
- Encrypted envelope IDs are deduplicated and expired entries are removed.
- Firebase Admin credentials are mounted only into the push container.

The default development Compose file publishes diagnostic ports only on `127.0.0.1`. They remain
available to the Android emulator through `10.0.2.2`, but are not reachable through the host's LAN
address.

## Production deployment

Production uses [`docker-compose.production.yml`](docker-compose.production.yml) as an override.
It adds a Caddy TLS edge, removes every direct host port from the Kotlin services, PostgreSQL, and
Redis, separates edge and backend networks, mounts credentials as per-service Compose secrets, and
runs the Kotlin containers with a read-only root filesystem, no Linux capabilities, and
`no-new-privileges`.

Requirements:

- Docker Compose 2.24.4 or newer because the override uses `!reset`;
- a public DNS `A` or `AAAA` record for the SecureChat domain;
- inbound TCP ports 80 and 443, plus UDP 443 for HTTP/3;
- the Firebase Admin JSON file already used by the push service.

Generate independent random database passwords and internal service tokens on the production host:

```powershell
.\server\scripts\New-ProductionSecrets.ps1 `
    -Domain "chat.example.com" `
    -FirebaseAdminCredentials "C:\secure\chat-project-firebase-adminsdk.json"
```

The script does not overwrite existing secrets. It creates ignored files under `server/secrets/`
and `server/.env.production`. Protect and back up those files; losing database passwords prevents a
replacement container from opening the existing database volumes.

Review the merged configuration before starting it:

```powershell
docker compose `
    --env-file server/.env.production `
    -f server/docker-compose.yml `
    -f server/docker-compose.production.yml `
    config
```

Start production:

```powershell
docker compose `
    --env-file server/.env.production `
    -f server/docker-compose.yml `
    -f server/docker-compose.production.yml `
    up -d --build
```

Caddy automatically obtains and renews the public certificate. Only Caddy publishes host ports in
the merged production configuration. Public traffic is restricted to these protocol routes:

| Public route | Service |
|---|---|
| `/relay` | Gateway WebSocket |
| `/v1/gateway` | Gateway node information used for signed client routes |
| `/push/*` | Push registration and opaque wake-up retrieval |
| `/v1/federation/*` | Signed node-to-node envelope delivery |
| `/v1/mailboxes/*` | Capability-protected mailbox operations |
| `/v1/nodes/*` | Signed node registry |

Configure clients with:

```text
securechat.registry.baseUrl=https://chat.example.com
securechat.registry.authorityNodeId=<authorityNodeId from /v1/nodes>
securechat.relay.httpBaseUrl=https://chat.example.com
```

Production builds must pin `authorityNodeId`. Trust on first use is intended only to make local
development with a newly generated registry identity convenient.

Verify the public registry and TLS certificate:

```powershell
curl.exe https://chat.example.com/v1/nodes
docker compose `
    --env-file server/.env.production `
    -f server/docker-compose.yml `
    -f server/docker-compose.production.yml `
    ps
```

Do not copy the local development tokens into production. The production override removes those
environment values and makes every Kotlin service load only the secret files granted to it.

## Push persistence

The push service uses its own PostgreSQL database when `PUSH_DATABASE_URL` is configured. Docker
Compose configures this automatically through the private `push-database` service and retains its
data in the `push-database-data` volume.

The following data survives a push-service or complete Compose restart:

- FCM device-token registrations;
- pending encrypted relay envelopes;
- short-lived opaque wake-up mappings.

Pending envelopes expire after seven days by default. Wake-up mappings expire after fifteen
minutes. Expired rows are removed during normal store access. The limits can be configured with
`PUSH_MAXIMUM_ENVELOPES`, `PUSH_ENVELOPE_RETENTION_MILLISECONDS`, and
`PUSH_WAKE_UP_LIFETIME_MILLISECONDS`.

To verify restart durability, register the Android clients and check that `devices` is non-zero:

```powershell
curl.exe http://localhost:8095/health
docker compose -f server/docker-compose.yml restart push
curl.exe http://localhost:8095/health
```

Both responses must include `persistence=postgresql` and the same device count. Do not use
`docker compose down --volumes` because that explicitly deletes the database volume.

The optional PostgreSQL integration test can be enabled against the Compose database:

```powershell
docker compose -f server/docker-compose.yml up -d push-database
$env:PUSH_TEST_DATABASE_URL = "jdbc:postgresql://localhost:5435/securechat_push"
$env:PUSH_TEST_DATABASE_USER = "securechat_push"
$env:PUSH_TEST_DATABASE_PASSWORD = "local-development-password"
.\gradlew.bat :server:push:test
```

## Mailbox persistence

The mailbox service has its own PostgreSQL database when `MAILBOX_DATABASE_URL` is configured.
Docker Compose configures it through `mailbox-database` and retains the database in the
`mailbox-database-data` volume. Capability hashes, mailbox expiry, and queued encrypted envelopes
survive mailbox-container and complete Compose restarts. Raw send and retrieval capabilities are
never stored.

The health endpoint reports both the active adapter and mailbox count:

```powershell
curl.exe http://localhost:8092/health
docker compose -f server/docker-compose.yml restart mailbox
curl.exe http://localhost:8092/health
```

Both responses must contain `persistence=postgresql`. Mailbox and envelope expiry cleanup happens
during normal access. Limits can be configured with `MAILBOX_MAXIMUM_ENVELOPE_BYTES` and
`MAILBOX_MAXIMUM_MAILBOX_BYTES`.

The optional PostgreSQL integration test can be enabled against the Compose database:

```powershell
docker compose -f server/docker-compose.yml up -d mailbox-database
$env:MAILBOX_TEST_DATABASE_URL = "jdbc:postgresql://localhost:5436/securechat_mailbox"
$env:MAILBOX_TEST_DATABASE_USER = "securechat_mailbox"
$env:MAILBOX_TEST_DATABASE_PASSWORD = "local-development-password"
.\gradlew.bat :server:mailbox:test
```

## Presence persistence

The presence directory uses Redis when `PRESENCE_REDIS_URL` is configured. Docker Compose starts a
private `presence-redis` service with append-only persistence and retains its data in the
`presence-redis-data` volume. Active signed routes therefore survive a presence-service or short
Compose restart while their original expiration time is still valid.

Registration generation checks, replacement of older generations, and TTL cleanup are executed
atomically inside Redis. The health endpoint reports the active adapter and unexpired route count:

```powershell
curl.exe http://localhost:8091/health
docker compose -f server/docker-compose.yml restart presence-directory
curl.exe http://localhost:8091/health
```

Both responses must contain `persistence=redis`. Routes expire after at most two minutes by default;
configure this with `PRESENCE_MAXIMUM_TTL_MILLISECONDS`.

The Compose gateway advertises a 90-second route lifetime and refreshes at 30 seconds. After
rebuilding and reconnecting all Android clients, active routes should match active WebSockets:

```powershell
curl.exe http://localhost:8091/health
curl.exe http://localhost:8094/health
```

For example, three connected clients should report `routes=3` and `connections=3`. A route can lag a
new WebSocket by a few seconds only when local identity keys are not yet available; the client retries
signed registration every five seconds.

The optional Redis integration test can be enabled against the Compose instance:

```powershell
docker compose -f server/docker-compose.yml up -d presence-redis
$env:PRESENCE_TEST_REDIS_URL = "redis://:local-development-password@localhost:6380"
.\gradlew.bat :server:presence-directory:test
```

## Node registry persistence

The node registry uses its own PostgreSQL database when `NODE_REGISTRY_DATABASE_URL` is configured.
Docker Compose provides `node-registry-database` and retains its data in the
`node-registry-database-data` volume. Signed node descriptors, heartbeat timestamps, and accepted
heartbeat nonces survive registry-container and complete Compose restarts. Persisting nonces keeps
replay protection effective across restarts.

The registry signing identity remains in the separate `registry-identity` volume. The health
endpoint reports the active adapter and currently healthy node count:

```powershell
curl.exe http://localhost:8090/health
docker compose -f server/docker-compose.yml restart node-registry
curl.exe http://localhost:8090/health
```

Both responses must contain `persistence=postgresql`. A node is included only while its signed
descriptor is valid and its last heartbeat remains inside the configured grace period.

The optional PostgreSQL integration test can be enabled against the Compose database:

```powershell
docker compose -f server/docker-compose.yml up -d node-registry-database
$env:NODE_REGISTRY_TEST_DATABASE_URL = "jdbc:postgresql://localhost:5437/securechat_registry"
$env:NODE_REGISTRY_TEST_DATABASE_USER = "securechat_registry"
$env:NODE_REGISTRY_TEST_DATABASE_PASSWORD = "local-development-password"
.\gradlew.bat :server:node-registry:test
```

## Federation outbound persistence

The federation service uses its own PostgreSQL database when `FEDERATION_DATABASE_URL` is
configured. Docker Compose provides `federation-database` and retains its data in the
`federation-database-data` volume. Pending encrypted envelopes, delivery attempt counts, and the
next retry time survive federation-container and complete Compose restarts.

A retry worker starts with the federation service, immediately loads due rows, and retries online
delivery followed by the recipient-selected mailbox fallback. Failed attempts use exponential
backoff. Delivered and expired envelopes are not retried. Configure the worker with
`FEDERATION_RETRY_POLL_INTERVAL_MILLISECONDS`, `FEDERATION_RETRY_BASE_DELAY_MILLISECONDS`,
`FEDERATION_RETRY_MAXIMUM_DELAY_MILLISECONDS`, and `FEDERATION_RETRY_BATCH_SIZE`.

The health endpoint reports the active adapter and number of pending outbound envelopes:

```powershell
curl.exe http://localhost:8093/health
docker compose -f server/docker-compose.yml restart federation
curl.exe http://localhost:8093/health
```

Both responses must contain `persistence=postgresql`. If the destination remains unavailable, the
same non-zero pending count remains after restart. Do not use `docker compose down --volumes`
because that explicitly deletes every service database.

The optional PostgreSQL integration test can be enabled against the Compose database:

```powershell
docker compose -f server/docker-compose.yml up -d federation-database
$env:FEDERATION_TEST_DATABASE_URL = "jdbc:postgresql://localhost:5438/securechat_federation"
$env:FEDERATION_TEST_DATABASE_USER = "securechat_federation"
$env:FEDERATION_TEST_DATABASE_PASSWORD = "local-development-password"
.\gradlew.bat :server:federation:test
```

All service-owned production persistence adapters are now wired: PostgreSQL for node registry,
mailbox, federation, and push; Redis for presence. Each service still falls back to its in-memory
adapter when its persistence URL is absent, keeping isolated tests and development outside Docker
Compose simple. No service accesses another service's database tables.

## Verification

```bash
./gradlew \
  :feature:transport:allTests \
  :server:protocol:test \
  :server:security:test \
  :server:persistence:test \
  :server:node-registry:test \
  :server:presence-directory:test \
  :server:mailbox:test \
  :server:federation:test \
  :server:gateway:test \
  :server:push:test
```
