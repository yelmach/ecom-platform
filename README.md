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

## Quick start (Docker backend + local frontend)

### Prerequisites
- Docker + Docker Compose
- OpenSSL
- Node.js + npm

### 1) Generate gateway TLS certs (one-time)
```bash
cd backend
mkdir -p certs
openssl req -x509 -nodes -newkey rsa:2048 -sha256 -days 825 \
  -keyout certs/gateway.key \
  -out certs/gateway.crt \
  -subj "/CN=localhost" \
  -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"
openssl pkcs12 -export \
  -out certs/gateway.p12 \
  -inkey certs/gateway.key \
  -in certs/gateway.crt \
  -name gateway \
  -passout pass:changeit
```

### 2) Run backend stack
```bash
cd backend
docker compose --env-file .env.docker up --build
```

### 3) Run frontend
```bash
cd frontend
npm install
npm run start:https
```

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

## Stop commands

Stop backend:
```bash
cd backend
docker compose down
```

Stop backend and remove volumes:
```bash
cd backend
docker compose down -v
```
