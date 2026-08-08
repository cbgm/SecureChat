#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
RELEASE_ENV="$SCRIPT_DIR/release.env"
RUNTIME_ENV="$SCRIPT_DIR/.env.runtime"
SECRETS_DIR="$SCRIPT_DIR/secrets"
PREPARE_ONLY="${1:-}"

if [[ ! -f "$RELEASE_ENV" ]]; then
  echo "The deployment bundle is incomplete: release.env is missing." >&2
  exit 1
fi
if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is not installed or is not available to the current user." >&2
  exit 1
fi

COMPOSE_VERSION="$(docker compose version --short 2>/dev/null || true)"
COMPOSE_VERSION_NUMBER="$(printf '%s' "$COMPOSE_VERSION" | grep -Eo '[0-9]+\.[0-9]+\.[0-9]+' | head -n 1)"
if [[ -z "$COMPOSE_VERSION_NUMBER" ]] || [[ "$(printf '%s\n' '2.24.4' "$COMPOSE_VERSION_NUMBER" | sort -V | head -n 1)" != "2.24.4" ]]; then
  echo "Docker Compose 2.24.4 or newer is required." >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "$RELEASE_ENV"
set +a

: "${CONTROL_PLANE_URL:?release.env is missing CONTROL_PLANE_URL}"
: "${SECURECHAT_IMAGE_PREFIX:?release.env is missing SECURECHAT_IMAGE_PREFIX}"
: "${SECURECHAT_IMAGE_TAG:?release.env is missing SECURECHAT_IMAGE_TAG}"

control_plane_host() {
  printf '%s' "$CONTROL_PLANE_URL" | sed -E 's#^[a-zA-Z]+://([^/:]+).*#\1#'
}

primary_ipv4() {
  local destination
  destination="$(control_plane_host)"

  if command -v getent >/dev/null 2>&1; then
    resolved="$(getent ahostsv4 "$destination" 2>/dev/null | awk 'NR == 1 {print $1}')"
    if [[ -n "$resolved" ]]; then
      destination="$resolved"
    fi
  fi

  if command -v ip >/dev/null 2>&1; then
    ip route get "$destination" 2>/dev/null | awk '{for (i=1; i<=NF; i++) if ($i == "src") {print $(i+1); exit}}'
    return
  fi

  if command -v route >/dev/null 2>&1 && command -v ipconfig >/dev/null 2>&1; then
    interface_name="$(route -n get "$destination" 2>/dev/null | awk '/interface:/ {print $2; exit}')"
    if [[ -n "$interface_name" ]]; then
      ipconfig getifaddr "$interface_name"
      return
    fi
  fi

  hostname -I 2>/dev/null | awk '{print $1}'
}

HOST_ADDRESS="$(primary_ipv4)"
if [[ -z "$HOST_ADDRESS" ]]; then
  echo "Could not determine a usable IPv4 address for this node." >&2
  exit 1
fi

mkdir -p "$SECRETS_DIR"
ensure_secret() {
  local path="$1"
  if [[ ! -f "$path" ]]; then
    head -c 48 /dev/urandom | base64 | tr -d '\n' > "$path"
    chmod 600 "$path" 2>/dev/null || true
  fi
}

ensure_secret "$SECRETS_DIR/mailbox-database-password.txt"
ensure_secret "$SECRETS_DIR/federation-database-password.txt"
ensure_secret "$SECRETS_DIR/federation-internal-api-token.txt"
ensure_secret "$SECRETS_DIR/gateway-internal-api-token.txt"

cat > "$RUNTIME_ENV" <<EOF_RUNTIME
COMMUNITY_NODE_PROJECT_NAME=securechat-community-node
COMMUNITY_NODE_BIND_ADDRESS=0.0.0.0
COMMUNITY_NODE_HTTP_PORT=8490
COMMUNITY_NODE_SITE_ADDRESS=:80
CONTROL_PLANE_URL=$CONTROL_PLANE_URL
CLIENT_ENDPOINT=ws://$HOST_ADDRESS:8490/relay
FEDERATION_ENDPOINT=http://$HOST_ADDRESS:8490
MAILBOX_ENDPOINT=http://$HOST_ADDRESS:8490
SECURECHAT_IMAGE_PREFIX=$SECURECHAT_IMAGE_PREFIX
SECURECHAT_IMAGE_TAG=$SECURECHAT_IMAGE_TAG
SECURECHAT_UPDATE_INTERVAL_SECONDS=300
MAILBOX_DATABASE_PASSWORD_FILE=./secrets/mailbox-database-password.txt
FEDERATION_DATABASE_PASSWORD_FILE=./secrets/federation-database-password.txt
FEDERATION_INTERNAL_API_TOKEN_FILE=./secrets/federation-internal-api-token.txt
GATEWAY_INTERNAL_API_TOKEN_FILE=./secrets/gateway-internal-api-token.txt
EOF_RUNTIME

COMPOSE=(
  docker compose
  --env-file "$RUNTIME_ENV"
  -f "$SCRIPT_DIR/docker-compose.yml"
  -f "$SCRIPT_DIR/docker-compose.release.yml"
)

cd "$SCRIPT_DIR"
"${COMPOSE[@]}" config --quiet

if [[ "$PREPARE_ONLY" != "--prepare-only" ]]; then
  "${COMPOSE[@]}" pull
  "${COMPOSE[@]}" up -d --remove-orphans --wait --wait-timeout 300
fi
