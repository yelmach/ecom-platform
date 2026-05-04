# GHCR Deployment Guide

This project deploys production releases by building Docker images in Jenkins, pushing them to GitHub Container Registry (GHCR), then asking the VM to pull and run the exact image tag that passed CI.

The goal is to make deployment and rollback depend on immutable image tags, not on rebuilding old code during rollback.

## 1) Required Files

The deployment flow uses these files:

- `Jenkinsfile`
- `docker-compose.prod.yml`
- `scripts/ci/build-push-images.sh`
- `scripts/ci/deploy-prod.sh`
- `scripts/ci/health-check.sh`
- `scripts/ci/rollback-prod.sh`

## 2) Jenkins Credentials

Jenkins needs a GHCR credential:

- kind: `Username with password`
- ID: `ghcr-credentials`
- username: your GitHub username
- password: a GitHub token with package permissions

The Jenkinsfile exposes this credential only inside the build/push stage:

```groovy
withCredentials([usernamePassword(
    credentialsId: 'ghcr-credentials',
    usernameVariable: 'GHCR_USER',
    passwordVariable: 'GHCR_TOKEN'
)]) {
    sh './scripts/ci/build-push-images.sh'
}
```

## 3) Main-Only Deployment

The Docker build, push, deploy, and health-check stages run only for the real `main` branch:

```groovy
when {
    allOf {
        branch 'main'
        not { changeRequest() }
    }
}
```

Pull requests run validation, tests, SonarQube analysis, and Quality Gate, but they do not deploy.

## 4) Image Tagging

During checkout, Jenkins records the current commit:

```groovy
env.CURRENT_COMMIT = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
env.CURRENT_SHORT_COMMIT = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
```

For deployment, the image tag is the short commit SHA:

```groovy
env.IMAGE_TAG = env.CURRENT_SHORT_COMMIT
env.IMAGE_REGISTRY = 'ghcr.io/yelmach'
```

Example image:

```text
ghcr.io/yelmach/ecom-user-service:a1b2c3d
```

## 5) Build And Push Images

The script `scripts/ci/build-push-images.sh`:

1. validates that required variables exist
2. writes `.release.env`
3. logs in to GHCR
4. builds each backend service image
5. builds the frontend image
6. pushes all images to GHCR

The release file looks like this:

```env
IMAGE_REGISTRY=ghcr.io/yelmach
IMAGE_TAG=a1b2c3d
```

This file is important because deploy and rollback use it to know exactly which image version to run.

## 6) Production Compose File

Production deployment uses:

```text
docker-compose.prod.yml
```

Unlike past development compose, it does not build images from source. It pulls images from GHCR:

```yaml
image: ${IMAGE_REGISTRY}/ecom-user-service:${IMAGE_TAG}
```

The values come from `.release.env`.

## 7) Deploy To The VM

The script `scripts/ci/deploy-prod.sh`:

1. verifies `.release.env` exists
2. copies only the deployment files to `/home/opc/ecom-platform-deploy`
3. leaves runtime-only files that are not stored in Git untouched
4. copies `.release.env` into the deploy directory
5. pulls the GHCR images
6. starts the stack with Docker Compose

Runtime-only files preserved on the VM:

```text
/home/opc/ecom-platform-deploy/backend/docker.env
/home/opc/ecom-platform-deploy/backend/certs/
/home/opc/ecom-platform-deploy/backend/keys/
```

Deployment files copied by Jenkins:

```text
/home/opc/ecom-platform-deploy/docker-compose.prod.yml
/home/opc/ecom-platform-deploy/Makefile
/home/opc/ecom-platform-deploy/scripts/ci/
/home/opc/ecom-platform-deploy/.release.env
```

Deploy command:

```bash
docker compose --env-file backend/docker.env --env-file .release.env -f docker-compose.prod.yml up -d --remove-orphans
```

`--remove-orphans` removes old containers for services that no longer exist in the compose file.

## 8) Health Check

The script `scripts/ci/health-check.sh` checks:

- gateway: `https://<DEPLOY_HOST>:8443/actuator/health`
- frontend: `https://<DEPLOY_HOST>:4200`

If both checks pass, Jenkins saves the current release as:

```text
/home/opc/ecom-platform-deploy/.last-successful-release.env
```

That file is the rollback target.

## 9) Rollback

If a deployment was attempted and the pipeline fails after that point, The rollback script uses:

```text
/home/opc/ecom-platform-deploy/.last-successful-release.env
```

Then it pulls and starts the previous successful image tag:

```bash
docker compose --env-file backend/docker.env --env-file .last-successful-release.env -f docker-compose.prod.yml up -d --remove-orphans
```

This is safer than rebuilding an old commit because the rollback runs the exact images that already passed a previous deployment health check.

## 10) Useful VM Checks

After a successful main deployment:

```bash
cat .release.env
cat .last-successful-release.env
docker compose --env-file backend/docker.env --env-file .release.env -f docker-compose.prod.yml ps
```

To inspect the running image tags:

```bash
docker ps --format 'table {{.Names}}\t{{.Image}}\t{{.Status}}'
```
