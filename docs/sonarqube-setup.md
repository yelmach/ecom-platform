# Tutorial: SonarQube analysis and Jenkins quality gate

This setup analyzes all five Java services and the Angular frontend as one
SonarQube project, `ecom-platform`. Jenkins builds/tests the application, submits
one analysis, and waits for its quality gate before reporting success.

```text
Checkout → Build and test → SonarQube analysis → Quality gate
```

The Oracle VM uses native ARM64 images. Compose selects SonarQube Community
Build `26.10.0.133078-community` and PostgreSQL `17.11-bookworm` by version tag.
Docker pulls the matching host architecture. These references do not pin image
digests; a publisher can update the content behind a tag.

## 1. Files and responsibilities

| File | Purpose |
| --- | --- |
| [SonarQube Compose](../infra/sonarqube/docker-compose.yml) | Runs SonarQube/PostgreSQL, persists their data and connects SonarQube to Jenkins. |
| [Analysis properties](../sonar-project.properties) | Defines the project, source/test scopes, Java version, bytecode and coverage locations. |
| [Jenkinsfile](../Jenkinsfile) | Defines checkout, verification, analysis and quality-gate stages. |
| [Agent Dockerfile](../infra/jenkins/agent/Dockerfile) | Installs SonarScanner CLI `8.1.0.6389` using the existing Java runtime. |
| [Jenkins plugins](../infra/jenkins/controller/plugins.txt) | Includes SonarQube Scanner plugin `2.19.0`. |
| [Jenkins configuration](../infra/jenkins/controller/jenkins.yaml) | Registers the internal SonarQube URL and the analysis credential ID. |
| [CI job definition](../infra/jenkins/controller/jobs/ecom-ci.groovy) | Creates the `ecom-ci` job from the configured repository and branch. |
| [Makefile](../Makefile) | Provides SonarQube operations and `make sonar-scan`. |

Server configuration lives in Compose; analysis configuration lives in Git.
The project, tokens and webhook are created once through the UI. Jenkins keeps
credentials in its persistent home, referenced by fixed IDs in configuration.

## 2. Prepare the VM

Use the same VM checkout and root `.env` as the application and Jenkins. Pull
the branch containing these files. The configured `JENKINS_REPOSITORY_BRANCH`
(currently `deployment`) must also contain the root Jenkinsfile.

Elasticsearch requires these host settings:

```bash
sysctl vm.max_map_count fs.file-max
```

The minimum values are `524288` and `131072` respectively. The following stores
persistent values while preserving any higher current values:

```bash
sonar_map_count=$(sysctl -n vm.max_map_count)
sonar_file_max=$(sysctl -n fs.file-max)
printf 'vm.max_map_count=%s\nfs.file-max=%s\n' \
  "$((sonar_map_count < 524288 ? 524288 : sonar_map_count))" \
  "$((sonar_file_max < 131072 ? 131072 : sonar_file_max))" \
  | sudo tee /etc/sysctl.d/99-sonarqube.conf
sudo sysctl -p /etc/sysctl.d/99-sonarqube.conf
```

Compose sets SonarQube's `nofile` limit to `131072` and `nproc` to `8192`.
These satisfy Elasticsearch prerequisites; they are not CPU or RAM caps.
SonarQube uses its image's default JVM settings. Keep sufficient free memory
and disk space while running builds alongside the application; SonarQube's
small-installation guidance starts at 4 GB RAM and 30 GB disk space.

Add these values to the existing `.env`, preserving the other application and
Jenkins settings:

```dotenv
SONARQUBE_PORT=6310
SONAR_DB_NAME=sonar_db
SONAR_DB_USER=sonarqube
SONAR_DB_PASSWORD='REPLACE_WITH_A_STRONG_PASSWORD'
```

Keep `.env` private (`chmod 600 .env`). Do not put analysis or webhook tokens in
this file. PostgreSQL uses these credentials to initialize a fresh database;
changing `.env` does not rotate an existing PostgreSQL user's password.

## 3. Start Jenkins, then SonarQube

```bash
make jenkins-up
make sonarqube-config
make sonarqube-up
make sonarqube-ps
curl -fsS http://127.0.0.1:6310/api/system/status
```

Jenkins creates `jenkins_jenkins_network`. The SonarQube stack treats that network
as external, so it must exist first. The SonarQube container joins this network
and its own database network; PostgreSQL joins only the database network and
publishes no host port. No Docker socket is mounted into the build agent.

The first startup downloads images and initializes the database and search
indexes. `sonarqube-up` waits up to seven minutes for healthy services. Expect
`"status":"UP"`. On failure, inspect `make sonarqube-logs` for database,
Elasticsearch or host-limit errors; keep Elasticsearch bootstrap checks enabled.

The addresses are:

| Caller | Address |
| --- | --- |
| Browser | `https://sonar.yelmach.com` |
| Host-installed Caddy | `http://127.0.0.1:6310` |
| Jenkins controller and build agent | `http://sonarqube:9000` |
| SonarQube webhook callback | `http://jenkins:8080/sonarqube-webhook/` |

