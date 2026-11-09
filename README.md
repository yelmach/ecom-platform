# ecom-platform

Microservices-based e-commerce platform with a Spring Boot backend and Angular frontend.

## Architecture

Backend services:

- `discovery-service` (Eureka service registry)
- `gateway-service` (single entrypoint, JWT auth, routing)
- `user-service` (auth + profile)
- `product-service` (catalog management)
- `media-service` (image upload + media metadata)

Supporting infrastructure:

- MongoDB
- MinIO (object storage)
- Kafka (product deletion events and asynchronous media cleanup)

Frontend:

- Angular app consuming gateway APIs

## Service docs

- [discovery-service](backend/discovery-service/README.md)
- [gateway-service](backend/gateway-service/README.md)
- [user-service](backend/user-service/README.md)
- [product-service](backend/product-service/README.md)
- [media-service](backend/media-service/README.md)

## Choose how to run the project

| Mode | What runs in Docker | What runs on your computer/VM |
| --- | --- | --- |
| Full Docker development | All application services and infrastructure, built from source | Browser |
| Local application development | MongoDB, MinIO, Kafka and Eureka | Four business/gateway services and Angular |
| All Java services locally | MongoDB, MinIO and Kafka | All five Java services and Angular |
| Manual production | Published application images and infrastructure | Caddy on Ubuntu |
| Jenkins deployment | Same production stack plus separate Jenkins/SonarQube stacks | Caddy on Ubuntu; deployment runs through SSH |

Run commands from the repository root unless a command changes directory.
Local development modes share ports, so stop one mode before starting another.
Jenkins and SonarQube are not required to develop the application locally.

## One-time local setup

Install Docker Engine with Compose, Git, Make and OpenSSL. For services running outside
Docker, also install Java 21 and Node 24/npm. Chrome/Chromium is needed
when running frontend tests. Production prerequisites are in the
[manual deployment tutorial](docs/manual-deployment.md).

For a fresh checkout, create the private environment files:

```bash
cp .env.example .env
cp .env.local.example .env.local
```

Preserve existing files when updating. Use different credentials on your laptop
and production VM.

| File | Purpose |
| --- | --- |
| `.env` | Docker service configuration and credentials; Jenkins/SonarQube settings when used |
| `.env.local` | Host-local endpoints for Java services and Angular |
| `.deploy/current/image-tag` | Active production image tag; never put `IMAGE_TAG` in `.env` |

For local services connecting to Docker infrastructure, MongoDB and MinIO
credentials in both environment files must agree. `.env.local` uses host
addresses such as `localhost:6210` and Kafka `localhost:6232`; `.env` uses Docker
service names. The committed templates provide these defaults.

For a new installation, generate JWT keys once:

```bash
mkdir -p backend/keys
chmod 700 backend/keys
(umask 077; openssl genpkey -algorithm RSA -out backend/keys/private.pem -pkeyopt rsa_keygen_bits:2048)
openssl pkey -in backend/keys/private.pem -pubout -out backend/keys/public.pem
```

Keep existing keys when updating; replacing them invalidates existing tokens.
Local development uses HTTP. Production HTTPS terminates at Caddy.

## Run with Docker for development

```bash
make dev-up
```

This builds the images from the checkout and starts all nine services. Open
`http://localhost:6100`. The default `make` command also starts this mode.
Jenkins/GHCR credentials and a production image tag are not required.

Inspect and stop the stack:

```bash
docker compose --env-file .env -f docker-compose.dev.yml ps
docker compose --env-file .env -f docker-compose.dev.yml logs --tail=100 -f
make dev-down
```

`dev-down` preserves data. `make dev-down-v` explicitly deletes development
volumes; use it only when intending to reset MongoDB, MinIO and Kafka data.

## Run application services locally

If the full Docker stack is running, run `make dev-down` first to avoid port
conflicts and duplicate service registrations. Start Docker infrastructure,
including Eureka:

```bash
make dev-infra-up
```

Run each command below from the repository root in its **own terminal**. Start
user/product before media so their Kafka topics are available:

```bash
cd backend/user-service && ./mvnw spring-boot:run
cd backend/product-service && ./mvnw spring-boot:run
cd backend/media-service && ./mvnw spring-boot:run
cd backend/gateway-service && ./mvnw spring-boot:run
```

