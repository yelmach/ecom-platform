# MR-Jenk Implementation Plan

## 1) Scope and target outcome

This project is a **CI/CD automation project** for your existing e-commerce platform.  
The required outcome is a Jenkins pipeline that does:

1. `checkout` code from Git automatically
2. `build` backend and frontend
3. `test` backend and frontend
4. `deploy` automatically when all previous stages pass
5. `stop on failure` at any stage
6. `notify` team on success/failure
7. `rollback` when deployment/health checks fail

---

## 2) Current repo status (what you already have)

You already have the core platform from project 1:

- Spring Boot microservices: discovery, gateway, user, product, media
- Angular frontend
- Docker Compose deployment (`docker-compose.yml`, `docker-compose.dev.yml`)
- Root `Makefile` with `prod-up`, `prod-down`, and dev infra targets

Main gaps for MR-Jenk:

- No `Jenkinsfile` yet
- Frontend `package.json` has no `test` script
- Test coverage is not clearly wired for CI yet
- No deployment/rollback scripts dedicated for pipeline execution
- No notification configuration documented

---

## 3) Deliverables checklist

Create and commit these project assets:

1. `Jenkinsfile` at repo root
2. `scripts/deploy.sh`
3. `scripts/health-check.sh`
4. `scripts/rollback.sh`
5. CI/CD section in `README.md` (setup + runbook)

Pipeline must demonstrate:

1. automated trigger on code push
2. build + test for backend and frontend
3. deployment after success only
4. rollback on deployment failure
5. notifications with clear status

---

## 4) Implementation phases

## Phase A: Make the project CI-ready

Goal: every command needed by Jenkins must run non-interactively.

1. Backend build normalization
- For each service (`backend/*-service`), verify `./mvnw clean verify` works.
- Ensure Maven wrapper exists and is executable.

2. Frontend command normalization
- Add test command(s) in `frontend/package.json`, e.g.:
  - `test`
  - `test:ci` (headless mode)
- Keep `npm ci` as install command in pipeline.

3. Baseline tests
- Backend: ensure at least smoke/unit tests exist and run.
- Frontend: ensure headless test run is stable in CI.

4. Environment prerequisites
- Document cert/key requirements (`backend/certs`, `backend/keys`).
- Decide Jenkins strategy:
  - Pre-provision keys/certs on Jenkins node (simplest for this project), or
  - Generate once in a controlled setup step.

Success criteria for Phase A:

- `./mvnw clean verify` succeeds for services
- `npm ci && npm run build && npm run test:ci` succeeds in frontend

## Phase B: Jenkins setup

Goal: Jenkins can execute your stack lifecycle.

1. Install Jenkins (Docker or native)
- Recommended: Dockerized Jenkins for quick reproducibility.

2. Install required plugins
- Git
- Pipeline
- Docker Pipeline
- Credentials Binding
- Workspace Cleanup
- JUnit
- Email Extension or Slack Notification plugin
- Timestamper / ANSI Color (optional but useful)

3. Configure tools
- JDK 17
- Node.js
- Maven (if not strictly using wrappers)
- Docker CLI access for deployment steps

4. Configure credentials
- Git credentials (if repo is private)
- Notification credentials (SMTP or Slack webhook)
- Optional registry credentials if pushing images

## Phase C: Build the pipeline

Goal: implement a clean declarative pipeline with strict stage gating.

Recommended stage order:

1. `Checkout`
- Pull latest code from repository.

2. `Prepare`
- Clean workspace.
- Print tool versions.
- Validate required files (`backend/docker.env`, certs/keys).

3. `Backend Build`
- Build each Spring service.

4. `Backend Test`
- Run Maven tests.
- Publish JUnit reports.

5. `Frontend Install`
- Run `npm ci` in `frontend/`.

6. `Frontend Build`
- Run `npm run build`.

7. `Frontend Test`
- Run headless tests (`npm run test:ci`).

