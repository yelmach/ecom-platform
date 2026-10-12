# Media Service

## Purpose

`media-service` handles image uploads and media metadata for product images and user avatars.

## Main endpoints

- `POST /media/images` (multipart: `productId`, `files[]`)
- `GET /media/images/{productId}` — product image metadata
- `POST /media/profile` (multipart: `file`)
- `GET /media/profile/{userId}`

## Core behavior

- Stores binary objects in MinIO and metadata in MongoDB
- Consumes `PRODUCT_DELETED` from Kafka and removes all product-owned objects and
  metadata, including unlinked uploads. Objects are deleted before metadata;
  failures receive three retries five seconds apart, then are logged and skipped;
  duplicate events are safe. Failed cleanup may leave images behind.
- Uploads already in progress can finish after deletion cleanup and leave images behind.
- Consumes `PRODUCT_IMAGES_DETACHED` and deletes only listed images belonging
  to that product. Missing images are skipped; retained images and avatars are preserved.
- Product upload limit: max 5 files per request
- Checks product ownership with product service before validating or storing files
- Validates every file in a product batch before uploading any file
- If a later upload or metadata save fails, earlier saved images remain; the current object is deleted on a best-effort basis and remaining files are skipped
- Avatar uploads save a new object and metadata without deleting the previous avatar.
- User service validates ownership by media ID and saves the new `avatarMediaId`. Previous and unlinked avatars remain stored; replacement cleanup is deferred to a future Kafka implementation.
- Failed upload/metadata-save cleanup for the current object remains in place.
- Validates files:
  - JPEG and PNG only (GIF and WebP are unsupported)
  - max 2 MB per file; 11 MB per multipart request to allow five files plus form overhead
  - detects the content type from the bytes with Apache Tika, ignoring the supplied filename and MIME header
  - verifies that ImageIO can decode the image, then preserves the original bytes
  - extensions mapped from the detected format (`jpg`, `png`)
- Initializes MinIO bucket and applies public-read object policy

## Dependencies

- MongoDB
- Kafka (`KAFKA_BOOTSTRAP_SERVERS`); consumes `product.events`, created by product service at startup
- MinIO
- Eureka (`discovery-service`)
- Product service (internal ownership check; discovered through Eureka and reached without the gateway)

## Code organization

Controllers handle HTTP requests and `MediaService` coordinates product and avatar workflows. `FileValidationService` validates images; `ProductOwnershipService` calls product service through Eureka. `MediaStorageService` uploads bytes, saves metadata, and cleans up the current object on failure. `ProductEventConsumer` receives product deletion and detached-image events and `MediaCleanupService` deletes their images. `storage/` contains the MinIO implementation, while `MediaUrlBuilder` builds response URLs.

Configuration stays in `config/`: `MediaProperties` binds and validates settings, `MinioConfig` creates the client, `MinioBucketInitializer` prepares the bucket and its policy, and `HttpClientConfig` creates the load-balanced product client.

## Configuration

- `MEDIA_SERVICE_PORT` (default `8083`)
- `MONGO_*`, `EUREKA_*`
- `MINIO_ENDPOINT`, `MINIO_ROOT_USER`, `MINIO_ROOT_PASSWORD`, `MINIO_BUCKET`
- `MEDIA_PUBLIC_BASE_URL`

Missing or blank MinIO settings and public base URL fail startup. `media.max-file-size-bytes` must be positive and defaults to 2 MB.

## Ownership and failures

JWT validation stays at the gateway. Media trusts the forwarded `X-User-Id` on the private backend network and calls `GET /internal/products/{productId}/ownership` with that identity. Its load-balanced RestClient uses the logical address `http://product-service`; Spring Cloud LoadBalancer selects an instance registered in Eureka and supplies its host and port. The client retains its 1-second connection timeout and 3-second response timeout.

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

## Image access and internal avatar contracts

`GET /media/images/{productId}` returns image metadata and object URLs. The browser loads those URLs through the gateway’s `/ecom-media/**` route directly from MinIO; media service does not stream image bytes.

User service calls `GET /internal/media/avatars/{mediaId}/ownership` with `X-User-Id` before linking an avatar. There is no activation or replacement-cleanup endpoint. Old avatars remain stored until asynchronous cleanup is implemented. Internal endpoints are unavailable through the gateway. Deploy media's new endpoints before the updated user/product callers.
