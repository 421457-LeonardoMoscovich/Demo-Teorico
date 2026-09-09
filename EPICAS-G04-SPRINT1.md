# Tema 04 — Teóricos y Encuestas
## Épicas del Sprint 1

**Grupo 4** · Programación IV · Back End
Stack: Spring Boot · Eureka · Spring Cloud Gateway · PostgreSQL

> Este documento deriva de `DISENIO-G04-SPRINT1.md`. Ese es el contrato de diseño;
> este es el plan de trabajo. Cada historia referencia el TO-DO del que sale y el
> requisito del PRD que la justifica, para que nadie tenga que preguntar por qué existe.
> Los supuestos abiertos están en `PREGUNTAS-INTEGRACION-G04.md`.

---

## 0. Cómo leer este documento

**10 épicas, 44 TO-DOs, 2 microservicios.** Las épicas no son carpetas: cada una es un
frente de trabajo con un entregable propio y un criterio de terminado verificable. Están
dimensionadas para que dos personas puedan tomar una sin pisarse en los merges.

| Campo | Qué significa |
|---|---|
| **Servicio** | Dónde vive el código: `ms-teoricos`, `ms-encuestas` o transversal |
| **TO-DOs** | Los ítems de la sección 6 del documento de diseño que esta épica consume |
| **Puntos** | Fibonacci relativo. La referencia es E-01/H-01 (scaffolding) = 3 |
| **Riesgo si no se hace** | Qué se rompe. Sirve para negociar recortes con evidencia, no con opinión |

**Convención de códigos:** `E-nn` épica, `H-nn` historia dentro de la épica.

### Orden de arranque

```
   E-02  ─────────────────────────────────►  primero, antes que cualquier feature
     │                                        (si los roles de base salen mal,
     │                                         todo lo de arriba se revisa)
     ▼
   E-01  ──┬──►  E-03  ──►  E-04  ──►  E-05  ──┐
           │                                    ├──►  E-09  ──►  E-10
           └──►  E-06  ──►  E-07  ──►  E-08  ──┘
```

**Ruta crítica:** `B-02` → `A-01 → A-02 → A-06 → A-08 → A-09` en teóricos,
`B-01 → B-02 → B-06` en encuestas.

---

## E-01 · Cimientos de los dos servicios

**Servicio:** transversal · **Puntos:** 13 · **TO-DOs:** A-01, B-01, C-06, C-07

Levantar los dos microservicios hasta el punto en que un compañero puede clonar, correr
`docker compose up` y ver ambos registrados en Eureka respondiendo `/health`. Nada de dominio
todavía. Esta épica existe para que las otras nueve arranquen el mismo día y no en cascada.

Se incluye acá la externalización de textos porque es una decisión que hay que tomar en el
primer commit: si los literales entran embebidos, sacarlos después es un barrido por todo el
código. Lo mismo el aviso de móvil — es una respuesta del backend, no una decisión del front.

| # | Historia | Criterios de aceptación | TO-DO |
|---|---|---|---|
| H-01 | Como equipo, quiero `ms-teoricos` levantado y registrado en Eureka, para que los demás grupos puedan descubrirlo | Responde `/actuator/health` con `UP`; aparece en el dashboard de Eureka; conecta a Postgres; las migraciones corren solas al arrancar | A-01 |
| H-02 | Como equipo, quiero `ms-encuestas` levantado y registrado en Eureka, con el mismo esqueleto | Ídem H-01, base de datos propia y separada | B-01 |
| H-03 | Como equipo, quiero el entorno local reproducible en un comando, para no perder horas de sprint en setup | `docker compose up` levanta Eureka, gateway, las dos bases y los dos servicios; README con los pasos | A-01, B-01 |
| H-04 | Como alumno que no habla español, quiero que todo texto salga de una clave i18n, para poder usar la plataforma en mi idioma | Cero literales de usuario en el código: se verifica con un test que falla si aparece un mensaje embebido en un `throw` o en un DTO; el idioma se resuelve por preferencia del usuario, no por `Accept-Language` del navegador (RF-NFR-07) | C-06 |
| H-05 | Como alumno que abre un teórico en el celular, quiero un aviso claro de que necesito una computadora, para no perder el intento a mitad de camino | Los endpoints de lectura de teórico y de encuesta devuelven un código y una clave i18n de "requiere escritorio" cuando el cliente es móvil; **no** hay degradación parcial de la vista (D-12, RF-NFR-06, Tabla 9 del PRD) | C-07 |

