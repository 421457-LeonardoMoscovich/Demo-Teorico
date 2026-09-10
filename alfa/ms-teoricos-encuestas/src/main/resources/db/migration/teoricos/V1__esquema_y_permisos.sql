-- Alfa: solo el esquema `teoricos`. Los tres esquemas del modulo `encuestas`
-- (catalogo, cumplimiento, respuestas) y sus roles entran con G04-HU06.
-- Esta migracion corre con el rol app_owner, el unico con DDL.

CREATE SCHEMA IF NOT EXISTS teoricos AUTHORIZATION app_owner;

GRANT USAGE ON SCHEMA teoricos TO app_teoricos;

GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA teoricos TO app_teoricos;

-- Sin esto, cada migracion futura crea tablas que app_teoricos no puede leer
-- y nadie entiende por que. Ver G04-HU06 paso 2.
ALTER DEFAULT PRIVILEGES FOR ROLE app_owner IN SCHEMA teoricos
    GRANT SELECT, INSERT, UPDATE ON TABLES TO app_teoricos;

-- gen_random_uuid() es nativo desde PostgreSQL 13: no hace falta pgcrypto
-- y por lo tanto no hace falta un superusuario.
