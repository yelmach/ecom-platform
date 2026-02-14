# Frontend

## Purpose
Angular app for the marketplace UI (auth, profile, product browsing, seller dashboard, and media upload flows).

## How it connects
- Talks to the gateway at `https://localhost:8443`
- Uses endpoints under `/auth`, `/users`, `/products`, `/media`
- Stores JWT in browser storage (`jwt_token`)

## Main features
- Login/register flows (with optional avatar upload)
- Seller product create/edit/delete
- Product image upload and rendering through media IDs
- User profile update and avatar change/removal

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
