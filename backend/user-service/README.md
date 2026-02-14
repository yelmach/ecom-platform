# User Service

## Purpose
`user-service` handles authentication and current-user profile management.

## Main endpoints
- `POST /auth/register`
- `POST /auth/login`
- `GET /users/me`
- `PATCH /users/me`

## Core behavior
- Creates JWT tokens (RSA private key)
- Stores users in MongoDB (`CLIENT` or `SELLER` role)
- Supports profile updates (username, email, password, role)
- Stores `avatarMediaId` and validates it via `media-service`

## Dependencies
- MongoDB
- Eureka (`discovery-service`)
- Media service (`MEDIA_SERVICE_BASE_URL`)
- JWT private key (`JWT_PRIVATE_KEY_PATH`)

## Configuration
- `USER_SERVICE_PORT` (default `8081`)
- `MONGO_*`, `EUREKA_*`, `JWT_*`, `MEDIA_SERVICE_BASE_URL`

## Quick run
```bash
cd backend/user-service
./mvnw spring-boot:run
```
