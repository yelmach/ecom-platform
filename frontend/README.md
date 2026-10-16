# Frontend

## Purpose
Angular app for the marketplace UI (auth, profile, product browsing, seller dashboard, and media upload flows).

## How it connects
- `npm start` loads root `.env.local` and serves HTTP on `FRONTEND_PORT` (6100).
- `proxy.conf.cjs` forwards API routes to local HTTP `GATEWAY_PORT` (6200).
- Docker uses `nginx/default.conf`: frontend listens on 6100 and proxies to gateway port 6200. Compose reads the published host port from `.env`.
- Production Caddy proxies `https://ecom.yelmach.com` to `http://127.0.0.1:6100`.
- Uses endpoints under `/auth`, `/users`, `/products`, `/media`, and `/ecom-media`.
- Stores JWT in browser storage (`jwt_token`).

## Main features
- Login/register flows (with optional avatar upload)
- Seller product create/edit/delete
- Product image upload and rendering through media IDs
- User profile update and avatar change/removal

## Backend contracts

- Passwords are 6–72 characters for registration, login, and a profile password change. An empty profile password is omitted from the request.
- Usernames contain only letters and numbers, with 4–15 characters.
- Image selection accepts non-empty JPEG/PNG files up to 2 MB each, with up to five product images. These browser checks are preliminary; the backend checks the actual contents and decodes the image.
- Uploads use `FormData`, allowing the browser to set the multipart boundary. Nginx allows 11 MB requests to cover five 2 MB files plus multipart overhead.
- Product pagination returns `{ content, page: { number, size, totalElements, totalPages } }`. The UI derives the last page from `number` and `totalPages`.
- Product image metadata comes from `GET /media/images/{productId}`; the UI displays images referenced by the product's `mediaIds`. Image bytes use the returned URLs under `/ecom-media/**`, forwarded through the gateway to MinIO.
- Avatar upload precedes saving `avatarMediaId` with `PATCH /users/me`. A successful upload is reused when retrying a failed profile save in the open dialog. Clearing the avatar sends `null`; ordinary edits omit the avatar field.
- `GET /media/profile/{userId}` returns the latest uploaded avatar. The UI checks its ID against the saved `avatarMediaId` before displaying it. If they differ, the UI uses a placeholder; this endpoint cannot retrieve the older active avatar after a reload.
- Product creation precedes image upload and saving the returned media IDs. Retries on the same form reuse the created product and successfully returned upload IDs. A failed upload can leave earlier files stored without returning their IDs; reliable recovery from that case needs backend support. Uploaded images and previous avatars are not deleted by the frontend.
- Backend error messages are displayed for request failures, including `400`, `403`, `404`, `415`, and `503`. Only `401` clears the session.
- Eureka discovery and internal ownership endpoints are backend concerns. The browser continues to call the gateway using the existing public routes and a Bearer token.

## Run locally

Create root `.env.local` from `.env.local.example`, then:

```bash
cd frontend
npm ci
npm start
```

Open `http://localhost:6100`. No TLS certificates or `start:https` command are
needed. API calls and image URLs use the same frontend origin. For production,
see [Oracle deployment](../docs/oracle-deployment.md).

## How runtime configuration works

For manual development, `npm start` loads `.env.local` using Node's
`--env-file` option. The small `scripts/serve.mjs` launcher passes `FRONTEND_PORT`
to Angular and runs the CLI directly in the same process. Angular handles
validation, terminal output, and stopping the development server.

`proxy.conf.cjs` has one route rule for API and media requests. It reads
`GATEWAY_PORT` and forwards requests to the local HTTP gateway. JavaScript is
needed here because static JSON cannot read environment variables. Both files
run only in local development tooling, not in the browser or production container.

For Docker, the multi-stage build compiles Angular using Node and copies the
static files and `nginx/default.conf` into the Nginx image. No configuration
templating or extra environment variables are needed.

Nginx listens on **6100** and proxies API/media requests to
`http://gateway-service:6200`. Compose publishes its host port using
`FRONTEND_PORT` from `.env`; `EXPOSE 6100` is image metadata.

If the gateway's internal port changes, update `nginx/default.conf` alongside
`.env` and rebuild the frontend image. Changing Nginx's listening port also
requires updating Compose's container port and health check. Changing only the
published host port does not require a rebuild; adjust Caddy's upstream to match.

The config handles SPA routes, the 11 MB upload limit, `/healthz`, and Caddy's
forwarded HTTPS scheme. Local connections use HTTP; no frontend certificate is needed.
