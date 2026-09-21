# User Service

## Purpose
`user-service` handles authentication and current-user profile management.

## Main endpoints
- `POST /auth/register`
- `POST /auth/login`
- `GET /users/me`
- `PATCH /users/me`

## Core behavior
- Authenticates email/password credentials with Spring Security and BCrypt
- Creates RS256 JWT tokens with Spring Security's `JwtEncoder`
- Leaves JWT validation and protected-route enforcement to `gateway-service`
- Stores users in MongoDB (`CLIENT` or `SELLER` role)
- Allows role selection during registration only
- Supports profile updates (username, email, password, avatar)
- Stores `avatarMediaId` and validates it via `media-service`

`GET /users/me` and `PATCH /users/me` trust the sanitized `X-User-Id`
header created by `gateway-service`. Do not expose this service through a route
that bypasses the gateway.

## Access rules

| Method and path | Access at user-service |
|---|---|
| `POST /auth/register` | Permitted; public at the gateway |
| `POST /auth/login` | Permitted; public at the gateway |
| `GET /users/me` | Permitted after gateway authentication |
| `PATCH /users/me` | Permitted after gateway authentication |
| `GET /actuator/health` | Permitted for health checks |
| Everything else | Denied |

## Dependencies
- MongoDB
- Eureka (`discovery-service`)
- Media service (`MEDIA_SERVICE_BASE_URL`)
- JWT RSA key pair (`JWT_PRIVATE_KEY_PATH`, `JWT_PUBLIC_KEY_PATH`)

## Configuration
- `USER_SERVICE_PORT` (default `8081`)
- `MONGO_*`, `EUREKA_*`, `JWT_PRIVATE_KEY_PATH`, `JWT_PUBLIC_KEY_PATH`
- `JWT_EXPIRATION` as a duration such as `24h`
- `MEDIA_SERVICE_BASE_URL`

## Quick run
```bash
cd backend/user-service
./mvnw spring-boot:run
```