**Definición de hecho:** un integrante que no tocó el repo lo levanta entero en menos de 15 minutos siguiendo el README.

**Riesgo si no se hace:** ninguna otra épica puede empezar. Es el único bloqueo total del sprint.

---

## E-02 · Aislamiento de datos para el anonimato

**Servicio:** `ms-encuestas` · **Puntos:** 21 · **TO-DOs:** B-02, B-15, B-16

**Esta es la épica más importante del sprint y va primero.** Todo el diseño de encuestas
descansa en una afirmación: *no existe consulta que reconstruya el vínculo entre un alumno y
su respuesta*. Esa afirmación tiene que ser una propiedad del motor de base de datos, no una
promesa del equipo. Si se implementa después de tener funcionalidad encima, cada endpoint ya
escrito hay que auditarlo de nuevo.

El entregable no es una feature: es una **garantía verificable**. Por eso la historia central
es un test que intenta romper el anonimato por los cinco canales conocidos y afirma que falla.

| # | Historia | Criterios de aceptación | TO-DO |
|---|---|---|---|
| H-01 | Como responsable de privacidad, quiero tres esquemas con tres roles de Postgres sin permisos cruzados, para que el aislamiento lo imponga el motor | Existen `cumplimiento`, `catalogo` y `respuestas`; los roles `app_cumplimiento` y `app_respuestas` no tienen ningún permiso sobre el esquema del otro; ambos tienen `SELECT` sobre `catalogo`; no hay una sola FK que cruce esquemas | B-02 |
| H-02 | Como responsable de privacidad, quiero que ni los logs ni las trazas persistan el vínculo, para cerrar el canal más fácil de olvidar | El `trace_id` del gateway no se guarda junto a la respuesta; el body del `POST` de encuesta nunca se escribe en log en ningún nivel, ni siquiera `DEBUG`; verificado con un test que inspecciona el appender | B-15 |
| H-03 | Como equipo, quiero un test de reconstrucción que ataque los cinco canales y falle en los cinco, para poder afirmar el anonimato con evidencia | Un test de integración intenta: (1) join por clave, (2) correlación por timestamp, (3) correlación por orden de inserción, (4) correlación por secuencia de PK, (5) correlación por trazas — y **afirma que cada uno falla**. El intento de join se ejecuta con cada rol real y se verifica que Postgres lo rechaza por permisos, no que devuelva vacío (criterio de release 15b) | B-16 |
| H-04 | Como auditor, quiero el residual documentado en el código y no solo en el diseño, para que nadie lo descubra en producción | El test de H-03 incluye un caso que documenta explícitamente el residual de k-anonimato (`curso + período` con un solo respondente) y lo enlaza con el umbral PAR-18 que lo mitiga (E-08) | B-16 |

**Definición de hecho:** el test de reconstrucción corre en CI y es bloqueante. Si alguien agrega una FK entre esquemas, el build se cae.

**Riesgo si no se hace:** RSK-13. El diseño de anonimato queda decorativo y el compromiso con el alumno es falso. No es un riesgo técnico, es un riesgo de confianza.

---

## E-03 · Banco de ítems versionado

**Servicio:** `ms-teoricos` · **Puntos:** 21 · **TO-DOs:** A-02, A-03, A-04, A-05

El profesor necesita una colección reutilizable de preguntas (RF-DES-02). La sutileza está en
el versionado: RF-CUR-05 permite editar un desafío que ya está en uso, pero un ítem que alguien
ya respondió no puede cambiar bajo sus pies. La solución es la misma que el PRD aplica a
`rubric_version` (RF-IA-13): la identidad del ítem es estable, el contenido es inmutable y
versionado. Editar crea una versión nueva.

