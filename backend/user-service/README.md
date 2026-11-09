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
- Publishes avatar replacement/clearing cleanup events to Kafka (`KAFKA_BOOTSTRAP_SERVERS`)

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
- Media service (Eureka discovery)
- JWT RSA key pair (`JWT_PRIVATE_KEY_PATH`, `JWT_PUBLIC_KEY_PATH`)

## Configuration
- `USER_SERVICE_PORT` (default `6201`)
- `MONGO_*`, `EUREKA_*`, `JWT_PRIVATE_KEY_PATH`, `JWT_PUBLIC_KEY_PATH`
- `JWT_EXPIRATION` as a duration such as `24h`
- Media calls use `http://media-service` through Spring Cloud LoadBalancer, with 1-second connection and 3-second response timeouts.

## Quick run
```bash
cd backend/user-service
./mvnw spring-boot:run
```

Password DTOs enforce a maximum of 72 characters. Avatar ownership is validated through media’s internal ownership endpoint before saving the profile. Replacing or clearing an existing avatar publishes `USER_AVATAR_CHANGED` to `user.events` after saving the profile. Publication failures are logged without failing the successful update; there is no replay queue. First selection and unchanged references emit nothing. Media unavailability during ownership validation returns `503`. Deploy the new media endpoints before this caller.
