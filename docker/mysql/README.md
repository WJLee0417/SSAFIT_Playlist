# SSAFIT MySQL Docker

This service initializes a new MySQL volume with the project schema only.

## Initial schema

- Source: `ssafit/src/main/resources/sql/schema.sql`
- Mounted as: `/docker-entrypoint-initdb.d/01-schema.sql`
- Runs only when the named volume is empty and MySQL creates a fresh data directory.

## Local access

- Compose service name: `mysql`
- Container port: `3306`
- Host port: `${MYSQL_HOST_PORT:-3306}`
- STS/Spring Boot on the host can connect through `localhost:${MYSQL_HOST_PORT:-3306}`.
  If you change `MYSQL_HOST_PORT`, update the local Spring datasource URL for
  that run as well.

## Seed data

No seed data is loaded by default. Do not add personal accounts, Spotify OAuth
tokens, refresh tokens, Spotify user IDs, or local database dumps to Git.

If non-sensitive sample data is needed later, keep it separate from the schema
and mount it after `01-schema.sql` with a higher init filename.

## Schema changes

This project does not currently use Flyway or Liquibase. Keep the fresh-install
baseline in `ssafit/src/main/resources/sql/schema.sql`. Put future existing-DB
change scripts under `ssafit/src/main/resources/sql/migrations/` and apply them
manually until a migration tool is introduced.

Do not run volume-deleting commands automatically. Removing the named volume
destroys the local Docker database and is only for an explicitly confirmed reset.
