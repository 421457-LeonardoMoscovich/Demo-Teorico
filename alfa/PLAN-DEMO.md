# Plan de la demo — Grupo 4

> **Qué es este documento.** El plan de trabajo de la alfa mientras se prepara la defensa.
> No es diseño (eso está en `sprint-1/`) ni contrato (eso está en `CONTRATO-INTEGRACION-G04.md`):
> es qué se está construyendo, en qué orden y por qué.

---

## Objetivo y contexto

**Esto es una demo, no el Sprint 1.** El sprint todavía no arrancó: están los documentos y nada
más. La demo existe para defender *más o menos cuál es la idea*, no para entregar el sprint.

**Cuatro días hasta la defensa** (fijado el 2026-09-10).

Consecuencia directa en las prioridades: todo lo que se hace acá tiene que servir para **contar
la historia**, no para adelantar backlog. `G04-HU06` —los cuatro esquemas y roles, la historia
marcada como la más importante del sprint— **queda afuera a propósito**: es lo primero cuando
arranque el sprint de verdad, y no aporta nada a una demo.

---

## El problema que resuelve lo que se está construyendo

La alfa, como estaba, **empezaba en el medio**: un profesor que ya tiene un banco de ítems, en
un curso que era una constante hardcodeada (`CURSO = '11111111-…'` en `profesor-armar.ts`).

La demo nueva empieza donde empieza el profesor de verdad: **entra a un curso, elige una unidad
del roadmap, y ahí adentro crea un desafío.** Eso hace visible lo que el grupo tiene que
defender: **dónde encaja nuestra pieza en la plataforma, y qué NO es nuestro.**

### La regla que gobierna esta parte

De quién es cada cosa, verificado contra el PRD y el contrato:

| Concepto | Dueño |
|---|---|
| Curso y cohorte | **Tema 02** |
| Roadmap y unidades | **Tema 02** (RF-CUR-01/06) |
| Desafío dentro de la unidad | **Tema 03** |
| El cuestionario adentro del desafío | **nosotros, Tema 04** |

**Todo lo que no es nuestro vive en el stub.** Si el código de cursos y unidades terminara en
`ms-teoricos-encuestas`, la demo dejaría de ser una virtud y pasaría a ser la prueba de que no
entendimos el reparto. Bien hecho es al revés: se ve una plataforma completa y se ve que **una
sola caja es nuestra**.

### Dos cosas que esto destraba para la defensa

1. **La tabla de ruteo tipo → servicio la tiene el front, no el Tema 03** (CI-02 y CI-16: la
   misma regla ganando en dos momentos distintos del ciclo). Hoy es imposible de mostrar porque
   solo existe un tipo. Con el selector de tipo en la unidad —ofreciendo `Teórico` y
   `Práctico · Tema 05` deshabilitado— se ve de un vistazo.

2. **H-02, un hueco del PRD que ya detectamos y hoy no podemos exhibir.** El material de la
   unidad **no tiene dueño asignado**: el PDF de arquitectura le da el grafo de contenidos al
   Tema 10, y el PRD mete el roadmap en la sección de Cursos, que es del 02. Lo necesitamos
   nosotros para generar ítems, y lo va a necesitar el Tema 05 para generar consignas sobre la
   misma unidad. Con la pantalla de la unidad a la vista, hay pie para plantearlo en la sesión
   de integración. Por eso `Unidad` en el stub **no tiene materiales**: preferimos que se note
   el hueco antes que inventarle un dueño.

**Detalle para decir en voz alta antes de que alguien lo lea como olvido:** `unidadId` **no se
guarda en nuestro modelo, a propósito**. Viaja del front al Tema 03 junto a la ficha (CI-09),
porque lo que hace es ubicar el desafío en el roadmap, y eso es del 03.

---

## Orden de trabajo

| # | Trabajo | Est. | Estado |
|---|---|---|---|
| 1 | Sembrar datos de demo | ~2 h | ✅ **hecho** |
| 2 | Curso → unidad → desafío | ~14 h | ✅ **hecho** |
| 3 | Editar ítem desde el front | ~3 h | ✅ **hecho** |
| 4 | Guardar el borrador del intento | ~2 h | ✅ **hecho** |

