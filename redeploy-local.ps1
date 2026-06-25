# Tear down any existing local SUT deployment and redeploy the local stack.
# Run from the project root directory.
#
# Usage:
#   .\redeploy-local.ps1             # full build + deploy
#   .\redeploy-local.ps1 -NoBuild    # skip image build, just redeploy

param(
    [switch]$NoBuild
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$EnvFile   = Join-Path $ScriptDir ".retorch\envfiles\local.env"
$ProjectName = if ($env:COMPOSE_PROJECT_NAME) { $env:COMPOSE_PROJECT_NAME } else { "local" }
$SutUrl = if ($env:SUT_URL) { $env:SUT_URL } else { "http://localhost:8080" }

if (Test-Path $EnvFile) {
    Get-Content $EnvFile | ForEach-Object {
        if ($_ -match '^\s*([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
            $name = $Matches[1]
            $value = $Matches[2].Trim().Trim('"').Trim("'")
            [System.Environment]::SetEnvironmentVariable($name, $value)
        }
    }
}

$ComposeFile = if ($env:COMPOSE_FILE) { $env:COMPOSE_FILE } else { "docker-compose.yml" }
$OverrideFile = if ($env:COMPOSE_OVERRIDE_FILE) { $env:COMPOSE_OVERRIDE_FILE } else { "docker-compose.local-override.yml" }
$ProjectName = if ($env:COMPOSE_PROJECT_NAME) { $env:COMPOSE_PROJECT_NAME } else { "local" }
$SutUrl = if ($env:SUT_URL) { $env:SUT_URL } else { "http://localhost:8080" }

if (-not [System.IO.Path]::IsPathRooted($ComposeFile)) {
    $ComposeFile = Join-Path $ScriptDir $ComposeFile
}

if (-not [System.IO.Path]::IsPathRooted($OverrideFile)) {
    $OverrideFile = Join-Path $ScriptDir $OverrideFile
}

if (-not (Test-Path $ComposeFile)) {
    Write-Error "Compose file not found at $ComposeFile. Set COMPOSE_FILE and COMPOSE_OVERRIDE_FILE in .retorch/envfiles/local.env or add the compose files to the repository."
    exit 1
}

# On Windows with Docker Desktop, host.docker.internal is automatically available
# in both containers and the Windows hosts file. No detection needed.
$env:HOST_IP = "host.docker.internal"

function Invoke-Compose {
    param([string[]]$CommandArgs)

    $baseArgs = @(
        "compose",
        "-f", $ComposeFile,
        "--env-file", $EnvFile,
        "-p", $ProjectName
    )

    if (Test-Path $OverrideFile) {
        $baseArgs += @("-f", $OverrideFile)
    }

    & docker @baseArgs @CommandArgs
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose failed with exit code $LASTEXITCODE"
    }
}

Write-Host "=== [1/4] Tearing down existing SUT deployment ===" -ForegroundColor Cyan
try {
    Invoke-Compose @("down", "--volumes", "--remove-orphans")
} catch {
    Write-Host "  (no existing deployment or teardown warning - continuing)" -ForegroundColor Yellow
}

if (-not $NoBuild) {
    Write-Host "=== [2/4] Building images ===" -ForegroundColor Cyan
    Invoke-Compose @("build")
} else {
    Write-Host "=== [2/4] Skipping full build (-NoBuild flag set) ===" -ForegroundColor Yellow
}

Write-Host "=== [3/4] Starting SUT containers ===" -ForegroundColor Cyan
Invoke-Compose @("up", "-d")

Write-Host ""
Write-Host "Waiting for the SUT to respond at $SutUrl (up to 200 seconds)..." -ForegroundColor Cyan
$Counter = 0
$WaitLimit = 40
$Ready = $false

while ($Counter -lt $WaitLimit) {
    try {
        $response = Invoke-WebRequest -Uri $SutUrl -UseBasicParsing -TimeoutSec 5 -ErrorAction SilentlyContinue
        if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 500) {
            $Ready = $true
            break
        }
    } catch {
        # Not ready yet
    }
    $Counter++
    Write-Host ("  attempt {0}/{1}" -f $Counter, $WaitLimit) -NoNewline
    Write-Host "`r" -NoNewline
    Start-Sleep -Seconds 5
}

Write-Host ""

if (-not $Ready) {
    Write-Host "ERROR: SUT did not become ready in time. Showing last container logs:" -ForegroundColor Red
    try { Invoke-Compose @("logs", "--tail", "30") } catch {}
    exit 1
}

Write-Host "============================================" -ForegroundColor Green
Write-Host " SUT is ready!" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
Write-Host "  SUT URL     $SutUrl"
Write-Host ""
Write-Host "To tear down:"
Write-Host "  docker compose -f $ComposeFile --env-file .retorch\envfiles\local.env -p $ProjectName down --volumes"

Write-Host "Process complete."
Read-Host -Prompt "Press Enter to exit"