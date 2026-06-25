#!/bin/bash
# Tear down any existing local SUT deployment and redeploy the local stack.
# Run from the project root directory.
#
# Usage:
#   ./redeploy-local.sh           # full build + deploy
#   ./redeploy-local.sh --no-build  # skip image build, just redeploy

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.retorch/envfiles/local.env"
PROJECT_NAME="${COMPOSE_PROJECT_NAME:-local}"
SKIP_BUILD=false

for arg in "$@"; do
  [ "$arg" = "--no-build" ] && SKIP_BUILD=true
done

if [ -f "$ENV_FILE" ]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

resolve_path() {
  local path="$1"
  if [ -z "$path" ]; then
    return 0
  fi

  case "$path" in
    /*) printf '%s\n' "$path" ;;
    *) printf '%s/%s\n' "$SCRIPT_DIR" "$path" ;;
  esac
}

COMPOSE_FILE_PATH="$(resolve_path "${COMPOSE_FILE:-docker-compose.yml}")"
OVERRIDE_FILE_PATH="$(resolve_path "${COMPOSE_OVERRIDE_FILE:-docker-compose.local-override.yml}")"
SUT_URL="${SUT_URL:-http://localhost:8080}"

if [ ! -f "$COMPOSE_FILE_PATH" ]; then
  echo "ERROR: compose file not found at $COMPOSE_FILE_PATH."
  echo "Set COMPOSE_FILE and COMPOSE_OVERRIDE_FILE in .retorch/envfiles/local.env or add the compose files to the repository."
  exit 1
fi

COMPOSE=(docker compose -f "$COMPOSE_FILE_PATH")
if [ -n "$OVERRIDE_FILE_PATH" ] && [ -f "$OVERRIDE_FILE_PATH" ]; then
  COMPOSE+=( -f "$OVERRIDE_FILE_PATH" )
fi
COMPOSE+=( --env-file "$ENV_FILE" -p "$PROJECT_NAME" )

echo "=== [1/4] Tearing down existing SUT deployment ==="
"${COMPOSE[@]}" down --volumes --remove-orphans 2>/dev/null || true

if [ "$SKIP_BUILD" = false ]; then
  echo "=== [2/4] Building images ==="
  "${COMPOSE[@]}" build
else
  echo "=== [2/4] Skipping build (--no-build flag set) ==="
fi

echo "=== [3/4] Starting SUT containers ==="
"${COMPOSE[@]}" up -d

echo ""
echo "Waiting for the SUT to respond at $SUT_URL (up to 200 seconds)..."
COUNTER=0
WAIT_LIMIT=40

until curl -sf "$SUT_URL" >/dev/null 2>&1; do
  printf "  attempt %d/%d\r" "$((COUNTER + 1))" "$WAIT_LIMIT"
  sleep 5
  COUNTER=$((COUNTER + 1))
  if [ "$COUNTER" -ge "$WAIT_LIMIT" ]; then
    echo ""
    echo "ERROR: SUT did not become ready in time. Last container logs:"
    "${COMPOSE[@]}" logs --tail=30
    exit 1
  fi
done

echo ""
echo "============================================"
echo " SUT is ready!"
echo "============================================"
echo "  SUT URL     $SUT_URL"
echo ""
echo "To tear down:"
echo "  docker compose -f $COMPOSE_FILE_PATH \\"
if [ -n "$OVERRIDE_FILE_PATH" ] && [ -f "$OVERRIDE_FILE_PATH" ]; then
  echo "    -f $OVERRIDE_FILE_PATH \\"
fi
echo "    --env-file .retorch/envfiles/local.env \\"
echo "    -p $PROJECT_NAME down --volumes"