In another terminal, start Angular:

```bash
cd frontend
npm ci
npm start
```

Java services load root `.env.local` relative to their service directories;
`npm start` also reads `.env.local`. Open `http://localhost:6100`.

Stop the local processes with Ctrl+C, then stop infrastructure:

```bash
make dev-infra-down
```

### Run Eureka locally too

If you want all five Java services in your IDE, start only the three backing
services in Docker instead of `make dev-infra-up`:

```bash
docker compose --env-file .env -f docker-compose.dev.yml up -d mongo minio kafka
```

Start discovery first in its own terminal:

```bash
cd backend/discovery-service && ./mvnw spring-boot:run
```

Then run the same four services and Angular commands above. Stop all local
processes before stopping Docker infrastructure with `make dev-infra-down`.
If switching from a full Docker stack, run `make dev-down` first so Docker
application services do not compete for ports or register duplicate instances.

Kafka topic auto-creation is disabled. User and product services create their
application topics at startup; Kafka must be ready before starting them.
`make kafka-up`, `make kafka-logs` and `make kafka-topics` provide individual
broker operations. See [Kafka events](docs/kafka-events.md) for event details.

## Main URLs

- Frontend: `http://localhost:6100`
- Gateway: `http://localhost:6200`
- Eureka: `http://localhost:6204`
- MinIO API: `http://localhost:6220`
- MinIO Console: `http://localhost:6221`

## Port allocation

| Component | Port |
| --- | ---: |
| Frontend / Caddy upstream | 6100 |
| Gateway | 6200 |
| User service | 6201 |
| Product service | 6202 |
| Media service | 6203 |
| Eureka | 6204 |
| MongoDB | 6210 |
| MinIO API / console | 6220 / 6221 |
| Kafka Docker / controller / host | 6230 / 6231 / 6232 |
| Jenkins / SonarQube | 6300 / 6310 |

Both Compose files read ports from `.env`; Angular reads frontend and gateway
ports from `.env.local` through `npm start`. Docker Nginx uses fixed internal ports (frontend 6100, gateway 6200).
Changing these internal ports requires editing `frontend/nginx/default.conf`
and rebuilding the frontend image; Compose reads the published frontend host port from `.env`. Caddy's public ports remain 80/443.

`CORS_ALLOWED_ORIGINS=http://localhost:6100` is the local frontend origin.
On the VM use `https://ecom.yelmach.com`. CORS is configured by the gateway's
`CorsConfig` and enabled by its security configuration. Media uses frontend URLs:
local `MEDIA_PUBLIC_BASE_URL=http://localhost:6100/ecom-media`, production
`MEDIA_PUBLIC_BASE_URL=https://ecom.yelmach.com/ecom-media`.

## Run production manually on Oracle

Follow [manual deployment](docs/manual-deployment.md) to prepare Ubuntu, private
`.env`, JWT keys, published ARM64 images, the Cloudflare record and Caddy.
Production uses the separate `ecom-prod` project and persistent volumes.
Only frontend port `127.0.0.1:6100` is published; Caddy serves
`https://ecom.yelmach.com`. Service traffic inside Docker remains HTTP.

For the first installation, save the production Compose file and the published
tag separately. Use the Compose file matching those published images:

```bash
mkdir -p .deploy/current
cp docker-compose.yml .deploy/current/docker-compose.yml
printf '%s\n' YOUR_PUBLISHED_IMAGE_TAG > .deploy/current/image-tag
```

Then run directly, without production Make targets:

```bash
export IMAGE_TAG="$(cat .deploy/current/image-tag)"
docker compose --env-file .env --project-directory "$PWD" -f .deploy/current/docker-compose.yml pull
docker compose --env-file .env --project-directory "$PWD" -f .deploy/current/docker-compose.yml up -d --no-build --wait --wait-timeout 300
docker compose --env-file .env --project-directory "$PWD" -f .deploy/current/docker-compose.yml ps
```

Check `https://ecom.yelmach.com`. For logs, use the same file/environment with
`logs --tail=100 -f`. To intentionally stop production, use `down` with that file
and tag; do not add `-v` unless destroying its data is intended.

For a later release with rollback checks, call the script from a tested checkout:

```bash
bash deploy/deploy.sh NEW_IMAGE_TAG "$PWD" "$PWD/docker-compose.yml"
```

