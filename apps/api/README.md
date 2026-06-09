# SlideForge API

Spring Boot backend for SlideForge. The current backend is an MVP-ready scaffold for frontend integration and the one-page PPT workflow.

## Tech Stack

```text
Java 21
Spring Boot 3.3.5
Spring Web
Spring Validation
Spring Data JPA
Flyway
H2 for local development
PostgreSQL via environment variables
Spring Security Crypto for BCrypt
```

## Local Run

From `apps/api`:

```bash
mvn spring-boot:run
```

If Maven is not on PATH, this machine has Maven at:

```text
C:\Users\24590\.maven\maven-3.9.15\bin\mvn.cmd
```

The API runs on:

```text
http://localhost:8080
```

## Local Database

By default the app uses an in-memory H2 database:

```text
jdbc:h2:mem:slideforge;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1
```

Flyway creates the `users` table. `DevDataInitializer` seeds a local test account:

```text
username: admin
password: 123456
```

For PostgreSQL, set:

```text
DB_URL=jdbc:postgresql://127.0.0.1:5432/slideforge
DB_USERNAME=slideforge
DB_PASSWORD=your-password
DB_DRIVER=org.postgresql.Driver
```

## Endpoints

```text
GET  /api/health

POST /login
POST /api/auth/mock-login

GET  /api/users/me
GET  /api/users/profile
PUT  /api/users/me

GET    /api/settings/ai
PUT    /api/settings/ai
POST   /api/settings/ai/test
DELETE /api/settings/ai/key

POST /api/one-page/drafts
GET  /api/one-page/drafts/{draftId}
POST /api/one-page/drafts/{draftId}/consult
POST /api/one-page/drafts/{draftId}/brief
PUT  /api/one-page/drafts/{draftId}/brief
POST /api/one-page/drafts/{draftId}/research
POST /api/one-page/drafts/{draftId}/page-plan
PUT  /api/one-page/drafts/{draftId}/page-plan
POST /api/one-page/drafts/{draftId}/svg
POST /api/one-page/drafts/{draftId}/svg/regenerate
```

## Response Shape

Success:

```json
{
  "code": 0,
  "message": "OK",
  "data": {}
}
```

Error:

```json
{
  "code": 400,
  "message": "请求参数不正确",
  "data": null
}
```

## Current Scope

Implemented:

```text
Spring Boot app entry
CORS for Vue dev server and /login
Unified API response
Global exception handling
Health check
BCrypt login with seeded admin user
Current user profile
BYOK AI settings placeholder
AI Provider Adapter interface
One-page draft workflow
Requirement brief generation placeholder
Research pack generation placeholder
Page plan generation placeholder
Bento Grid SVG generation placeholder
SVG sanitization and validation
```

Not implemented yet:

```text
JWT validation
Persistent user AI settings
Encrypted API Key storage
Real model provider HTTP calls
Search-assisted research
Project persistence
PPTX export
```
