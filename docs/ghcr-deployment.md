# Tutorial: publish to GHCR and deploy with Jenkins

This tutorial extends the working [Jenkins setup](jenkins-setup.md) and
[SonarQube setup](sonarqube-setup.md) with image publishing and approved Oracle
deployment. Complete the first [manual deployment](manual-deployment.md) before
using automatic rollback; the script upgrades an existing installation.

```text
Checkout → Build/test → Analysis → Quality gate → Build/push images
→ Approve → SSH deployment → Local health checks → Success or rollback
```

## 1. Understand the files and build environment

| File | Purpose |
| --- | --- |
| [Jenkinsfile](../Jenkinsfile) | Captures the verified commit, checks it out after the gate, builds images with short commit tags and pushes them to GHCR. |
| [Agent Dockerfile](../infra/jenkins/agent/Dockerfile) | Copies Docker CLI and Buildx from `docker:29.9.2-cli`; retains Java 21, Node 24 and SonarScanner. |
| [Jenkins Compose](../infra/jenkins/docker-compose.yml) | Mounts the host socket into the agent and supplies its supplementary group and registry prefix. |
| [Agent verification](../infra/jenkins/agent-check.Jenkinsfile) | Checks Docker connectivity, Buildx and the default builder alongside existing tools. |
| [.env.example](../.env.example) | Documents the registry prefix and private GHCR credential inputs. |
| [Credential startup script](../infra/jenkins/controller/init.groovy.d/ghcr-credentials.groovy) | Creates or updates only `ghcr-push` from runtime environment values. |
| [Deployment script](../deploy/deploy.sh) | Pulls the whole stack, applies Compose, checks health and attempts rollback once. |
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

## 2. Update the VM checkout

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

## 3. Supply the GHCR credential

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

The controller applies these credential values when rebuilt in step 6.

The pipeline uses `--password-stdin` and disables command tracing before
registry authentication. A shell trap removes its temporary Docker
authentication directory on normal completion, failure or shell exit. A hard
host/container crash may bypass shell cleanup; do not reuse or archive temporary
authentication files. The VM's private-package pull credential remains separate.

## 4. Configure SSH deployment

Add these settings to the VM's private `.env`:

```dotenv
DEPLOY_HOST=host.docker.internal
DEPLOY_PORT=22
DEPLOY_DIR=/home/ubuntu/apps/ecom-platform
```

Use the actual directory, without spaces or shell metacharacters. The agent's
Compose configuration maps `host.docker.internal` to the Docker host. The SSH
account must own the deployment directory and be able to run Docker without
interactive sudo. Docker Compose, Bash and curl must be available on Ubuntu.

For private GHCR images, run `docker login ghcr.io -u yelmach` on the VM as this
account and enter a token with `read:packages`. Jenkins' publishing login is
separate from the VM account's pull access.

### Add the two SSH credentials once

On the VM, generate a dedicated key using a new filename:

```bash
mkdir -p ~/.ssh
chmod 700 ~/.ssh
ssh-keygen -t ed25519 -f ~/.ssh/jenkins-ecom-deploy -C jenkins-ecom-deploy -N ''
touch ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
{ printf '\nrestrict '; cat ~/.ssh/jenkins-ecom-deploy.pub; } >> ~/.ssh/authorized_keys
```

In **Manage Jenkins > Credentials > System > Global**, create:

| Kind | ID | Value |
| --- | --- | --- |
| SSH Username with private key | `oracle-deploy-key` | Username `ubuntu`, private key from `~/.ssh/jenkins-ecom-deploy`, empty passphrase for this example. |
| Secret file | `oracle-known-hosts` | The trusted server host-key file created below. |

Create the host-key file directly on the VM:

```bash
sudo ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub
{ printf 'host.docker.internal '; cat /etc/ssh/ssh_host_ed25519_key.pub; } > /tmp/ecom-known-hosts
```

Copy it to the computer running your browser, then upload it to Jenkins:

```bash
scp ubuntu@YOUR_VM_PUBLIC_IP:/tmp/ecom-known-hosts /tmp/ecom-known-hosts
```

For a custom SSH port, the host-key entry must use
`[host.docker.internal]:PORT`. Host-key verification stays enabled. Protect the
private key and never commit it. This key allows commands as the deployment
account, including Docker operations.

If Ubuntu's firewall blocks SSH from the Jenkins bridge subnet, allow that
internal connection according to your firewall policy. No new public port is
needed. Find the bridge subnet with:

```bash
docker network inspect jenkins_jenkins_network --format '{{range .IPAM.Config}}{{.Subnet}}{{end}}'
```

## 5. Initialize the current deployment before running CI

```text
.deploy/
  current/
    docker-compose.yml
    image-tag
  rollback/
    docker-compose.yml
    image-tag
```

Before the first automated deployment, save the currently working version once:

```bash
mkdir -p .deploy/current
cp docker-compose.yml .deploy/current/docker-compose.yml
printf '%s\n' YOUR_CURRENT_WORKING_TAG > .deploy/current/image-tag
```