The script uses `.deploy/current` and `.deploy/rollback`, each containing a
Compose file and `image-tag`. It keeps root `.env`, root Compose and keys
unchanged. Backups are manual; container rollback does not reverse data changes.

## Run CI and approved deployment with Jenkins

Follow these tutorials in order:

1. [Jenkins setup](docs/jenkins-setup.md): controller, agent and configuration in code.
2. [SonarQube setup](docs/sonarqube-setup.md): server, project, credentials and signed webhook.
3. [Manual deployment](docs/manual-deployment.md): establish the first working production version.
4. [GHCR deployment](docs/ghcr-deployment.md): registry credentials, SSH setup, publishing and deployment.

Images must be available for the initial manual deployment. The manual tutorial
includes native image build/push commands; use them if no release exists yet.

After configuration, start the tooling with:

```bash
make jenkins-up
make jenkins-agent-up
make sonarqube-up
make jenkins-agent-check
```

Open `https://jenkins.yelmach.com`, run **ecom-ci**, and approve the release after
publishing. SonarQube is available at `https://sonar.yelmach.com`.

```text
Checkout → Build/test → SonarQube → Quality gate → Build/push images
→ Manual approval → SSH → Apply Compose → Local health checks → Success or rollback
```

The pipeline currently uses the configured `deployment` branch and manual job
execution. Local failures trigger one rollback attempt. Public-only check failures
print warnings without failing an otherwise healthy deployment. GitHub triggers,
automatic `main` deployment and external notifications are later work.

Stop SonarQube before removing the Jenkins network:

```bash
make sonarqube-down
make jenkins-down
```

These commands retain tooling data volumes and do not stop the separate app stack.

## Build and test without starting the application

Prerequisites: Java 21, Node.js (Angular accepts `^20.19.0`, `^22.12.0`, or
`>=24.0.0`; this baseline was checked with Node 24), npm, and Chrome/Chromium.
The Maven wrappers download Maven when needed. Dependency installation needs
network access on a fresh machine. Tests start local HTTP servers and a headless
browser, so the test runner must permit local socket binding. They do not require
a running MongoDB, MinIO, Kafka, or Eureka stack.

From the repository root:

```bash
make verify
```

Or run `make verify-backend` and `make verify-frontend` separately. Backend
verification runs `clean verify` in each of the five Maven projects, stopping
on failure. Frontend verification installs the locked dependencies, builds the
production app, and runs headless tests with coverage, sequentially. If Chrome
is not detected, set `CHROME_BIN` to your installed browser executable.

Generated reports:

- Backend JUnit: `backend/<service>/target/surefire-reports/TEST-*.xml`
- Backend coverage: `backend/<service>/target/site/jacoco/jacoco.xml` and `index.html`
- Frontend JUnit: `frontend/reports/junit/frontend-tests.xml`
- Frontend coverage: `frontend/coverage/frontend/lcov.info` and `index.html`

Discovery service currently has no tests, so it builds without a test or coverage
report. Passing these checks establishes a build/test baseline; it does not
verify the running Docker stack or Oracle deployment.

## API flow notes

### Auth and profile avatar

1. `POST /auth/register`
2. Optional avatar upload: `POST /media/profile`
3. Link avatar to profile: `PATCH /users/me` with `avatarMediaId`

### Product and images

1. Create product: `POST /products` with `mediaIds: []`
2. Upload images: `POST /media/images` with `productId` + `files[]`
3. Save image IDs on product: `PUT /products/{id}` with `mediaIds`

User and product discover media service through Eureka with explicit HTTP timeouts; `MEDIA_SERVICE_BASE_URL` is no longer needed. Avatar uploads preserve previous images until a profile change publishes `USER_AVATAR_CHANGED` to clean up the previous avatar. Product image metadata is available at `GET /media/images/{productId}`; the browser loads returned object URLs through Gateway → MinIO.

Product uploads verify ownership through an internal product-service endpoint before validating files. Media discovers product service through Eureka using Spring Cloud LoadBalancer; a fixed `PRODUCT_SERVICE_BASE_URL` is no longer required. Media accepts JPEG and PNG using Apache Tika content type detection and ImageIO decoding checks; GIF and WebP are unsupported. Deploy product service before media service when introducing this ownership check. See the media-service README for upload failure and cleanup behavior.
