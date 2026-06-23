[![Build Status](https://github.com/giis-uniovi/retorch-st-socialnetwork/actions/workflows/test.yml/badge.svg)](https://github.com/giis-uniovi/retorch-st-socialnetwork/actions)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=my%3Aretorch-st-socialnetwork&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=my%3Aretorch-st-socialnetwork)
# RETORCH Social Network End-to-End Test Suite

End-to-End test suite for the DeathStarBench Social Network microservices application, used as a demonstrator of the [RETORCH Framework](https://github.com/giis-uniovi/retorch).

The Social Network is a distributed benchmark application based on [DeathStarBench](https://github.com/delimitrou/DeathStarBench), built with Thrift RPC, Nginx/OpenResty, MongoDB, Redis, Memcached and RabbitMQ — all running in Docker containers.

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (Windows / macOS) or Docker Engine (Linux)
- Git
- Java 8+, Maven 3.x

## Deployment

The SUT is vendored in the [`sut/`](sut/) directory of this repository. The deploy scripts create the `jenkins_network` Docker network, start all containers using the root `docker-compose.yml` (which mounts config, Lua scripts and generated Thrift bindings from `sut/`), and wait up to 300 seconds for the Nginx gateway to be ready.

### Windows (PowerShell)

```powershell
# Start the SUT on the default port (8080)
.\deploy-local.ps1

# Start on a custom port
.\deploy-local.ps1 -Port 9090

# Tear down all containers and volumes
.\deploy-local.ps1 -Down
```

### Linux / macOS

```bash
# Make the script executable (first time only)
chmod +x deploy-local.sh

# Start the SUT on the default port (8080)
./deploy-local.sh

# Start on a custom port
./deploy-local.sh --port 9090

# Tear down all containers and volumes
./deploy-local.sh --down
```

Once up, the SUT is accessible at `http://localhost:8080` (default).


## CI deployment — Jenkins

The `Jenkinsfile` at the repository root defines the full pipeline used by the on-premises Jenkins instance.
It relies on the lifecycle scripts in `.retorch/scripts/` and the environment files in `.retorch/envfiles/`.
The GitHub Actions workflow (`.github/workflows/test.yml`) compiles the project; actual test execution is delegated to Jenkins via RETORCH orchestration.