Los siete tipos de ítem tienen formas incompatibles entre sí, por eso `payload` y `criterio`
van en `jsonb`. Siete tablas con columnas nulas sería peor y cerraría la puerta a los tipos
que vengan después.

| # | Historia | Criterios de aceptación | TO-DO |
|---|---|---|---|
| H-01 | Como profesor, quiero que mis ítems tengan identidad estable y contenido versionado, para poder corregir una pregunta sin invalidar lo ya respondido | Tablas `item` e `item_version`; editar crea una versión nueva y **no toca** las anteriores; existe un test que intenta modificar una `item_version` ya respondida y verifica que se rechaza (D-04) | A-02 |
| H-02 | Como profesor, quiero dar de alta, listar, editar y dar de baja ítems de mi banco, para reutilizarlos entre cursos | `POST/GET/PUT /teoricos/items`, filtrable por tipo; la baja es **lógica**, nunca `DELETE` físico (RF-NFR-01); el listado devuelve solo los ítems del profesor autenticado | A-03 |
| H-03 | Como profesor, quiero cargar los cuatro tipos autocorregibles con su clave de corrección, para que el sistema corrija solo | `payload` y `criterio` definidos y validados para opción múltiple, verdadero/falso, emparejar conceptos y ordenar secuencias; cada tipo tiene su validador de forma y rechaza un payload mal armado con un mensaje útil | A-04 |
| H-04 | Como profesor, quiero cargar ítems de respuesta abierta, para evaluar comprensión y no solo memoria | `payload` de respuesta abierta con enunciado y consigna; se marca como de corrección humana; **conversación y debate estructurado** quedan definidos en el enum pero se rechazan al crearse, con un mensaje que dice que están diferidos (D-09) | A-05 |

**Definición de hecho:** un profesor carga los cinco tipos habilitados desde Postman y los recupera filtrados por tipo.

**Riesgo si no se hace:** sin banco no hay desafío teórico. Bloquea E-04 y E-05 completas.

---

## E-04 · Composición y entrega del desafío teórico

**Servicio:** `ms-teoricos` · **Puntos:** 13 · **TO-DOs:** A-06, A-07, A-08, A-17

Acá se cruza la frontera con el Tema 03. Ellos son dueños del ciclo de vida del desafío —
estados, fechas, versionado, entrega. Nosotros somos dueños del **contenido teórico** de ese
desafío. El Tema 03 nos manda un `desafio_id` y nosotros componemos los ítems.

Dos cuidados. Al publicar se **congela** la `item_version_id`: si el profesor edita la pregunta
mañana, el alumno de hoy sigue viendo la que respondió. Y el endpoint que consume el alumno
**nunca** devuelve el campo `criterio`: es la clave de corrección.

| # | Historia | Criterios de aceptación | TO-DO |
|---|---|---|---|
| H-01 | Como Tema 03, quiero componer un desafío teórico con ítems del banco, para que el alumno tenga qué responder | `POST /teoricos/desafios` recibe el `desafio_id` y `curso_cohorte_id`; congela `item_version_id` por ítem con su orden y puntaje; recomponer después no altera los desafíos ya publicados (D-04) | A-06 |
| H-02 | Como alumno, quiero ver los enunciados de mi desafío teórico, para poder resolverlo | `GET /teoricos/desafios/{id}` devuelve enunciados, opciones y orden, **sin** el campo `criterio` en ninguna rama del JSON; hay un test específico que serializa la respuesta y afirma que la clave no aparece | A-07 |
| H-03 | Como alumno, quiero enviar mi intento completo de una vez, para no perder respuestas a mitad de camino | `POST /teoricos/desafios/{id}/respuestas` recibe todas las respuestas del intento en un request; se persisten atómicamente; el número de intento lo informa el Tema 03 (supuesto 3) | A-08 |
| H-04 | Como alumno que perdió una vida, quiero reintentar el desafío de recuperación sin tope, para no quedarme trabado | El campo `intento` no tiene máximo en el modelo ni en la validación; un test reintenta 10 veces y las 10 se persisten (RF-REC-04/06; el límite de RF-DES-07 **no** aplica a este tipo de desafío) | A-17 |

