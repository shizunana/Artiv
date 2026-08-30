FROM mysql:8.4

COPY backend/sql/init.sql /tmp/artiv-init.sql
RUN awk '/^INSERT INTO `work`/{exit} {print}' /tmp/artiv-init.sql \
      > /docker-entrypoint-initdb.d/01-schema-and-accounts.sql \
    && rm /tmp/artiv-init.sql
COPY docker/mysql/02-demo-accounts.sql /docker-entrypoint-initdb.d/02-demo-accounts.sql
