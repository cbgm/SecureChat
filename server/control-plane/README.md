# SecureChat control plane

This Compose project runs only the services shared by all independently hosted nodes:

- signed node registry
- presence directory
- push service
- PostgreSQL and Redis persistence
- Caddy edge proxy

It contains no gateway, federation service, or mailbox. Community-node operators therefore do not
receive database credentials or a shared control-plane API token.

## Local start

Copy `.env.example` to `.env`, set `FIREBASE_ADMIN_CREDENTIALS`, and run from the repository root:

```powershell
docker compose `
    --env-file server/control-plane/.env `
    -f server/control-plane/docker-compose.yml `
    up -d --build
```

The control-plane API is then available at `http://localhost:8390`. Local diagnostics remain bound
to loopback:

```powershell
curl.exe http://localhost:8391/health
curl.exe http://localhost:8392/health
curl.exe http://localhost:8395/health
```

The edge exposes only these public API families:

```text
/v1/nodes/**       signed node registration, heartbeat, and directory
/v1/routes/**      node-authenticated presence writes and public resolution
/v1/node-push/**   node-authenticated push coordination
/push/**           client token registration and wake-up inbox
```

## Production

Provide the secret files listed in `.env.example`, configure `CONTROL_PLANE_DOMAIN`, and merge the
production override:

```powershell
docker compose `
    --env-file server/control-plane/.env.production `
    -f server/control-plane/docker-compose.yml `
    -f server/control-plane/docker-compose.production.yml `
    up -d --build
```

Caddy obtains TLS certificates for `CONTROL_PLANE_DOMAIN`. Only ports 80 and 443 remain published;
database, Redis, and diagnostic ports are removed.
