# Jenkins CI Guide

## 1) What Jenkins Is

Jenkins is an automation server.

In this project, Jenkins does these tasks automatically:

1. Fetch source code from GitHub
2. Build backend services
3. Run backend unit tests
4. Install frontend dependencies
5. Run frontend unit tests
6. Build frontend app
7. Publish test/coverage outputs in Jenkins

This is Continuous Integration (CI): every code change is validated quickly and consistently.

## 2) Concepts You Should Know

- `Pipeline`: the sequence of stages Jenkins executes
- `Stage`: a logical step like Build, Test, Deploy
- `Agent`: machine/container where Jenkins runs commands
- `SCM`: Source Control Management (GitHub here)
- `Jenkinsfile`: pipeline-as-code file stored in the repository
- `Post actions`: always/success/failure actions after stages

## 3) Project Jenkins Setup

Jenkins runtime files:

- [jenkins/docker-compose.yml](../jenkins/docker-compose.yml)
- [jenkins/Dockerfile](../jenkins/Dockerfile)

Pipeline definition:

- [Jenkinsfile](../Jenkinsfile)

### Important note about this repo

Jenkins container is configured with `user: root` in `jenkins/docker-compose.yml`.

Because of that, frontend browser tests must run with a no-sandbox browser launcher in CI.

## 4) Start Jenkins

From repo root:

```bash
cd jenkins
docker compose up -d --build
docker compose ps
```

Command explanation:

- `cd jenkins`:
  - move into the folder that contains Jenkins Docker files.
- `docker compose up -d --build`:
  - build Jenkins image (if needed) and start container in background.
  - `-d` means detached mode (terminal stays free).
  - `--build` forces image rebuild so Dockerfile changes are applied.
- `docker compose ps`:
  - show running containers and status/ports.

Open Jenkins UI:

`http://localhost:8080`

If Jenkins is fresh, unlock it:

```bash
docker exec -it jenkins-server cat /var/jenkins_home/secrets/initialAdminPassword
```

Command explanation:

- `docker exec`:
  - run a command inside a running container.
- `-it`:
  - interactive terminal mode.
- `jenkins-server`:
  - container name from `jenkins/docker-compose.yml`.
- `cat /var/jenkins_home/secrets/initialAdminPassword`:
  - print first-time Jenkins unlock password.

Then install suggested plugins and create your admin user.

## 5) Required Jenkins Plugins and Tools

### Plugins

- Pipeline
- Git
- NodeJS
- JUnit

### Global Tool configuration

Go to:

`Manage Jenkins -> Tools`

Add NodeJS installation named exactly:

`nodejs`

Why this name matters:
the `Jenkinsfile` references `tools { nodejs 'nodejs' }`.

## 6) Create the Pipeline Job

1. Jenkins -> `New Item`
2. Name: `ecom-market` (or any name you prefer)
3. Type: `Pipeline`
4. In job config, set:
   - Definition: `Pipeline script from SCM`
   - SCM: `Git`
   - Repository URL: your repo URL
   - Branch Specifier: your branch (example: `*/main` or `*/ci_Pipeline`)
   - Script Path: `Jenkinsfile`
5. Save
6. Click `Build Now`

## 7) What This Jenkinsfile Does

### 7.1 Jenkinsfile Explanation

Reference file:

- [Jenkinsfile](../Jenkinsfile)

Explination of content : 

- `pipeline {}`
  - starts a declarative Jenkins pipeline block.
- `agent any`
  - Jenkins can run this pipeline on any available agent/executor.

- `options {}`
  - pipeline-level execution options start.
- `timestamps()`
  - adds timestamps to each log line for easier debugging.
- `disableConcurrentBuilds()`
  - prevents two runs of the same job from executing at the same time.

- `tools {}`
  - declares tools Jenkins should provide to pipeline steps.
- `nodejs 'nodejs'`
  - use Jenkins NodeJS tool installation named `nodejs`.

- `environment {}`
  - define pipeline-wide environment variables.
- `CHROME_BIN = '/usr/bin/chromium'`
  - sets Chromium executable path for frontend test browser launching.
- `BACKEND_SERVICES = 'discovery-service gateway-service user-service product-service media-service'`
  - one variable listing all backend service directories to iterate over.

- `stages {}`
  - starts all pipeline stages.

- `stage('Checkout Code')`
  - `checkout scm` checks out repository code configured for the job.

