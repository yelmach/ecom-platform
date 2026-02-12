### Overview

In this project, you will build an **end-to-end e-commerce platform** using **Spring Boot microservices** on the backend and **Angular** on the frontend.  
Users can register as **clients** or **sellers**; sellers manage products and related media (images).

You will practice **service decomposition**, **inter-service communication** and a clean Angular UI that consumes these services.

### Role Play

You are a **full-stack engineer** designing a small but realistic marketplace composed of independently deployable services.  
Your mission: deliver a **secure**, **observable**, and **scalable** platform where **clients** browse products and **sellers** manage their catalog and media.

### Learning Objectives

- Design and implement **Spring Boot microservices** (User, Product, Media)
- Apply **asynchronous communication** (e.g., Kafka) where appropriate
- Implement **JWT or OAuth2** with **Spring Security** for role-based access
- Enforce **secure file uploads** with validation and size limits
- Model and persist data in **MongoDB** (or polyglot where justified)
- Build an **Angular** SPA with routing, guards, interceptors, and forms

### Instructions

#### 1) Microservices Setup

- Create separate services:
  - **User Service** (auth, profiles, roles: CLIENT, SELLER)
  - **Product Service** (CRUD for products, image references)
  - **Media Service** (image upload/download, validation, 2 MB limit)
- Configure **Kafka** (optional but recommended) for events such as `PRODUCT_CREATED`, `IMAGE_UPLOADED` (useful for audit, cache invalidation, thumbnails).

#### 2) Enhanced Database Design

![DataBase Design](./Database-Design.png)

#### 3) API Development Enhancement

- **User Service**
  - **Auth**: `POST /auth/register` (role: CLIENT | SELLER), `POST /auth/login` → JWT/OAuth2 token
  - **Profile**: `GET /me`, `PUT /me` (seller may upload/update avatar → delegated to Media Service)
- **Product Service**
  - **Public**: `GET /products` (list), `GET /products/{id}`
  - **Seller-only**: `POST /products`, `PUT /products/{id}`, `DELETE /products/{id}` (enforce ownership)
  - Associate `imageUrls[]`; uploading images is performed via Media Service then linked here.
- **Media Service**
  - `POST /media/images` (seller-only): validate **MIME type (image/\*)** and **≤ 2 MB**
  - `GET /media/images/{id}`: serve image (with proper caching headers)
  - Optional: `DELETE /media/images/{id}` (seller must own media)

> All services expose **/actuator/health**; gateway routes external traffic and applies cross-cutting filters (CORS, auth propagation, rate limiting if added).

#### 4) Front-End Development with Angular

- **Auth Pages**: Sign-in & Sign-up (role selection). Sellers can upload/update avatar.
- **Seller Dashboard**:
  - Manage products (create/edit/delete) and **attach images** (preview, remove).
  - Show validation messages (price > 0, required fields).
- **Product Listing (Public)**:
  - Simple grid/list of products (no search/filter required).
- **Media Management**:
  - Dedicated view to upload/manage images for the seller’s products.
  - Enforce file type/size in the UI before calling the API.
- **Technical**
  - Use **route guards** (AuthGuard, RoleGuard), **HTTP interceptors** (attach token, handle 401/403), **Reactive Forms**, and **Angular Material/Bootstrap** for responsive UI.

#### 5) Authentication & Authorization

- **Spring Security** with **JWT or OAuth2** at the gateway and propagated downstream.
- Roles: **CLIENT** (browse) vs **SELLER** (manage own products/media). **ADMIN** (optional) for moderation.
- Enforce **ownership checks** in Product/Media services (sellerId == auth.subject).

#### 6) Error Handling & Validation

- Return meaningful status codes:
  - `400` invalid input / file type / file too large
  - `401/403` unauthenticated/unauthorized
  - `404` not found (product/media not owned or missing)
  - Avoid unhandled `5xx` via global exception handlers.
- Angular: show inline form errors; display toast/snackbar for upload failures, oversized files, or forbidden actions.

#### 7) Security Measures

- **HTTPS** end-to-end (e.g., **Let’s Encrypt** for certs).
- **Password Security**: hash+salt with **BCrypt** in User Service (never expose password).
- **Input Validation**: validate filenames/MIME; verify content by **sniffing** headers; reject non-image payloads.
- **Access Control**: only the creating **seller** can modify/delete a product or its images.
- **CORS**: gateway enforces allowed origins and headers.
- (Optional) **Rate Limiting** at the gateway for auth and media endpoints.

### Constraints

- Backend split into at least **3 microservices** (User, Product, Media) + **Gateway** + **Discovery**.
- **MongoDB** for persistence; images stored in object storage (not in DB).
- Public endpoints: product **GET**s; all write operations require auth and role checks.
- Media uploads: **image/\*** only, max **2 MB**; reject others.
- Provide a comprehensive **README** with run scripts (Docker Compose encouraged).

### Evaluation

- ⚙️ **Functionality**: Role flow works; sellers can CRUD products & images; clients can browse.
- 🔐 **Security**: JWT/OAuth2, password hashing, ownership enforcement, CORS.
- 🧩 **Architecture**: Clean service boundaries, gateway & discovery configured, optional Kafka events.
- 🚫 **Reliability**: Proper error handling (no unhandled 5xx); health checks.
- 🎨 **UX**: Angular app is responsive, guards/interceptors implemented, clear validation.
