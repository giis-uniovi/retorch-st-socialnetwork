<#
.SYNOPSIS
    Deploys or tears down the DeathStarBench Social Network SUT locally using Docker Compose.
.PARAMETER Down
    Tear down the running deployment instead of starting it.
.PARAMETER Port
    Host port nginx-thrift listens on (default: 8080).
.EXAMPLE
    .\deploy-local.ps1
    .\deploy-local.ps1 -Port 9090
    .\deploy-local.ps1 -Down
#>
param(
    [switch]$Down,
    [int]$Port = 8080
)

$ErrorActionPreference = "Stop"

$TJOB_NAME     = "default"
$NETWORK_NAME  = "jenkins_network"
$COMPOSE_FILE  = "docker-compose.yml"
$MAX_WAIT_SECS = 300
$POLL_INTERVAL = 5

function Write-Step([string]$msg) { Write-Host "[>] $msg" -ForegroundColor Cyan }
function Write-OK([string]$msg)   { Write-Host "[+] $msg" -ForegroundColor Green }
function Write-Fail([string]$msg) { Write-Host "[!] $msg" -ForegroundColor Red; exit 1 }

# ── Prerequisites ──────────────────────────────────────────────────────────────
Write-Step "Checking prerequisites..."

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Fail "docker is not installed or not in PATH."
}
$ErrorActionPreference = "Continue"
docker info *>$null
$dockerOk = ($LASTEXITCODE -eq 0)
$ErrorActionPreference = "Stop"
if (-not $dockerOk) {
    Write-Fail "Docker daemon is not running. Please start Docker Desktop."
}
Write-OK "Prerequisites satisfied."

# ── Export env vars for docker compose ────────────────────────────────────────
$env:TJOB_NAME    = $TJOB_NAME
$env:frontend_port = $Port

# ── Teardown mode ──────────────────────────────────────────────────────────────
if ($Down) {
    Write-Step "Tearing down project '$TJOB_NAME'..."
    docker compose -f $COMPOSE_FILE --ansi never -p $TJOB_NAME down --volumes
    if ($LASTEXITCODE -ne 0) { Write-Fail "docker compose down failed." }
    Write-OK "Teardown complete."
    exit 0
}

# ── External Docker network ────────────────────────────────────────────────────
Write-Step "Ensuring Docker network '$NETWORK_NAME' exists..."
$existingNetworks = docker network ls --format "{{.Name}}"
if ($existingNetworks -notcontains $NETWORK_NAME) {
    docker network create $NETWORK_NAME
    if ($LASTEXITCODE -ne 0) { Write-Fail "Failed to create Docker network '$NETWORK_NAME'." }
    Write-OK "Network '$NETWORK_NAME' created."
} else {
    Write-OK "Network '$NETWORK_NAME' already exists."
}

# ── Build images ────────────────────────────────────────────────────────────────
Write-Step "Building images from sut/ (this is slow on the first run)..."
docker compose -f $COMPOSE_FILE --ansi never -p $TJOB_NAME build
if ($LASTEXITCODE -ne 0) { Write-Fail "docker compose build failed." }
Write-OK "Images built."

# ── Start containers ──────────────────────────────────────────────────────────
Write-Step "Starting containers (project: '$TJOB_NAME', nginx port: $Port)..."
docker compose -f $COMPOSE_FILE --ansi never -p $TJOB_NAME up -d
if ($LASTEXITCODE -ne 0) { Write-Fail "docker compose up failed." }
Write-OK "Containers started."

# ── Wait for SUT ──────────────────────────────────────────────────────────────
$sutUrl  = "http://localhost:$Port"
$elapsed = 0
$ready   = $false

Write-Step "Waiting for SUT at $sutUrl (up to ${MAX_WAIT_SECS}s)..."
while ($elapsed -lt $MAX_WAIT_SECS) {
    try {
        $resp = Invoke-WebRequest -Uri "$sutUrl/" -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
        if ($resp.Content -match "DeathStar|Death Star") {
            $ready = $true
            break
        }
    } catch {}
    Start-Sleep -Seconds $POLL_INTERVAL
    $elapsed += $POLL_INTERVAL
    Write-Host "  ... $elapsed / ${MAX_WAIT_SECS}s"
}

if ($ready) {
    Write-OK "Social Network is ready at $sutUrl"
} else {
    Write-Host "[!] SUT did not become healthy within ${MAX_WAIT_SECS}s." -ForegroundColor Red
    Write-Step "Collecting container logs (last 50 lines)..."
    docker compose -f $COMPOSE_FILE --ansi never -p $TJOB_NAME logs --tail 50
    Write-Step "Tearing down failed deployment..."
    docker compose -f $COMPOSE_FILE --ansi never -p $TJOB_NAME down --volumes
    exit 1
}
