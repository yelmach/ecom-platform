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

- `backend/discovery-service/README.md`
- `backend/gateway-service/README.md`
- `backend/user-service/README.md`
- `backend/product-service/README.md`
- `backend/media-service/README.md`

## Config files

- `.env` contains Docker-network settings for both Compose workflows.
- `.env.local` contains host-local settings for manually launched services and Angular.
- `.env.example` and `.env.local.example` are the committed templates.
- Use different `.env` values on your laptop and VM; never reuse development credentials in production.
- Mongo logical DB isolation uses:
  - `MONGO_USER_DATABASE`
  - `MONGO_PRODUCT_DATABASE`
  - `MONGO_MEDIA_DATABASE`
    on the same MongoDB instance.

## One-time setup

Copy the environment templates before starting the application:

```bash
cp .env.example .env
cp .env.local.example .env.local
```

For an existing checkout, preserve your current credentials and Kafka cluster ID.
Docker infrastructure credentials in `.env` must agree with `.env.local` when
running services from the IDE. No self-signed HTTPS certificates are needed.
Public production HTTPS terminates at Caddy; internal traffic uses HTTP.
See [Oracle deployment](docs/oracle-deployment.md) for VM settings.

### Generate JWT RSA keys (user-service signs, gateway verifies)

```bash
mkdir -p backend/keys
openssl genpkey -algorithm RSA -out backend/keys/private.pem -pkeyopt rsa_keygen_bits:2048
openssl pkey -in backend/keys/private.pem -pubout -out backend/keys/public.pem
```

## Migrating an existing local stack

After updating, run `make dev-up` to rebuild the frontend and recreate containers
with the new ports; keep existing volumes. Open `http://localhost:6100`.
For manual development, stop Docker application containers first (`make dev-down`),
then use `make dev-infra-up` and launch the Java services and `npm start`.
Full Docker mode and manual mode share frontend/Eureka ports, so run one mode at a time.
Do not use `dev-down-v` to migrate ports: it deletes data. Existing TLS files may
remain on disk but are no longer mounted or required. JWT keys remain required.

## Launch modes

### Full development stack (Dockerfile builds)

```bash
make dev-up
```

Stop the stack with `make dev-down`, or use `make dev-down-v` to also remove volumes.

The development Compose file uses the pinned community build `pgsty/minio:RELEASE.2026-08-04T00-00-00Z`.
The previously configured `quay.io/minio/minio:latest` image is no longer publicly pullable.
This replacement is published by [PGSTY](https://github.com/pgsty/silo/tree/minio), an independent MinIO fork; upstream MinIO now documents [source-only distribution](https://github.com/minio/minio#source-only-distribution).
MinIO retains its service name and data volume; its API/console now use ports 6220/6221.

### Infrastructure-only development

1. Start infra (`mongo`, `minio`, `discovery-service`, `kafka`) from repo root:

```bash
make dev-infra-up
```

To start only Kafka, run `make kafka-up`. Inspect it with `make kafka-logs`
or list topics with `make kafka-topics`.

Kafka uses the official `apache/kafka:4.3.1` image in single-node KRaft mode,
with persistent storage and a health check. Docker services connect to
`kafka:6230`; services running from the IDE connect to `localhost:6232`.
Set `KAFKA_BOOTSTRAP_SERVERS` accordingly in `.env` and `.env.local` when
adding the service producers and consumers. Topic auto-creation is disabled.
Product service creates `product.events` and User service creates
`user.events` at application startup using Spring Kafka `NewTopic` beans.
Existing topics are reused. Start Kafka before either application;
startup fails if Kafka is unavailable. `make kafka-up` starts only the broker.
See [Kafka events](docs/kafka-events.md) for the event payloads.

Product deletion, detached-image cleanup, and avatar replacement/clearing cleanup are implemented.
The `kafka-data` volume preserves broker state; `make dev-down-v` removes it
along with the other development volumes.

2. Run backend services manually (each in a separate terminal). Start product
and user services first so they create the topics consumed by media service:

```bash
cd backend/product-service && ./mvnw spring-boot:run
cd backend/user-service && ./mvnw spring-boot:run
cd backend/media-service && ./mvnw spring-boot:run
cd backend/gateway-service && ./mvnw spring-boot:run
```

3. Run frontend:

```bash
cd frontend
npm ci
npm start
```

4. Stop infra:

```bash
make dev-infra-down
```

In this mode, manually launched backend services load host-local values from root `.env.local`.
Frontend in dev mode still runs manually with `npm start`.

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
| Future Jenkins / SonarQube | 6300 / 6310 |

Both Compose files read ports from `.env`; Angular reads frontend and gateway
ports from `.env.local` through `npm start`. Docker Nginx uses fixed internal ports (frontend 6100, gateway 6200).
Changing these internal ports requires editing `frontend/nginx/default.conf`
and rebuilding the frontend image; Compose reads the published frontend host port from `.env`. The two future tools are not
installed yet. Caddy's public ports remain 80/443.

`CORS_ALLOWED_ORIGINS=http://localhost:6100` is the local frontend origin.
On the VM use `https://ecom.yelmach.com`. CORS is configured by the gateway's
`CorsConfig` and enabled by its security configuration. Media uses frontend URLs:
local `MEDIA_PUBLIC_BASE_URL=http://localhost:6100/ecom-media`, production
`MEDIA_PUBLIC_BASE_URL=https://ecom.yelmach.com/ecom-media`.

## Production on Oracle

`docker-compose.yml` uses `.env` and pulls versioned GHCR images. Only the
frontend is published, on `127.0.0.1:6100`, for host-installed Caddy to proxy
`https://ecom.yelmach.com`. HTTP is used on loopback and the Docker network;
public TLS terminates at Caddy. This setup does not encrypt internal service traffic.
Production uses the separate `ecom-prod` Compose project and persistent volumes.

See [Oracle deployment](docs/oracle-deployment.md) for environment setup, JWT
keys, GHCR access, Caddy configuration, and verification. Images must be
published before the first deployment. Jenkins controller/agent setup is documented
in [infra/jenkins](infra/jenkins/README.md); SonarQube and the delivery pipeline
are not configured yet.

## Build and test baseline

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
