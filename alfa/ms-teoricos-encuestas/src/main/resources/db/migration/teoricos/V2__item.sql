-- G04-HU10: identidad estable + contenido versionado e inmutable (D-04).

CREATE TABLE teoricos.item (
  id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  profesor_id    uuid        NOT NULL,
  tipo           varchar(24) NOT NULL,
  version_actual int         NOT NULL DEFAULT 0,
  baja_logica    timestamptz NULL
);

CREATE INDEX ix_item_profesor_tipo ON teoricos.item (profesor_id, tipo);

CREATE TABLE teoricos.item_version (
  id        uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  item_id   uuid  NOT NULL REFERENCES teoricos.item(id),
  version   int   NOT NULL,
  enunciado text  NOT NULL,
  payload   jsonb NOT NULL,
  criterio  jsonb NULL,
  creada_en timestamptz NOT NULL DEFAULT now(),
  -- dos publicaciones concurrentes no pueden generar dos "version 3"
  UNIQUE (item_id, version)
);

CREATE INDEX ix_item_version_item ON teoricos.item_version (item_id);

-- La inmutabilidad la garantiza el motor, no la disciplina del equipo.
CREATE FUNCTION teoricos.f_item_version_inmutable() RETURNS trigger AS $$
BEGIN
  RAISE EXCEPTION 'item_version es inmutable: editar publica una version nueva (D-04)';
END $$ LANGUAGE plpgsql;

CREATE TRIGGER t_item_version_no_update
  BEFORE UPDATE ON teoricos.item_version
  FOR EACH ROW EXECUTE FUNCTION teoricos.f_item_version_inmutable();
