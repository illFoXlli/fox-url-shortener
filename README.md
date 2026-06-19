# Fox URL Shortener

Training REST API service for shortening long URLs. A user can register,
authenticate, create short links, manage them, and view redirect statistics.
Opening a short URL is public; all management API endpoints are under `/api/v1`.

## Stack

Java 21, Gradle, Spring Boot, Spring Web, Spring Security, Spring Data JPA,
PostgreSQL, Flyway, JWT, Springdoc OpenAPI, JUnit 5, Mockito, Testcontainers,
Docker, GitHub Actions, Checkstyle, Spotless, JaCoCo.

## Spring profiles

The application uses two Spring profiles:

- `dev` - local development profile.
- `prod` - production profile.

Spring always reads `application.yml` first. Then it reads one profile-specific
file:

- `application-dev.yml` when the active profile is `dev`
- `application-prod.yml` when the active profile is `prod`

`application.yml` contains common settings loaded from environment variables:
JPA, Flyway, OpenAPI paths, logging, short-code length, and the default Spring
profile.

The default profile is `dev`. This is useful for local startup from IntelliJ
IDEA by running `AppLauncher.java` directly. Production startup must explicitly
use the `prod` profile through Docker Compose or environment configuration.

The profile files contain environment-dependent settings:

- database connection
- application URLs
- admin bootstrap credentials
- JWT settings
- cookies
- CORS
- reverse proxy headers

The project-local `.env` keeps profile-specific variables separate:

- `DEV_*` variables are read by `application-dev.yml`.
- `PROD_*` variables are read by `application-prod.yml`.

If you run only the dev profile, prod variables may stay empty. If you run the
prod profile, prod variables must be filled.

There are no hardcoded fallback secrets or passwords in Spring configuration.
If a required environment variable is missing, the application must fail during
startup instead of silently using `change_me` values.

## Docker layout

The project keeps the application and PostgreSQL as separate Docker Compose
contexts.

Dev:

- `docker-compose.yml` - dev backend only.
- `docker-compose.db.yml` - dev PostgreSQL only.

Prod:

- `docker-compose.prod.yml` - prod backend only.
- `docker-compose.prod.db.yml` - prod PostgreSQL only, used only if production
  PostgreSQL does not already exist.

Expected dev Docker names:

```text
fox-url-shortener-dev
  fox-url-shortener-dev

postgres-infra-dev
  postgres-server-dev
```

Expected prod Docker names:

```text
fox-url-shortener
  fox-url-shortener

postgres-infra
  postgres-server
```

The backend compose files do not own PostgreSQL. PostgreSQL can already exist,
or it can be started separately with the matching database compose file.

For Docker backend startup, the compose files intentionally override the
profile database address:

- `*_DB_HOST` / `*_DB_PORT` are the database address used by local Java startup
  and by the startup script availability check.
- `*_DB_DOCKER_HOST` / `*_DB_EXTERNAL_PORT` are the database address used by the
  backend container.

When the database is the matching compose PostgreSQL container, these ports are
usually the same as the published host port. When the database already exists,
fill both pairs with the address that is reachable from the matching runtime.

## Local dev run

Use this when you want to run the project from its own folder with dev settings.

```bash
cd /Users/denysrud/Servers/fox-url-shortener
cp .env.example .env
# Fill .env values.
```

Quick dev startup:

```bash
./scripts/docker-up.sh
```

The script reads `SPRING_PROFILES_ACTIVE` from `.env`. For the `dev` profile,
it checks whether the configured PostgreSQL is available. If PostgreSQL is
available, it starts only the backend. If PostgreSQL is not available, it starts
the dev PostgreSQL container from `docker-compose.db.yml`, waits for it, and
then starts the backend from `docker-compose.yml`.

Manual Docker Compose startup remains available when you want to control the
database and backend separately.

Start dev PostgreSQL only:

```bash
docker compose -f docker-compose.db.yml up -d
```

Start dev backend only:

```bash
docker compose up -d --build
```

Check containers:

```bash
docker compose -f docker-compose.db.yml ps
docker compose ps
```

Application:

```text
http://localhost:${DEV_APP_EXTERNAL_PORT}
```

