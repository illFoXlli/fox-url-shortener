# Fox URL Shortener

Training REST API service for shortening long URLs. A user can register,
authenticate, create short links, manage them, and view redirect statistics.
Opening a short URL is public; all management API endpoints are under `/api/v1`.

## Stack

Java 21, Gradle, Spring Boot, Spring Web, Spring Security, Spring Data JPA,
Spring Data Redis, PostgreSQL, Redis, Flyway, JWT, Springdoc OpenAPI, JUnit 5,
Mockito, Testcontainers, Docker, GitHub Actions, Checkstyle, Spotless, JaCoCo.

## Package layout

Feature packages are split by responsibility first. Interfaces and their
implementations stay together in the responsibility package:

- `controller` - HTTP controllers.
- `dto` - request and response records used by controllers and services.
- `model` - JPA entities, enums, and domain models.
- `repository` - Spring Data repositories.
- `security` - auth/security contracts and their implementations.
- `scheduler` - scheduled jobs, when a feature needs them.
- `service` - service/cache contracts and their implementations.
- `support` - internal helpers that do not need a separate interface.

This layout is used for `auth`, `admin`, and `link`.

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
- Redis connection
- redirect cache settings
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

The project keeps the application, PostgreSQL, and Redis as separate Docker
Compose contexts.

Dev:

- `docker-compose.yml` - dev backend only.
- `docker-compose.db.yml` - dev PostgreSQL only.
- `docker-compose.redis.yml` - dev Redis only.

Prod:

- `docker-compose.prod.yml` - prod backend only.
- `docker-compose.prod.db.yml` - prod PostgreSQL only, used only if production
  PostgreSQL does not already exist.
- `docker-compose.prod.redis.yml` - prod Redis only, used only if production
  Redis does not already exist.

Expected dev Docker names:

```text
fox-url-shortener-dev
  fox-url-shortener-dev

postgres-infra-dev
  postgres-server-dev

redis-infra-dev
  redis-server-dev
```

Expected prod Docker names:

```text
fox-url-shortener
  fox-url-shortener

postgres-infra
  postgres-server

redis-infra
  redis-server
```

The backend compose files do not own PostgreSQL or Redis. Infrastructure can
already exist, or it can be started separately with the matching compose file.

For Docker backend startup, the compose files intentionally override the
profile database address:

- `*_DB_HOST` / `*_DB_PORT` are the database address used by local Java startup
  and by the startup script availability check.
- `*_DB_DOCKER_HOST` / `*_DB_EXTERNAL_PORT` are the database address used by the
  backend container.
- `*_REDIS_HOST` / `*_REDIS_PORT` are the Redis address used by local Java
  startup and by the startup script availability check.
- `*_REDIS_DOCKER_HOST` / `*_REDIS_EXTERNAL_PORT` are the Redis address used by
  the backend container.

When the database is the matching compose PostgreSQL container, these ports are
usually the same as the published host port. When the database already exists,
fill both pairs with the address that is reachable from the matching runtime.
The same rule applies to Redis.

Redis is used as an optional redirect cache. If Redis is unavailable, the
application logs a warning and falls back to PostgreSQL for every redirect.
When Redis is available, redirect targets are cached by short code. Clicks are
counted in Redis and periodically flushed back to PostgreSQL.
The project-created Redis containers use an ACL user from `*_REDIS_USERNAME`;
the default Redis user is disabled. Redis is configured as a bounded cache with
`*_REDIS_MAXMEMORY` and `*_REDIS_MAXMEMORY_POLICY`.

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
then checks Redis. If Redis is not available, it starts the dev Redis container
from `docker-compose.redis.yml`, waits for it, and then starts the backend from
`docker-compose.yml`.

Manual Docker Compose startup remains available when you want to control the
database and backend separately.

Start dev PostgreSQL only:

```bash
docker compose -f docker-compose.db.yml up -d
```

Start dev Redis only:

```bash
docker compose -f docker-compose.redis.yml up -d
```

Start dev backend only:

```bash
docker compose up -d --build
```

Check containers:

```bash
docker compose -f docker-compose.db.yml ps
docker compose -f docker-compose.redis.yml ps
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

Stop and remove the dev Redis container and cache volume:

```bash
docker compose -f docker-compose.redis.yml down -v --remove-orphans
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

4. Start Redis if you want redirect caching locally. If Redis is not running,
   the application still starts and falls back to PostgreSQL on redirects:

```bash
docker compose -f docker-compose.redis.yml up -d
```

5. Stop the Docker backend if it is running, because IntelliJ uses the same
   application port:

```bash
docker compose down --remove-orphans
```

6. Open `AppLauncher.java` and click Run next to the `main` method.

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

If prod Redis does not already exist and you want to create it from this
project, start it explicitly:

