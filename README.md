# Todo List Backend API

Production-style backend for a Todo List application built with Java 17, Spring Boot, PostgreSQL, JWT authentication, refresh tokens, Liquibase migrations and OpenAPI documentation. The project is designed as a clean, portfolio-ready REST API that can be used by a future web or mobile frontend.

## Technology Stack

- Java 17
- Spring Boot 3
- Spring Web
- Spring Data JPA
- Spring Security
- JWT (access token + refresh token flow)
- PostgreSQL
- Liquibase
- Bean Validation
- springdoc OpenAPI / Swagger UI
- Gradle
- Docker / Docker Compose
- JUnit 5, Mockito, Spring Boot Test, MockMvc
- Lombok

## Features

- User registration by email
- User login by email and password
- Stateless JWT-based authentication
- Refresh token rotation-friendly persistence in PostgreSQL
- Logout via refresh token revocation
- Full CRUD for todos
- Ownership isolation: users only work with their own todos
- Filtering by `completed`, `priority`, `dueDateFrom`, `dueDateTo`
- Sorting by `createdAt` or `dueDate`
- Pagination support
- Centralized error handling
- Swagger UI with Bearer authentication support
- Database schema managed by Liquibase

## Architecture Notes

- API contracts use DTOs only; JPA entities are never exposed directly.
- Package structure is split by responsibility: `config`, `controller`, `dto`, `entity`, `exception`, `mapper`, `repository`, `security`, `service`, `util`.
- Security is fully stateless and based on `SecurityFilterChain`, not `WebSecurityConfigurerAdapter`.
- Refresh tokens are stored in the database and can be revoked explicitly on logout.
- Todo ownership is enforced at the repository/service level using `findByIdAndOwnerId(...)`.
- If a user requests another user's todo, the API returns `404 Todo not found` instead of revealing that the resource exists. This avoids leaking resource existence across accounts.

## Project Structure

```text
.
├── Dockerfile
├── docker-compose.yml
├── build.gradle
├── settings.gradle
├── README.md
├── src
│   ├── main
│   │   ├── java/com/example/todolist
│   │   │   ├── TodoListApplication.java
│   │   │   ├── config
│   │   │   ├── controller
│   │   │   ├── dto
│   │   │   ├── entity
│   │   │   ├── enums
│   │   │   ├── exception
│   │   │   ├── mapper
│   │   │   ├── repository
│   │   │   ├── security
│   │   │   ├── service
│   │   │   └── util
│   │   └── resources
│   │       ├── application.yml
│   │       └── db/changelog
│   └── test
│       ├── java/com/example/todolist
│       │   ├── controller
│       │   └── service
│       └── resources/application-test.yml
```

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `SERVER_PORT` | `8080` | Application port |
| `DB_URL` | `jdbc:postgresql://localhost:5432/todolist` | PostgreSQL JDBC URL |
| `DB_USERNAME` | `postgres` | Database username |
| `DB_PASSWORD` | `postgres` | Database password |
| `JWT_SECRET` | `0123456789abcdef...` | HMAC secret for signing access tokens |
| `JWT_ACCESS_TOKEN_EXPIRATION_MINUTES` | `15` | Access token TTL |
| `JWT_REFRESH_TOKEN_EXPIRATION_DAYS` | `7` | Refresh token TTL |
| `LIQUIBASE_CONTEXTS` | empty | Optional Liquibase contexts, e.g. `local-seed` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:5173` | Allowed frontend origins |

## Database and Liquibase

- The application uses `spring.jpa.hibernate.ddl-auto=validate`.
- Schema creation is handled exclusively by Liquibase.
- Master changelog: `src/main/resources/db/changelog/db.changelog-master.yaml`
- Main change sets:
  - `001-create-users.yaml`
  - `002-create-todos.yaml`
  - `003-create-refresh-tokens.yaml`
  - `004-seed-local-data.yaml` with context `local-seed`

To enable local seed data, set:

```bash
export LIQUIBASE_CONTEXTS=local-seed
```

Seed user:

- email: `demo@example.com`
- password: `ChangeMe123!`

## Local Run Without Docker

1. Start PostgreSQL and create a database named `todolist`.
2. Export environment variables if you are not using defaults.
3. Run the application:

```bash
./gradlew bootRun
```

Run tests:

```bash
./gradlew test
```

## Testing

Current automated tests are located in `src/test/java/com/example/todolist` and cover:

- `controller/AuthControllerIntegrationTest`
  - successful registration
  - successful login
  - duplicate email rejection
- `controller/TodoControllerIntegrationTest`
  - authenticated todo creation
  - user-scoped todo listing
  - foreign todo access returns `404`
  - todo status update
  - owned todo deletion
- `service/RefreshTokenServiceTest`
  - expired refresh token rejection
  - valid refresh token verification

Run the full automated test suite:

```bash
./gradlew test
```

Run a full build including tests:

```bash
./gradlew clean build
```

Generate a JaCoCo coverage report:

```bash
./gradlew clean test jacocoTestReport
```

Run the full verification pipeline including the configured minimum coverage threshold:

```bash
./gradlew clean check
```

Coverage reports:

- HTML report: `build/reports/jacoco/test/html/index.html`
- XML report: `build/reports/jacoco/test/jacocoTestReport.xml`

Test reports:

- HTML report: `build/reports/tests/test/index.html`
- XML results: `build/test-results/test`

## Run With Docker Compose

The full stack can be started with a single command:

```bash
docker compose up --build
```

This starts:

- PostgreSQL on `localhost:5432`
- API on `http://localhost:8080`

