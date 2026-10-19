# Jenkins controller and ARM build agent

Run commands from the repository root. This stack is separate from the deployed
application, with its own network and persistent volumes. The controller listens
on `127.0.0.1:6300`, behind the VM's host-installed Caddy.

## What is configured in code

| File | Purpose |
| --- | --- |
| `controller/Dockerfile` | Build the controller with its plugins and configuration |
| `controller/plugins.txt` | Declare the required plugins |
| `controller/jenkins.yaml` | Set URL, users, permissions, executors and agent |
| `controller/jobs/agent-check.groovy` | Create the verification Pipeline job |
| `agent-check.Jenkinsfile` | Check tools, checkout, and optionally run `make verify` |
| `agent/Dockerfile` | Install Java 21, Node 24/npm, Git, Make, Chromium and build tools |
| `docker-compose.yml` | Run the controller/agent and preserve their data |

JCasC (Jenkins Configuration as Code) loads YAML on startup. Job DSL creates or
updates `agent-check` without scheduling a build. The setup wizard is disabled
because the administrator account and permissions are supplied by configuration.
The controller has zero executors; `jenkins-agent` has one executor, label `arm64`,
and accepts jobs requesting its label. It connects over WebSocket, with no port
50000 needed. Anonymous users have no granted permissions; account signup is off.

The old `init.groovy.d/01-controller.groovy` hook is replaced by YAML. Configuration
files are baked into the controller image: after changing them, run
`make jenkins-up` to rebuild and recreate the controller. Code-managed settings,
including the configured administrator password and job definition, are reapplied
on startup; make lasting changes in Git or `.env`, rather than the dashboard.
The controller base and directly listed plugins have explicit versions;
the plugin installer resolves their dependencies automatically.

The controller is capped at 2 GB / 0.5 CPU, with a 1 GB maximum Java heap. The
non-root agent is capped at 6 GB / 2 CPUs. These are caps, not reserved resources.
No host Docker socket or application data volumes are mounted. Image publishing,
SonarQube, webhooks and deployment belong to later pipeline steps.

## 1. Set the root .env on the VM

Commit/push these files, then update the VM checkout to the branch containing
them. This does not redeploy the running application. Add these entries to your
existing `.env`; do not replace its application settings:

```dotenv
JENKINS_PORT=6300
JENKINS_PUBLIC_URL=https://jenkins.yelmach.com/
JENKINS_ADMIN_ID=admin
JENKINS_ADMIN_PASSWORD='REPLACE_WITH_A_STRONG_PASSWORD'
JENKINS_REPOSITORY_URL=https://github.com/YOUR_OWNER/YOUR_REPOSITORY.git
JENKINS_REPOSITORY_BRANCH=deployment
JENKINS_GIT_CREDENTIALS_ID=
JENKINS_AGENT_SECRET=
```

Use the actual repository HTTPS URL and the branch containing the Jenkinsfile.
Do not include a token in the URL. Single quotes in `.env` prevent `$` interpolation;
choose a password without a single quote when using the example quoting above.
The Compose definition explicitly passes only Jenkins settings into the controller.
Do not change an existing valid agent secret to blank.

```bash
chmod 600 .env
make jenkins-config
```

`jenkins-config` validates Compose without printing secrets. Empty required
controller values fail validation. It does not validate JCasC; startup does that.
Do not publish full `docker compose config` output: it includes credentials.

If this VM already has Jenkins data, back up the Jenkins home volume before
switching configuration. The existing volume is reused, users/jobs are not wiped,
but YAML replaces the active security policy and resets the configured admin
password. Any old `oracle-arm-agent` node is not automatically deleted; remove it
through the UI if it is no longer used. The old bind mount itself did not copy its
Groovy file into the home volume.

## 2. Start the controller

```bash
make jenkins-up
make jenkins-ps
```

The first build downloads plugins. Subsequent unchanged builds use Docker cache.
The command waits up to five minutes for the login endpoint. On failure, inspect
`make jenkins-logs` for plugin, JCasC or Job DSL errors. Never disable authentication
to work around a configuration error. There is no initial unlock password/wizard:
log in using `JENKINS_ADMIN_ID` and `JENKINS_ADMIN_PASSWORD`.

## 3. Configure DNS and Caddy

Create a Cloudflare `A` record for `jenkins.yelmach.com` pointing to the VM. Only
add `AAAA` if the VM is reachable over IPv6. Add this to the existing
`/etc/caddy/Caddyfile`, preserving other sites:

```caddyfile
jenkins.yelmach.com {
    reverse_proxy 127.0.0.1:6300
}
```

```bash
sudo caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile
sudo systemctl reload caddy
```