> **Estado al 2026-09-10: las cuatro terminadas**, verificadas en el navegador de punta a punta
> y con los 52 tests del backend en verde. La demo está lista para ensayar.

### 1 · Sembrar datos de demo — HECHO

`teoricos/aplicacion/SembradorDeDemo.java`: siembra 8 ítems de los cuatro tipos con contenido
real de la materia (capas, API gateway, base por servicio, idempotencia, circuit breaker/saga/
outbox, niveles de aislamiento, ciclo de una request, saga con compensación).

- Corre **solo con `app.demo.sembrar=true`** y **solo si el banco está vacío**.
- Default `true` en `application.yml`; **`BaseIT` lo pone en `false`**, porque si no
  `BancoAjenoNoSeVeIT` dejaría de estar afirmando sobre lo que el propio test creó.
- Los ítems son de la profesora del adaptador falso: el banco es de su dueño (RF-USR-07), no
  existe un banco "del sistema".

**Ojo para el día de la demo:** el sembrador no toca un banco que ya tiene datos. La base local
`g04` quedó con ítems de prueba de las corridas de desarrollo ("Cual es la capital de Francia?"
y similares). Para arrancar limpio: `docker compose down -v && docker compose up --build`.

### 2 · Curso → unidad → desafío — HECHO

**Stub (`stub-tema-03`)**
- [x] `CursoController` (Tema 02): `GET /cursos`, `GET /cursos/{id}`. Dos cursos fijos con 4 y 3
      unidades. Ids de cohorte estables entre arranques.
- [x] `Desafio` gana `unidadId`; `POST /desafios` lo recibe; `GET /desafios` filtra por
      `?curso=` y `?unidad=`.

**Front**
- [x] `modelos.ts`: `Curso`, `Unidad`, y `unidadId` en `Desafio`.
- [x] `api.service.ts`: `cursos()`, `curso(id)`, `crearDesafio` con `unidadId`,
      `desafiosAbiertos(curso, unidad)`. `URL_TEMA_02` separada de `URL_TEMA_03` a propósito.
- [x] `paginas/cursos.ts` — lista de cursos, sirve a los dos roles.
- [x] `paginas/profesor-curso.ts` — roadmap con los desafíos de cada unidad + "Crear desafío"
      con el selector de tipo.
- [x] `paginas/alumno-curso.ts` — el mismo roadmap, con "Empezar".
- [x] `profesor-armar.ts` — toma `curso` y `unidad` de la query; si faltan, redirige al
      roadmap en vez de inventar un curso. Manda `unidadId` al Tema 03.
- [x] `app.routes.ts` — rutas nuevas de cursos para los dos roles.
- [x] `app.ts` — nav: "Cursos" y "Banco" en profesora, "Cursos" en alumno.
- [x] Estilos de las clases nuevas: `.unidad`, `.descripcion-unidad`, `.tipos-desafio`,
      `.lista.compacta`, `.fuente` (el rótulo de procedencia, en violeta en modo claro porque
      el cian no se lee como texto sobre blanco).
- [x] Redirecciones de `alumno-responder` / `alumno-resultado` a `/alumno/cursos`.
- [x] Borrado `paginas/alumno-desafios.ts`, que quedó sin uso.
- [x] Build + recorrido completo en el navegador, en los dos roles.

**Bug encontrado y arreglado en el camino:** en `profesor-armar.ts`, `titulo` era un campo común
y `listo` un `computed`. Un `computed` solo recalcula cuando cambia una *signal*, así que
escribir el título **no habilitaba el botón Publicar** hasta que algo más tocara la lista. En la
demo el profesor escribe el título al final, así que se habría visto. `titulo` pasó a ser signal.

### 3 · Editar ítem desde el front — HECHO

Botón "Editar" en cada fila del banco. Carga el ítem en el formulario y al guardar llama a
`PUT /teoricos/items/{id}`, que **publica la versión siguiente**.