**Definición de hecho:** el ciclo completo componer → leer como alumno → responder funciona punta a punta, y el test de no-fuga del criterio está en CI.

**Riesgo si no se hace:** la fuga del `criterio` es la falla más grave posible de esta épica: convierte todo teórico en un examen con las respuestas al dorso.

---

## E-05 · Motor de corrección

**Servicio:** `ms-teoricos` · **Puntos:** 21 · **TO-DOs:** A-09, A-10, A-11, A-12, A-15, A-16

El objetivo declarado del PRD es que las respuestas abiertas las corrija un LLM. En el MVP las
corrige el profesor (D-01). La decisión de diseño que importa no es *quién corrige*, es que el
contrato se escribe **asincrónico e intercambiable desde el día uno**, para que incorporar el
LLM sea agregar un adaptador y no rehacer el flujo.

De ahí sale la parte más delicada: un desafío mixto (tres opciones múltiples y una abierta)
tiene nota parcial y estado `PENDIENTE`. **El Tema 03 tiene que saber convivir con eso** — es
el mismo patrón que el PRD ya define para el score de IA diferido en RF-IA-27.

| # | Historia | Criterios de aceptación | TO-DO |
|---|---|---|---|
| H-01 | Como alumno, quiero que los ítems objetivos se corrijan al instante, para saber cómo me fue sin esperar | Corrector automático para los cuatro tipos autocorregibles; nota de 0 a 100 y `aprobado` según el umbral; el detalle devuelve puntaje obtenido por ítem | A-09 |
| H-02 | Como equipo, quiero la corrección detrás de un puerto con adaptadores intercambiables, para que sumar el LLM sea un adaptador y no un refactor | Interfaz de corrección con implementaciones `AUTOMATICO` y `HUMANO`; el flujo es asincrónico aunque el adaptador automático responda al instante; sumar `LLM` no requiere tocar el llamador (D-01) | A-10 |
| H-03 | Como profesor, quiero una cola con las respuestas abiertas que me esperan, para corregirlas sin perseguir alumnos | `GET /teoricos/correcciones/pendientes` devuelve solo lo del profesor autenticado; `POST /teoricos/correcciones/{id}` asienta la nota | A-11 |
| H-04 | Como Tema 03, quiero recibir una corrección parcial en estado `PENDIENTE`, para no quedarme esperando una nota que todavía no existe | Un desafío mixto devuelve `estado: PENDIENTE` con `nota: null` y el detalle de lo ya corregido; al cerrarse la última abierta pasa a `FINAL` y se emite el evento (supuesto 2, RF-IA-27) | A-12 |
| H-05 | Como coordinador de carrera, quiero que toda corrección manual quede auditada con motivo, para poder revisar un reclamo de nota | `correccion_historial` guarda profesor, fecha, nota anterior, nota nueva y **motivo obligatorio**; sin motivo el endpoint devuelve 400; el historial no se puede editar ni borrar (D-08, criterio de release 13) | A-16 |
| H-06 | Como equipo, quiero la batería de tests del motor, para poder tocarlo sin miedo | Tests de: corrección correcta de los cuatro tipos autocorregibles, inmutabilidad de la versión respondida, y no fuga del criterio en ninguna respuesta de la API | A-15 |

**Definición de hecho:** un desafío mixto recorre el ciclo completo — parcial automático, cola del profesor, cierre a `FINAL`, evento emitido — con el historial de auditoría poblado.

**Riesgo si no se hace:** sin corrección no hay nota, sin nota el Tema 10 no reparte XP y la gamificación entera queda sin insumo.

---

## E-06 · Instrumentos de encuesta y cumplimiento

**Servicio:** `ms-encuestas` · **Puntos:** 13 · **TO-DOs:** B-03, B-04, B-10, B-11, B-14, B-18

El marcador de cumplimiento es el único lugar del subsistema donde el alumno aparece con nombre
y apellido, y es deliberado: registra **que** cumplió, nunca **qué** respondió (RF-ENC-12).

El `instrumento_id` es la **campaña**, no la encuesta individual de un alumno. Si fuera
individual, un join entre esquemas reconstruiría el vínculo y E-02 sería decorativa. Es un
valor que comparten todos los respondentes de la misma campaña, y por eso no es un canal de
correlación.