## 4. Configure DNS and Caddy

Create a Cloudflare `A` record for `sonar.yelmach.com` pointing to the VM's public
IP. Add this to the existing `/etc/caddy/Caddyfile`, preserving the application
and Jenkins site blocks:

```caddyfile
sonar.yelmach.com {
    reverse_proxy 127.0.0.1:6310
}
```

```bash
sudo caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile
sudo systemctl reload caddy
```

Keep port 6310 bound to localhost. Caddy terminates public HTTPS and uses local
HTTP upstream. With Cloudflare proxying, use Full (strict) after Caddy has its
valid certificate. Add an IPv6 record only if that route works on the VM.

Open `https://sonar.yelmach.com`. Initially log in with `admin` / `admin` and
change the password when prompted. In Administration > Configuration > General
Settings, set the server base URL to `https://sonar.yelmach.com`. This gives users
public links while scanners and callbacks communicate inside Docker.

For a local installation, use `http://localhost:6310` without local HTTPS.

## 5. Create the project and Jenkins credentials

1. Create a local SonarQube project with key **ecom-platform** and display name
   **Ecom Platform**. Keep the built-in language profiles and Sonar way quality
   gate. Review the new-code definition when establishing the initial baseline.
2. Treat the configured repository branch as this project's single analyzed
   branch. Do not send `sonar.branch.name` or pull-request parameters. Other
   branches must not overwrite the same project's analysis history.
3. Generate a **project analysis token** for `ecom-platform`, using an account
   with analysis and project browsing access. Choose an appropriate expiration;
   record the rotation date. SonarQube shows the token when it is generated.
4. In Jenkins, open Manage Jenkins > Credentials > System > Global and create a
   **Secret text** credential with the token and ID **sonarqube-token**.
5. Generate a separate random webhook secret, for example:

   ```bash
   python3 -c 'import secrets; print(secrets.token_hex(32))'
   ```

6. Store that value as another Jenkins **Secret text** credential with ID
   **sonarqube-webhook-secret**.

The analysis token authenticates scanner/API requests. The webhook secret
verifies callbacks; it is a different value. Keep both out of Git, console
output and command-line arguments. Rotate the Jenkins analysis credential when
its SonarQube token expires or is revoked.

The current jobs use anonymous checkout of the public GitHub repository.
Private repositories require a Git credential referenced by both Job DSL files;
see the [Jenkins tutorial](jenkins-setup.md).

## 6. Configure the project webhook

