# Product Service

## Purpose
`product-service` manages the seller catalog: create, list, update, and delete products.

## Main endpoints
- `GET /products`
- `GET /products/{id}`
- `GET /products/me` (seller products)
- `POST /products`
- `PUT /products/{id}`
- `DELETE /products/{id}`

## Internal endpoint

- `GET /internal/products/{productId}/ownership`, requiring `X-User-Id`
- Returns `204` for the owner, `403` for another user, `404` for a missing product, and `400` for missing or blank identity
- Reuses the same ownership check as product update/delete
- Called directly by media service on the trusted private backend network; unavailable through the gateway
- Deploy this endpoint before deploying media service's ownership-checking upload flow

## Core behavior
- Uses pagination for listing
- Enforces ownership checks on update/delete
- Requires `mediaIds` to be empty on create
- Validates updated `mediaIds` against `GET /media/images/{productId}` in `media-service`

## Dependencies
- MongoDB
- Eureka (`discovery-service`)
- Media service (Eureka discovery)

## Configuration
- `PRODUCT_SERVICE_PORT` (default `8082`)
- `MONGO_*`, `EUREKA_*`

## Quick run
```bash
cd backend/product-service
./mvnw spring-boot:run
```
