# Gateway Service

## Purpose
`gateway-service` is the single backend entry point. It routes requests to internal services and enforces JWT-based access rules.

## Main routes
- `/auth/**`, `/users/**` -> `USER-SERVICE`
- `/products/**` -> `PRODUCT-SERVICE`
- `/media/**` -> `MEDIA-SERVICE`
- `/ecom-media/**` -> MinIO object endpoint (`http://minio:9000`)

## Auth behavior
- Public: `POST /auth/login`, `POST /auth/register`
- Public read: `GET /products/**`, `GET /media/images/**`, `GET /media/profile/**`
- Seller-only writes: non-`GET` on `/products/**` and `/media/images/**`
- On valid JWT, forwards:
  - `X-User-Id`
  - `X-User-Email`
  - `X-User-Role`

## Configuration
- `GATEWAY_PORT` (Docker setup uses `8443`)
- `JWT_PUBLIC_KEY_PATH`
- Optional TLS config via `GATEWAY_SSL_*`
- CORS allows `http://localhost:4200` and `https://localhost:4200`

## Quick run
```bash
cd backend/gateway-service
./mvnw spring-boot:run
```