Swagger:

```text
http://localhost:${DEV_APP_EXTERNAL_PORT}/swagger-ui/index.html
```

Stop only the dev backend:

```bash
docker compose down --remove-orphans
```

Stop and remove the dev PostgreSQL container and database volume:

```bash
docker compose -f docker-compose.db.yml down -v --remove-orphans
```

Do not use `down -v` on production or on any database with important data.

## Local dev run from IntelliJ IDEA

The application entry point is:

```text
src/main/java/com/fox/urlshortener/AppLauncher.java
```

It is located in the root package:

```text
com.fox.urlshortener
```

Before running from IntelliJ:

1. Install JDK 21 and import the project as a Gradle project.
2. Create `.env` from `.env.example` and fill dev values.
3. Start PostgreSQL if it is not already running:

```bash
cd /Users/denysrud/Servers/fox-url-shortener
docker compose -f docker-compose.db.yml up -d
```

4. Stop the Docker backend if it is running, because IntelliJ uses the same
   application port:

```bash
docker compose down --remove-orphans
```

5. Open `AppLauncher.java` and click Run next to the `main` method.

The default Spring profile is `dev`, so running `AppLauncher.java` directly from
IntelliJ starts the application with `application-dev.yml`.

When started successfully, the log should contain a dev profile message and
Tomcat should start on the configured dev port, for example:

```text
No active profile set, falling back to 1 default profile: "dev"
Tomcat started on the configured dev port
```

If PostgreSQL is not running, the application will fail during startup. That is
expected for a real PostgreSQL-backed application: running the Java entry point
does not start Docker containers automatically.

## Local prod-profile run

Use this only when you want to test production profile behavior from the project
folder.

If prod PostgreSQL does not already exist, start it:

```bash
cd /Users/denysrud/Servers/fox-url-shortener
docker compose -f docker-compose.prod.db.yml up -d
```

Start the prod backend:

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

Check containers:

```bash
docker compose -f docker-compose.prod.db.yml ps
docker compose -f docker-compose.prod.yml ps
```

For a real production deployment, use production-safe values in `.env`, such as:

- HTTPS URLs
- secure cookies
- strong private secrets
- production database credentials

If production PostgreSQL already exists as `postgres-infra / postgres-server`,
do not start `docker-compose.prod.db.yml`. Fill the prod database variables for
the existing database and start only the prod backend.

If production PostgreSQL does not exist yet, keep the production database
variables filled and create it explicitly:

```bash
docker compose -f docker-compose.prod.db.yml up -d
```

Then start the prod backend.

The production database compose identity/image/internal-port variables are only
needed when you want to create PostgreSQL from `docker-compose.prod.db.yml`.
They are not needed for a backend-only start against an already existing
database.

## Optional startup script

The project includes an optional startup helper:

```bash
./scripts/docker-up.sh
```

The script reads `SPRING_PROFILES_ACTIVE` from `.env`.

For the `dev` profile, it checks whether the configured PostgreSQL is available.
If PostgreSQL is available, the script starts only the backend. If PostgreSQL is
not available, the script starts the dev PostgreSQL container from
`docker-compose.db.yml`, waits until it is available, and then starts the dev
backend from `docker-compose.yml`.

For the `prod` profile, the script does not create a production database
automatically. If the configured production PostgreSQL is not available, the
script stops and prints an instruction to start the database explicitly.
If production database compose variables are missing, the script prints which
ones must be filled before using `docker-compose.prod.db.yml`.

The script does not write temporary runtime environment overrides. Docker
database host and port overrides are declared directly in the compose files.
For database checks, the script prefers a real PostgreSQL connection through
`psql`, then `pg_isready`, then a temporary PostgreSQL Docker client, and only
falls back to a plain TCP port check when no PostgreSQL-aware check is
available.
If PostgreSQL is reachable but the configured database/user/password do not
match the existing volume, the script stops with a credential-specific message.
For local dev only, a stale database volume can be recreated with:

```bash
docker compose -f docker-compose.db.yml down -v --remove-orphans
./scripts/docker-up.sh
```

Manual Docker Compose commands remain the source of truth:

```bash
docker compose -f docker-compose.db.yml up -d
docker compose up -d --build
```

