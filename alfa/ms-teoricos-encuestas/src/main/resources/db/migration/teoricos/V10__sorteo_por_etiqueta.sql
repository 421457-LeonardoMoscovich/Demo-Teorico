-- Preguntas al azar por etiqueta: el cuestionario deja de ser SOLO una lista y
-- puede llevar ademas una REGLA — "cinco de microservicios, 20 puntos cada una".
--
-- Que el sorteo sea por ALUMNO y no una tirada unica al armar es la decision de
-- fondo: dos companeros reciben preguntas distintas del mismo cuestionario. Lo
-- que lo hace posible sin romper CI-19 es que el sorteo se DERIVA de
-- (contenido_id, alumno_id) con la misma maquina que el barajado
-- (BarajadorDeterministico): la misma entrada da siempre la misma salida, asi
-- que recargar devuelve lo mismo y no hay una sola fila que escribir al leer.
--
-- La poblacion es VIVA: se sortea sobre los items que existan con esa etiqueta
-- en el momento de la lectura. Cargar una pregunta nueva enriquece los
-- cuestionarios ya publicados. La contracara, y hay que decirla: dos alumnos
-- que rinden en dias distintos pueden haber sorteado de poblaciones distintas.
-- Para un autoevaluativo es la gracia; para un parcial hay que saberlo.
--
-- Una regla por cuestionario, no varias (por eso contenido_id es la PK). El
-- cuestionario mixto que se quiso es "estas dos las tienen todos, y tres al
-- azar de esta etiqueta": las fijas viven en contenido_item, como siempre.
CREATE TABLE teoricos.contenido_regla (
  contenido_id uuid PRIMARY KEY REFERENCES teoricos.contenido(id),
  etiqueta     varchar(40) NOT NULL,
  cuantos      int NOT NULL CHECK (cuantos > 0),
  puntaje      int NOT NULL CHECK (puntaje > 0)
);

COMMENT ON TABLE teoricos.contenido_regla IS
    'Sorteo por etiqueta. El subconjunto de cada alumno se deriva de (contenido_id, alumno_id) y NO se guarda: la lectura sigue sin estado (CI-19).';

COMMENT ON COLUMN teoricos.contenido_regla.etiqueta IS
    'Normalizada igual que en item_etiqueta: recortada y en minusculas.';

COMMENT ON COLUMN teoricos.contenido_regla.puntaje IS
    'Lo que vale CADA pregunta sorteada. Uniforme a proposito: el peso no puede depender de cual le toco a quien, o dos alumnos rendirian examenes de pesos distintos.';

-- Sin DELETE, como el resto del esquema: una composicion no se borra. La
-- unica tabla donde borrar es correcto sigue siendo item_etiqueta (ver V8).
GRANT SELECT, INSERT, UPDATE ON teoricos.contenido_regla TO app_teoricos;
