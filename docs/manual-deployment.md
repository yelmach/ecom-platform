# Tutorial: deploy manually on Oracle Cloud

Target: Ubuntu 24 on Oracle Ampere A1 (ARM64), with Caddy installed directly
on Ubuntu. Public URL: `https://ecom.yelmach.com`.

## Configuration layout

| Workflow | Compose file | Environment |
| --- | --- | --- |
| Initial production setup | `docker-compose.yml` | `.env` from `.env.example` |
| Production after automated deployment | `.deploy/current/docker-compose.yml` | Root `.env` plus `.deploy/current/image-tag` |
| Docker development | `docker-compose.dev.yml` | `.env` from `.env.example` |
| IDE development | Infrastructure from development Compose | `.env.local` from `.env.local.example` |

Only `.env` (Docker) and `.env.local` (manual local services) are used, with
matching committed `.example` templates. The default `make` starts Docker
development. Both Compose files use `.env`; laptop and VM copies have different
credentials and browser URLs. Keep existing data volumes and JWT keys when
updating a deployment. No local certificate is required.

Production has the distinct Compose project name `ecom-prod`; its volumes are
`ecom-prod_mongo-data`, `ecom-prod_minio-data`, and `ecom-prod_kafka-data`.
It starts with its own data, not a copy of the development database or uploads.

## 1. Prepare the VM checkout and environment

Install Docker Engine, the Docker Compose plugin, Git, Make, and OpenSSL if
they are missing. Use an account authorized to run Docker. Clone this repository
on the VM and run subsequent commands from its root directory.

```bash
cp .env.example .env
chmod 600 .env
```

Edit `.env` and fill in:

- `GHCR_PREFIX`: for example, `ghcr.io/yelmach`.
- `MONGO_PASSWORD` and `MINIO_ROOT_PASSWORD`: separate generated passwords.
  Run `openssl rand -hex 32` twice. Hex passwords avoid Mongo URI escaping issues.
