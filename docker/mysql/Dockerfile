FROM mysql:8.4

COPY backend/sql/init.sql /docker-entrypoint-initdb.d/01-schema-and-accounts.sql
COPY docker/mysql/02-demo-accounts.sql /docker-entrypoint-initdb.d/02-demo-accounts.sql