```bash
docker compose -f docker-compose.prod.redis.yml up -d
```

Start the prod backend:

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

Check containers:

```bash
docker compose -f docker-compose.prod.db.yml ps
docker compose -f docker-compose.prod.redis.yml ps
docker compose -f docker-compose.prod.yml ps
```

For a real production deployment, use production-safe values in `.env`, such as:

- HTTPS URLs
- secure cookies
- strong private secrets
- production database credentials
- production Redis credentials

If production PostgreSQL already exists as `postgres-infra / postgres-server`,
do not start `docker-compose.prod.db.yml`. Fill the prod database variables for
the existing database and start only the prod backend.

If production Redis already exists as `redis-infra / redis-server`, do not
start `docker-compose.prod.redis.yml`. Fill the prod Redis variables for the
existing Redis service and start only the prod backend.

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

The same applies to the production Redis compose identity/image/internal-port
variables and `docker-compose.prod.redis.yml`.

## Optional startup script

The project includes an optional startup helper:

```bash
./scripts/docker-up.sh
```

The script reads `SPRING_PROFILES_ACTIVE` from `.env`.

For the `dev` profile, it checks whether the configured PostgreSQL is available.
If PostgreSQL is available, the script starts only the backend. If PostgreSQL is
not available, the script starts the dev PostgreSQL container from
`docker-compose.db.yml`, waits until it is available, then checks Redis. If
Redis is not available, it starts the dev Redis container from
`docker-compose.redis.yml`, waits until it is available, and then starts the dev
backend from `docker-compose.yml`.

For the `prod` profile, the script does not create a production database
automatically. If the configured production PostgreSQL is not available, the
script stops and prints an instruction to start the database explicitly.
If production database compose variables are missing, the script prints which
ones must be filled before using `docker-compose.prod.db.yml`.
If production Redis is not available, the script prints a warning and starts
the backend anyway, because Redis is an optional cache and the application
falls back to PostgreSQL.

The script does not write temporary runtime environment overrides. Docker
database host and port overrides are declared directly in the compose files.
For database checks, the script prefers a real PostgreSQL connection through
`psql`, then `pg_isready`, then a temporary PostgreSQL Docker client, and only
falls back to a plain TCP port check when no PostgreSQL-aware check is
available.
For Redis checks, it uses `redis-cli`, then a temporary Redis Docker client,
then a plain TCP port check.
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
docker compose -f docker-compose.redis.yml up -d
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

## Redis checks

Check the dev Redis container:

```bash
docker compose -f docker-compose.redis.yml ps
```

Check Redis ping:

```bash
docker compose -f docker-compose.redis.yml exec redis sh -c 'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli --user "$REDIS_USERNAME" -p "$REDIS_PORT" ping'
```

If port `6380` is already allocated, another local Redis container is already
using that port. Either stop the old container or change
`DEV_REDIS_EXTERNAL_PORT` and `DEV_REDIS_PORT` in `.env` to another free port.

Check the prod Redis container:

```bash
docker compose -f docker-compose.prod.redis.yml ps
```

Check prod Redis ping:

```bash
docker compose -f docker-compose.prod.redis.yml exec redis sh -c 'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli --user "$REDIS_USERNAME" -p "$REDIS_PORT" ping'
```

Check all dev redirect-cache entries:

```bash
docker compose -f docker-compose.redis.yml exec redis sh -c '
REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli --user "$REDIS_USERNAME" -p "$REDIS_PORT" --scan --pattern "dev:fox-url-shortener:redirect:*" |
while read key; do
  echo "KEY: $key"
  echo "TTL: $(REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli --user "$REDIS_USERNAME" -p "$REDIS_PORT" TTL "$key")"
  echo "VAL: $(REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli --user "$REDIS_USERNAME" -p "$REDIS_PORT" GET "$key")"
  echo
done
'
```

Check all prod redirect-cache entries:

```bash
docker compose -f docker-compose.prod.redis.yml exec redis sh -c '
REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli --user "$REDIS_USERNAME" -p "$REDIS_PORT" --scan --pattern "prod:fox-url-shortener:redirect:*" |
while read key; do
  echo "KEY: $key"
  echo "TTL: $(REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli --user "$REDIS_USERNAME" -p "$REDIS_PORT" TTL "$key")"
  echo "VAL: $(REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli --user "$REDIS_USERNAME" -p "$REDIS_PORT" GET "$key")"
  echo
done
'
```

Redirect-cache keys look like:

```text
prod:fox-url-shortener:redirect:ktoMpw:url
prod:fox-url-shortener:redirect:ktoMpw:clicks
```

