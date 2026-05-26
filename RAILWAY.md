# Railway Deploy Config

This repo is a monorepo with two deployable services:

- `backend` (Spring Boot)
- `frontend` (React Router app server)

Each service has its own `railway.toml` so Railway uses Dockerfile builds.

## 1) Create two Railway services from this repo

1. Service A:
   - Name: `backend`
   - Root Directory: `backend`
2. Service B:
   - Name: `frontend`
   - Root Directory: `frontend`

Railway will pick up:

- `backend/railway.toml`
- `frontend/railway.toml`

## 2) Configure backend env vars

Minimum (in-memory fallback, no external DB):

- `APP_ALLOWED_ORIGINS=https://<frontend-domain>`

Optional (persistent Postgres):

- `SPRING_DATASOURCE_URL=jdbc:postgresql://...`
- `SPRING_DATASOURCE_USERNAME=...`
- `SPRING_DATASOURCE_PASSWORD=...`
- `SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver`
- `SPRING_FLYWAY_ENABLED=true`
- `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`

If Postgres vars are not set, backend falls back to H2 in-memory.

## 3) Configure frontend env vars

- `VITE_API_BASE_URL=https://<backend-domain>`

Then redeploy frontend so the build-time `VITE_*` value is baked in.

## 4) Order of deployment

1. Deploy backend first.
2. Copy backend public URL into frontend `VITE_API_BASE_URL`.
3. Deploy frontend.
4. Add frontend URL to backend `APP_ALLOWED_ORIGINS`.
5. Redeploy backend.
