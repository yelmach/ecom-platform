# Jenkins CI/CD Guide

## 1) What Jenkins Does In This Project

Jenkins is the automation server for `MR-Jenk`.

In this project, Jenkins does this flow:

1. Fetch the latest code from GitHub
2. Run backend unit tests
3. Run frontend unit tests
4. Build the frontend
5. Run SonarQube analysis and wait for the Quality Gate
6. On `main`, build Docker images and push them to GHCR
7. On `main`, deploy the tested image tag to the OCI VM
8. Run post-deploy health checks
9. Roll back to the previous successful GHCR image release if deployment fails
10. Publish test results and send notifications

## 2) Jenkins Container Setup

### Why Jenkins Runs In Docker

This project runs Jenkins in Docker so setup is reproducible and easy to move to the OCI VM.

The Jenkins container has:

- Docker CLI + Docker Compose plugin
- Chromium for Angular/Karma tests
- `rsync` for copying the small deployment bundle into the deploy directory
- Docker socket access so Jenkins can build, push, pull, and run Docker images

### Current Jenkins Container Design

From [jenkins/docker-compose.yml](../jenkins/docker-compose.yml):

- Jenkins runs as `root`
- Docker socket is mounted:
  - `/var/run/docker.sock:/var/run/docker.sock`
- Stable deploy directory is mounted:
  - `/home/opc/ecom-platform-deploy:/home/opc/ecom-platform-deploy`
- Host access alias is configured:
  - `host.docker.internal:host-gateway`

That last part is important because health checks run **inside the Jenkins container**, but the deployed app is exposed on the **VM host ports**.

## 3) Start Jenkins

From `/home/opc/ecom-platform` on the VM:

```bash
docker compose -f jenkins/docker-compose.yml up -d --build
docker compose -f jenkins/docker-compose.yml ps
```

Open Jenkins:

```text
http://<VM_PUBLIC_IP>:8080
```

If Jenkins is fresh, get the initial password:

```bash
docker exec -it jenkins-server cat /var/jenkins_home/secrets/initialAdminPassword
```

Then:

1. Unlock Jenkins
2. Install plugins
3. Create admin user

## 4) Required Jenkins Plugins And Tools

### Plugins

- Pipeline
- Git
- GitHub
- NodeJS
- JUnit
- Mailer
- SonarQube Scanner

### Global Tool Configuration

Go to:

`Manage Jenkins -> Tools`

Add NodeJS installation named exactly:

`nodejs`

Why:

[Jenkinsfile](../Jenkinsfile) uses:

```groovy
tools {
    nodejs 'nodejs'
}
```

## 5) Stable Deployment Directory

This pipeline does **not** deploy from the Jenkins workspace.

Instead, it deploys from a fixed VM directory:

```text
/home/opc/ecom-platform-deploy
```

Why:

- Jenkins workspace is temporary
- deploy directory is stable
- secrets stay outside Git
- rollback can redeploy a previously successful GHCR image release

### Required Deploy Directory Content

Before Jenkins deployment works, this directory must already contain the runtime-only files:

```text
/home/opc/ecom-platform-deploy/backend/docker.env
/home/opc/ecom-platform-deploy/backend/certs/
/home/opc/ecom-platform-deploy/backend/keys/
```

These files are **not** copied from Git by Jenkins. They are preserved in the deploy directory.

Jenkins also manages these release files in the deploy directory:

```text
/home/opc/ecom-platform-deploy/.release.env
/home/opc/ecom-platform-deploy/.last-successful-release.env
```

`.release.env` stores the image tag for the current deployment.

`.last-successful-release.env` stores the last deployment that passed health checks and is used for rollback.

## 6) Create The Jenkins Job

Use a Multibranch Pipeline so Jenkins can validate pull requests and deploy only from `main`.

1. Jenkins -> `New Item`
2. Name it, for example:
   - `ecom-platform`
