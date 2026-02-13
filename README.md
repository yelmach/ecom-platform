# ecom-platform

## Media Integration (User + Product + Frontend)

### User avatar reference
- User profile now stores `avatarMediaId` (not `avatarUrl`).
- `PATCH /users/me` accepts `avatarMediaId`:
  - set `avatarMediaId` to a media ID to link avatar
  - set `avatarMediaId` to `null` to remove avatar reference
- Register flow is two-step:
1. `POST /auth/register`
2. Optional `POST /media/profile` with avatar file
3. `PATCH /users/me` with `{ "avatarMediaId": "<uploadedAvatarId>" }`

### Product media reference
- Product keeps `mediaIds` as canonical image references.
- Product creation requires empty `mediaIds`:
1. `POST /products` with `mediaIds: []`
2. `POST /media/images` with `productId` + files
3. `PUT /products/{id}` with uploaded media IDs
- Product update validates submitted `mediaIds` against `GET /media/images/{productId}` in media-service.

### Media-service usage
- Product images:
  - `POST /media/images` (`productId`, `files[]`, max 5 files)
  - `GET /media/images/{productId}`
- Profile avatar:
  - `POST /media/profile` (`file`)
  - `GET /media/profile/{userId}`

### Frontend behavior
- Registration supports optional avatar upload.
- Profile dialog supports avatar upload and avatar removal (`avatarMediaId: null`).
- Product create/edit uploads images through media-service and persists returned IDs.
- Shop/seller/product details resolve image URLs via `GET /media/images/{productId}`.
