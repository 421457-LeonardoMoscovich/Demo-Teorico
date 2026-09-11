-- La retroalimentacion del item (CI-58): lo que el alumno lee DESPUES de que
-- su respuesta ya fue corregida.
--
-- Columna propia y no un campo mas adentro de `criterio`, aunque criterio ya
-- tenga la garantia de no viajar al alumno. El motivo es que son dos cosas
-- distintas: `criterio` dice COMO SE PUNTUA —lo consume el corrector— y
-- `devolucion` dice QUE SE LE EXPLICA —no lo consume nadie mas que la pantalla
-- de resultado—. Se nota sobre todo en ABIERTA, donde el criterio es la rubrica
-- que guia a quien corrige: meter ahi adentro el texto que lee el alumno seria
-- juntar el instructivo del corrector con la devolucion del corregido.
--
-- NULL en todas las filas que ya existen, y eso esta bien: un item sin
-- devolucion se comporta como siempre. No hay nada que migrar.
--
-- Forma del jsonb:
--   { "general": "texto para cualquiera que haya contestado",
--     "porOpcion": [ { "id": "a", "texto": "por que esa opcion esta bien" } ] }

ALTER TABLE teoricos.item_version ADD COLUMN devolucion jsonb NULL;

COMMENT ON COLUMN teoricos.item_version.devolucion IS
    'Retroalimentacion del item (CI-58). general + porOpcion. Nunca viaja en vista-alumno.';
