# Standalone SecureChat community node

This directory is the independently hostable SecureChat data plane. A node contains only:

- gateway
- federation
- mailbox
- mailbox PostgreSQL
- federation PostgreSQL
- Caddy

Every machine gets its own persistent Ed25519 node identity and its own Docker volumes. No node
shares a Docker network, database, token, or identity volume with the control plane or another node.

## Zero-input deployment bundle

The `Server Images` workflow publishes multi-architecture server images and creates a
`securechat-community-node-<channel>` artifact. It contains a ZIP for Windows and a TAR.GZ for
Linux/macOS. Both bundles already contain the control-plane URL, image prefix, and moving image
channel for the branch that built them.

After extracting the artifact, the operator does not edit an env file and does not type Docker
commands.

### Windows

Double-click `Start-SecureChatNode.cmd`.

### Linux

Launch `start-securechat-node.sh` from the desktop/file manager or configure it as an executable
launcher.

### macOS

Double-click `Start-SecureChatNode.command`.

The bootstrapper automatically:

1. detects the machine's primary IPv4 address;
2. creates strong database passwords and internal API tokens once;
3. writes the runtime Compose environment;
4. advertises the detected machine address on port `8490`;
5. pulls the published mailbox, federation, and gateway images;
6. starts the node and waits for all services to become healthy;
7. preserves the node identity, databases, and generated secrets on every restart;
8. starts the image updater, which follows the branch/release channel embedded in the bundle.

The generated files are local deployment state and are not part of the source repository:

```text
.env.runtime
secrets/
```

A later image published to the same channel is picked up automatically by the updater. Compose
configuration changes still require a newer deployment bundle.

## Network requirement

A community node can run on any Docker-capable Windows, Linux, or macOS machine that can reach the
configured SecureChat control plane and whose advertised `8490` port is reachable by SecureChat
clients and other nodes. The bundle automatically discovers the machine address; it cannot create
router/NAT port-forwarding rules or a public DNS name.

The current development bundle falls back to the development control-plane URL when no repository
variable is configured. Production should set the GitHub Actions repository variable
`SECURECHAT_CONTROL_PLANE_URL` to the public control-plane URL; bundles then contain that URL
automatically.

## Persistence

Do not delete the Docker volumes unless the node is intentionally being destroyed. In particular,
removing the `node-identity` volume creates a new node identity and therefore a new registry node ID.

The one-click bootstrapper uses normal `docker compose up` semantics and never runs `down -v`.

## Development compose

`docker-compose.yml` remains the source-build/local-development stack used by the existing server
smoke tests. `docker-compose.release.yml` is only the release/deployment override and replaces the
three SecureChat build targets with published GHCR images.