El texto de las preguntas lo fija la plataforma, versionado (D-10): si cada profesor redacta la
suya, el KPI-02 deja de ser comparable entre cursos — es el mismo argumento de RF-CFG-05.

| # | Historia | Criterios de aceptación | TO-DO |
|---|---|---|---|
| H-01 | Como plataforma, quiero un catálogo de instrumentos versionados con clave i18n, para que el KPI sea comparable entre cursos y traducible | Esquema `catalogo` con `instrumento`; las tres dimensiones `CURSO`, `CONTENIDO`, `PLATAFORMA`; el campo es `clave_texto`, **no** el literal (D-10, RF-NFR-07); versión incremental | B-03, B-18 |
| H-02 | Como plataforma, quiero registrar por alumno e instrumento si cumplió, para poder aplicar el gate sin guardar su opinión | Esquema `cumplimiento` con `marcador`; PK `(alumno_id, instrumento_id)`; guarda `curso_cohorte_id`; estados `PENDIENTE` / `CUMPLIDA`; **ningún** campo de contenido de la respuesta | B-04 |
| H-03 | Como alumno, quiero ver qué encuestas tengo pendientes, para saber qué me está bloqueando el resultado | `GET /encuestas/pendientes?alumno={id}` devuelve las bloqueantes; el bloqueo lo aplica el front en el Sprint 1, con el filtro de gateway documentado como paso siguiente (D-06, supuesto 8) | B-10 |
| H-04 | Como Tema 02/10, quiero consultar si un alumno cumplió, para poder liberar su resultado académico | `GET /encuestas/cumplimiento?alumno&instrumento` devuelve `{cumplida: bool}` y nada más; **nunca** una lista nominal de quién cumplió (RF-ENC-11, RSK-13) | B-11 |
| H-05 | Como plataforma, quiero disparar la encuesta de plataforma a los 30 días y una por período, para medir satisfacción global sin fatigar | La campaña de dimensión `PLATAFORMA` se genera automáticamente a los 30 días del alta del alumno y una vez por período; no se duplica si el job corre dos veces | B-14 |

**Definición de hecho:** el gate de RF-ENC-11 responde correctamente y no existe endpoint que devuelva una lista nominal de cumplimiento.

**Riesgo si no se hace:** sin marcador no hay gate, y RF-ENC-11 queda sin implementar. Es la única palanca real de tasa de respuesta.

> **Supuesto de mayor impacto (D-07, supuesto 0):** el gate bloquea que **el alumno vea su resultado**, no que el profesor **archive el curso**. Con la lectura contraria, un alumno en estado `abandonó` que nunca responde deja el curso sin cerrar para siempre. Confirmar con el Tema 02 en la sesión de integración.

---

## E-07 · Respuesta anónima de encuesta

**Servicio:** `ms-encuestas` · **Puntos:** 21 · **TO-DOs:** B-05, B-06, B-07, B-08, B-09

La épica donde el anonimato se gana o se pierde en tiempo de ejecución. E-02 puso las paredes;
acá se escribe adentro sin abrir ventanas.

El punto no obvio: el marcador y la respuesta **no pueden escribirse en la misma transacción ni
en el mismo request**. Si lo hicieran, el orden de inserción correlaciona a los dos lados aunque
no compartan ninguna clave. Por eso la respuesta se encola y la escribe un consumidor con jitter
aleatorio y flush por lotes. Y por eso la PK es `gen_random_uuid()` (v4) y no UUIDv7 ni ULID:
esos codifican el instante de creación en el propio identificador.

