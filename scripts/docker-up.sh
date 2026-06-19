#!/bin/sh
set -eu

project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$project_dir"

fail() {
  echo "error: $*" >&2
  exit 1
}

env_value() {
  awk -v key="$1" '
    index($0, key "=") == 1 {
      sub(/^[^=]*=/, "")
      sub(/^"/, "")
      sub(/"$/, "")
      sub(/^'\''/, "")
      sub(/'\''$/, "")
      print
      exit
    }
  ' .env
}

require_env() {
  value=$(env_value "$1")
  [ -n "$value" ] || fail "$1 is required"
}

if [ ! -f .env ]; then
  fail ".env is required. Copy .env.example to .env and fill it first."
fi

require_env APP_IMAGE
require_env SPRING_PROFILES_ACTIVE

spring_profile=$(env_value SPRING_PROFILES_ACTIVE)

case "$spring_profile" in
  dev)
    profile_prefix=DEV
    app_compose_file=docker-compose.yml
    db_compose_file=docker-compose.db.yml
    redis_compose_file=docker-compose.redis.yml

    require_env DEV_APP_PROJECT_NAME
    require_env DEV_APP_CONTAINER_NAME
    ;;
  prod)
    profile_prefix=PROD
    app_compose_file=docker-compose.prod.yml
    db_compose_file=docker-compose.prod.db.yml
    redis_compose_file=docker-compose.prod.redis.yml

    require_env PROD_APP_PROJECT_NAME
    require_env PROD_APP_CONTAINER_NAME
    ;;
  *)
    fail "SPRING_PROFILES_ACTIVE must be dev or prod"
    ;;
esac

for suffix in DB_HOST DB_PORT DB_DOCKER_HOST DB_NAME DB_USERNAME DB_PASSWORD DB_EXTERNAL_PORT; do
  require_env "${profile_prefix}_${suffix}"
done

for suffix in REDIS_HOST REDIS_PORT REDIS_DOCKER_HOST REDIS_USERNAME REDIS_EXTERNAL_PORT REDIS_TIMEOUT REDIS_SSL_ENABLED REDIRECT_CACHE_ENABLED REDIRECT_CACHE_KEY_PREFIX REDIRECT_CACHE_TTL_SECONDS REDIRECT_CACHE_CLICK_FLUSH_INTERVAL_MILLIS; do
  require_env "${profile_prefix}_${suffix}"
done

db_host=$(env_value "${profile_prefix}_DB_HOST")
db_port=$(env_value "${profile_prefix}_DB_PORT")
db_user=$(env_value "${profile_prefix}_DB_USERNAME")
db_password=$(env_value "${profile_prefix}_DB_PASSWORD")
db_name=$(env_value "${profile_prefix}_DB_NAME")
db_image=$(env_value "${profile_prefix}_DB_IMAGE")
redis_host=$(env_value "${profile_prefix}_REDIS_HOST")
redis_port=$(env_value "${profile_prefix}_REDIS_PORT")
redis_username=$(env_value "${profile_prefix}_REDIS_USERNAME")
redis_password=$(env_value "${profile_prefix}_REDIS_PASSWORD")
redis_image=$(env_value "${profile_prefix}_REDIS_IMAGE")

database_compose_missing_vars() {
  missing=""

  for suffix in DB_PROJECT_NAME DB_CONTAINER_NAME DB_IMAGE DB_INTERNAL_PORT; do
    key="${profile_prefix}_${suffix}"
    value=$(env_value "$key")

    if [ -z "$value" ]; then
      missing="${missing} ${key}"
    fi
  done

  echo "$missing"
}

redis_compose_missing_vars() {
  missing=""

  for suffix in REDIS_PROJECT_NAME REDIS_CONTAINER_NAME REDIS_IMAGE REDIS_USERNAME REDIS_PASSWORD REDIS_INTERNAL_PORT REDIS_MAXMEMORY REDIS_MAXMEMORY_POLICY; do
    key="${profile_prefix}_${suffix}"
    value=$(env_value "$key")

    if [ -z "$value" ]; then
      missing="${missing} ${key}"
    fi
  done

  echo "$missing"
}

