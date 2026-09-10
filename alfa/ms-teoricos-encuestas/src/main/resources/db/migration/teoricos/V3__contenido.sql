-- Composicion del cuestionario. Antes se llamaba `desafio_teorico`; con CI-03
-- el desafio es del Tema 03 y lo nuestro es el CONTENIDO.

CREATE TABLE teoricos.contenido (
  id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  profesor_id      uuid        NOT NULL,
  curso_cohorte_id uuid        NOT NULL,
  titulo           varchar(200) NOT NULL,
  version          int         NOT NULL DEFAULT 1,
  escala           varchar(16) NOT NULL,   -- PORCENTUAL | LIBRE
  baja_logica      timestamptz NULL,
  creado_en        timestamptz NOT NULL DEFAULT now()
);

-- CI-13: la referencia al item es FLOTANTE. Guardar item_version_id aca
-- pinnearia la version y romperia RF-CUR-05. La estampa vive en `respuesta`.
CREATE TABLE teoricos.contenido_item (
  contenido_id uuid NOT NULL REFERENCES teoricos.contenido(id),
  item_id      uuid NOT NULL REFERENCES teoricos.item(id),
  orden        int  NOT NULL,
  puntaje      int  NOT NULL CHECK (puntaje > 0),
  PRIMARY KEY (contenido_id, item_id),
  UNIQUE (contenido_id, orden)
);
