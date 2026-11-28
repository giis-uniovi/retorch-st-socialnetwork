# CLAUDE.md — retorch-st-socialnetwork

Demonstrator of the [RETORCH](https://github.com/giis-uniovi/retorch) framework with three cooperating parts:

1. **The E2E suite** (`src/test`): Selenium UI tests and HTTP API tests.
2. **The SUT** (`sut/`): a fork of DeathStarBench Social Network (C++23 Thrift microservices behind an OpenResty gateway). Its architecture,
   endpoints and known quirks are in [`sut/CLAUDE.md`](sut/CLAUDE.md): do not duplicate them here.
3. **RETORCH orchestration** (`.retorch/`, `Jenkinsfile`, `orchestrate.ps1`): the CI pipeline that schedules the tests into parallel TJobs.

See `README.md` for the user documentation.

## Iron rule: changes under `sut/` need rebuild, redeploy and a green run

If you change anything under `sut/` (C++, headers, CMake, Dockerfiles, nginx/Lua, HTML/JS, configs, thrift), the task is not done until you have:

1. rebuilt the images (`docker compose -p <project> build`; C++ is compiled inside the image),
2. redeployed (`./deploy.sh` / `.\deploy.ps1`) and confirmed `http://localhost:8080` serves the "Death Star" page,
3. run the whole suite (`mvn test`) and seen it green.

No exceptions, not even for comment or HTML changes: nothing under `sut/` is reliably hot-reloaded.

## RETORCH rules (do not break)

- Every test method carries `@AccessMode(...)` annotations (`giis.retorch.annotations`) that declare the logical resources it uses and
  how (`READONLY`/`READWRITE`, concurrency, sharing). The TJob schedule of the `Jenkinsfile` is **generated from them**.
  - Never drop or alter existing `@AccessMode` annotations when refactoring.
  - Never rename test classes or methods: the `Jenkinsfile` references them as `Class#method`.
  - A new test method needs sensible `@AccessMode` values (copy a sibling) and is only run by CI after regenerating the pipeline.
- `Jenkinsfile`, `.retorch/scripts/`, `.retorch/envfiles/` (except `local.env`), `.retorch/executionplan.json` and the resource model in
  `.retorch/configurations/*SystemResources.json` are **generated** by `orchestrate.ps1` (it needs the
  `retorch-orchestration-*-standalone.jar` of the RETORCH tool in the repository root). Do not edit them by hand; put SUT-specific
  commands in `.retorch/customscriptscode/` and settings in `.retorch/configurations/retorchCI.properties`.
- Only `retorch-annotations` is a dependency of the tests; the orchestration code is never part of this project.
- `profiling=true` in `retorchCI.properties` makes the pipeline measure the resources of every TJob and publish
  `artifacts/usageprofile/usageprofile.html`. `.retorch/customscriptscode/custom-usageprofile` temporarily builds the profiling tool from
  source; delete it when a release publishes the jar.

## Layout

| Path | What it is |
|---|---|
| `src/test/java/giis/socialnetwork/e2e/functional/` | `tests/` (UI), `tests/api/`, `pages/` (page objects), `common/` (base classes), `dto/`, `utils/` (waits, clicks) |
| `src/test/resources/test.properties` | `BROWSER_USER`, `LOCALHOST_URL=http://localhost:8080` |
| `docker-compose.yml` | The whole SUT: external network `jenkins_network`, containers prefixed `${TJOB_NAME}-`, only `nginx-thrift` publishes a port (`${frontend_port}:8080`) |
| `deploy.sh` / `deploy.ps1` | Local deployment (`--port`, `--no-build`, `--down`); project `default`, port 8080 |
| `.retorch/` | Generated pipeline files, `envfiles/` (`local.env` + one per TJob), `customscriptscode/`, `configurations/` |
| `orchestrate.ps1` + `Jenkinsfile` | Regenerate / the generated pipeline: 14 TJobs (`tjoba`…`tjobn`) in 3 stages |
| `sonar-project.properties` | SonarCloud (`my:retorch-st-socialnetwork`): scans `sut/` and `src/test` |

RETORCH resources: `web-browser`, `frontend`, `user`, `post`, `social-graph`, `home-timeline`, `user-timeline`.

## Build and test

- Java 8 source level (no `var`, records or `List.of`), JUnit Jupiter, Selenium 4, Selema (owns the WebDriver lifecycle and the screenshots /
  video on failure: do not add your own driver manager or watcher), Gson, Apache HttpClient 4.x, SLF4J / Log4j2.
- `mvn test` (or `-Dtest=TestApiUsers`) against a deployed SUT. Target: `SUT_URL` property → `SUT_URL` env var → `LOCALHOST_URL`.
  `SELENOID_PRESENT` switches Selenium to the remote hub (`http://selenium-hub:4444/wd/hub`).
- Surefire has `testFailureIgnore=true`: the build stays green when tests fail, so read the `Tests run / Failures / Errors` totals.

## CI

- Jenkins, agent label `xretorch-agent`. Every TJob deploys its **own** copy of the SUT (`--env-file .retorch/envfiles/<tjob>.env
  -p <tjob>`) on the external network `jenkins_network`, runs its tests and tears it down; the TJobs of a stage run in parallel.
- The TJob runs `mvn test -Dtest="<Class#method>,..." -DTJOB_NAME=<tjob> -DSUT_URL=<url>`.
- `pom.xml` builds into `target/${TJOB_NAME}` so parallel TJobs never share compiled classes or reports (profile `local-execution` sets
  `TJOB_NAME=local` when the property is absent).
- `.github/workflows/test.yml` only triggers the SonarCloud scan.

## Test conventions

- Users are unique (`System.currentTimeMillis()`); API fixtures use `BaseApiClass.createUserWithName(label)`, UI fixtures the `BaseLoggedClass` helpers.
- The home timeline is eventually consistent (async fan-out): API tests poll (`TestApiTimeline`), UI tests refresh-retry (`Waiter.waitForPostText`). Never replace polling with sleeps.
- Respect the SUT quirks of `sut/CLAUDE.md`: have at least one follower before composing (empty `ZADD` gives a 500), tolerate `{}` for an empty
  home timeline, and do not navigate right after a follow / unfollow submit (redirect race).
- A rare Chrome flake on the signup navigation exists: re-run the class before suspecting a regression.
