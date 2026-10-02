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
- Checks product ownership with product service before validating or storing files
- Validates every file in a product batch before uploading any file
- If a later upload or metadata save fails, earlier saved images remain; the current object is deleted on a best-effort basis and remaining files are skipped
- Uses the same upload/save cleanup for avatars; existing avatar replacement ordering is unchanged
- Avatar flow keeps only one active avatar per user
- Validates files:
  - JPEG and PNG only (GIF and WebP are unsupported)
  - max 2 MB per file
  - detects the content type from the bytes with Apache Tika, ignoring the supplied filename and MIME header
  - verifies that ImageIO can decode the image, then preserves the original bytes
  - extensions mapped from the detected format (`jpg`, `png`)
- Initializes MinIO bucket and applies public-read object policy

## Dependencies
- MongoDB
- MinIO
- Eureka (`discovery-service`)
- Product service (internal ownership check; discovered through Eureka and reached without the gateway)

## Configuration
- `MEDIA_SERVICE_PORT` (default `8083`)
- `MONGO_*`, `EUREKA_*`
- `MINIO_ENDPOINT`, `MINIO_ROOT_USER`, `MINIO_ROOT_PASSWORD`, `MINIO_BUCKET`
- `MEDIA_PUBLIC_BASE_URL`

## Ownership and failures

JWT validation stays at the gateway. Media trusts the forwarded `X-User-Id` on the private backend network and calls `GET /internal/products/{productId}/ownership` with that identity. Its load-balanced RestClient uses the logical address `http://product-service`; Spring Cloud LoadBalancer selects an instance registered in Eureka and supplies its host and port. The client retains its 1-second connection timeout and 5-second response timeout.

- `400`: invalid input, unsupported detected file type, unreadable image contents, oversized file, or malformed ownership request
- `403`: product belongs to another seller
- `404`: product does not exist
- `503`: product service cannot verify ownership, including no discovered instances, timeouts, and unexpected responses
- `500`: upload or metadata persistence failed

Rejected ownership checks and invalid batches create no objects or metadata. A storage failure can leave earlier successfully saved images even though the request returns an error; use the existing product image listing to retrieve them before retrying. Cleanup failures are logged with the object key. Cleanup is not a transaction across MongoDB and MinIO and does not guarantee recovery from process crashes or ambiguous writes.

## Deployment order

Deploy product service's internal ownership endpoint first, then deploy media service. Product must be registered in Eureka before ownership checks can succeed. `PRODUCT_SERVICE_BASE_URL` is no longer used and can be removed from existing environment files. Keep `/internal/**` inaccessible through the gateway and product service on the trusted backend network.

## Quick run
```bash
cd backend/media-service
./mvnw spring-boot:run
```
