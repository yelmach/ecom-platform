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

Frontend:
- Angular app consuming gateway APIs

## Service docs

- `backend/discovery-service/README.md`
- `backend/gateway-service/README.md`
- `backend/user-service/README.md`
- `backend/product-service/README.md`
- `backend/media-service/README.md`
- `frontend/README.md`

## Config files

- `backend/.env` for manual local service runs (dev mode).
- `backend/docker.env` for Docker Compose runs (prod and dev infra).
- `backend/.env.example` and `backend/docker.env.example` as templates.

## Prerequisites

- Docker + Docker Compose
- OpenSSL
- Java 17
- Node.js + npm

## One-time setup

### 1) Generate gateway TLS certs (for HTTPS gateway)
```bash
mkdir -p backend/certs
openssl req -x509 -nodes -newkey rsa:2048 -sha256 -days 825 \
  -keyout backend/certs/gateway.key \
  -out backend/certs/gateway.crt \
  -subj "/CN=localhost" \
  -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"
openssl pkcs12 -export \
  -out backend/certs/gateway.p12 \
  -inkey backend/certs/gateway.key \
  -in backend/certs/gateway.crt \
  -name gateway \
  -passout pass:changeit
```

### 2) Generate JWT RSA keys (user-service signs, gateway verifies)
```bash
mkdir -p backend/keys
openssl genpkey -algorithm RSA -out backend/keys/private.pem -pkeyopt rsa_keygen_bits:2048
openssl pkey -in backend/keys/private.pem -pubout -out backend/keys/public.pem
```

## Launch modes

### Prod mode (all backend + infra in Docker)

From repo root:
```bash
make prod-up
```

Stop:
```bash
make prod-down
```

Stop and remove volumes:
```bash
make prod-down-v
```

Direct Docker Compose equivalent:
```bash
docker compose --env-file backend/docker.env -f docker-compose.yml up --build -d
```

### Dev mode (infra in Docker, services/frontend manual)

1. Start infra (`mongo`, `minio`, `discovery-service`) from repo root:
```bash
make dev-infra-up
```

2. Run backend services manually (each in a separate terminal):
```bash
cd backend/media-service && ./mvnw spring-boot:run
cd backend/user-service && ./mvnw spring-boot:run
cd backend/product-service && ./mvnw spring-boot:run
cd backend/gateway-service && ./mvnw spring-boot:run
```

3. Run frontend:
```bash
cd frontend
npm install
npm run start:https
```

4. Stop infra:
```bash
make dev-infra-down
```

Note: in dev mode, backend services load local env values from `backend/.env`.

## Main URLs

- Frontend: `https://localhost:4200`
- Gateway: `https://localhost:8443`
- Eureka: `http://localhost:8761`
- MinIO API: `http://localhost:9000`
- MinIO Console: `http://localhost:9001`

## API flow notes

### Auth and profile avatar
1. `POST /auth/register`
2. Optional avatar upload: `POST /media/profile`
3. Link avatar to profile: `PATCH /users/me` with `avatarMediaId`

### Product and images
1. Create product: `POST /products` with `mediaIds: []`
2. Upload images: `POST /media/images` with `productId` + `files[]`
3. Save image IDs on product: `PUT /products/{id}` with `mediaIds`

## Compatibility note

- Canonical compose files are at repo root: `docker-compose.yml` and `docker-compose.dev.yml`.
- `backend/docker-compose.yml` is kept for compatibility with existing commands.