## Swagger / OpenAPI

After startup:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

To authorize in Swagger UI:

1. Register or log in.
2. Copy the `accessToken`.
3. Click `Authorize`.
4. Paste `Bearer <accessToken>`.

## Authentication Flow

### Register

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "email": "john.doe@example.com",
    "password": "StrongPassword123"
  }'
```

### Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.doe@example.com",
    "password": "StrongPassword123"
  }'
```

Example response:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "24ef8fb0-08fc-4435-a3ea-3d4a6a36218b",
  "tokenType": "Bearer",
  "expiresInSeconds": 900,
  "user": {
    "id": 1,
    "username": "john_doe",
    "email": "john.doe@example.com",
    "role": "USER"
  }
}
```

### Refresh token flow

```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "24ef8fb0-08fc-4435-a3ea-3d4a6a36218b"
  }'
```

### Logout / revoke refresh token

```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "24ef8fb0-08fc-4435-a3ea-3d4a6a36218b"
  }'
```

## Using Bearer Token

```bash
export ACCESS_TOKEN="eyJhbGciOiJIUzI1NiJ9..."
```

```bash
curl -X GET "http://localhost:8080/api/todos?page=0&size=10&sort=createdAt&direction=DESC" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}"
```

## Todo API Examples

### Create todo

```bash
curl -X POST http://localhost:8080/api/todos \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Prepare portfolio project",
    "description": "Finalize backend documentation",
    "priority": "HIGH",
    "dueDate": "2026-03-31T20:00:00"
  }'
```

### List todos with filters

```bash
curl -X GET "http://localhost:8080/api/todos?page=0&size=5&sort=dueDate&direction=ASC&completed=false&priority=HIGH&dueDateFrom=2026-03-01T00:00:00&dueDateTo=2026-03-31T23:59:59" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}"
```

### Get one todo

```bash
curl -X GET http://localhost:8080/api/todos/1 \
  -H "Authorization: Bearer ${ACCESS_TOKEN}"
```

### Replace todo

```bash
curl -X PUT http://localhost:8080/api/todos/1 \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Prepare portfolio project v2",
    "description": "Refine README and tests",
    "completed": false,
    "priority": "MEDIUM",
    "dueDate": "2026-04-02T18:00:00"
  }'
```

### Patch status

```bash
curl -X PATCH http://localhost:8080/api/todos/1/status \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "completed": true
  }'
```

### Delete todo

```bash
curl -X DELETE http://localhost:8080/api/todos/1 \
  -H "Authorization: Bearer ${ACCESS_TOKEN}"
```

## Error Response Format

All API errors are normalized to the following structure:

```json
{
  "timestamp": "2026-03-22T08:21:10.123Z",
  "status": 404,
  "error": "Not Found",
  "message": "Todo not found",
  "path": "/api/todos/99",
  "validationErrors": null
}
```

Validation failures additionally populate `validationErrors`.

## Testing

Covered by tests:

- registration success
- login success
- duplicate email handling
- authenticated todo creation
- todo listing scoped to current user
- foreign todo access returns `404`
- todo status update
- owned todo deletion
- refresh token verification logic

## Future Improvements

- role-based admin endpoints
- refresh token rotation on each refresh call
- tags, categories and labels
- soft delete / audit history
- rate limiting for auth endpoints
- email verification and password reset
- Testcontainers-based PostgreSQL integration tests
- CI pipeline with linting and image publishing