docker_check_host() {
  case "$db_host" in
    localhost|127.0.0.1)
      echo "host.docker.internal"
      ;;
    *)
      echo "$db_host"
      ;;
  esac
}

redis_check_host() {
  case "$redis_host" in
    localhost|127.0.0.1)
      echo "host.docker.internal"
      ;;
    *)
      echo "$redis_host"
      ;;
  esac
}

database_is_available() {
  if command -v psql >/dev/null 2>&1; then
    PGPASSWORD=$db_password psql \
      -h "$db_host" \
      -p "$db_port" \
      -U "$db_user" \
      -d "$db_name" \
      -c "select 1" >/dev/null 2>&1
    return $?
  fi

  if command -v pg_isready >/dev/null 2>&1; then
    pg_isready \
      -h "$db_host" \
      -p "$db_port" \
      -U "$db_user" \
      -d "$db_name" >/dev/null 2>&1
    return $?
  fi

  if command -v docker >/dev/null 2>&1 && [ -n "$db_image" ]; then
    docker run --rm \
      --add-host=host.docker.internal:host-gateway \
      -e PGPASSWORD="$db_password" \
      "$db_image" \
      psql \
      -h "$(docker_check_host)" \
      -p "$db_port" \
      -U "$db_user" \
      -d "$db_name" \
      -c "select 1" >/dev/null 2>&1
    return $?
  fi

  if command -v nc >/dev/null 2>&1; then
    nc -z "$db_host" "$db_port" >/dev/null 2>&1
    return $?
  fi

  fail "Cannot check PostgreSQL availability. Install psql, pg_isready, nc, or Docker."
}

redis_is_available() {
  if command -v redis-cli >/dev/null 2>&1; then
    if [ -n "$redis_username" ] && [ -n "$redis_password" ]; then
      REDISCLI_AUTH="$redis_password" redis-cli \
        --user "$redis_username" \
        -h "$redis_host" \
        -p "$redis_port" \
        ping >/dev/null 2>&1
    elif [ -n "$redis_password" ]; then
      REDISCLI_AUTH="$redis_password" redis-cli \
        -h "$redis_host" \
        -p "$redis_port" \
        ping >/dev/null 2>&1
    else
      redis-cli -h "$redis_host" -p "$redis_port" ping >/dev/null 2>&1
    fi
    return $?
  fi

  if command -v docker >/dev/null 2>&1 && [ -n "$redis_image" ]; then
    if [ -n "$redis_username" ] && [ -n "$redis_password" ]; then
      docker run --rm \
        --add-host=host.docker.internal:host-gateway \
        -e REDISCLI_AUTH="$redis_password" \
        "$redis_image" \
        redis-cli \
        --user "$redis_username" \
        -h "$(redis_check_host)" \
        -p "$redis_port" \
        ping >/dev/null 2>&1
    elif [ -n "$redis_password" ]; then
      docker run --rm \
        --add-host=host.docker.internal:host-gateway \
        -e REDISCLI_AUTH="$redis_password" \
        "$redis_image" \
        redis-cli \
        -h "$(redis_check_host)" \
        -p "$redis_port" \
        ping >/dev/null 2>&1
    else
      docker run --rm \
        --add-host=host.docker.internal:host-gateway \
        "$redis_image" \
        redis-cli \
        -h "$(redis_check_host)" \
        -p "$redis_port" \
        ping >/dev/null 2>&1
    fi
    return $?
  fi

  if command -v nc >/dev/null 2>&1; then
    nc -z "$redis_host" "$redis_port" >/dev/null 2>&1
    return $?
  fi

  fail "Cannot check Redis availability. Install redis-cli, nc, or Docker."
}

database_port_is_open() {
  if command -v nc >/dev/null 2>&1; then
    nc -z "$db_host" "$db_port" >/dev/null 2>&1
    return $?
  fi

  if command -v pg_isready >/dev/null 2>&1; then
    pg_isready -h "$db_host" -p "$db_port" >/dev/null 2>&1
    return $?
  fi

  return 1
}

