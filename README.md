# FlowGate — Login & Signup (Auth Module)

A standalone authentication/authorization module — React frontend + Spring Boot
backend — designed as the login/signup layer for the Enterprise AI Workflow
Automation Platform. It's a clean starting point: wire your workflow, invoice,
and approval endpoints in behind the same `SecurityFilterChain`.

## Stack

- **Frontend:** React 18 + Vite, React Router, Axios
- **Backend:** Spring Boot 3.3, Spring Security 6, JWT (jjwt), Spring Data JPA
- **Database:** PostgreSQL

## Folder structure

```
authapp-backend/    Spring Boot API (port 8080)
authapp-frontend/   React app (port 5173)
```

## 1. Database

Install PostgreSQL locally (or use a container), then create the database:

```bash
psql -U postgres -f authapp-backend/setup.sql
```

Tables (`users`, `user_roles`) are created automatically on first run via
`spring.jpa.hibernate.ddl-auto=update` — no manual migration needed for this
starter.

## 2. Backend

Edit `authapp-backend/src/main/resources/application.properties` if your
Postgres credentials differ from the defaults (`postgres` / `postgres`), or
set the equivalent environment variables. **Also change `app.jwt.secret`
before deploying anywhere real** — it's currently a placeholder.

```bash
cd authapp-backend
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080`.

### Endpoints

| Method | Path                | Auth required | Purpose                          |
|--------|---------------------|----------------|-----------------------------------|
| POST   | `/api/auth/signup`  | No             | Create an account                 |
| POST   | `/api/auth/login`   | No             | Log in, returns a JWT             |
| GET    | `/api/public/ping`  | No             | Health check                      |
| GET    | `/api/user/profile` | Yes (USER/ADMIN) | Example protected endpoint     |
| GET    | `/api/admin/dashboard` | Yes (ADMIN)  | Example role-restricted endpoint |

**Signup body:**
```json
{ "username": "jane.doe", "email": "jane@company.com", "password": "at-least-8-chars" }
```

**Login body:**
```json
{ "username": "jane.doe", "password": "at-least-8-chars" }
```

**Login response:**
```json
{
  "token": "eyJhbGciOi...",
  "type": "Bearer",
  "id": 1,
  "username": "jane.doe",
  "email": "jane@company.com",
  "roles": ["ROLE_USER"]
}
```

Send the token on every subsequent request as `Authorization: Bearer <token>`.

## 3. Frontend

```bash
cd authapp-frontend
npm install
npm run dev
```

Opens on `http://localhost:5173`. The dev server is already whitelisted in
the backend's CORS config (`app.cors.allowedOrigins`).

Flow: `/signup` → creates the account → redirects to `/login` → on success,
the JWT + user info are stored in `localStorage` and you land on the
`/dashboard` placeholder (a stand-in for the real approval-queue UI).

## How the security layer works

- **Passwords** are hashed with BCrypt (`PasswordEncoder` bean) — never
  stored or compared in plaintext.
- **Stateless JWT auth**: no server-side session. `WebSecurityConfig`'s
  `SecurityFilterChain` sets `SessionCreationPolicy.STATELESS` and disables
  CSRF (not needed without cookie-based sessions).
- **`AuthTokenFilter`** runs once per request, reads the `Authorization`
  header, validates the JWT (`JwtUtils`), and — if valid — populates the
  `SecurityContext` so `@PreAuthorize` / `hasRole(...)` checks work.
- **`AuthEntryPointJwt`** returns a clean JSON 401 for unauthenticated
  requests to protected routes, instead of a redirect to an HTML login page
  (this is a pure REST API).
- **Authorization** is both route-based (`.requestMatchers("/api/admin/**").hasRole("ADMIN")`
  in `WebSecurityConfig`) and available at the method level via
  `@EnableMethodSecurity` + `@PreAuthorize`.
- **Roles** (`ROLE_USER`, `ROLE_ADMIN`) live in a `user_roles` collection
  table tied to `User`; extend `ERole` as your workflow needs more (e.g.
  `ROLE_MANAGER`, `ROLE_FINANCE`) for approval routing.

## Next steps for the wider platform

This module only covers identity. To connect it to the workflow-automation
platform described in your project scope:

1. Add `ROLE_MANAGER` / `ROLE_FINANCE` etc. to `ERole` and assign them at
   signup or via an admin endpoint.
2. Build the Workflow, Request, and Approval entities/controllers behind
   the same filter chain, scoping endpoints with `hasRole(...)` per stage.
3. Consider short-lived access tokens + refresh tokens if you need
   longer-lived sessions without re-entering credentials.