| # | Historia | Criterios de aceptación | TO-DO |
|---|---|---|---|
| H-01 | Como alumno, quiero que mi respuesta se guarde sin nada que la fije en el tiempo, para que no se me pueda identificar por cuándo respondí | `respuesta_encuesta` con PK `gen_random_uuid()`; **no existe** columna `created_at`, solo `periodo`; sin `alumno_id` ni ninguna FK hacia cumplimiento | B-05 |
| H-02 | Como responsable de privacidad, quiero que marcador y respuesta se escriban desacoplados, para cerrar el canal de orden de inserción | El `POST` escribe el marcador y **encola** la respuesta; un consumidor la persiste con jitter aleatorio y flush por lotes; nunca en la misma transacción ni en el mismo request; test que verifica que el orden de las filas de respuesta no coincide con el orden de los marcadores | B-06 |
| H-03 | Como alumno, quiero poder abstenerme explícitamente, para no tener que inventar una nota que no siento | `POST /encuestas/{instrumento}/respuesta` acepta `abstencion: true` con `estrellas: null`; la abstención marca cumplimiento igual que una respuesta (RF-ENC-09); la tasa de abstención se registra como métrica propia | B-07 |
| H-04 | Como profesor, quiero que los extremos vengan con comentario, para saber qué mejorar y no solo cuánto | Comentario **obligatorio** con 1 y con 5 estrellas, opcional entre 2 y 4; sin comentario en un extremo el endpoint devuelve 400 con clave i18n (RF-ENC-05) | B-08 |
| H-05 | Como plataforma, quiero los comentarios moderados antes de publicarse, sin que eso bloquee el sprint | Puerto de moderación definido contra el contrato del Tema 11; **stub tras feature flag** que aprueba todo, con el flag apagado por defecto en producción y el TODO visible en el código (D-05, supuesto 6) | B-09 |

**Definición de hecho:** 50 respuestas concurrentes se persisten sin que el orden de las filas permita reconstruir el orden de los respondentes.

**Riesgo si no se hace:** RSK-13 otra vez. Todo lo construido en E-02 se anula con un solo `save()` en la misma transacción.

---

## E-08 · KPIs y umbral de publicación

**Servicio:** `ms-encuestas` · **Puntos:** 8 · **TO-DOs:** B-12, B-13

Las encuestas existen para producir dos números: KPI-01 (satisfacción de plataforma) y KPI-02
(satisfacción por curso). Sin esta épica todo lo anterior es un buzón donde nadie lee.

El umbral PAR-18 es la mitigación del residual declarado en E-02: con `curso + período`, un
curso donde respondió una sola persona la identifica. Bajo el mínimo de 5 respuestas se devuelve
únicamente el conteo, nunca el agregado. Y no es solo cuestión de privacidad: un promedio sobre
dos respuestas tampoco es información, es ruido.

| # | Historia | Criterios de aceptación | TO-DO |
|---|---|---|---|
| H-01 | Como profesor, quiero el CSAT de mi curso con su desglose, para saber qué ajustar el período que viene | `GET /encuestas/kpi?curso&periodo` devuelve CSAT, % de satisfechos, % de detractores y tasa de abstención; la abstención **no** contamina el promedio, se informa aparte (KPI-01, KPI-02) | B-12 |
| H-02 | Como ADMIN, quiero el consolidado de plataforma por período, para seguir la salud global del producto | `GET /encuestas/kpi/plataforma?periodo` con los mismos indicadores agregados sobre la dimensión `PLATAFORMA` | B-12 |
| H-03 | Como alumno de un curso chico, quiero que no se publiquen agregados que me identifiquen, para poder responder con sinceridad | Con menos de 5 respuestas el endpoint devuelve **solo el conteo** y una clave i18n de "muestra insuficiente"; nunca el promedio ni los comentarios; los resultados se publican recién al cierre del período (PAR-18) | B-13 |

**Definición de hecho:** un curso con 4 respuestas devuelve el conteo y nada más; con 5, el agregado completo. Ambos casos con test.

**Riesgo si no se hace:** el KPI-02 no existe y el ciclo de mejora del PRD queda abierto. Sin H-03, además, se reabre el residual de k-anonimato justo en el endpoint público.

---

## E-09 · Integración con los otros temas

**Servicio:** transversal · **Puntos:** 13 · **TO-DOs:** A-13, B-17, C-01, C-02, C-04, C-05

Somos un tema de nueve, no un producto aislado. Esta épica es todo lo que cruza nuestra
frontera: lo que publicamos, lo que consumimos y lo que documentamos para que otros nos usen.