Use the image tag already running on the VM. Later deployments update these
files automatically. Keep the Compose project name unchanged.

Each directory contains the Compose file and its release tag as plain text.
The script reads the tag file and supplies `IMAGE_TAG` to Compose as a process
environment variable. Do not put `IMAGE_TAG` in `.env`. Rollback uses the saved
Compose file and tag. It cannot restore old image content if that tag has been
overwritten. The publishing pipeline currently permits reruns with the same tag. Keep
`.deploy/` private and out of Git.

## 6. Rebuild Jenkins and start the pipeline

After the deployment settings, SSH credentials and current files are ready:

```bash
make jenkins-config
make jenkins-up
make jenkins-agent-up
make jenkins-agent-check
```

The agent must be online and the tool check must show Docker and SSH availability.
The controller startup script creates/updates `ghcr-push`; the two SSH credentials
and two SonarQube credentials remain in Jenkins Credentials.

Open **ecom-ci > Build Now > Console Output**. Jenkins builds and tests the
application, runs SonarQube analysis and requires an OK quality gate. Then it
checks out the same verified commit, builds all six images sequentially using
`docker build`, and pushes them to GHCR. The six images share the first 12
characters of the commit SHA; the full SHA is stored in image revision labels.

After publishing, the approval shows the tag, full commit and domain. Click
**Deploy** for the intended release. Approval waits up to 15 minutes without
holding an agent executor. Deployment has a 25-minute timeout. The **whole
pipeline has a 60-minute timeout**, including CI, approval and deployment;
remaining global time can shorten an individual stage's allowance.

Jenkins transfers only `docker-compose.yml` and `deploy/deploy.sh` from the
verified commit through SSH. It uses a temporary incoming directory and runs
the script on Ubuntu. The host Git checkout is unchanged. The host's existing
`.env` and keys are used; root `.env` and root Compose are not rewritten.

## 7. Understand deployment and rollback

1. Copy current Compose/tag into `rollback`, then put the incoming files in `current`.
2. Pull all images before changing containers. If pulling fails, restore the
   current files; the running stack stays unchanged.
3. Run the whole current Compose stack with `--no-build --pull never --remove-orphans`.
   Wait up to five minutes for container health, then retry local
   `/healthz` and `/products` checks for about a minute.
4. On failure, run the rollback Compose with its tag, check it and restore those
   files as current if recovery succeeds. On recovery failure, print
   `ROLLBACK FAILED` and stop. In that case `current` still holds the attempted
   release. Do not assume its metadata represents a healthy version. Jenkins
   fails either way when deployment fails.
5. On success, keep current as the active release. Public-only failures print a
   warning and leave the Jenkins build successful.

The script runs directly on Ubuntu. Image tags are supplied to Compose by
the script from the selected tag file.

There is no deployment lock. Jenkins prevents concurrent runs of `ecom-ci`;
do not run a separate manual deployment at the same time. Backups remain manual.
Container/configuration rollback cannot undo data writes or incompatible database
or storage upgrades. Take appropriate backups before those changes. Single-instance
updates may briefly interrupt service. A hard kill or VM/SSH failure can interrupt
recovery; this is not a transactional or zero-downtime deployment.

Run deployment directly when needed:

```bash
bash deploy/deploy.sh YOUR_12_CHARACTER_SHA "$PWD" "$PWD/docker-compose.yml"
```

To inspect the active stack directly:

```bash
IMAGE_TAG="$(cat .deploy/current/image-tag)" docker compose --env-file .env \
  --project-directory "$PWD" -f .deploy/current/docker-compose.yml ps
```

The Compose `${IMAGE_TAG}` placeholder stays: its value comes from the tag file,
not `.env`. Avoid pruning images needed by rollback. With tag-based recovery,
republishing a tag can prevent restoration of the original image content.

## 8. Check the result

A deployment success means Compose health and local `/healthz` and `/products`
passed. Public-only failures print a warning without failing the build. Open
`https://ecom.yelmach.com` and check normal application use after the first release.
Check GitHub Packages for all six images with the printed short SHA tag.

If a pull fails, the running containers are unchanged and the script restores
the current files. A container/local-check failure attempts rollback once. A
successful rollback still produces a failed Jenkins deployment. A failed rollback
prints `ROLLBACK FAILED`; no further recovery attempt is made.

Publishing is not transactional: a failed push can leave some images published,
but Jenkins does not deploy until all six pushes finish. Rerunning the same
commit can overwrite its tags because base images or downloads may change.
Tag-based rollback therefore does not guarantee the original image content.
It also cannot reverse data writes or database/storage-format changes.

Inspect Docker disk use with `docker system df`; keep images needed for rollback.
Do not prune volumes as cache cleanup. Backups are manual. The pipeline has no
deployment lock; avoid simultaneous manual runs. A killed SSH session or host
failure can interrupt recovery. Automatic deployment from `main`, GitHub triggers
and external notifications remain later work.
