#!/usr/bin/env bash
# deploy-local.sh — Deploy or tear down the DeathStarBench Social Network SUT locally
#
# Usage:
#   ./deploy-local.sh                — start on default port 8080
#   ./deploy-local.sh --port 9090    — start on a custom port
#   ./deploy-local.sh --down         — tear down the running deployment
#   ./deploy-local.sh --down --port 9090

set -euo pipefail

TJOB_NAME="default"
NETWORK_NAME="jenkins_network"
COMPOSE_FILE="docker-compose.yml"
MAX_WAIT_SECS=300
POLL_INTERVAL=5
PORT=8080
DOWN=false

step() { echo "[>] $*"; }
ok()   { echo "[+] $*"; }
fail() { echo "[!] $*" >&2; exit 1; }

# ── Parse arguments ────────────────────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
    case "$1" in
        --down)   DOWN=true; shift ;;
        --port)   PORT="$2"; shift 2 ;;
        *) fail "Unknown argument: $1. Valid options: --down, --port <number>" ;;
    esac
done

# ── Prerequisites ──────────────────────────────────────────────────────────────
step "Checking prerequisites..."
command -v docker >/dev/null 2>&1 || fail "docker is not installed."
docker info >/dev/null 2>&1       || fail "Docker daemon is not running."
ok "Prerequisites satisfied."

# ── Export env vars for docker compose ────────────────────────────────────────
export TJOB_NAME
export frontend_port="$PORT"

# ── Teardown mode ──────────────────────────────────────────────────────────────
if $DOWN; then
    step "Tearing down project '$TJOB_NAME'..."
    docker compose -f "$COMPOSE_FILE" --ansi never -p "$TJOB_NAME" down --volumes
    ok "Teardown complete."
    exit 0
fi

# ── External Docker network ────────────────────────────────────────────────────
step "Ensuring Docker network '$NETWORK_NAME' exists..."
if ! docker network ls --format "{{.Name}}" | grep -qx "$NETWORK_NAME"; then
    docker network create "$NETWORK_NAME"
    ok "Network '$NETWORK_NAME' created."
else
    ok "Network '$NETWORK_NAME' already exists."
fi

# ── Build images ────────────────────────────────────────────────────────────────
step "Building images from sut/ (this is slow on the first run)..."
docker compose -f "$COMPOSE_FILE" --ansi never -p "$TJOB_NAME" build
ok "Images built."

# ── Start containers ──────────────────────────────────────────────────────────
step "Starting containers (project: '$TJOB_NAME', nginx port: $PORT)..."
docker compose -f "$COMPOSE_FILE" --ansi never -p "$TJOB_NAME" up -d
ok "Containers started."

# ── Wait for SUT ──────────────────────────────────────────────────────────────
SUT_URL="http://localhost:$PORT"
elapsed=0
ready=false

step "Waiting for SUT at $SUT_URL (up to ${MAX_WAIT_SECS}s)..."
while [[ $elapsed -lt $MAX_WAIT_SECS ]]; do
    if curl --silent --max-time 5 "$SUT_URL/" 2>/dev/null \
        | grep -q "DeathStar\|Death Star"; then
        ready=true
        break
    fi
    sleep $POLL_INTERVAL
    elapsed=$((elapsed + POLL_INTERVAL))
    echo "  ... $elapsed / ${MAX_WAIT_SECS}s"
done

if $ready; then
    ok "Social Network is ready at $SUT_URL"
else
    echo "[!] SUT did not become healthy within ${MAX_WAIT_SECS}s." >&2
    step "Collecting container logs (last 50 lines)..."
    docker compose -f "$COMPOSE_FILE" --ansi never -p "$TJOB_NAME" logs --tail 50
    step "Tearing down failed deployment..."
    docker compose -f "$COMPOSE_FILE" --ansi never -p "$TJOB_NAME" down --volumes
    exit 1
fi