3. Type: `Multibranch Pipeline`
4. Add a GitHub branch source for this repository
5. Set the Jenkinsfile path:
   - `Jenkinsfile`
6. Recommended discovery behavior:
   - discover branches: `Exclude branches that are also filed as PRs`
   - discover pull requests from origin: merge the pull request with the current target branch revision
   - discover pull requests from forks: only if you need fork PRs
7. Save and scan the repository

With this setup:

- PRs run validation, tests, SonarQube, and Quality Gate
- `main` runs validation, tests, SonarQube, Quality Gate, image publishing, deploy, and health checks
- scheduled weekday scans run validation, tests, SonarQube, and Quality Gate without publishing images or deploying

## 7) Webhook / Automatic Trigger

For a Multibranch Pipeline, GitHub should notify Jenkins when branches or pull requests change.

In GitHub repo settings, add a webhook:

- Payload URL:
  `http://<VM_PUBLIC_IP>:8080/github-webhook/`
- Content type:
  `application/json`
- Events:
  `Pushes` and `Pull requests`

Then Jenkins can rescan and run the correct branch or PR job automatically.

## 8) Scheduled SonarQube Scans

The Jenkinsfile includes a weekday cron trigger:

```groovy
triggers {
    cron('H H(2-4) * * 1-5')
}
```

Jenkins chooses a stable hashed time between 02:00 and 04:59 from Monday to Friday. These scheduled runs continuously refresh the SonarQube dashboard even when nobody pushes code.

Timer-triggered builds are treated as quality-monitoring runs. They still execute tests, coverage generation, SonarQube analysis, and the Quality Gate, but they skip image publishing, deployment, and health checks.

## 9) Jenkinsfile Flow

Reference:

- [Jenkinsfile](../Jenkinsfile)

### Parameters

- `ENABLE_SONAR_ANALYSIS`
  - if `true`, run SonarQube analysis and enforce the Quality Gate
  - if `false`, skip SonarQube temporarily during troubleshooting
- `EMAIL_RECIPIENTS`
  - comma-separated notification recipients
- `DEPLOY_HOST`
  - host used by post-deploy health checks, for example `host.docker.internal` or the VM public IP

### Environment

- `CHROME_BIN=/usr/bin/chromium`
  - used by frontend tests
- `DEPLOY_DIR=/home/opc/ecom-platform-deploy`
  - stable deployment directory
- `RELEASE_ENV_FILE=/home/opc/ecom-platform-deploy/.release.env`
  - stores the current image release values
- `LAST_SUCCESSFUL_RELEASE_FILE=/home/opc/ecom-platform-deploy/.last-successful-release.env`
  - stores the last image release that passed health checks

### Stages

1. `Checkout Code`
   - `checkout scm`
   - saves current Git commit SHA

2. `Verify Backend`
   - loops through backend services
   - runs:
     ```bash
     ./mvnw -B -ntp clean verify
     ```

3. `Install Frontend Dependencies`
   - runs:
     ```bash
     npm ci
     ```

4. `Test Frontend`
   - runs:
     ```bash
     npm run test:ci
     ```

5. `Build Frontend`
   - runs:
     ```bash
     npm run build
     ```

6. `SonarQube Analysis`
   - runs `sonar-scanner` from the repo root
   - sends source, test, and coverage metadata to SonarQube

7. `Quality Gate`
   - waits for SonarQube to return the Quality Gate result
   - fails the pipeline if the status is not `OK`

8. `Build and Push Docker Images`
   - runs only on `main`, not pull requests
   - calls:
     ```bash
     ./scripts/ci/build-push-images.sh
     ```
   - builds backend and frontend images
   - pushes them to GHCR
   - writes `.release.env`

9. `Deploy`
   - runs only on `main`, not pull requests
   - calls:
     ```bash
     ./scripts/ci/deploy-prod.sh
     ```
   - copies `docker-compose.prod.yml`, `Makefile`, and `scripts/ci/` into the stable deploy directory
   - leaves runtime-only secrets and certs untouched
   - deploys from `docker-compose.prod.yml` by pulling GHCR images