The script is only a convenience wrapper for local startup.

## PostgreSQL checks

Check the dev PostgreSQL container:

```bash
docker compose -f docker-compose.db.yml ps
```

Check database tables:

```bash
docker compose -f docker-compose.db.yml exec db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "\dt"'
```

Check Flyway history:

```bash
docker compose -f docker-compose.db.yml exec db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "select version, description, success from flyway_schema_history order by installed_rank;"'
```

Check the app endpoint:

```bash
curl -I http://localhost:${DEV_APP_EXTERNAL_PORT}/swagger-ui/index.html
```

If port `5433` is already allocated, another local PostgreSQL container is
already using that port. Either stop the old container or change
`DEV_DB_EXTERNAL_PORT` and `DEV_DB_PORT` in `.env` to another free port.

## Environment

`.env` contains real values and is ignored by Git. `.env.example` contains the
same keys grouped the same way, but without private values.

### Project identity

| Variable | Description |
| --- | --- |
| `APP_NAME` | Spring application name. |
| `APP_IMAGE` | Docker image name built by Docker Compose. |
| `SPRING_PROFILES_ACTIVE` | Default local profile value. The application also has `dev` as default profile in `application.yml`. |

### Common Spring settings

| Variable | Description |
| --- | --- |
| `JPA_DDL_AUTO` | Hibernate schema mode, usually `validate`. |
| `JPA_OPEN_IN_VIEW` | Whether Spring Open Session in View is enabled. |
| `FLYWAY_ENABLED` | Whether Flyway migrations are enabled. |
| `JACKSON_DEFAULT_PROPERTY_INCLUSION` | JSON serialization inclusion setting. |
| `SERVER_FORWARD_HEADERS_STRATEGY` | Reverse-proxy forwarded header strategy. |
| `SPRINGDOC_API_DOCS_PATH` | OpenAPI docs path. |
| `SPRINGDOC_SWAGGER_UI_PATH` | Swagger UI path. |

### Common application settings

| Variable | Description |
| --- | --- |
| `SHORT_LINK_CODE_MIN_LENGTH` | Minimum generated short-code length. |
| `SHORT_LINK_CODE_MAX_LENGTH` | Maximum generated short-code length. |
| `SHORT_LINK_DEFAULT_EXPIRATION_DAYS` | Default short-link expiration period. |
| `LOG_SECURITY_CONFIG_LEVEL` | Logging level for noisy Spring Security configuration warnings. |
| `LOG_SPRINGDOC_INITIALIZER_LEVEL` | Logging level for noisy SpringDoc startup warnings. |

### Dev Docker identity

| Variable | Description |
| --- | --- |
| `DEV_APP_PROJECT_NAME` | Docker Compose project name for the dev backend. |
| `DEV_APP_CONTAINER_NAME` | Docker container name for the dev backend. |
| `DEV_DB_PROJECT_NAME` | Docker Compose project name for dev PostgreSQL. |
| `DEV_DB_CONTAINER_NAME` | Docker container name for dev PostgreSQL. |

### Dev profile variables

