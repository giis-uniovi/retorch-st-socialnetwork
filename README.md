[![Status](https://github.com/giis-uniovi/retorch-st-socialnetwork/actions/workflows/test.yml/badge.svg)](https://github.com/giis-uniovi/retorch-st-socialnetwork/actions)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=my%3Aretorch-st-socialnetwork&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=my%3Aretorch-st-socialnetwork)

# retorch-st-socialnetwork

This repository contains an end-to-end test suite for the DeathStarBench social network benchmark, orchestrated with RETORCH. The suite exercises the social network UI and APIs through Selenium and HTTP-based tests.

## Prerequisites

- Java 8 or newer
- Maven 3.8+
- Docker Engine with Docker Compose v2
- A locally reachable SUT instance (the default target is http://localhost:8080)

## Local configuration

The file [.retorch/envfiles/local.env](.retorch/envfiles/local.env) stores the local defaults used by the deployment helper scripts and the test harness. Review it before running the local workflow and adjust the URL or compose settings if your environment differs.

```bash
# Example values
SUT_URL=http://localhost:8080
SELENOID_PRESENT=false
COMPOSE_PROJECT_NAME=local
COMPOSE_FILE=docker-compose.yml
COMPOSE_OVERRIDE_FILE=docker-compose.local-override.yml
```

## Local deployment helpers

The repository provides two convenience scripts at the root:

- [redeploy-local.sh](redeploy-local.sh) for Unix-like shells
- [redeploy-local.ps1](redeploy-local.ps1) for PowerShell

Both scripts:

- load the values from [.retorch/envfiles/local.env](.retorch/envfiles/local.env)
- use the compose files defined by `COMPOSE_FILE` and `COMPOSE_OVERRIDE_FILE`
- build and start the local stack, then wait until the SUT responds

Run them from the repository root:

```bash
./redeploy-local.sh
```

```powershell
./redeploy-local.ps1
```

Use the `--no-build` flag (or `-NoBuild` in PowerShell) to skip the image build step and redeploy the existing stack.

## Running the tests locally

Once the SUT is available, run the suite with Maven:

```bash
mvn test
```

The tests are also wired for CI execution through [Jenkinsfile](Jenkinsfile) and the scripts in [.retorch](.retorch).

## Tear-down

To stop and remove the local compose deployment:

```bash
docker compose --env-file .retorch/envfiles/local.env -p local down --volumes
```
