# Frontend

## Purpose
Angular app for the marketplace UI (auth, profile, product browsing, seller dashboard, and media upload flows).

## How it connects
- In local dev (`npm run start:https`), proxy forwards API routes to `https://localhost:8443`
- In Docker prod mode, Nginx in the frontend container proxies API routes to `https://gateway-service:8443`
- Uses endpoints under `/auth`, `/users`, `/products`, `/media`
- Stores JWT in browser storage (`jwt_token`)

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
```bash
cd frontend
npm install
npm run start:https
```

Alternative (HTTP):
```bash
npm start
```

## Notes
- `start:https` reuses `backend/certs/gateway.crt` and `backend/certs/gateway.key`
- Proxy config forwards API routes to `https://localhost:8443`
- Dockerized frontend serves the built app via Nginx on `https://localhost:4200`