- La cabecera lo dice explícito: *"Guardar no modifica la versión 1: publica la 2 y deja la
  anterior intacta. Quien ya respondió sobre la 1 se sigue corrigiendo con esa."* Es la regla
  más fácil de malinterpretar del módulo, así que se lee en pantalla, no se deduce.
- **El select de tipo se deshabilita al editar:** el tipo vive en `item` y no en
  `item_version`, así que es inmutable entre versiones por modelo. Se bloquea en vez de
  validarlo después.
- En los ítems de *ordenar*, el formulario muestra los elementos en el **orden correcto** (el
  del criterio), no en el orden mezclado del payload: es lo que el profesor necesita ver para
  poder corregirlo.

Verificado: editar un ítem lo dejó en `v2` con el enunciado nuevo, y el otro ítem del mismo
tipo siguió en `v1`.

### 4 · Guardar el borrador del intento — HECHO

`IntentoStore` gana `guardarBorrador` / `leerBorrador` / `limpiarBorrador`, en `sessionStorage`
y con clave por `entregaId`. Se guarda en cada cambio, se restaura al cargar la pantalla y se
borra al entregar bien.

- Se restaura **ítem por ítem y solo para los que siguen estando en la vista**: si el profesor
  editó el cuestionario en el medio, la versión del ítem cambia y esa respuesta vieja ya no
  aplica (CI-13).
- Cuando recupera algo, avisa: *"Recuperamos lo que habías contestado antes de recargar."*

Verificado con una recarga completa a mitad del examen: volvió con "3 de 3 contestadas", los
selects y el radio marcado; y al entregar, el borrador desapareció de `sessionStorage`.

---

## Lo que queda afuera, y por qué

**De la demo:**

- **Respuesta abierta + cola de corrección** (~12-16 h). Es la que mejor defiende el diseño —el
  puerto `Corrector` deja de ser una promesa— y destraba el modo `DIFERIDA` de la ficha (CI-07),
  el estado de espera del alumno, y `CI-47`, que hoy es un `// TODO` porque no hay ítems de
  corrección humana que rechazar. **No entra en cuatro días** y no cuenta una historia que las
  cuatro de arriba no cuenten mejor.
- **OpenAPI publicado** (C-04, ~2 h), **test de arquitectura ArchUnit** (HU02, ~4 h) e **i18n**
  (HU04, ~6 h). Son del sprint, no de la demo. Si sobra tiempo, OpenAPI es el más barato y es un
  entregable hacia los otros grupos.
- **Analítica de dificultad por ítem** (~4 h). Ya guardamos `evaluacion_detalle`: sale con un
  `GROUP BY`. Lo lindo no es la métrica sino el argumento — solo es posible porque los ítems
  tienen identidad versionada (D-04).
- **Desglose con el texto de la respuesta en vez de los ids.** Hoy muestra `i1 → d1 · i2 → d2`.
  Necesita que el endpoint de resultado devuelva el payload del ítem, que hoy no manda.

**Del producto, por contrato:**

- **Cronómetro y barajado de preguntas.** Suenan a mejora obvia de un examen, pero **CI-19 los
  verificó contra el PRD y no existen en la plataforma**. Agregarlos sería inventar producto, y
  encima rompería que la lectura del contenido no tenga estado.
- **Apelación (CI-35) y recálculo (CI-50).** Declarados y caros; el propio diseño los pone en
  "backlog posterior".
- **Notificar al alumno que su nota está lista.** **CI-37 lo prohíbe explícitamente**: se
  enteraría antes de que el Tema 03 aplique la penalidad por tardanza y el Tema 10 el XP.

---

## Cuando arranque el Sprint 1

El orden cambia por completo. `G04-HU06` (cuatro esquemas, cuatro roles de PostgreSQL sin
permisos cruzados) va primero: si los roles no quedan bien desde el arranque, cada línea de
persistencia escrita encima hay que revisarla. Después `HU02` (test de arquitectura), que es lo
que impide que el aislamiento se rompa desde el código, y recién ahí el módulo `encuestas`.
