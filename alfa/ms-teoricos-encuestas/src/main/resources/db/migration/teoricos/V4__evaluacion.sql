-- Recepcion del despacho del Tema 03 (CI-22) y su correccion.

CREATE TABLE teoricos.evaluacion (
  id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  -- CI-25: la idempotencia del despacho puesta en el motor, no en un if.
  entrega_id       uuid        NOT NULL UNIQUE,
  desafio_id       uuid        NOT NULL,
  alumno_id        uuid        NOT NULL,
  curso_cohorte_id uuid        NOT NULL,
  intento          int         NOT NULL,
  contenido_id     uuid        NOT NULL REFERENCES teoricos.contenido(id),
  nota             int         NULL,
  estado           varchar(20) NOT NULL,   -- EN_CURSO | FINAL | SIN_CORRECCION
  corrector        varchar(16) NULL,       -- AUTOMATICO | LLM | HUMANO
  revision         int         NOT NULL DEFAULT 1,
  creada_en        timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX ix_evaluacion_alumno ON teoricos.evaluacion (alumno_id, desafio_id);

-- La estampa de version (CI-12, CI-53): que vio este alumno cuando respondio.
CREATE TABLE teoricos.respuesta (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  evaluacion_id   uuid  NOT NULL REFERENCES teoricos.evaluacion(id),
  item_version_id uuid  NOT NULL REFERENCES teoricos.item_version(id),
  contenido       jsonb NOT NULL,
  respondida_en   timestamptz NOT NULL DEFAULT now(),
  UNIQUE (evaluacion_id, item_version_id)
);

CREATE TABLE teoricos.evaluacion_detalle (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  evaluacion_id   uuid NOT NULL REFERENCES teoricos.evaluacion(id),
  item_version_id uuid NOT NULL REFERENCES teoricos.item_version(id),
  orden           int  NOT NULL,
  puntaje         int  NOT NULL,
  obtenido        int  NOT NULL,
  UNIQUE (evaluacion_id, item_version_id)
);
