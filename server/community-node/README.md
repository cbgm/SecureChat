# Standalone SecureChat community node

This Compose project is the independently hostable data-plane package. It contains only:

- gateway
- federation service
- mailbox
- mailbox and federation PostgreSQL databases
- Caddy edge proxy

It shares no Docker network, volume, database, or static API token with the central control plane.
The mailbox, gateway, and federation containers share one persistent Ed25519 node identity. That
identity signs registry heartbeats, presence mutations, push coordination, and federation traffic.

## Local isolated start

Start the control plane first, copy `.env.example` to `.env`, and run:

```powershell
docker compose `
    --env-file server/community-node/.env `
    -f server/community-node/docker-compose.yml `
    up -d --build
```

Verify the node and registry:

```powershell
curl.exe http://localhost:8492/health
curl.exe http://localhost:8493/health
curl.exe http://localhost:8494/health
curl.exe http://localhost:8391/health
```

The registry health response must increase to `nodes=1`. Restarting the project without `down -v`
preserves both the node ID and queued federation/mailbox data.

`host.docker.internal` is appropriate for the local container-isolation test. Android emulators and
real remote nodes need an address resolvable by both clients and other node hosts. Use a real HTTPS
DNS name for production.

## Run a second independent node

Create another env file with a different project name and ports, for example:

```dotenv
COMMUNITY_NODE_PROJECT_NAME=securechat-community-node-b
COMMUNITY_NODE_HTTP_PORT=8590
CLIENT_ENDPOINT=ws://host.docker.internal:8590/relay
FEDERATION_ENDPOINT=http://host.docker.internal:8590
MAILBOX_ENDPOINT=http://host.docker.internal:8590
MAILBOX_DIAGNOSTIC_PORT=8592
FEDERATION_DIAGNOSTIC_PORT=8593
GATEWAY_DIAGNOSTIC_PORT=8594
MAILBOX_DATABASE_PORT=5736
FEDERATION_DATABASE_PORT=5738
```

Start it with `--env-file`. Compose project names isolate every network and volume.


## Automated isolation proof

From the repository root, run:

```powershell
.\server\scripts\Test-StandaloneCommunityNodes.ps1 -BuildImages
```

The test launches the control plane and two separately named community-node projects, registers two
persistent node identities, sends a federated envelope from A to B and from B to A, checks signed
presence and push access, and rejects any shared Docker network or volume between the projects.

## Production

Set `COMMUNITY_NODE_DOMAIN`, use the public HTTPS control-plane URL, provide the four secret files,
and merge the production override:

```powershell
docker compose `
    --env-file server/community-node/.env.production `
    -f server/community-node/docker-compose.yml `
    -f server/community-node/docker-compose.production.yml `
    up -d --build
```

The advertised endpoints become:

```text
wss://<COMMUNITY_NODE_DOMAIN>/relay
https://<COMMUNITY_NODE_DOMAIN>/v1/federation/**
https://<COMMUNITY_NODE_DOMAIN>/v1/mailboxes/**
```

Do not copy another operator's `node-identity` volume. A new operator must generate a new identity.
Do not run `docker compose down -v` unless intentionally deleting the node identity and all queued
data.