As the SonarQube system administrator, open **Administration > Configuration >
General settings > Security** and disable **Enable local webhooks validation**
(`sonar.validateWebhooks`). SonarQube 26.10 otherwise rejects the private Docker
address below. This setting permits internal destinations for all instance
webhooks, so keep project administration restricted to trusted users. The
callback remains HMAC-signed and is delivered inside the Docker network. See
the [official destination-validation documentation](https://docs.sonarsource.com/sonarqube-community-build/project-administration/webhooks#validating-destinations).

In SonarQube, open the project's Administration > Webhooks and create a webhook:

| Setting | Value |
| --- | --- |
| Name | `jenkins` |
| URL | `http://jenkins:8080/sonarqube-webhook/` |
| Secret | The value stored as `sonarqube-webhook-secret` in Jenkins |

Keep the trailing slash. Jenkins' SonarQube plugin provides this callback path.
The callback stays on the Docker network and does not pass through Cloudflare.
The pipeline checks its signature using the referenced Jenkins credential.

## 7. Build the agent and run CI

```bash
make jenkins-up
make jenkins-agent-up
make jenkins-agent-check
```

Confirm `jenkins-agent` is online and open the automatically created **ecom-ci**
job. Start it manually with **Build Now**. The root Jenkinsfile always runs the
full verification; it does not have the quick job's optional test parameter.
The existing `agent-check` job remains useful for checking infrastructure.

The CI pipeline:

1. Allocates the `arm64` agent, clears its job workspace, and checks out the
   configured branch. Maven/npm/scanner caches are outside that workspace.
2. Runs `make verify`. Failing builds/tests stop progress; available JUnit
   reports are published even when verification fails.
3. Runs `make sonar-scan` inside `withSonarQubeEnv('sonarqube')`. The token is
   passed through the environment. The Make target invokes `sonar-scanner`
   directly from the repository root, using `JAVA_HOME` for the Java analysis
   runtime. One submission includes Java, TypeScript, HTML and CSS.
4. Releases the agent executor and waits up to ten minutes for the quality
   gate. Any non-OK status or timeout fails CI. The overall timeout is one hour.

Java coverage comes from the four tested services' JaCoCo XML files; frontend
coverage comes from `frontend/coverage/frontend/lcov.info`. Discovery source and
bytecode are analyzed even though discovery currently has no tests/report.
The properties file lists compiled test classes and Surefire report directories
for the four tested services. Tests are classified separately from production
source. Generated outputs,
coverage files and dependencies are not indexed as application source.

In SonarQube, verify that backend and frontend files appear, both coverage formats
were imported and the analyzed revision matches the Jenkins checkout. Inspect
webhook delivery status as well as the Jenkins console. Check scanner logs for
missing coverage reports and unresolved Java types. If dependency resolution
warnings appear, configure Java library paths based on those diagnostics.
A rejected quality gate
can reveal real application issues; fix those separately rather than weakening
the gate to make integration look successful.

## 8. Verify persistence and failure handling

Run these checks after the first real analysis:

- Restart SonarQube/PostgreSQL using the Compose command below, wait for healthy
  services, and confirm the project and analysis history remain available.
- Restart the Jenkins controller, confirm the agent reconnects, and run `ecom-ci`
  again. Jenkins credentials and the generated job should persist.
- On Oracle, confirm `uname -m` reports `aarch64` and the scanner runs natively.

```bash
docker compose --env-file .env -f infra/sonarqube/docker-compose.yml restart
make sonarqube-up
```

Use a separate disposable project/job to test deliberate failures, keeping the
real project's history and webhook intact:

- An invalid/revoked analysis token must fail the analysis stage.
- Assign a deliberately rejecting quality gate to the test project: the Jenkins
  gate stage must fail on the reported non-OK status.
- For the missing-webhook timeout check, use an isolated callback endpoint or
  temporarily block callback delivery and ensure processing is still pending
  when `waitForQualityGate` starts. The step first checks completed task status,
  so a fast analysis can finish successfully without waiting for a callback.
- A test/build failure must prevent scanning. Confirm the successful build
  produces all configured coverage reports before running analysis; the scanner
  can warn about missing reports and continue without importing their coverage.

## Operations and backups

| Command | Action |
| --- | --- |
| `make sonarqube-config` | Validate Compose without printing resolved credentials. |
| `make sonarqube-up` | Start PostgreSQL/SonarQube and wait for readiness. |
| `make sonarqube-ps` | Show service state. |
| `make sonarqube-logs` | Follow both services' logs. |
| `make sonarqube-down` | Remove SonarQube containers/private network, preserving volumes. |
| `make sonar-scan` | Scan an already verified checkout; requires scanner and environment credentials. |

Stop SonarQube **before** `make jenkins-down` so its external network connection
is released. Restart Jenkins first. `down` preserves volumes; do not use `-v`
for the real installation. SonarQube's project configuration and history are in
PostgreSQL. Search data, extensions and logs have their own named volumes.

A database backup can be written outside the checkout:

```bash
mkdir -p "$HOME/sonarqube-backups"
chmod 700 "$HOME/sonarqube-backups"
docker compose --env-file .env -f infra/sonarqube/docker-compose.yml \
  exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' \
  > "$HOME/sonarqube-backups/sonarqube.dump"
```

Protect the dump and back up Jenkins home and any custom SonarQube extensions.
Before changing image versions, take a backup and test the upgrade.
Database migrations can prevent rollback to an older server image without a
matching database restore. Record the image tags and running image digests
with the backup metadata.

To scan manually from a development machine, install the same CLI on `PATH`
and Java 21 with `JAVA_HOME` set. Run `make verify` first, then run
`make sonar-scan` with `SONAR_HOST_URL` and `SONAR_TOKEN` supplied through the
process environment. This target uses the static paths in
`sonar-project.properties`; it does not build the application or read `.env`. GitHub build triggers, image publishing, deployment and
notifications are separate follow-up work; this job is triggered manually.

## Verify the installation

Run `ecom-ci` on the Oracle VM and confirm:

- The agent and scanner run natively on ARM64.
- Build and test stages succeed and Jenkins publishes JUnit results.
- Analysis includes all five Java services and the Angular frontend, with
  production and test files classified correctly.
- JaCoCo and LCOV reports are imported, and scanner logs contain no unexplained
  missing-report or unresolved-type warnings.
- The signed webhook reaches Jenkins and the quality-gate stage completes.
- Project history and Jenkins credentials survive service restarts.

Follow the isolated failure checks in step 8 to verify token rejection, gate
rejection and callback timeout. Public DNS/TLS and administrator login also need
to be checked on the VM. A local configuration check does not verify a complete
analysis or its quality gate.

References: [Linux prerequisites](https://docs.sonarsource.com/sonarqube-community-build/server-installation/pre-installation/linux),
[SonarScanner CLI](https://docs.sonarsource.com/sonarqube-community-build/analyzing-source-code/scanners/sonarscanner),
[Java analysis](https://docs.sonarsource.com/sonarqube-community-build/analyzing-source-code/languages/java),
[Jenkins quality gate](https://docs.sonarsource.com/sonarqube-community-build/analyzing-source-code/ci-integration/jenkins-integration/pipeline-pause).
