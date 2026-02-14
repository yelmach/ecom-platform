# Media Service

## Purpose
`media-service` handles image uploads and media metadata for product images and user avatars.

## Main endpoints
- `POST /media/images` (multipart: `productId`, `files[]`)
- `GET /media/images/{productId}`
- `POST /media/profile` (multipart: `file`)
- `GET /media/profile/{userId}`

## Core behavior
- Stores binary objects in MinIO and metadata in MongoDB
- Product upload limit: max 5 files per request
- Avatar flow keeps only one active avatar per user
- Validates files:
  - image MIME only
  - max 2 MB per file
  - extensions mapped from content type (`jpg`, `png`, `gif`, `webp`)
- Initializes MinIO bucket and applies public-read object policy

## Dependencies
- MongoDB
- MinIO
- Eureka (`discovery-service`)

## Configuration
- `MEDIA_SERVICE_PORT` (default `8083`)
- `MONGO_*`, `EUREKA_*`
- `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_BUCKET`
- `MEDIA_PUBLIC_BASE_URL`

## Quick run
```bash
cd backend/media-service
./mvnw spring-boot:run
```