| Variable | Description |
| --- | --- |
| `DEV_APP_EXTERNAL_PORT` | Host port for dev backend. |
| `DEV_APP_INTERNAL_PORT` | Container/server port for dev backend. |
| `DEV_DB_HOST` | Database host reachable from IntelliJ/local Java and the startup script. Usually `localhost`. |
| `DEV_DB_PORT` | Database port reachable from IntelliJ/local Java and the startup script. |
| `DEV_DB_DOCKER_HOST` | Database host reachable from the backend container. Usually `host.docker.internal`. |
| `DEV_DB_NAME` | Dev database name. |
| `DEV_DB_USERNAME` | Dev database username. |
| `DEV_DB_PASSWORD` | Dev database password. |
| `DEV_DB_IMAGE` | PostgreSQL image for dev database container. |
| `DEV_DB_EXTERNAL_PORT` | Host port exposed by dev PostgreSQL and used by the Docker backend through `DEV_DB_DOCKER_HOST`. |
| `DEV_DB_INTERNAL_HOST` | Reserved internal database host name for compose-style setups. |
| `DEV_DB_INTERNAL_PORT` | Internal PostgreSQL port inside the container. |
| `DEV_ADMIN_LOGIN` | Dev default admin login. |
| `DEV_ADMIN_PASSWORD` | Dev default admin password. |
| `DEV_ADMIN_DISPLAY_NAME` | Dev default admin display name. |
| `DEV_JWT_SECRET` | Dev JWT signing secret. |
| `DEV_JWT_ACCESS_EXPIRATION_MINUTES` | Dev access token lifetime. |
| `DEV_JWT_REFRESH_EXPIRATION_DAYS` | Dev refresh token lifetime. |
| `DEV_APP_BASE_URL` | Dev public API base URL. |
| `DEV_SHORT_URL_BASE_URL` | Dev public short URL base URL. |
| `DEV_REDIRECT_CACHE_MAX_AGE_SECONDS` | Dev redirect response cache lifetime in seconds. |
| `DEV_COOKIE_ACCESS_TOKEN_NAME` | Dev access cookie name. |
| `DEV_COOKIE_REFRESH_TOKEN_NAME` | Dev refresh cookie name. |
| `DEV_COOKIE_SECURE` | Whether dev cookies require HTTPS. |
| `DEV_COOKIE_SAME_SITE` | Dev SameSite cookie mode. |
| `DEV_COOKIE_DOMAIN` | Dev cookie domain. Can be empty for localhost. |
| `DEV_CORS_ALLOWED_ORIGINS` | Allowed dev frontend origins. |
| `DEV_FORWARDED_PROTO_HEADER` | Forwarded proto header name. |
| `DEV_FORWARDED_HOST_HEADER` | Forwarded host header name. |
| `DEV_FORWARDED_PORT_HEADER` | Forwarded port header name. |

### Prod Docker identity

| Variable | Description |
| --- | --- |
| `PROD_APP_PROJECT_NAME` | Docker Compose project name for the prod backend. |
| `PROD_APP_CONTAINER_NAME` | Docker container name for the prod backend. |
| `PROD_DB_PROJECT_NAME` | Docker Compose project name for prod PostgreSQL, if it needs to be created. |
| `PROD_DB_CONTAINER_NAME` | Docker container name for prod PostgreSQL, if it needs to be created. |

### Prod profile variables

| Variable | Description |
| --- | --- |
| `PROD_APP_EXTERNAL_PORT` | Host port for prod backend. |
| `PROD_APP_INTERNAL_PORT` | Container/server port for prod backend. |
| `PROD_DB_HOST` | Database host for prod profile when running locally/non-Docker and for the startup script availability check. |
| `PROD_DB_PORT` | Database port for prod profile when running locally/non-Docker and for the startup script availability check. |
| `PROD_DB_DOCKER_HOST` | Database host reachable from the prod backend container. |
| `PROD_DB_NAME` | Prod database name. |
| `PROD_DB_USERNAME` | Prod database username. |
| `PROD_DB_PASSWORD` | Prod database password. |
| `PROD_DB_IMAGE` | PostgreSQL image for prod database container, if it needs to be created. |
| `PROD_DB_EXTERNAL_PORT` | Host port exposed by prod PostgreSQL and used by the Docker backend through `PROD_DB_DOCKER_HOST`. Fill it even when the database already exists. |
| `PROD_DB_INTERNAL_HOST` | Reserved internal prod database host name for compose-style setups. |
| `PROD_DB_INTERNAL_PORT` | Internal PostgreSQL port inside the container. |
| `PROD_ADMIN_LOGIN` | Prod default admin login. |
| `PROD_ADMIN_PASSWORD` | Prod default admin password. |
| `PROD_ADMIN_DISPLAY_NAME` | Prod default admin display name. |
| `PROD_JWT_SECRET` | Prod JWT signing secret. |
| `PROD_JWT_ACCESS_EXPIRATION_MINUTES` | Prod access token lifetime. |
| `PROD_JWT_REFRESH_EXPIRATION_DAYS` | Prod refresh token lifetime. |
| `PROD_APP_BASE_URL` | Prod public API base URL. |
| `PROD_SHORT_URL_BASE_URL` | Prod public short URL base URL. |
| `PROD_REDIRECT_CACHE_MAX_AGE_SECONDS` | Prod redirect response cache lifetime in seconds. |
| `PROD_COOKIE_ACCESS_TOKEN_NAME` | Prod access cookie name. |
| `PROD_COOKIE_REFRESH_TOKEN_NAME` | Prod refresh cookie name. |
| `PROD_COOKIE_SECURE` | Whether prod cookies require HTTPS. |
| `PROD_COOKIE_SAME_SITE` | Prod SameSite cookie mode. |
| `PROD_COOKIE_DOMAIN` | Prod cookie domain. |
| `PROD_CORS_ALLOWED_ORIGINS` | Allowed prod frontend origins. |
| `PROD_FORWARDED_PROTO_HEADER` | Forwarded proto header name. |
| `PROD_FORWARDED_HOST_HEADER` | Forwarded host header name. |
| `PROD_FORWARDED_PORT_HEADER` | Forwarded port header name. |

