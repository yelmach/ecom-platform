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

## Core behavior
- Uses pagination for listing
- Enforces ownership checks on update/delete
- Requires `mediaIds` to be empty on create
- Validates updated `mediaIds` against `GET /media/images/{productId}` in `media-service`

## Dependencies
- MongoDB
- Eureka (`discovery-service`)
- Media service (`MEDIA_SERVICE_BASE_URL`)

## Configuration
- `PRODUCT_SERVICE_PORT` (default `8082`)
- `MONGO_*`, `EUREKA_*`, `MEDIA_SERVICE_BASE_URL`

## Quick run
```bash
cd backend/product-service
./mvnw spring-boot:run
```
