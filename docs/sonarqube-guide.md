# SonarQube Integration Guide

## Overview

This project integrates SonarQube into the existing Jenkins pipeline so code quality analysis runs automatically before deployment.

The integration covers:

- a Dockerized SonarQube stack in `sonarQube/docker-compose.yml`
- backend Java coverage with JaCoCo
- frontend Angular coverage with LCOV
- a root `sonar-project.properties` for monorepo analysis
- Jenkins stages for SonarQube analysis and Quality Gate enforcement

The overall pipeline flow is:

1. checkout code
2. run backend tests
3. run frontend tests
4. build the frontend
5. submit analysis to SonarQube
6. wait for the Quality Gate result
7. deploy only if the Quality Gate passes

## Files In This Repo

The main files used for the integration are:

- `sonarQube/docker-compose.yml`
- `sonar-project.properties`
- `Jenkinsfile`
- `backend/*/pom.xml`

## 1) SonarQube Runtime Stack

SonarQube runs in its own Docker Compose stack with PostgreSQL.

File:

- `sonarQube/docker-compose.yml`

It defines:

- `postgres`
- `sonarqube`

Important details:

- the SonarQube image is `sonarqube:community`
- SonarQube is exposed on host port `9002`
- PostgreSQL credentials come from `backend/docker.env`
- Docker named volumes persist database and SonarQube data

### Required Environment Variables

Make sure `backend/docker.env` contains:

```env
SONARQUBE_DB_NAME=sonarqube
SONARQUBE_DB_USER=sonar
SONARQUBE_DB_PASSWORD=your-strong-password
```

The template already includes these keys:

- `backend/docker.env.example`

## 2) Start SonarQube

From the repo root:

```bash
make sonar-up
```

Open SonarQube in the browser:

```text
http://<VM_PUBLIC_IP>:9002
```

Useful commands:

```bash
make sonar-logs
make sonar-down
```

If you want to confirm the containers are running:

```bash
docker compose --env-file backend/docker.env -f sonarQube/docker-compose.yml ps
```

## 3) First-Time SonarQube Setup In The UI

After opening SonarQube:

1. sign in as admin
2. change the default admin password
3. create a local project
4. use:
   - project name: `ecom-platform`
   - project key: `ecom-platform`
   - main branch: your real default branch, usually `main`
5. generate a token for Jenkins

Copy the token immediately. You will store it in Jenkins as a secret.

## 4) Jenkins Configuration

This project expects Jenkins to run in Docker on the same VM.

Because of that, Jenkins should reach SonarQube through:

```text
http://host.docker.internal:9002
```

That works because the Jenkins container already defines:

```yaml
extra_hosts:
  - "host.docker.internal:host-gateway"
```

### Install The Jenkins Plugin

In Jenkins, install:

- `SonarQube Scanner`

### Add The SonarQube Token

In Jenkins credentials:

1. add credential
2. type: `Secret text`
3. use:
   - scope: `Global`
   - secret: paste the SonarQube token
   - ID: `sonarqube-token`
   - description: `SonarQube token for ecom-platform`

### Add The SonarQube Server

Go to:

- `Manage Jenkins -> System`

In the SonarQube section, add a server with:

- name: `sonarqube`
- server URL: `http://host.docker.internal:9002`
- credentials: `sonarqube-token`

The server name must match the name used in `Jenkinsfile`.

### Add The Sonar Scanner Tool

Go to:

- `Manage Jenkins -> Tools`

Under `SonarQube Scanner installations`, add a scanner with:

- name: `sonar-scanner`

The tool name must also match the `Jenkinsfile`.

## 5) Required SonarQube Webhook

The Jenkins pipeline uses `waitForQualityGate()`, so SonarQube must send the analysis result back to Jenkins.

In SonarQube, add a webhook:

- name: `jenkins`
- URL: `http://<VM_PUBLIC_IP>:8080/sonarqube-webhook/`

Keep the trailing slash.

## 6) What The Jenkinsfile Does

The Jenkins integration is already implemented in:

- `Jenkinsfile`

### New Pipeline Parameter

The pipeline now includes:

- `ENABLE_SONAR_ANALYSIS`

This allows you to temporarily skip SonarQube if needed during troubleshooting.

### SonarQube Analysis Stage

The `SonarQube Analysis` stage:

- checks `ENABLE_SONAR_ANALYSIS`
- loads the Jenkins tool named `sonar-scanner`
- uses the Jenkins SonarQube server named `sonarqube`
- runs `sonar-scanner` from the repo root

### Quality Gate Stage

The `Quality Gate` stage:

- waits for SonarQube to finish processing
- receives the result through the webhook
- fails the pipeline if the Quality Gate status is not `OK`

Because this stage runs before deploy, bad code quality can stop deployment.

## 7) Coverage Configuration

### Backend Coverage

Each backend service now generates JaCoCo coverage during Maven `verify`.

This is configured in:

- `backend/discovery-service/pom.xml`
- `backend/gateway-service/pom.xml`
- `backend/user-service/pom.xml`
- `backend/product-service/pom.xml`
- `backend/media-service/pom.xml`

Each service produces:

```text
target/site/jacoco/jacoco.xml
```

JaCoCo is needed because SonarQube reads coverage reports; it does not generate Java coverage by itself.

### Frontend Coverage

Angular/Karma already generates LCOV coverage.

SonarQube reads:

```text
frontend/coverage/frontend/lcov.info
```

## 8) sonar-project.properties

The root scanner config is:

- `sonar-project.properties`

This file tells SonarQube:

- the project key and name
- where backend and frontend source files are
- where test files are
- which build/generated folders to exclude
- where Java binaries are
- where JUnit reports are
- where JaCoCo XML reports are
- where frontend LCOV coverage is
- which TypeScript config files to use

This repository is analyzed as one monorepo SonarQube project, not as separate SonarQube projects per service.

## 9) First Validation Run

After SonarQube and Jenkins are configured, run the Jenkins job first with:

- `ENABLE_SONAR_ANALYSIS=true`
- `ENABLE_DEPLOY=false`

This first run is only to validate the quality pipeline without deployment.

You want to see:

- backend tests pass
- frontend tests pass
- SonarQube Analysis passes
- Quality Gate passes

If that works, run the pipeline again with deploy enabled.