redis_port_is_open() {
  if command -v nc >/dev/null 2>&1; then
    nc -z "$redis_host" "$redis_port" >/dev/null 2>&1
    return $?
  fi

  return 1
}

wait_for_database() {
  echo "Waiting for PostgreSQL at ${db_host}:${db_port}..."

  attempt=1

  while [ "$attempt" -le 30 ]; do
    if database_is_available; then
      echo "PostgreSQL is available at ${db_host}:${db_port}"
      return 0
    fi

    sleep 1
    attempt=$((attempt + 1))
  done

  if database_port_is_open; then
    fail "PostgreSQL is reachable at ${db_host}:${db_port}, but login to database '${db_name}' as '${db_user}' failed. Check ${profile_prefix}_DB_NAME, ${profile_prefix}_DB_USERNAME, and ${profile_prefix}_DB_PASSWORD. For a stale local dev volume, recreate it with: docker compose -f ${db_compose_file} down -v --remove-orphans"
  fi

  fail "PostgreSQL is not available at ${db_host}:${db_port}"
}

wait_for_redis() {
  echo "Waiting for Redis at ${redis_host}:${redis_port}..."

  attempt=1

  while [ "$attempt" -le 30 ]; do
    if redis_is_available; then
      echo "Redis is available at ${redis_host}:${redis_port}"
      return 0
    fi

    sleep 1
    attempt=$((attempt + 1))
  done

  if redis_port_is_open; then
    fail "Redis is reachable at ${redis_host}:${redis_port}, but authentication failed. Check ${profile_prefix}_REDIS_PASSWORD."
  fi

  fail "Redis is not available at ${redis_host}:${redis_port}"
}

if database_is_available; then
  echo "Using configured PostgreSQL at ${db_host}:${db_port}"
elif [ "$spring_profile" = "prod" ]; then
  echo "Configured production PostgreSQL is not available at ${db_host}:${db_port}"
  if database_port_is_open; then
    echo "PostgreSQL port is reachable, but login to database '${db_name}' as '${db_user}' failed."
    echo "Check ${profile_prefix}_DB_NAME, ${profile_prefix}_DB_USERNAME, and ${profile_prefix}_DB_PASSWORD."
  fi
  echo "Production database will not be created automatically by this script."
  missing=$(database_compose_missing_vars)
  if [ -n "$missing" ]; then
    echo "Before creating production PostgreSQL from compose, fill:${missing}"
  fi
  echo "If you really need to create it, run:"
  echo "  docker compose -f ${db_compose_file} up -d"
  echo "Then run this script again."
  exit 1
else
  echo "Configured PostgreSQL is not available at ${db_host}:${db_port}"
  echo "Starting local dev PostgreSQL from ${db_compose_file}"

  missing=$(database_compose_missing_vars)
  [ -z "$missing" ] || fail "Cannot start local dev PostgreSQL. Missing:${missing}"

  docker compose -f "$db_compose_file" up -d

  wait_for_database
fi

if redis_is_available; then
  echo "Using configured Redis at ${redis_host}:${redis_port}"
elif [ "$spring_profile" = "prod" ]; then
  echo "warning: configured production Redis is not available at ${redis_host}:${redis_port}"
  if redis_port_is_open; then
    echo "Redis port is reachable, but authentication failed."
    echo "Check ${profile_prefix}_REDIS_PASSWORD."
  fi
  echo "Backend will start and fall back to PostgreSQL while Redis is unavailable."
  missing=$(redis_compose_missing_vars)
  if [ -n "$missing" ]; then
    echo "Before creating production Redis from compose, fill:${missing}"
  fi
  echo "If you need to create it, run:"
  echo "  docker compose -f ${redis_compose_file} up -d"
else
  echo "Configured Redis is not available at ${redis_host}:${redis_port}"
  echo "Starting local dev Redis from ${redis_compose_file}"

  missing=$(redis_compose_missing_vars)
  [ -z "$missing" ] || fail "Cannot start local dev Redis. Missing:${missing}"

  docker compose -f "$redis_compose_file" up -d

  wait_for_redis
fi

echo "Starting backend from ${app_compose_file}"

docker compose -f "$app_compose_file" up -d --build
