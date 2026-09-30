# Discovery Service

## Purpose
`discovery-service` is the Eureka registry for the backend. Other services register here and use it for service lookup.

## What it does
- Runs a Eureka server (`@EnableEurekaServer`)
- Tracks live service instances
- Exposes health/actuator endpoints

## Key endpoints
- `GET /eureka/*` - Eureka dashboard and registry APIs
- `GET /actuator/health`

## Configuration
- `EUREKA_HOST` (default `localhost`)
- `EUREKA_PORT` (default `8761`)
- Optional host-local env import from root `.env.local`

## Quick run
```bash
cd backend/discovery-service
./mvnw spring-boot:run
```
