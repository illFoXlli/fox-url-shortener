#!/bin/sh
set -eu

PGHOST="${DB_HOST:-host.docker.internal}"
PGPORT="${DB_PORT:-5432}"
APP_DB="${DB_NAME:-fox_url_shortener}"
APP_USER="${DB_USERNAME:-fox}"
APP_PASSWORD="${DB_PASSWORD:-change_me}"
ADMIN_USER="${FOX_DB_ADMIN_USERNAME:-fox_admin}"
ADMIN_PASSWORD="${FOX_DB_ADMIN_PASSWORD:-change_me}"

export PGPASSWORD="$ADMIN_PASSWORD"

until psql -h "$PGHOST" -p "$PGPORT" -U "$ADMIN_USER" -d postgres -c "select 1" >/dev/null 2>&1; do
  echo "Waiting for PostgreSQL at $PGHOST:$PGPORT..."
  sleep 2
done

psql -h "$PGHOST" -p "$PGPORT" -U "$ADMIN_USER" -d postgres <<SQL
do \$\$
begin
  if not exists (select from pg_roles where rolname = '$APP_USER') then
    create role $APP_USER login password '$APP_PASSWORD';
  else
    alter role $APP_USER with login password '$APP_PASSWORD';
  end if;
end
\$\$;
select 'create database $APP_DB owner $APP_USER'
where not exists (select from pg_database where datname = '$APP_DB')\\gexec
grant all privileges on database $APP_DB to $APP_USER;
SQL

echo "Database '$APP_DB' is ready."
