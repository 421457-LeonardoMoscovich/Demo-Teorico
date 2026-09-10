-- La correccion humana (D-01) entra a la alfa: el puntaje de un item ABIERTA
-- no existe en el momento del despacho, aparece cuando el profesor lo pone.
--
-- Eso obliga a admitir el hueco en la base. `obtenido` deja de ser NOT NULL, y
-- ese NULL es el que significa "esto todavia lo tiene que mirar un humano":
-- no hace falta una columna `pendiente` que pueda contradecirlo.

ALTER TABLE teoricos.evaluacion_detalle ALTER COLUMN obtenido DROP NOT NULL;

-- Quien corrigio y cuando. Va en el DETALLE y no en la evaluacion porque en un
-- cuestionario mixto conviven items automaticos y humanos, y decir que la
-- evaluacion entera la corrigio HUMANO seria falso.
ALTER TABLE teoricos.evaluacion_detalle ADD COLUMN corregido_por uuid        NULL;
ALTER TABLE teoricos.evaluacion_detalle ADD COLUMN corregido_en  timestamptz NULL;

-- La cola del profesor se consulta seguido (el badge sondea) y siempre pregunta
-- por lo mismo: los detalles sin puntaje. Indice parcial, que solo indexa las
-- filas pendientes y se mantiene chico aunque la tabla crezca.
CREATE INDEX ix_detalle_pendiente
    ON teoricos.evaluacion_detalle (evaluacion_id)
    WHERE obtenido IS NULL;

COMMENT ON COLUMN teoricos.evaluacion.estado IS
    'EN_CURSO | EN_ESPERA (falta correccion humana) | FINAL | SIN_CORRECCION';
