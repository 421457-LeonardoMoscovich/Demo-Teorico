-- Corre SOLO la primera vez que arranca el contenedor (volumen vacio).
-- Si cambias este archivo: docker compose down -v
--
-- Los roles son de la instancia, no del esquema, y app_owner tiene que existir
-- antes de que corra la primera migracion. Por eso vive fuera de Flyway.

CREATE ROLE app_owner    LOGIN PASSWORD 'owner';
CREATE ROLE app_teoricos LOGIN PASSWORD 'teoricos';

-- Sin esto, cualquier rol puede crear una tabla en `public` y usarla de puente
-- entre esquemas. En la alfa hay un solo esquema, pero la barrera se pone ahora
-- porque despues nadie se acuerda.
REVOKE ALL ON SCHEMA public FROM PUBLIC;

GRANT CONNECT ON DATABASE g04 TO app_owner, app_teoricos;

-- app_owner es el unico que crea objetos. Sin este GRANT no puede crear el
-- esquema y la primera migracion falla. app_teoricos NO lo tiene: no crea nada.
GRANT CREATE ON DATABASE g04 TO app_owner;

-- Base aparte para los tests de integracion, para que no se pisen con los
-- datos de la demo. Ver el README: ALFA_DB_URL.
CREATE DATABASE g04_test;
GRANT CONNECT ON DATABASE g04_test TO app_owner, app_teoricos;
GRANT CREATE  ON DATABASE g04_test TO app_owner;
