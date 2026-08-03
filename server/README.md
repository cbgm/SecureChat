# Federated SecureChat server

The `server/` directory contains the first runnable implementation of the federated node architecture.
The existing `:relay` service remains available during migration.

## Modules

| Module | Purpose | Default port |
|---|---|---:|
| `:server:protocol` | Shared serialized API models only | - |
| `:server:security` | Ed25519 node identities, signing, verification, and replay protection | - |
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
COMPOSE_PARALLEL_LIMIT=1
```

Start the network from the repository root:

```powershell
docker compose -f server/docker-compose.yml up --build
```

Configure the Android emulator to use:

```text
ws://10.0.2.2:8094/relay
http://10.0.2.2:8095
```

Use the gateway URL as `serverUrl` and the push URL as `httpBaseUrl` in
`RelayTransportConfig`. The push health response must contain `fcmEnabled=true`.

The gateway accepts the existing relay WebSocket frames. Existing clients work locally without a
signed presence route. Cross-node routing becomes available when the client sends the optional route
proof fields in its `register` frame and includes the recipient-selected `mailboxRoute` in outgoing
envelopes.

## Security behavior

- A node identity is generated once and persisted in the shared node identity volume.
- `nodeId` is the SHA-256 digest of the Ed25519 public key.
- Node descriptors and heartbeats are signed.
- Federation requests are signed over method, path, timestamp, nonce, and body hash.
- Request nonces are retained for a bounded window to reject replays.
- Presence routes are accepted only with a valid client signature and non-stale generation.
- Mailbox IDs and capabilities are random. Only capability hashes are retained.
- Encrypted envelope IDs are deduplicated and expired entries are removed.
- Firebase Admin credentials are mounted only into the push container.

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

## Remaining persistence work

The remaining services use bounded in-memory adapters behind service-owned stores. This keeps the
complete topology executable and testable without allowing one service to access another service's
tables. Their production adapters remain intentionally local to their owning service. The federation
outbound queue still needs PostgreSQL.

The push, mailbox, presence, and node-registry services deliberately fall back to bounded in-memory
stores when their persistence URLs are absent, which keeps isolated tests and development outside
Docker Compose simple.

Replacing an adapter does not change `:server:protocol` or create service-to-service implementation
dependencies.

## Verification

```bash
./gradlew \
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
