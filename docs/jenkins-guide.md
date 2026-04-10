# Jenkins CI/CD Guide

## 1) What Jenkins Does In This Project

Jenkins is the automation server for `MR-Jenk`.

In this project, Jenkins does this flow:

1. Fetch the latest code from GitHub
2. Run backend unit tests
3. Run frontend unit tests
4. Build the frontend
5. Deploy the tested code the OCI VM
6. Run post-deploy health checks
7. Roll back to the previous successful commit if deployment health checks fail
8. Publish test results and send notifications

## 2) Jenkins Container Setup

### Why Jenkins Runs In Docker

This project runs Jenkins in Docker so setup is reproducible and easy to move to the OCI VM.

The Jenkins container has:

- Docker CLI + Docker Compose plugin
- Chromium for Angular/Karma tests
- `rsync` for copying tested code into the deploy directory
- Docker socket access so Jenkins can build and run the application stack

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
- rollback can redeploy a previously successful commit

### Required Deploy Directory Content

Before Jenkins deployment works, this directory must already contain the runtime-only files:

```text
/home/opc/ecom-platform-deploy/backend/docker.env
/home/opc/ecom-platform-deploy/backend/certs/
/home/opc/ecom-platform-deploy/backend/keys/
```

These files are **not** copied from Git by Jenkins. They are preserved in the deploy directory.

## 6) Create The Jenkins Job

1. Jenkins -> `New Item`
2. Name it, for example:
   - `ecom-platform-main`
3. Type: `Pipeline`
4. Configure:
   - Definition: `Pipeline script from SCM`
   - SCM: `Git`
   - Repository URL: your repo URL
   - Branch Specifier: the branch for this job
   - Script Path: `Jenkinsfile`
5. Save


## 7) Webhook / Automatic Trigger

This pipeline defines:

```groovy
triggers {
    githubPush()
}
```

To make that work end-to-end:

1. In Jenkins job config, enable:
   - `GitHub hook trigger for GITScm polling`
2. In GitHub repo settings, add webhook:
   - Payload URL:
     `http://<VM_PUBLIC_IP>:8080/github-webhook/`
   - Content type:
     `application/json`
   - Event:
     `Just the push event`

Then a push should trigger Jenkins automatically.

## 8) Jenkinsfile Flow

Reference:

- [Jenkinsfile](../Jenkinsfile)

### Parameters

- `ENABLE_DEPLOY`
  - if `true`, run deploy + health checks
  - if `false`, run CI only
- `EMAIL_RECIPIENTS`
  - comma-separated notification recipients

### Environment

- `CHROME_BIN=/usr/bin/chromium`
  - used by frontend tests
- `DEPLOY_DIR=/home/opc/ecom-platform-deploy`
  - stable deployment directory
- `LAST_SUCCESSFUL_DEPLOY_FILE=/home/opc/ecom-platform-deploy/.jenkins-last-successful-deploy`
  - stores last known good deployed commit

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

6. `Deploy`
   - runs only when `ENABLE_DEPLOY=true`
   - copies the tested workspace into the stable deploy directory with `rsync`
   - preserves:
     - `backend/docker.env`
     - `backend/certs/`
     - `backend/keys/`
     - `.jenkins-last-successful-deploy`
   - deploys from the stable directory using:
     ```bash
     docker compose --env-file backend/docker.env -f docker-compose.yml up --build -d
     ```

7. `Health Check`
   - runs only when `ENABLE_DEPLOY=true`
   - checks:
     - gateway health endpoint
     - frontend availability
   - uses retries
   - only if health checks pass, saves current commit as the last successful deployed commit

## 9) Why Health Checks Use `host.docker.internal`

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

## 10) Rollback Strategy

Rollback is a **basic commit-based rollback**.

How it works:

1. a successful deployment writes the commit SHA into:
   - `/home/opc/ecom-platform-deploy/.jenkins-last-successful-deploy`
2. if a later deployment fails after deployment was attempted:
   - Jenkins reads the previous successful commit
   - checks out that commit in the workspace
   - syncs it back into the deploy directory with `rsync`
   - redeploys it with Docker Compose

## 11) Test Reports In Jenkins

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

## 12) Notifications

The pipeline sends emails on:

- success
- failure

Current notifications include:

- job name
- build number
- commit SHA
- deploy enabled
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

## 13) How To Validate The Pipeline

### Happy Path

1. Push a harmless visible change
2. Jenkins triggers automatically
3. Backend tests pass
4. Frontend tests pass
5. Deploy runs
6. Health check passes
7. App is reachable in browser

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
