# Gateway Service

## Purpose
`gateway-service` is the single backend entry point. It routes requests to internal services and uses Spring Security OAuth2 Resource Server to verify RSA-signed JWTs and enforce access rules.

## Main routes
- `/auth/**`, `/users/**` -> `USER-SERVICE`
- `/products/**` -> `PRODUCT-SERVICE`
- `/media/**` -> `MEDIA-SERVICE`
- `/ecom-media/**` -> the MinIO object endpoint configured by `MINIO_ENDPOINT`

## Auth behavior
- Public: `POST /auth/login`, `POST /auth/register`
- Public reads: `GET /products`, `GET /products/{id}`, `GET /media/images/{productId}`, `GET /media/profile/{userId}`
- Authenticated: `GET /products/me`, `GET|PATCH /users/me`, `POST /media/profile`
- Seller-only: `POST /products`, `PUT|DELETE /products/{id}`, `POST /media/images`
- Public stored media: `GET|HEAD /ecom-media/**`
- All other requests are denied by default.

For authenticated requests, the gateway validates the JWT signature and time claims, removes client-supplied identity headers, and forwards trusted values as:
  - `X-User-Id`
  - `X-User-Email`
  - `X-User-Role`

Public endpoints do not attempt JWT authentication, so an expired token cannot prevent public product browsing or login.

## Configuration
- `GATEWAY_PORT` (Docker setup uses `8443`)
- `JWT_PUBLIC_KEY_PATH`
- `MINIO_ENDPOINT`
- Optional TLS config via `GATEWAY_SSL_*`
- `CORS_ALLOWED_ORIGINS` (defaults to `http://localhost:4200` and `https://localhost:4200`)

## Quick run
```bash
cd backend/gateway-service
./mvnw spring-boot:run
```
