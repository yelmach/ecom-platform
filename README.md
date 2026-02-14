# ecom-platform

## Media Integration (User + Product + Frontend)

### User avatar reference
- User profile now stores `avatarMediaId` (not `avatarUrl`).
- `PATCH /users/me` accepts `avatarMediaId`:
  - set `avatarMediaId` to a media ID to link avatar
  - set `avatarMediaId` to `null` to remove avatar reference
- Register flow is two-step:
1. `POST /auth/register`
2. Optional `POST /media/profile` with avatar file
3. `PATCH /users/me` with `{ "avatarMediaId": "<uploadedAvatarId>" }`

### Product media reference
- Product keeps `mediaIds` as canonical image references.
- Product creation requires empty `mediaIds`:
1. `POST /products` with `mediaIds: []`
2. `POST /media/images` with `productId` + files
3. `PUT /products/{id}` with uploaded media IDs
- Product update validates submitted `mediaIds` against `GET /media/images/{productId}` in media-service.

### Media-service usage
- Product images:
  - `POST /media/images` (`productId`, `files[]`, max 5 files)
  - `GET /media/images/{productId}`
- Profile avatar:
  - `POST /media/profile` (`file`)
  - `GET /media/profile/{userId}`

### Frontend behavior
- Registration supports optional avatar upload.
- Profile dialog supports avatar upload and avatar removal (`avatarMediaId: null`).
- Product create/edit uploads images through media-service and persists returned IDs.
- Shop/seller/product details resolve image URLs via `GET /media/images/{productId}`.

## Backend Docker Runbook

### Prerequisites
- Docker + Docker Compose
- OpenSSL (for local self-signed cert generation)

### Generate gateway TLS certificates (one-time)
1. `cd backend`
2. `mkdir -p certs`
3. `openssl req -x509 -nodes -newkey rsa:2048 -sha256 -days 825 -keyout certs/gateway.key -out certs/gateway.crt -subj "/CN=localhost" -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"`
4. `openssl pkcs12 -export -out certs/gateway.p12 -inkey certs/gateway.key -in certs/gateway.crt -name gateway -passout pass:changeit`

### Run backend stack
1. `cd backend`
2. `docker compose --env-file .env.docker up --build`

### Stop backend stack
1. `cd backend`
2. `docker compose down`

### Stop and remove data volumes
1. `cd backend`
2. `docker compose down -v`

### Run frontend over HTTPS (ng serve)
1. `cd frontend`
2. `npm install`
3. `npm run start:https`

### Expected reachable URLs
- Frontend: `https://localhost:4200`
- Gateway: `https://localhost:8443`
- MinIO API: `http://localhost:9000`
- MinIO Console: `http://localhost:9001`