## Quality

Run all checks:

```bash
./gradlew check
```

Run formatting and individual checks:

```bash
./gradlew spotlessApply
./gradlew spotlessCheck
./gradlew checkstyleMain checkstyleTest
./gradlew test jacocoTestReport jacocoTestCoverageVerification
```

Install pre-commit hook:

```bash
cp scripts/pre-commit .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit
```

## Curl examples

Register:

```bash
curl -X POST http://localhost:${DEV_APP_EXTERNAL_PORT}/api/v1/auth/register \
  -c /tmp/fox-url-shortener.cookies \
  -H 'Content-Type: application/json' \
  -d '{"login":"fox","password":"Password123"}'
```

Login:

```bash
curl -X POST http://localhost:${DEV_APP_EXTERNAL_PORT}/api/v1/auth/login \
  -c /tmp/fox-url-shortener.cookies \
  -H 'Content-Type: application/json' \
  -d '{"login":"fox","password":"Password123"}'
```

Current user:

```bash
curl -b /tmp/fox-url-shortener.cookies http://localhost:${DEV_APP_EXTERNAL_PORT}/api/v1/auth/me
```

Refresh session:

```bash
curl -X POST -b /tmp/fox-url-shortener.cookies -c /tmp/fox-url-shortener.cookies \
  http://localhost:${DEV_APP_EXTERNAL_PORT}/api/v1/auth/refresh
```

Create short link:

```bash
curl -X POST http://localhost:${DEV_APP_EXTERNAL_PORT}/api/v1/links \
  -b /tmp/fox-url-shortener.cookies \
  -H 'Content-Type: application/json' \
  -d '{"originalUrl":"https://example.com/some/very/long/url","expiresInDays":30}'
```

Get links:

```bash
curl -b /tmp/fox-url-shortener.cookies http://localhost:${DEV_APP_EXTERNAL_PORT}/api/v1/links
```

Disable link:

```bash
curl -X PATCH http://localhost:${DEV_APP_EXTERNAL_PORT}/api/v1/links/1/status \
  -b /tmp/fox-url-shortener.cookies \
  -H 'Content-Type: application/json' \
  -d '{"active":false}'
```

Hard delete link:

```bash
curl -X DELETE -b /tmp/fox-url-shortener.cookies \
  http://localhost:${DEV_APP_EXTERNAL_PORT}/api/v1/links/1/hard
```

Admin get users:

```bash
curl -b /tmp/fox-url-shortener-admin.cookies http://localhost:${DEV_APP_EXTERNAL_PORT}/api/v1/admin/users
```

Admin get user links:

```bash
curl -b /tmp/fox-url-shortener-admin.cookies \
  http://localhost:${DEV_APP_EXTERNAL_PORT}/api/v1/admin/users/1/links
```

Admin disable any link:

```bash
curl -X PATCH http://localhost:${DEV_APP_EXTERNAL_PORT}/api/v1/admin/links/1/status \
  -b /tmp/fox-url-shortener-admin.cookies \
  -H 'Content-Type: application/json' \
  -d '{"active":false}'
```

Redirect by code:

```bash
curl -i http://localhost:${DEV_APP_EXTERNAL_PORT}/aB12xZ
```
