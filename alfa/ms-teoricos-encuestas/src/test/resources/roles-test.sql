-- Los mismos roles que docker/postgres/00-roles.sql, para el contenedor de test.
-- La base del contenedor de Testcontainers se llama `test`.

CREATE ROLE app_owner    LOGIN PASSWORD 'owner';
CREATE ROLE app_teoricos LOGIN PASSWORD 'teoricos';

REVOKE ALL ON SCHEMA public FROM PUBLIC;

GRANT CONNECT ON DATABASE test TO app_owner, app_teoricos;
GRANT CREATE  ON DATABASE test TO app_owner;