The `url` key stores the redirect target. The `clicks` key stores accumulated
clicks until the scheduled flush writes them back to PostgreSQL.

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
| `DEV_REDIS_PROJECT_NAME` | Docker Compose project name for dev Redis. |
| `DEV_REDIS_CONTAINER_NAME` | Docker container name for dev Redis. |

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
| `DEV_REDIS_HOST` | Redis host reachable from IntelliJ/local Java and the startup script. Usually `localhost`. |
| `DEV_REDIS_PORT` | Redis port reachable from IntelliJ/local Java and the startup script. |
| `DEV_REDIS_DOCKER_HOST` | Redis host reachable from the backend container. Usually `host.docker.internal`. |
| `DEV_REDIS_USERNAME` | Dev Redis username. Usually empty for the default Redis user. |
| `DEV_REDIS_PASSWORD` | Dev Redis password. |
| `DEV_REDIS_IMAGE` | Redis image for dev Redis container. |
| `DEV_REDIS_EXTERNAL_PORT` | Host port exposed by dev Redis and used by the Docker backend through `DEV_REDIS_DOCKER_HOST`. |
| `DEV_REDIS_INTERNAL_HOST` | Reserved internal Redis host name for compose-style setups. |
| `DEV_REDIS_INTERNAL_PORT` | Internal Redis port inside the container. |
| `DEV_REDIS_MAXMEMORY` | Memory limit for the dev Redis cache, for example `64mb`. |
| `DEV_REDIS_MAXMEMORY_POLICY` | Dev Redis eviction policy, usually `allkeys-lru` for cache-only Redis. |
| `DEV_REDIS_TIMEOUT` | Dev Redis client command timeout, for example `1s`. |
| `DEV_REDIS_SSL_ENABLED` | Whether the dev Redis client uses SSL. |
| `DEV_REDIRECT_CACHE_ENABLED` | Whether Redis redirect caching is enabled in dev. |
| `DEV_REDIRECT_CACHE_KEY_PREFIX` | Dev Redis key prefix for redirect cache entries. |
| `DEV_REDIRECT_CACHE_TTL_SECONDS` | Maximum dev redirect URL cache TTL in seconds. |
| `DEV_REDIRECT_CACHE_CLICK_FLUSH_INTERVAL_MILLIS` | Interval for flushing dev Redis click counters to PostgreSQL. |
| `DEV_ADMIN_LOGIN` | Dev default admin login. |
| `DEV_ADMIN_PASSWORD` | Dev default admin password. |
| `DEV_ADMIN_DISPLAY_NAME` | Dev default admin display name. |
| `DEV_JWT_SECRET` | Dev JWT signing secret. |
| `DEV_JWT_ACCESS_EXPIRATION_MINUTES` | Dev access token lifetime. |
| `DEV_JWT_REFRESH_EXPIRATION_DAYS` | Dev refresh token lifetime. |
| `DEV_APP_BASE_URL` | Dev public API base URL. |
| `DEV_SHORT_URL_BASE_URL` | Dev public short URL base URL. |
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
| `PROD_REDIS_PROJECT_NAME` | Docker Compose project name for prod Redis, if it needs to be created. |
| `PROD_REDIS_CONTAINER_NAME` | Docker container name for prod Redis, if it needs to be created. |

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
| `PROD_REDIS_HOST` | Redis host for prod profile when running locally/non-Docker and for the startup script availability check. |
| `PROD_REDIS_PORT` | Redis port for prod profile when running locally/non-Docker and for the startup script availability check. |
| `PROD_REDIS_DOCKER_HOST` | Redis host reachable from the prod backend container. |
| `PROD_REDIS_USERNAME` | Prod Redis username. Usually empty for the default Redis user. |
| `PROD_REDIS_PASSWORD` | Prod Redis password. |
| `PROD_REDIS_IMAGE` | Redis image for prod Redis container, if it needs to be created. |
| `PROD_REDIS_EXTERNAL_PORT` | Host port exposed by prod Redis and used by the Docker backend through `PROD_REDIS_DOCKER_HOST`. Fill it even when Redis already exists. |
| `PROD_REDIS_INTERNAL_HOST` | Reserved internal prod Redis host name for compose-style setups. |
| `PROD_REDIS_INTERNAL_PORT` | Internal Redis port inside the container. |
| `PROD_REDIS_MAXMEMORY` | Memory limit for the prod Redis cache, for example `128mb`. |
| `PROD_REDIS_MAXMEMORY_POLICY` | Prod Redis eviction policy, usually `allkeys-lru` for cache-only Redis. |
| `PROD_REDIS_TIMEOUT` | Prod Redis client command timeout, for example `1s`. |
| `PROD_REDIS_SSL_ENABLED` | Whether the prod Redis client uses SSL. |
| `PROD_REDIRECT_CACHE_ENABLED` | Whether Redis redirect caching is enabled in prod. |
| `PROD_REDIRECT_CACHE_KEY_PREFIX` | Prod Redis key prefix for redirect cache entries. |
| `PROD_REDIRECT_CACHE_TTL_SECONDS` | Maximum prod redirect URL cache TTL in seconds. |
| `PROD_REDIRECT_CACHE_CLICK_FLUSH_INTERVAL_MILLIS` | Interval for flushing prod Redis click counters to PostgreSQL. |
| `PROD_ADMIN_LOGIN` | Prod default admin login. |
| `PROD_ADMIN_PASSWORD` | Prod default admin password. |
| `PROD_ADMIN_DISPLAY_NAME` | Prod default admin display name. |
| `PROD_JWT_SECRET` | Prod JWT signing secret. |
| `PROD_JWT_ACCESS_EXPIRATION_MINUTES` | Prod access token lifetime. |
| `PROD_JWT_REFRESH_EXPIRATION_DAYS` | Prod refresh token lifetime. |
| `PROD_APP_BASE_URL` | Prod public API base URL. |
| `PROD_SHORT_URL_BASE_URL` | Prod public short URL base URL. |
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

