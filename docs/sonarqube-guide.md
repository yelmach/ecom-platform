# SonarQube Guide

## What This Adds

This repository now includes the project-side setup for `SafeZone`:

- a Dockerized SonarQube stack in `sonarQube/docker-compose.yml`
- backend JaCoCo XML coverage reports for every Spring Boot service
- a root `sonar-project.properties` for monorepo analysis
- Jenkins stages for SonarQube analysis and Quality Gate enforcement

The remaining setup is done in the SonarQube and Jenkins web UIs.

## 1) Start SonarQube

Before the first start, make sure `backend/docker.env` contains the SonarQube database values:

```bash
sed -n '1,80p' backend/docker.env
```

If you ever need to recreate it, copy the template first:

```bash
cp backend/docker.env.example backend/docker.env
```

From the repo root:

```bash
make sonar-up
```

Open:

```text
http://localhost:9002
```

Stop it with:

```bash
make sonar-down
```

Follow logs with:

```bash
make sonar-logs
```

## 2) First-Time SonarQube Web Setup

In the SonarQube UI:

1. Sign in with the default admin account.
2. Change the default password.
3. Create a project with the key:
   - `ecom-platform`
4. Generate a token for Jenkins.

## 3) Jenkins Web Setup

In Jenkins, install/configure these pieces exactly with these names:

- Plugin:
  - `SonarQube Scanner`
- SonarQube server name:
  - `sonarqube`
- SonarScanner tool name:
  - `sonar-scanner`

Add SonarQube server settings in:

`Manage Jenkins -> System`

Add the scanner tool in:

`Manage Jenkins -> Tools`

Store the SonarQube token in Jenkins credentials and attach it to the `sonarqube` server configuration.

## 4) Add The Required Webhook

In SonarQube, create a webhook pointing to:

```text
http://<your-jenkins-host>:8080/sonarqube-webhook/
```

This is required for the Jenkins `waitForQualityGate()` stage.

## 5) What The Pipeline Now Does

The Jenkins pipeline order is now:

1. checkout code
2. verify backend services
3. install frontend dependencies
4. run frontend tests with coverage
5. build frontend
6. run SonarQube analysis
7. wait for the Quality Gate result
8. deploy only if the Quality Gate passes

## 6) Coverage Inputs Used By SonarQube

### Backend

Each backend service now generates JaCoCo XML reports during Maven `verify`:

```text
backend/*/target/site/jacoco/jacoco.xml
```

### Frontend

Angular/Karma coverage is read from:

```text
frontend/coverage/frontend/lcov.info
```

## 7) Useful Validation Commands

Run backend verification manually:

```bash
cd backend/gateway-service && ./mvnw -B -ntp clean verify
```

Run frontend tests manually:

```bash
cd frontend && npm ci && npm run test:ci
```

Run a local scanner manually after Jenkins/Sonar setup is complete:

```bash
sonar-scanner \
  -Dsonar.host.url=http://localhost:9002 \
  -Dsonar.token=<your-token>
```

## 8) Linux Host Note

If SonarQube fails to start on Linux, the host may need Docker/VM kernel tuning for Elasticsearch, especially `vm.max_map_count`.
Check the SonarQube container logs first before changing anything.
