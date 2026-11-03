# Tutorial: Publish ARM64 images from Jenkins to GHCR

The manually triggered `ecom-ci` job checks out code, builds/tests it, submits
SonarQube analysis and waits for its quality gate. Only a passing gate allows
the six application images to be built and published. Deployment remains manual.

## 1. Files and decisions

| File | Purpose |
| --- | --- |
| [Jenkinsfile](../Jenkinsfile) | Captures the verified commit, checks it out after the gate, builds images with short commit tags and pushes them to GHCR. |
| [Agent Dockerfile](../infra/jenkins/agent/Dockerfile) | Copies Docker CLI and Buildx from `docker:29.9.2-cli`; retains Java 21, Node 24 and SonarScanner. |
| [Jenkins Compose](../infra/jenkins/docker-compose.yml) | Mounts the host socket into the agent and supplies its supplementary group and registry prefix. |
| [Agent verification](../infra/jenkins/agent-check.Jenkinsfile) | Checks Docker connectivity, Buildx and the default builder alongside existing tools. |
| [.env.example](../.env.example) | Documents the registry prefix and private GHCR credential inputs. |
| [Credential startup script](../infra/jenkins/controller/init.groovy.d/02-ghcr-credentials.groovy) | Creates or updates only `ghcr-push` from runtime environment values. |
| [Makefile](../Makefile) | Detects the host Docker socket group for Jenkins Compose commands. |
| [Frontend Dockerfile](../frontend/Dockerfile) | Builds using Node 24 on Debian, matching the CI runtime family. |