Open `https://jenkins.yelmach.com/` and log in. Keep 6300 private; Caddy uses public
80/443. With Cloudflare proxying, use Full (strict) after Caddy has its valid origin
certificate. If using another hostname/port, update `.env` and Caddy together.
For a local test, use `JENKINS_PUBLIC_URL=http://localhost:6300/` and browse that URL;
no local HTTPS certificate is required.

## 4. Connect the automatically created agent

Under Manage Jenkins > Nodes, open **jenkins-agent**. The node already exists;
copy its inbound connection secret into root `.env` as `JENKINS_AGENT_SECRET`.
Keep it out of Git, chat and build logs. Then run:

```bash
make jenkins-agent-up
make jenkins-agent-check
```

The first agent build downloads packages including Chromium. The node must show
online in Jenkins. On Oracle, the tool check should print `aarch64`, Java 21,
Node 24/npm, Git, Make and Chromium, and pass directory write checks. Inspect
`make jenkins-agent-logs` if the node stays offline. A running container alone
does not prove authentication succeeded. Preserve the Jenkins home volume:
replacing it changes the generated agent secret.

Use `make jenkins-up` for the controller first. A plain Compose `up` starts both
services; an agent without its connection secret cannot connect.

## 5. Configure Git access if the repository is private

Public repositories need no credential. For a private repository, create a
**Username with password** credential under Manage Jenkins > Credentials >
System > Global: GitHub username and a token allowed to read this repository.
Use ID `github-read`, then set `JENKINS_GIT_CREDENTIALS_ID=github-read` in `.env`
and run `make jenkins-up` to update the job. The token is stored in Jenkins home,
not committed. This optional credential is the remaining manual Git-access step;
its ID is configured in code. Do not use the agent connection secret for Git.

## 6. Test the configuration and verification job

1. Open a private/incognito window: Jenkins must require login to view jobs.
2. Log in as the configured administrator. Check Manage Jenkins > System has
   the expected public URL and Built-In Node has zero executors.
3. Check `jenkins-agent` has one executor, label `arm64`, and is online.
4. Open the automatically created **agent-check** job. Select Build with
   Parameters (or Build Now), leaving `RUN_PROJECT_CHECKS` false. Expect a
   successful tools check and checkout, with the commit SHA in the console.
5. Run it again with `RUN_PROJECT_CHECKS` true. Expect backend/frontend builds
   and tests to pass, with JUnit results displayed. The first run downloads
   dependencies; later runs reuse Maven/npm caches. Production databases and
   the application's `.env` are not required in the job workspace.
6. Restart the controller with the command below. After it becomes healthy,
   log in again and confirm the job is still present once and the agent
   reconnects. Run the quick job again.

```bash
docker compose --env-file .env -f infra/jenkins/docker-compose.yml restart jenkins
make jenkins-ps
```

Local x86 execution does not prove ARM64 behavior; run these checks on the Oracle
VM. The `arm64` label selects the intended node; it does not emulate a CPU.

## Operations

| Command | Action |
| --- | --- |
| `make jenkins-config` | Validate Compose quietly |
| `make jenkins-up` | Build/start/wait for the controller only |
| `make jenkins-agent-up` | Build/start the agent with its configured secret |
| `make jenkins-ps` | Show container state |
| `make jenkins-logs` | Follow controller logs |
| `make jenkins-agent-logs` | Follow agent logs |
| `make jenkins-agent-check` | Check agent tools and cache permissions |
| `make jenkins-down` | Remove containers/network, preserving volumes |

Back up `jenkins_jenkins_home`, including its secrets, and protect that backup.
Do not use `down -v` on the real installation. Updating the controller base or
plugin versions requires rebuilding and testing before upgrading the VM.

## Validation performed

Local checks on 2026-10-19:

- Built the controller image with the pinned plugin list successfully.
- Validated Compose isolation and rejection of an empty admin password.
- Booted a disposable controller with the real JCasC and Job DSL configuration;
  verified administrator authentication, anonymous access denial, signup disabled,
  controller URL, executor counts, node label and generated job configuration.
- Connected the build agent over WebSocket and ran the actual verification
  Jenkinsfile successfully against a temporary Git repository, with project
  checks disabled. This exercised tools, writable caches and checkout.
- Restarted the controller and verified admin login, one generated job,
  preserved successful build history, the same agent secret and reconnection.
- Confirmed the pinned controller base image includes `linux/arm64`.

These checks use x86_64 locally. Run the VM procedure above to verify native
ARM64 execution, your actual GitHub access, public DNS/TLS/Caddy and the full
`RUN_PROJECT_CHECKS` build. Temporary test data is separate from real Jenkins data.

References: [Jenkins Docker image](https://github.com/jenkinsci/docker),
[JCasC](https://plugins.jenkins.io/configuration-as-code/),
[Job DSL and JCasC](https://github.com/jenkinsci/job-dsl-plugin/blob/master/docs/JCasC.md).