- `KAFKA_CLUSTER_ID`: generate once using the command in [Kafka cluster ID and listeners](kafka-events.md#kafka-cluster-id-and-listeners).

On the VM set the public domain in `.env`:

```dotenv
APP_DOMAIN=ecom.yelmach.com
```

Production Compose derives CORS and media URLs as `https://${APP_DOMAIN}` and
`https://${APP_DOMAIN}/ecom-media`. Development Compose uses the local HTTP
URL variables from `.env`. Use the port allocation in
the root README; `FRONTEND_PORT=6100` must match Caddy's upstream. Generate your
own production passwords rather than using the development template credentials.

Keep the port defaults unless you also adjust the Caddy configuration.
Never publish `.env`, tokens, or private keys. Do not share full `docker compose
config` output because it includes resolved secrets; use Compose `config --quiet` with the tag-file value supplied.

## 2. Generate production JWT keys

On a fresh VM checkout:

```bash
mkdir -p backend/keys
chmod 700 backend/keys
```

If keys already exist for this deployment, keep them. Otherwise generate them:

```bash
(umask 077; openssl genpkey -algorithm RSA -out backend/keys/private.pem -pkeyopt rsa_keygen_bits:2048)
openssl pkey -in backend/keys/private.pem -pubout -out backend/keys/public.pem
```

Back up the keys securely. Replacing the key pair invalidates existing JWTs.
Production mounts the private key only into user-service, and the public key
into user-service and gateway-service. Missing files cause startup to fail rather
than silently creating directories. No gateway TLS keystore is required in this
mode: Caddy terminates public HTTPS. Traffic behind it uses loopback/private
Docker HTTP, not end-to-end TLS.

## 3. Publish images and authorize GHCR pulls

Before deployment, build and publish these six images with ARM64 support:

```text
<GHCR_PREFIX>/ecom-discovery-service:<IMAGE_TAG>
<GHCR_PREFIX>/ecom-gateway-service:<IMAGE_TAG>
<GHCR_PREFIX>/ecom-user-service:<IMAGE_TAG>
<GHCR_PREFIX>/ecom-product-service:<IMAGE_TAG>
<GHCR_PREFIX>/ecom-media-service:<IMAGE_TAG>
<GHCR_PREFIX>/ecom-frontend:<IMAGE_TAG>
```

If the images already exist, skip building and select their published tag in
step 5. To build and publish manually on the ARM64 VM, log in to GHCR with a
token that has `write:packages`, then run from the repository root:

```bash
docker login ghcr.io -u yelmach
registry_prefix=ghcr.io/yelmach
image_tag=$(git rev-parse --short=12 HEAD)
for component in discovery-service gateway-service user-service product-service media-service frontend; do
    context="backend/$component"
    if [ "$component" = frontend ]; then context=frontend; fi
    docker build -t "$registry_prefix/ecom-$component:$image_tag" "$context"
    docker push "$registry_prefix/ecom-$component:$image_tag"
done
printf 'Published tag: %s\n' "$image_tag"
```

Use a registry prefix matching `GHCR_PREFIX` in `.env`. Docker builds for its
host architecture; this command on an x86 machine does not produce ARM64 images.
Use a tag corresponding to a committed source revision. For publishing through
Jenkins instead, follow [GHCR deployment](ghcr-deployment.md).

Each image must contain `linux/arm64`; include `linux/amd64` too if you want the
same release to run on x86 machines. Infrastructure images must also support the VM architecture.

The six images must exist before deployment. Tags can be overwritten on reruns.
The [GHCR deployment tutorial](ghcr-deployment.md) explains publishing through
Jenkins and deploying with approval, SSH, health checks and rollback.

For private GHCR packages, create a GitHub personal access token (classic) with
`read:packages`, granting access to the packages (and organization SSO if
required). On the VM, log in as the same OS user that will run deployments:

```bash
docker login ghcr.io -u YOUR_GITHUB_USERNAME
```

Enter the token at the password prompt. Public images can be pulled anonymously.
Jenkins publishing credentials need `write:packages`; keep those separate
from the VM's pull credentials.

## 4. Configure DNS and existing Caddy

In Cloudflare, ensure an `A` record named `ecom` points to the VM's public IPv4
address. Only add an `AAAA` record if IPv6 actually reaches this VM. Ensure
Oracle ingress rules and Ubuntu firewall rules allow TCP 80/443 to Caddy.
There is no need to open port 6100 or any database/service port publicly.

Add this block to the existing
`/etc/caddy/Caddyfile`, preserving its other sites:

```caddyfile
ecom.yelmach.com {
    encode zstd gzip
    reverse_proxy 127.0.0.1:6100
}
```

Validate the complete Caddy configuration, then reload it:

```bash
sudo caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile
sudo systemctl reload caddy
```

Caddy manages the public certificate. If Cloudflare's proxy is enabled, use
Full (strict) once Caddy has a valid origin certificate. DNS-only mode also
works. Do not switch to Flexible, which uses HTTP to the origin.

## 5. Deploy once the images are published

Save the published image tag separately; it does not belong in `.env`:

```bash
mkdir -p .deploy/current
cp docker-compose.yml .deploy/current/docker-compose.yml
printf '%s\n' YOUR_PUBLISHED_IMAGE_TAG > .deploy/current/image-tag
```

Use the tag file for Compose commands:

```bash
export IMAGE_TAG="$(cat .deploy/current/image-tag)"
docker compose --env-file .env --project-directory "$PWD" -f .deploy/current/docker-compose.yml config --quiet
docker compose --env-file .env --project-directory "$PWD" -f .deploy/current/docker-compose.yml pull
docker compose --env-file .env --project-directory "$PWD" -f .deploy/current/docker-compose.yml up -d --no-build --wait --wait-timeout 300
```

After the first manual deployment is working, later releases can use the script
or Jenkins. The script requires the `current` files prepared above:

```bash
bash deploy/deploy.sh NEW_TAG "$PWD" "$PWD/docker-compose.yml"
```

The script supplies the new tag from its tag file and handles rollback; it never reads a tag from `.env`.

Only the frontend publishes a host port: `127.0.0.1:6100` maps to container
port 6100. Caddy routes all application paths to it, and Nginx forwards API and
media-object requests to gateway HTTP port 6200. Responses use public media URLs
under `https://ecom.yelmach.com/ecom-media`.

Verify on the VM:

```bash
curl -fsS http://127.0.0.1:6100/healthz
curl -fsS http://127.0.0.1:6100/products
curl -fsS https://ecom.yelmach.com/healthz
curl -fsS https://ecom.yelmach.com/products
```

The health endpoint checks frontend availability; product listing also exercises
the gateway, Eureka routing, product service, and database. Open the public site
and verify registration/login, product CRUD, images, and avatars as you did locally.
Use Compose `logs` with the active file and tag for troubleshooting. A 502/503 immediately after startup may
need a brief retry while Eureka's registry caches populate; persistent failures
need investigation.

## 6. Inspect and stop the stack

Supply the active tag for each operation from the repository root:

```bash
IMAGE_TAG="$(cat .deploy/current/image-tag)" docker compose --env-file .env --project-directory "$PWD" -f .deploy/current/docker-compose.yml ps
IMAGE_TAG="$(cat .deploy/current/image-tag)" docker compose --env-file .env --project-directory "$PWD" -f .deploy/current/docker-compose.yml logs --tail=100 -f
IMAGE_TAG="$(cat .deploy/current/image-tag)" docker compose --env-file .env --project-directory "$PWD" -f .deploy/current/docker-compose.yml down
```

`down` stops the stack; use it only when intending to stop production. A later
`up` with the same file and tag starts it again.

- Use Compose `down` with the active file and tag to stop without deleting volumes. Do not run
  production `down -v`; it destroys stored data. Back up MongoDB, MinIO, `.env`,
  and keys before destructive changes or upgrades.
- Keep MongoDB credentials and Kafka cluster ID consistent with their existing
  volumes. Changing an environment password does not rotate existing Mongo users.
- This is a single-VM demo deployment, with no redundancy or zero-downtime release
  guarantee. Existing application Kafka delivery limitations still apply.
- Nginx configuration is baked into the frontend image. If its internal frontend
  or gateway ports change, update the config and rebuild/publish the image.
  Keep deployment configuration at the same revision as the release images.
- Deployment is run directly through the script or Jenkins; see
  [automated deployment](ghcr-deployment.md). External notifications remain
  separate work.

References: [Caddy reverse proxy](https://caddyserver.com/docs/caddyfile/directives/reverse_proxy),
[GitHub Container Registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry),
[Compose networking](https://docs.docker.com/compose/how-tos/networking/),
[Cloudflare Full (strict)](https://developers.cloudflare.com/ssl/origin-configuration/ssl-modes/full-strict/).