Successful `register`, `login`, and `refresh` responses include an
`accessToken` JWT in the response body and also set auth cookies. Protected API
requests may use the cookies or send the JWT as `Authorization: Bearer <token>`.

User link statistics are available through:

```text
GET /api/v1/links/{id}/stats
```

The stats response uses `ShortLinkStatsResponse`:

```json
{
  "id": 1,
  "code": "aB12xZ",
  "clickCount": 5,
  "active": true
}
```

Regular users can soft-delete links with `DELETE /api/v1/links/{id}` so their
link history and statistics remain available. Hard delete is available only in
the admin API through `DELETE /api/v1/admin/links/{linkId}/hard`.

List endpoints return Spring `Page` responses and accept standard zero-based
pagination query params:

```text
page=0&size=20&sort=createdAt,desc
```

This applies to:

- `GET /api/v1/links`
- `GET /api/v1/links/active`
- `GET /api/v1/admin/users`
- `GET /api/v1/admin/links`
- `GET /api/v1/admin/users/{userId}/links`
- `GET /api/v1/admin/users/{userId}/links/active`

Paged responses include items in `content` plus metadata such as `totalElements`,
`totalPages`, `number`, and `size`.

Opening an expired active short link returns `410 Gone`. Missing or disabled
short links still return `404 Not Found`.

A Postman collection is available at:

```text
postman/Shortener URL.postman_collection.json
```

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
curl -b /tmp/fox-url-shortener.cookies \
  'http://localhost:${DEV_APP_EXTERNAL_PORT}/api/v1/links?page=0&size=20&sort=createdAt,desc'
```

Get link stats:

```bash
curl -b /tmp/fox-url-shortener.cookies \
  http://localhost:${DEV_APP_EXTERNAL_PORT}/api/v1/links/1/stats
```

Disable link:

```bash
curl -X PATCH http://localhost:${DEV_APP_EXTERNAL_PORT}/api/v1/links/1/status \
  -b /tmp/fox-url-shortener.cookies \
  -H 'Content-Type: application/json' \
  -d '{"active":false}'
```

Soft delete link:

```bash
curl -X DELETE -b /tmp/fox-url-shortener.cookies \
  http://localhost:${DEV_APP_EXTERNAL_PORT}/api/v1/links/1
```

Admin get users:

```bash
curl -b /tmp/fox-url-shortener-admin.cookies \
  'http://localhost:${DEV_APP_EXTERNAL_PORT}/api/v1/admin/users?page=0&size=20&sort=createdAt,desc'
```

Admin get all links:

```bash
curl -b /tmp/fox-url-shortener-admin.cookies \
  'http://localhost:${DEV_APP_EXTERNAL_PORT}/api/v1/admin/links?page=0&size=20&sort=createdAt,desc'
```

Admin get user links:

```bash
curl -b /tmp/fox-url-shortener-admin.cookies \
  'http://localhost:${DEV_APP_EXTERNAL_PORT}/api/v1/admin/users/1/links?page=0&size=20&sort=createdAt,desc'
```

Admin disable any link:

```bash
curl -X PATCH http://localhost:${DEV_APP_EXTERNAL_PORT}/api/v1/admin/links/1/status \
  -b /tmp/fox-url-shortener-admin.cookies \
  -H 'Content-Type: application/json' \
  -d '{"active":false}'
```

Admin hard delete any link:

```bash
curl -X DELETE -b /tmp/fox-url-shortener-admin.cookies \
  http://localhost:${DEV_APP_EXTERNAL_PORT}/api/v1/admin/links/1/hard
```

Redirect by code:

```bash
curl -i http://localhost:${DEV_APP_EXTERNAL_PORT}/aB12xZ
```