El bus de eventos **no está definido todavía**. La respuesta a eso no es esperar: se escribe
contra un **puerto propio** con un envelope nuestro, y el adaptador se implementa cuando el
Tema 11 defina la tecnología. Es la misma jugada que con la moderación y con el LLM — tres
veces el mismo patrón, que es exactamente lo que uno quiere en un sistema con dependencias que
todavía no aterrizaron.

| # | Historia | Criterios de aceptación | TO-DO |
|---|---|---|---|
| H-01 | Como equipo, quiero un envelope de eventos propio detrás de un puerto, para no quedar bloqueados por una decisión que no es nuestra | Envelope con `eventId`, `tipo`, `ocurridoEn`, `payload`; puerto de publicación con adaptador en memoria para tests; a revisar con el Tema 11 | C-01 |
| H-02 | Como Tema 10, quiero enterarme de que un teórico se corrigió, para poder repartir XP y monedas | Se publica `teorico.corregido` con `{desafioId, alumnoId, intento, nota, aprobado, estado}` al pasar a `FINAL`; **no** se publica en `PENDIENTE` | A-13 |
| H-03 | Como Tema 02/10, quiero enterarme de que un alumno cumplió una encuesta, para liberar lo que dependa del gate | Se publican `encuesta.cumplida` con `{alumnoId, instrumentoId, cursoCohorteId}` y `encuesta.periodo_cerrado` con `{instrumentoId, cursoCohorteId, nRespuestas}`; ningún evento lleva contenido de respuesta | B-17 |
| H-04 | Como equipo, quiero consultar pertenencia a cohorte al Tema 02 sin acoplarme a su disponibilidad, para que su caída no nos tire | Cliente sincrónico por gateway con timeout explícito y fallback declarado; el comportamiento ante timeout está definido y testeado, no improvisado (supuesto 7) | C-02 |
| H-05 | Como los otros ocho grupos, quiero el contrato del Tema 04 publicado, para poder integrarme sin reunión | OpenAPI de ambos servicios publicado y accesible; incluye el caso `estado: PENDIENTE` con `nota: null` documentado como respuesta normal, no como error | C-04 |
| H-06 | Como grupo, quiero llevar nuestras preguntas a la sesión de integración, para cerrar los 8 supuestos vigentes | `PREGUNTAS-INTEGRACION-G04.md` presentado; cada supuesto queda confirmado o corregido, con el diseño actualizado en consecuencia | C-05 |

**Definición de hecho:** los ocho supuestos vigentes están confirmados o corregidos, y el OpenAPI está publicado donde los otros grupos lo encuentran.

**Riesgo si no se hace:** los supuestos se descubren falsos en la semana de integración. El supuesto 0 (D-07) es el caro: si el Tema 02 lo lee al revés, ningún curso cierra jamás.

---

## E-10 · Privacidad, autorización y normativa

**Servicio:** transversal · **Puntos:** 13 · **TO-DOs:** A-14, A-18, B-19, C-03

Los requisitos no funcionales que atraviesan todo. Van al final del sprint no por menos
importantes, sino porque se verifican sobre código que ya existe. Lo único que **no** puede
esperar es el aislamiento de encuestas — y por eso salió de acá y es la épica E-02, primera de
todas.

Un hallazgo propio que vale registrar: el mapa de PII del PRD **no contempla el marcador de
cumplimiento**. El marcador guarda `alumno_id` — dice «María cumplió la encuesta del curso X».
No es una respuesta, pero es dato personal. Probablemente la omisión viene de que el mapa se
escribió pensando en repositorios de contenido. Lo resolvemos nosotros (D-11b) en vez de dejarlo
en el limbo.

