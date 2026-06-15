# Fox URL Shortener

Учебный REST API сервис для сокращения длинных URL. Пользователь регистрируется,
создает короткие ссылки, управляет ими и смотрит статистику переходов. Переход
по короткой ссылке публичный, все API управления находятся под `/api/v1`.

## Stack

Java 21, Gradle, Spring Boot, Spring Web, Spring Security, Spring Data JPA,
PostgreSQL, Flyway, JWT, Springdoc OpenAPI, JUnit 5, Mockito, Testcontainers,
Docker, GitHub Actions, Checkstyle, Spotless, JaCoCo.

## Local run

```bash
cp .env.example .env
./gradlew clean build
APP_PORT=3396 ADMIN_USERNAME=admin ADMIN_PASSWORD=change_me \
JWT_SECRET=change_me_to_long_secret_change_me_to_long_secret \
./gradlew bootRun
```

IntelliJ IDEA can run `com.fox.urlshortener.AppLauncher` directly.

## Docker

The compose file expects the shared PostgreSQL server on
`host.docker.internal:5432`. The `db-init` container creates the application role
and database only when they are missing.

```bash
cp .env.example .env
docker compose down --remove-orphans
docker compose up -d --build
docker compose ps
```

Application: `http://localhost:3396`

Swagger: `http://localhost:3396/swagger-ui/index.html`

## Environment

`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` configure the
application database connection.

`FOX_DB_ADMIN_USERNAME`, `FOX_DB_ADMIN_PASSWORD` are used only by
`docker/db-init/init-db.sh` to connect to the shared PostgreSQL server.

`ADMIN_USERNAME`, `ADMIN_PASSWORD`, `ADMIN_DISPLAY_NAME` configure the default
administrator. The application fails on startup if username or password is
missing. Bootstrap is idempotent: existing admin users are not modified.

`JWT_SECRET`, `JWT_ACCESS_EXPIRATION_MINUTES`, `JWT_REFRESH_EXPIRATION_DAYS`
configure JWT and refresh-token lifetime.

`APP_PORT`, `APP_BASE_URL`, `FORWARDED_PROTO_HEADER`, `FORWARDED_HOST_HEADER`,
`FORWARDED_PORT_HEADER` configure HTTP and short URL generation behind a reverse
proxy.

## Quality

```bash
./gradlew spotlessApply
./gradlew spotlessCheck
./gradlew checkstyleMain checkstyleTest
./gradlew test jacocoTestReport jacocoTestCoverageVerification
./gradlew check
```

Install pre-commit hook:

```bash
cp scripts/pre-commit .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit
```

## Curl examples

Register:

```bash
curl -X POST http://localhost:3396/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"fox","password":"Password123"}'
```

Login:

```bash
curl -X POST http://localhost:3396/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"fox","password":"Password123"}'
```

Create short link:

```bash
curl -X POST http://localhost:3396/api/v1/links \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"originalUrl":"https://example.com/some/very/long/url","expiresInDays":30}'
```

Get links:

```bash
curl -H "Authorization: Bearer $ACCESS_TOKEN" http://localhost:3396/api/v1/links
```

Disable link:

```bash
curl -X PATCH http://localhost:3396/api/v1/links/1/status \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"active":false}'
```

Hard delete link:

```bash
curl -X DELETE -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:3396/api/v1/links/1/hard
```

Admin get users:

```bash
curl -H "Authorization: Bearer $ADMIN_TOKEN" http://localhost:3396/api/v1/admin/users
```

Admin get user links:

```bash
curl -H "Authorization: Bearer $ADMIN_TOKEN" \
  http://localhost:3396/api/v1/admin/users/1/links
```

Admin disable any link:

```bash
curl -X PATCH http://localhost:3396/api/v1/admin/links/1/status \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"active":false}'
```

Redirect by code:

```bash
curl -i http://localhost:3396/aB12xZ
```