Docker runs on the VM; the agent contains only its client. There is no extra
daemon, privileged agent or Docker TCP port. The agent remains the `jenkins`
user and receives the socket's group as a supplementary group. The controller
has no socket mount. Docker daemon access nevertheless gives pipeline code
control over the host, including production containers and files accessible
through Docker. Run only trusted code on this agent; do not add untrusted PR
builds to it. See [Docker security](https://docs.docker.com/engine/security/).

`docker build` uses the host engine and retains its build cache. The agent
keeps the Buildx plugin because modern Docker uses it for BuildKit builds.
Images build sequentially to reduce contention with Jenkins, SonarQube and the
application on the 4-OCPU VM. Each Dockerfile still builds its own application;
this repeats compilation after `make verify`, but keeps local Docker builds
self-contained. Optimizing artifact reuse is separate work.

Images use the Docker host's default architecture, which is ARM64 on the
Oracle VM. There is no explicit platform flag, architecture guard or QEMU
setup. Running the same pipeline against an x86 Docker host publishes x86
images. Local development continues through development Compose.

## 2. Configure the VM

Commit and push these changes to the configured `deployment` branch. Update
the existing VM checkout with `git pull --ff-only origin deployment`.
Preserve the existing `.env`, Jenkins agent secret, credentials and volumes.

Make detects the socket's numeric group ID using
`stat -c '%g' /var/run/docker.sock` and supplies `DOCKER_GID` to Compose for
every Jenkins command. No group ID needs to be stored in `.env`. The agent uses
that group to access the socket as the `jenkins` user. If the socket group
changes, run `make jenkins-agent-up` to recreate the agent with the new group.
Do not make the socket world-writable.

Use the Make commands below for normal operations. If running Jenkins Compose
directly, supply the group explicitly:

```bash
DOCKER_GID="$(stat -c '%g' /var/run/docker.sock)" \
  docker compose --env-file .env -f infra/jenkins/docker-compose.yml config --quiet
```

## 3. Supply the GHCR credential and start Jenkins

On GitHub, create a personal access token **classic** with `write:packages`.
That scope permits image upload/download; `delete:packages` is not needed.
Authorize organization SSO if applicable. For existing packages, the token's
account must also have write permission. See [GitHub's registry documentation](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry).

Add these values to the VM's private root `.env`, preserving other settings:

```dotenv
GHCR_PREFIX=ghcr.io/yelmach
GHCR_USERNAME=yelmach
GHCR_TOKEN='REPLACE_WITH_YOUR_GITHUB_TOKEN'
```

Keep `.env` private (`chmod 600 .env`) and out of Git. Compose passes the username
and token to the controller, where a startup script creates a Global
username/password credential with ID `ghcr-push`. The agent receives the credential only through
the pipeline's `withCredentials` block. No dashboard credential creation is
needed. Runtime container environment values are accessible to Docker
administrators; this follows the existing administrator-password setup.

With both values supplied, the startup script creates or updates only
`ghcr-push`, preserving the SonarQube credentials. If either value is empty,
it skips the update and preserves any existing credential. A fresh installation
needs both values before publishing. To rotate the token, update `.env` and run
`make jenkins-up`. Dashboard edits to `ghcr-push` are replaced on startup when
both environment values are supplied. SonarQube credentials continue to be
managed in Jenkins as documented separately.

```bash
make jenkins-config
make jenkins-up
make jenkins-agent-up
make jenkins-agent-check
```

The controller rebuild installs and runs the credential startup script. Its
`.override` destination refreshes the script in Jenkins home when the controller
image changes. The agent receives the detected socket group. Expect Docker client/server versions and Buildx information. Confirm the
agent is online in Jenkins. A Docker permission error usually means the agent
needs recreation or the host socket is unavailable.

The pipeline uses `--password-stdin` and disables command tracing before
registry authentication. A shell trap removes its temporary Docker
authentication directory on normal completion, failure or shell exit. A hard
host/container crash may bypass shell cleanup; do not reuse or archive temporary
authentication files. The VM's private-package pull credential remains separate.

## 4. Run the pipeline

Open **ecom-ci > Build Now > Console Output**. After the existing verification
and quality gate, expect:

1. **Checkout verified commit:** a clean workspace checks out the captured full
   SHA, even if the branch has moved while analysis was processing. An unavailable
   commit fails the stage rather than substituting another revision.
2. **Build images:** builds all six locally using `docker build`. Images
   carry repository and full commit labels. Build contexts exclude `.env` files,
   PEM keys, generated frontend reports and other existing ignored outputs.
3. **Push images:** authenticates and pushes each image. A login or push
   failure stops the stage; authentication files are cleaned up on shell exit.

All images use the first 12 characters of the verified Git SHA as their tag.
Jenkins prints `Image tag: <short-sha>` in the console. The full SHA is retained
for exact checkout and the image revision label:

```text
ghcr.io/yelmach/ecom-discovery-service:<short-sha>
ghcr.io/yelmach/ecom-gateway-service:<short-sha>
ghcr.io/yelmach/ecom-user-service:<short-sha>
ghcr.io/yelmach/ecom-product-service:<short-sha>
ghcr.io/yelmach/ecom-media-service:<short-sha>
ghcr.io/yelmach/ecom-frontend:<short-sha>
```

The overall timeout is 60 minutes, including agent waits and cold image builds.
The quality-gate timeout stays ten minutes. Concurrent runs of this job remain
disabled. Images are rebuilt on reruns, so the same commit tag is not immutable;
base images and package downloads may have changed.

## 5. Check publication and deploy manually

A successful pipeline means all six `docker push` commands succeeded. It does
not pull the images back, compare digests or archive a release record.
Check the six packages and their short SHA tags in GitHub Packages.

All six builds must pass before any push starts. Pushes are not transactional:
a registry failure may leave some images published. Fix the problem and rerun
CI; deploy only after all six pushes succeed. No automatic package deletion
is performed.

Fresh GHCR packages are private by default. Keep them private with authorized
pull access, or explicitly change package visibility in GitHub. The source
label links packages to the repository; verify the connection and access in
GitHub after the first publication.

For manual deployment, use the printed 12-character tag as `IMAGE_TAG` in the VM `.env`,
then follow [Oracle deployment](oracle-deployment.md). CI does not change the
production `.env`, run production Compose or restart the application.

## 6. Verify and operate

Check these scenarios on the VM:

| Scenario | Expected result |
| --- | --- |
| Passing CI and gate | All six images pushed with the same 12-character tag. |
| Failed tests or gate | Image stages skipped. |
| Failed image build | No publishing begins. |
| Invalid GHCR token | Login/push fails; pipeline fails. |
| Failed push | Job fails; some images may already be published. |
| Second successful run | Docker reuses applicable cached layers. |

Use a disposable job/registry namespace for deliberate failure tests. Do not
weaken the real quality gate or revoke shared production credentials for tests.
The registry tests need real credentials and native Oracle execution; local
configuration and mocked command checks do not establish that they pass.

The host retains local image tags and build cache to speed later runs. Monitor
disk usage with `docker system df`. Cleanup is a host operation to schedule
deliberately; the pipeline does not run broad pruning on the shared production
engine. Never use volume pruning as an image-cache cleanup method.

Automatic deployment, health checks, rollback, GitHub triggers and notifications
are subsequent phases.