8. `Package`
- Build Docker images (Compose build or explicit docker build).

9. `Deploy`
- Run `scripts/deploy.sh`.

10. `Health Check`
- Run `scripts/health-check.sh` and fail if any required endpoint is unhealthy.

11. `Post actions`
- On success: notify success.
- On failure: notify failure and trigger rollback (if deployment had started).

Rules:

- Any stage failure must stop subsequent stages.
- Deployment must happen only after all tests pass.

## Phase D: Deployment and rollback scripts

Goal: keep deployment operational logic outside Jenkinsfile.

1. `scripts/deploy.sh`
- Build/start services using compose:
  - `docker compose --env-file backend/docker.env -f docker-compose.yml up --build -d`
- Return non-zero exit code on failure.

2. `scripts/health-check.sh`
- Probe required endpoints, at minimum:
  - gateway `/actuator/health`
  - discovery `/actuator/health`
  - optional core service checks
- Use retries + timeout and fail fast when unhealthy.

3. `scripts/rollback.sh`
- Restore last known good deployment.
- Prefer one of:
  - previous image tags (recommended), or
  - previous successful commit redeploy.

Recommended rollback model for this project:

1. Tag images with Git SHA on each successful build.
2. Keep previous successful SHA in Jenkins metadata/file.
3. On failure, redeploy previous SHA and re-run health checks.

## Phase E: Notifications

Goal: provide immediate and clear build/deploy visibility.

1. Configure one channel:
- Email (SMTP), or
- Slack webhook

2. Send notifications for:
- Success
- Failure
- Rollback triggered

3. Include in message:
- Job name and build number
- Branch + commit SHA
- Failed stage (if any)
- Deployment/rollback result

## Phase F: Documentation

Update `README.md` with:

1. Jenkins installation/setup steps
2. Required plugins and credentials
3. How to create/connect the pipeline job
4. Trigger configuration (webhook or polling)
5. Stage-by-stage pipeline explanation
6. Deployment and rollback behavior
7. Notification setup
8. Troubleshooting section (common CI/CD failures)

---

## 5) Suggested working order (execution sequence)

Follow this sequence to reduce risk:

1. Stabilize backend and frontend build/test commands
2. Add missing test scripts (especially frontend `test:ci`)
3. Create `deploy.sh`, `health-check.sh`, `rollback.sh`
4. Install/configure Jenkins + plugins + credentials
5. Add and iterate on `Jenkinsfile`
6. Connect Git trigger
7. Run end-to-end pipeline test
8. Validate rollback with a forced failure scenario
9. Finalize documentation in `README.md`

---

## 6) Minimum pass criteria vs strong submission

Minimum pass criteria:

1. Jenkins pipeline triggered by code changes
2. Backend and frontend tests integrated and blocking on failure
3. Automatic deployment after success
4. Rollback implemented and demonstrable
5. Build/deploy notifications enabled

Stronger submission (bonus-aligned):

1. Parameterized builds (`dev`/`prod`)
2. Parallelized backend/frontend stages
3. Distributed builds with agents
4. Image tagging by commit SHA + artifact retention
5. Branch rules (e.g., deploy only from `main`)

---

## 7) Key risks to handle early

1. Missing or unstable test commands
- If tests are flaky, pipeline trust drops immediately.

2. Jenkins Docker permissions
- Jenkins must have permission to run Docker/Compose.

3. Certs/keys dependency
- Missing gateway certs or JWT keys can break deployment.

4. Rollback complexity
- Keep rollback simple and deterministic for this project scope.

---

## 8) Final acceptance checklist

Before considering MR-Jenk complete, verify:

1. Jenkins job runs automatically on push
2. Build fails when backend or frontend tests fail
3. Deploy runs only when all tests pass
4. Health checks validate deployment
5. Rollback runs on failed deployment/health check
6. Notifications are sent for success/failure/rollback
7. README explains setup and operation clearly