| # | Historia | Criterios de aceptación | TO-DO |
|---|---|---|---|
| H-01 | Como alumno, quiero que nadie más vea mis respuestas, y como profesor, que nadie edite mi banco | El profesor accede solo a su propio banco; el alumno accede solo a sus propias respuestas y correcciones; cada regla tiene su test de acceso denegado, no solo el de acceso permitido (RF-USR-07/08) | A-14 |
| H-02 | Como ADMIN, quiero desvincular al titular a los 5 años conservando el registro académico, para cumplir sin destruir historia | Al vencer PAR-16 y decidirlo el ADMIN, `respuesta.alumno_id` y `correccion.alumno_id` se reemplazan por un subrogado aleatorio **irreversible**; nota, contenido y series estadísticas se conservan (RF-NFR-10, D-11). El texto libre de una respuesta abierta que contenga un nombre se declara **residual conocido**, igual que el PRD declara los suyos | A-18 |
| H-03 | Como ADMIN, quiero que el marcador de cumplimiento también se anonimice, para no dejar un dato personal fuera del proceso | El marcador entra en el mismo camino de anonimización que los teóricos; las **respuestas** de encuesta están exentas por diseño porque nacieron sin PII y no hay vínculo que cortar (D-11b) | B-19 |
| H-04 | Como plataforma, quiero que ninguna entidad se borre físicamente, para poder auditar y revertir | Baja lógica verificada en **todas** las entidades de ambos servicios; un test recorre el modelo y falla si encuentra una entidad sin campo de baja o un `DELETE` físico en un repositorio (RF-NFR-01) | C-03 |

**Definición de hecho:** el proceso de anonimización corre sobre datos de prueba y se verifica que el registro académico sobrevive intacto y que el vínculo no se puede reconstruir.

**Riesgo si no se hace:** incumplimiento normativo (RF-NFR-10) y fuga de datos entre roles (RF-USR-07/08). H-03 es la que nadie va a reclamar porque no está en el PRD, y es justamente por eso que hay que hacerla.

---

## Resumen y reparto

| Épica | Servicio | Puntos | TO-DOs | Depende de |
|---|---|---|---|---|
| E-02 · Aislamiento de datos para el anonimato | encuestas | 21 | 3 | — |
| E-01 · Cimientos de los dos servicios | transversal | 13 | 4 | — |
| E-03 · Banco de ítems versionado | teóricos | 21 | 4 | E-01 |
| E-04 · Composición y entrega del desafío | teóricos | 13 | 4 | E-03 |
| E-05 · Motor de corrección | teóricos | 21 | 6 | E-04 |
| E-06 · Instrumentos y cumplimiento | encuestas | 13 | 6 | E-01, E-02 |
| E-07 · Respuesta anónima | encuestas | 21 | 5 | E-06 |
| E-08 · KPIs y umbral | encuestas | 8 | 2 | E-07 |
| E-09 · Integración con otros temas | transversal | 13 | 6 | E-05, E-07 |
| E-10 · Privacidad y normativa | transversal | 13 | 4 | E-03, E-06 |
| **Total** | | **157** | **44** | |

### Reparto sugerido para 10 personas

| Frente | Personas | Épicas |
|---|---|---|
| **Teóricos** | 4 | E-03 → E-04 → E-05 |
| **Encuestas** | 4 | E-02 → E-06 → E-07 → E-08 |
| **Plataforma e integración** | 2 | E-01, luego E-09 y E-10 |

Los dos frentes de dominio no comparten una sola línea de código: dos servicios, dos bases, dos
frentes de merge. Es la ventaja concreta de la decisión de partir el tema en dos.

**Los tres primeros días:** el frente de plataforma entrega E-01 para desbloquear a todos, y el
frente de encuestas arranca por E-02 sin esperar nada. El frente de teóricos empieza en cuanto
E-01/H-01 responde `UP`.

### Lo que este sprint deja explícitamente afuera

Declarado, no olvidado. Todo está contemplado en el modelo y en `DISENIO-G04-SPRINT1.md` §7.

- **Corrección de abiertas por LLM** — es un adaptador nuevo del puerto de E-05/H-02.
- **Conversación y debate estructurado** — falta la decisión de producto, no el modelo (D-09).
- **Filtro de bloqueo por encuesta en el gateway** — requiere al Tema 01; en el Sprint 1 lo aplica el front (D-06).
- **Moderación real de comentarios** — el puerto está; el adaptador espera al Tema 11 (D-05).
- **Vista del profesor sobre resultados de encuesta** — E-08/H-03 ya calcula con umbral; falta la pantalla.
