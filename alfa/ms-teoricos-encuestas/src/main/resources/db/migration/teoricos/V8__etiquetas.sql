-- Etiquetas del banco: como se encuentra una pregunta cuando hay doscientas.
--
-- Moodle resuelve esto con "Categoria", que es UNA por pregunta. Elegimos
-- varias y libres, y no es capricho: una sola categoria obliga a decidir hoy
-- cual es el eje —¿por unidad? ¿por tema? ¿por dificultad?— y el profesor
-- descubre a mitad del cuatrimestre que necesitaba otro. Con etiquetas los ejes
-- conviven.
--
-- Lo que NO es: un vinculo a un curso. Un item pertenece al profesor, y es el
-- CUESTIONARIO el que se cuelga de la unidad de un curso. Atar el item a un
-- curso romperia la reutilizacion entre cohortes y, peor, meteria adentro de
-- nuestro modulo un concepto del Tema 02 (CI-01). La etiqueta es texto nuestro,
-- no una clave ajena.
--
-- Tabla aparte y no un text[] en `item`: asi se puede indexar por etiqueta y
-- listar el vocabulario del profesor con un GROUP BY, que es lo que alimenta el
-- autocompletado.
--
-- Cuelga de `item` y NO de `item_version`, por la misma razon que `estado`
-- (ver V7): la version es inmutable por trigger, y reetiquetar no es publicar
-- una version nueva — el contenido de la pregunta no cambio.

CREATE TABLE teoricos.item_etiqueta (
  item_id  uuid        NOT NULL REFERENCES teoricos.item(id),
  etiqueta varchar(40) NOT NULL,
  PRIMARY KEY (item_id, etiqueta)
);

-- El filtro del banco es "mis items con esta etiqueta": se entra por etiqueta.
CREATE INDEX ix_item_etiqueta_etiqueta ON teoricos.item_etiqueta (etiqueta);

COMMENT ON TABLE teoricos.item_etiqueta IS
    'Etiquetas libres del profesor sobre sus propios items. No es un vinculo a un curso (CI-01).';

COMMENT ON COLUMN teoricos.item_etiqueta.etiqueta IS
    'Normalizada por la aplicacion: recortada, en minusculas y sin repetidos dentro del mismo item.';

-- DELETE, que app_teoricos no tenia: reetiquetar un item es borrar las filas
-- viejas y escribir las nuevas. Es la unica tabla del esquema donde borrar es
-- correcto — una etiqueta no es historia que haya que conservar, a diferencia
-- de una version o una correccion (D-04, CI-44), que tienen baja logica.
GRANT DELETE ON teoricos.item_etiqueta TO app_teoricos;
