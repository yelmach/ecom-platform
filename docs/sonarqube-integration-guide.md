# SonarQube Integration Guide

This guide implements the full SonarQube workflow for this e-commerce microservices platform.

## 1) SonarQube Setup With Docker

From repository root:

```bash
make sonar-up
```

Equivalent command:

```bash
docker compose -f docker-compose.sonarqube.yml up -d
```

Image tag used:

- `sonarqube:26.3.0.120487-community` (pinned latest published community tag)

Open SonarQube:

`http://localhost:9000`

Default login:

- Username: `admin`
- Password: `admin`

You will be prompted to set a new password on first login.

Stop SonarQube:

```bash
make sonar-down
```

Stop and remove SonarQube volumes:

```bash
make sonar-down-v
```

## 2) SonarQube Project Configuration

1. Go to `http://localhost:9000`.
2. Create project:
   - Project key: `ecom-platform`
   - Display name: `ecom-platform`
3. Generate a project token:
   - `Project Settings -> Security -> Generate Tokens`
4. Save the token securely (it is shown once).
5. Quality profiles:
   - Keep default language profiles, or assign stricter custom profiles per language (Java + TypeScript).
6. Quality gate:
   - Use built-in `Sonar way`, or create a custom gate with stricter thresholds.

Source and report mapping is defined in:

- [sonar-project.properties](../sonar-project.properties)

Coverage inputs:

- Backend JaCoCo XML reports from all services
- Frontend LCOV report from Angular/Karma

## 3) GitHub Integration (Automatic Scan On Push And PR)

This repository includes:

- [.github/workflows/sonarqube-analysis.yml](../.github/workflows/sonarqube-analysis.yml)

The workflow runs on every:

- `push` (all branches)
- `pull_request` (all branches)
- daily schedule (`0 2 * * *` UTC) for continuous monitoring

Required GitHub repository secrets:

- `SONAR_HOST_URL` (example: `http://<your-sonarqube-host>:9000`)
- `SONAR_TOKEN` (generated in SonarQube)

The workflow runs backend tests + frontend tests with coverage, then executes Sonar scan and waits for quality gate.
If the quality gate fails, the workflow fails.

## 4) CI/CD Pipeline Integration (Jenkins)

Jenkins pipeline has a new `SonarQube Analysis` stage in:

- [Jenkinsfile](../Jenkinsfile)

Behavior:

1. Runs after build/test stages
2. Launches Sonar Scanner CLI container
3. Waits for SonarQube quality gate (`sonar.qualitygate.wait=true`)
4. Fails pipeline automatically when gate fails

Required Jenkins environment/credentials:

- `SONAR_HOST_URL`
- `SONAR_TOKEN`

If these variables are missing, pipeline fails immediately with a clear message.

## 5) Continuous Monitoring

Recommended operations:

1. Keep SonarQube running (`make sonar-up`) in QA/DevOps environments.
2. Monitor dashboards:
   - Reliability
   - Security and security hotspots
   - Maintainability
   - Coverage
   - Duplications
3. Review trends after each merge and sprint.

## 6) Review And Approval Process

Pull request template added:

- [.github/pull_request_template.md](../.github/pull_request_template.md)

Process to enforce:

1. Require PR review approval before merge.
2. Require passing checks:
   - Jenkins pipeline
   - `SonarQube Analysis` GitHub workflow
3. Block merge when quality gate fails.
4. Resolve SonarQube issues before approval, or document explicit justification in SonarQube for accepted risks.

GitHub branch protection should be configured to require these checks on target branches.

## 7) Notes About Branch/PR Analysis Support

This setup triggers scans on every push and pull request.
Advanced branch/PR decoration capabilities in SonarQube depend on the SonarQube edition and ALM binding configuration.
