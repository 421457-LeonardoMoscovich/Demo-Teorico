-- Como recorre el alumno el cuestionario: de a una consigna por pantalla, y si
-- puede volver sobre lo que ya paso.
--
-- Es del CONTENIDO y no una preferencia del alumno, porque es una regla de la
-- evaluacion: "esta no se puede volver atras" es una decision pedagogica de la
-- profesora, igual que el peso de cada pregunta. Si viviera en el front,
-- cualquiera la esquiva recargando la pagina y ademas no quedaria registrada
-- en ningun lado el dia que un alumno reclame.
--
-- Lo que NO cambia: la ficha de cinco campos que sale al Tema 03 (CI-04). El
-- 03 no necesita saber como se navega adentro del cuestionario — es justamente
-- lo que la caja opaca le oculta.
--
-- DEFAULT 'LIBRE' y NOT NULL: los cuestionarios que ya existen se comportan
-- como hasta ahora. Un default distinto cambiaria, en silencio, las reglas de
-- una evaluacion ya armada.
ALTER TABLE teoricos.contenido
  ADD COLUMN navegacion varchar(16) NOT NULL DEFAULT 'LIBRE';

COMMENT ON COLUMN teoricos.contenido.navegacion IS
    'LIBRE | SECUENCIAL. SECUENCIAL no deja volver a una pregunta ya pasada. Avanzar sin contestar se permite en los dos: la pregunta queda en blanco y vale 0.';
