-- Borrador / listo (CI-59), que Moodle llama "Estado de pregunta".
--
-- Tapa un agujero que teniamos: hoy un item a medio cargar entra a un
-- cuestionario igual que uno terminado, y el alumno se lo come.
--
-- La columna va en `item` y NO en `item_version`, y eso no es comodidad: una
-- version es inmutable por trigger (D-04), asi que si el estado viviera ahi,
-- pasar de borrador a listo obligaria a publicar una version nueva. Y no es una
-- version nueva: el contenido de la pregunta no cambio, cambio la decision del
-- profesor sobre si ya se puede usar. Versionar eso ensuciaria el historial que
-- D-04 existe para mantener legible.
--
-- DEFAULT 'LISTO' para las filas que ya existen: nadie las marco borrador, y
-- ponerlas en borrador sacaria de circulacion items que hoy estan en uso.

ALTER TABLE teoricos.item
    ADD COLUMN estado varchar(16) NOT NULL DEFAULT 'LISTO';

-- El banco se filtra seguido por "lo que puedo usar", que es siempre lo mismo:
-- del profesor, vigente y listo.
CREATE INDEX ix_item_profesor_estado ON teoricos.item (profesor_id, estado);

COMMENT ON COLUMN teoricos.item.estado IS
    'BORRADOR | LISTO (CI-59). Un BORRADOR no se puede componer en un cuestionario.';