- `stage('Build Backend')`
  - enters scripted block to loop over backend services.
  - `line 28`: converts `BACKEND_SERVICES` string to list using `tokenize(' ')`.
  - `line 30`: iterates service-by-service.
  - `line 32`: changes into `backend/<service>` directory.
  - `line 33`: runs Maven build command:
    - `clean package` builds jar
    - `-DskipTests` skips tests here (tests run in dedicated stage).

- `stage('Test Backend')`
  - same loop pattern as build stage.
  -  runs `./mvnw -B -ntp test` for each service.
  - if any test fails, this stage fails and pipeline stops.

- `stage('Install Frontend Dependencies')`
  - moves into `frontend` directory and runs `npm ci`.
  - installs dependencies from lock file for reproducible CI installs.

- `stage('Test Frontend')`
  - runs Angular tests in CI mode:
  - `--watch=false`: single-run mode.
  - `--browsers=ChromeHeadlessNoSandbox`: browser mode compatible with root-container CI.
  - `--code-coverage`: produces frontend coverage artifacts.

- `stage('Build Frontend')`
  - if previous stages are green, runs `npm run build`.
  - generates production frontend build output.

- `post {   }`
  - defines actions executed after stage execution.
- `always { ... }`
  - always publish available backend JUnit reports.
  - always archive frontend coverage artifacts.
  - always print completion message.
- `success { ... }`
  - message shown when pipeline succeeds.
- `failure { ... }`
  - message shown when pipeline fails.

Current pipeline stages:

1. `Checkout Code`
2. `Build Backend`
3. `Test Backend`
4. `Install Frontend Dependencies`
5. `Test Frontend`
6. `Build Frontend`

### Build Backend

Builds these services one by one:

- discovery-service
- gateway-service
- user-service
- product-service
- media-service

Command pattern:

`./mvnw -B -ntp clean package -DskipTests`

Flag explanation:

- `./mvnw`:
  - project Maven wrapper (ensures consistent Maven runtime).
- `-B`:
  - batch mode, better for CI logs.
- `-ntp`:
  - no transfer progress, cleaner output.
- `clean`:
  - remove previous build artifacts from `target/`.
- `package`:
  - compile and package service into jar.
- `-DskipTests`:
  - skip test execution in build stage (tests run in separate stage).

### Test Backend

Runs tests for each backend service:

`./mvnw -B -ntp test`

Command explanation:

- `test` goal runs unit tests through Maven Surefire.
- Fails stage immediately if any test fails.

### Test Frontend

Runs Angular/Karma tests in CI mode:

`npm run test -- --watch=false --browsers=ChromeHeadlessNoSandbox --code-coverage`

Flag explanation:

- `npm run test`:
  - executes `ng test` script from `frontend/package.json`.
- `--`:
  - passes following flags to underlying Angular/Karma command.
- `--watch=false`:
  - single-run CI mode (do not wait for file changes).
- `--browsers=ChromeHeadlessNoSandbox`:
  - run tests in headless Chrome with no-sandbox launcher for root-container CI.
- `--code-coverage`:
  - generate coverage report files.

Why `ChromeHeadlessNoSandbox`:

- Jenkins container runs as root in this project setup
- Chrome blocks root mode unless no-sandbox is used

### Post Actions

Always executes:

- publish backend JUnit XML:
  `backend/**/target/surefire-reports/*.xml`
- archive frontend coverage:
  `frontend/coverage/**`

Also prints clear success/failure message.

## 8) How to Run and Validate CI

After clicking `Build Now`, verify:

1. Stages appear in order
2. `Test Backend` runs real tests (not skipped)
3. `Test Frontend` launches ChromeHeadlessNoSandbox
4. Build ends with `SUCCESS` when all pass
5. Build ends with `FAILURE` if any test fails

Check outputs in Jenkins build page:

- `Console Output` for logs
- `Test Result` for backend JUnit
- `Artifacts` for frontend coverage

## 9) Common Issues and Fixes

### A) "No changes" and no stages run

Usually wrong job type/entry.

Fix:

- use Pipeline job
- ensure job points to `Jenkinsfile`
- run the actual branch job if using multibranch

### B) Frontend test error: `Running as root without --no-sandbox`

Cause:

- container runs as root

Fix:

- use `--browsers=ChromeHeadlessNoSandbox` in CI command

### C) Frontend test error: `describe is not defined`

Cause:

- custom Karma config replaced Angular defaults and removed Jasmine setup

Fix:

- keep Angular default test builder config
- do not override Karma config unless you include full frameworks/plugins