10. `Health Check`
   - runs only on `main`, not pull requests
   - calls:
     ```bash
     ./scripts/ci/health-check.sh
     ```
   - checks:
     - gateway health endpoint
     - frontend availability
   - uses retries
   - only if health checks pass, saves `.release.env` as `.last-successful-release.env`

### CI Scripts

Long shell operations live in:

```text
scripts/ci/build-push-images.sh
scripts/ci/deploy-prod.sh
scripts/ci/health-check.sh
```

Keeping these scripts outside the Jenkinsfile makes the pipeline easier to read while still keeping Jenkins responsible for stages, credentials, branch rules, and notifications.

For the full image deployment and rollback flow, see:

- [GHCR Deployment Guide](ghcr-deployment-guide.md)

## 10) Why Health Checks Use `host.docker.internal`

Health checks run inside the Jenkins container.

So:

- `localhost` inside Jenkins means the Jenkins container itself
- it does **not** mean the VM host

Because the app is deployed on the VM host through Docker, Jenkins uses:

```text
host.docker.internal
```

This hostname is mapped by:

```yaml
extra_hosts:
  - "host.docker.internal:host-gateway"
```

So Jenkins can reach:

- `https://host.docker.internal:8443/actuator/health`
- `https://host.docker.internal:4200`

## 11) Rollback Strategy

Rollback is image-based.

How it works:

1. A successful deployment writes the release values into:
   - `/home/opc/ecom-platform-deploy/.last-successful-release.env`
2. If a later deployment fails after deployment was attempted:
   - Jenkins runs `scripts/ci/rollback-prod.sh`
   - Docker Compose reads `.last-successful-release.env`
   - the VM pulls and starts the previous successful GHCR image tag

This avoids rebuilding an old commit during rollback. The rollback target is the exact image release that already passed a previous health check.

## 12) Test Reports In Jenkins

### Backend

Backend Maven tests generate Surefire XML reports:

```text
backend/**/target/surefire-reports/*.xml
```

Jenkins publishes them with:

```groovy
junit ...
```

### Frontend

Frontend Karma tests generate JUnit XML:

```text
frontend/reports/junit/frontend-tests.xml
```

Frontend coverage is archived from:

```text
frontend/coverage/**
```

### Where To View Results

After a build:

- `Console Output` for logs
- `Test Result` for backend + frontend JUnit reports
- `Artifacts` for frontend coverage

## 13) Notifications

The pipeline sends emails on:

- success
- failure

Current notifications include:

- job name
- build number
- commit SHA
- SonarQube enabled
- deploy attempted
- deploy succeeded
- health check passed
- rollback triggered
- failing stage on failure
- Jenkins build URL

If `EMAIL_RECIPIENTS` is empty, the pipeline skips email without failing the build.

### SMTP Setup

Go to:

`Manage Jenkins -> Configure System`

Configure:

- SMTP server
- port
- authentication if required
- TLS/SSL depending on provider

Then either:

- set a default value for `EMAIL_RECIPIENTS` in the job configuration
- or fill it during `Build with Parameters`

## 14) How To Validate The Pipeline

### Happy Path

1. Push a harmless visible change
2. Jenkins triggers automatically
3. Backend tests pass
4. Frontend tests pass
5. SonarQube Quality Gate passes
6. Docker images are pushed to GHCR
7. Deploy runs on `main`
8. Health check passes
9. App is reachable in browser

### CI Failure

1. Break one backend test or frontend test
2. Push the change
3. Confirm pipeline fails before deployment

### Rollback

1. Start from one successful deployment
2. Push a change that breaks runtime health but still passes tests
3. Confirm:
   - deploy runs
   - health check fails
   - rollback runs
   - containers return to the image tag from `.last-successful-release.env`
