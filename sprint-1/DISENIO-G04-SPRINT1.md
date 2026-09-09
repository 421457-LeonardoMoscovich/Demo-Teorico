# Tema 04 — Teóricos y Encuestas
## Documento de diseño y planificación — Sprint 1

**Grupo 4** · Programación IV · Back End
Servicio: `ms-teoricos-encuestas`
Stack: Spring Boot · Eureka · Spring Cloud Gateway · PostgreSQL · Apache Kafka

---

## 1. Alcance del tema

### Lo que somos dueños

| Entidad | Descripción |
|---|---|
| Ítem teórico | La pregunta concreta, versionada, con su tipo, enunciado, opciones y criterio de corrección |
| Banco de ítems | Colección de ítems reutilizables del profesor (RF-DES-02) |
| Respuesta del alumno | Lo que contestó a cada ítem de un intento |
| Corrección | Nota y aprobado/desaprobado de un conjunto de respuestas |
| Instrumento de encuesta | La campaña: encuesta de cierre del curso X, período Y |
| Marcador de cumplimiento | Binario por alumno y encuesta, sin contenido (RF-ENC-12) |
| Respuesta de encuesta | Puntaje y comentario, sin autor (RF-ENC-04) |

### Lo que NO somos dueños

- **Ciclo de vida del desafío, entrega, estados, fechas, versionado** → Tema 03.
  Teórico y práctico son dos tipos del mismo desafío, y eso vive una sola vez, en el 03.
- **XP, monedas, vidas, niveles, ranking** → Tema 10. Nosotros emitimos el hecho
  de la corrección; el efecto económico lo decide el 10.
- **Pertenencia a la cohorte y su ciclo de vida** → Tema 02.
- **Identidad, roles y validación del token** → Tema 01.
- **Moderación de contenido** → Tema 11. Nosotros la consumimos.

### Decisiones ya tomadas (cerradas con el PO del grupo)

| # | Decisión | Fundamento |
|---|---|---|
| D-01 | La corrección de respuestas abiertas la hace el **profesor** en el MVP | El objetivo es que la haga el LLM; el contrato se diseña asincrónico e intercambiable para que sea un cambio de adaptador, no un refactor |
| D-02 | **No** hay encuesta por desafío | No está en el PRD; con abstención obligatoria por desafío la fatiga dispararía la tasa de abstención, que es el argumento de RF-ENC-02 para no extender la encuesta |
| D-03 | El único gate de encuesta es al **cierre de curso**, sobre el resultado académico final | RF-ENC-11 + RF-RNK-10. Consecuencia: no bloqueamos al 03 ni al 05 |
| D-04 | Los ítems se **versionan** y son inmutables una vez respondidos | RF-CUR-05 permite editar desafíos en uso; mismo criterio que `rubric_version` (RF-IA-13) y RF-CFG-06 |
| D-05 | Moderación: contrato cerrado + **stub tras feature flag** | No nos bloquea el sprint y no deja deuda silenciosa |
| D-06 | Bloqueo por encuesta pendiente: **front** en sprint 1, filtro de gateway documentado como paso siguiente | El bloqueo real requiere al Tema 01; ver pregunta 25 del archivo de integración |
| D-07 | El gate de encuesta bloquea que **el alumno vea su resultado**, no que el profesor **archive** el curso | Evita el deadlock: un alumno con estado `abandonó` nunca responde, y con la lectura contraria el curso no cierra jamás. Ver pregunta 17b |
| D-08 | Toda corrección manual queda **auditada con historial**: profesor, fecha, nota anterior y nueva, motivo | Criterio de release 13 exige auditoría sobre overrides de score académico. Mismo estándar que RF-IA-18 aplica al score de IA |
| D-09 | **Conversación** y **debate estructurado** quedan fuera del Sprint 1, declarados | No encajan en el modelo de "responder y enviar": uno es multi-turno, el otro involucra a terceros. Los otros cinco tipos entran completos |
| D-10 | El **texto de las preguntas de encuesta lo fija la plataforma**, versionado y externalizado | Si cada profesor redacta la suya, el KPI-02 deja de ser comparable entre cursos — mismo argumento de RF-CFG-05 |
| D-11 | El módulo `teoricos` implementa **anonimización**; las **respuestas** de encuesta están exentas por diseño | RF-NFR-10: a los 5 años el ADMIN puede desvincular al titular conservando el registro académico. Las respuestas de encuesta ya nacieron sin PII, así que no hay vínculo que cortar |
| D-11b | El **marcador de cumplimiento sí se anonimiza**, junto con el resto | El marcador guarda `alumno_id`: dice «María cumplió la encuesta del curso X». No es una respuesta, pero es dato personal, y el mapa de PII del PRD no lo contempla — probablemente porque fue escrito pensando en repositorios de contenido. Lo resolvemos nosotros en vez de dejarlo en el limbo |
| D-12 | Teóricos y encuestas son **solo escritorio**; en móvil se responde explícitamente, no se degrada | Verificado en la Tabla 9 del PRD: ambos ❌ en móvil. RF-NFR-06 exige el aviso explícito |
| D-14 | El evento de corrección va al **Tema 03**, no al Tema 10: publicamos `TEORICO_CORREGIDO` en `desafios.resultados` y el 03 consolida y emite `DESAFIO_RESUELTO` | Un desafío puede ser teórico o práctico. Quien sabe unificar los dos casos es el dueño del desafío; si cada productor de notas le avisara al 10 por su cuenta, el 10 tendría que conocerlos a todos |
| D-15 | La **moderación pasa a ser asincrónica** por el tópico `sistema.moderacion`: el comentario espera en estado pendiente | Revisión de D-05 con Kafka ya definido. El comentario se persiste sin exponerse y se publica cuando el Tema 11 responde |
| D-13 | **Un único microservicio**, `ms-teoricos-encuestas`, con dos módulos internos sin dependencias cruzadas | Restricción del reparto de la cátedra. El aislamiento que daba el despliegue se reemplaza por esquemas y roles de base separados, más un test de arquitectura. Ver sección 2 |

---

## 2. Decisión estructural: un servicio, dos módulos aislados

El Tema 04 se despliega como **un único microservicio, `ms-teoricos-encuestas`**, con dos
módulos internos que no comparten ninguna entidad:

- **módulo `teoricos`** — banco de ítems, respuestas, corrección
- **módulo `encuestas`** — instrumentos, marcadores, respuestas anónimas, KPIs

**Por qué un solo servicio.** Es la restricción del reparto: cada grupo entrega un microservicio.
No es una decisión de diseño nuestra y no la discutimos; lo que sí decidimos es cómo preservar,
dentro de un único proceso, las garantías que un despliegue separado nos habría dado gratis.

**El problema que eso crea.** Los dos subdominios comparten exactamente un concepto
(`curso_cohorte_id`) y ninguna entidad. No hay una sola operación que los cruce. Pero el
anonimato de las encuestas se sostenía, en parte, en que vivieran aparte: con todo en un mismo
proceso, cualquiera puede inyectar el repositorio equivocado y unir el marcador de cumplimiento
con la respuesta anónima. Lo que antes era imposible por despliegue ahora es un descuido de dos
líneas.

**Cómo lo resolvemos.** El aislamiento baja un nivel: de propiedad del despliegue a propiedad
del motor de base de datos, que sigue siendo mucho más fuerte que la disciplina del equipo.

| Barrera | Qué impide |
|---|---|
| **Cuatro esquemas, cuatro roles de PostgreSQL** sin permisos cruzados, con un origen de datos por rol | Que una consulta una el marcador con la respuesta. Lo rechaza el motor, no el código |
| **Módulos con dependencias prohibidas**, verificado con un test de arquitectura | Que el código de encuestas importe clases de teóricos, o que un repositorio use el origen de datos que no le toca |
| **Migraciones separadas por esquema** | Que una clave foránea entre esquemas se cuele en una migración |

El test de reconstrucción del criterio de release 15b sigue valiendo **palabra por palabra**: se
ejecuta con cada rol real y verifica que PostgreSQL rechaza el join por permisos.

**Lo que sí perdimos.** Con dos servicios, las diez personas eran dos repos que no se pisaban en
los merges. Ahora son un repo con dos paquetes, así que la separación de frentes hay que
sostenerla con convención y con el test de arquitectura, no con la topología.

**Costo asumido:** configurar varios orígenes de datos en una misma aplicación es más trabajo que
tener uno solo, y es el precio de no degradar la garantía de anonimato a una promesa.

---

## 3. Diseño de datos

### 3.1 Módulo `teoricos` — esquema `teoricos`

```
item                      -- identidad estable del ítem
  id                uuid pk
  profesor_id       uuid              -- dueño, para el banco (RF-DES-02)
  tipo              enum              -- OPCION_MULTIPLE | VERDADERO_FALSO | EMPAREJAR |
                                      -- ORDENAR | ABIERTA | CONVERSACION | DEBATE
  version_actual    int
  baja_logica       timestamp null    -- RF-NFR-01: nunca hard delete

item_version              -- lo que se responde; inmutable
  id                uuid pk
  item_id           uuid fk
  version           int
  enunciado         text
  payload           jsonb             -- opciones, pares, secuencia: varía por tipo
  criterio          jsonb             -- clave de corrección; nunca se expone al alumno
  creada_en         timestamp

desafio_teorico           -- composición de ítems para un desafío del 03
  id                uuid pk
  desafio_id        uuid              -- id que nos da el Tema 03
  curso_cohorte_id  uuid
  baja_logica       timestamp null

desafio_teorico_item
  desafio_teorico_id uuid fk
  item_version_id    uuid fk          -- congelado al publicar (D-04)
  orden              int
  puntaje            int

respuesta
  id                uuid pk
  desafio_teorico_id uuid fk
  item_version_id   uuid fk
  alumno_id         uuid
  intento           int               -- nos lo informa el 03
  contenido         jsonb
  respondida_en     timestamp

correccion
  id                uuid pk
  desafio_teorico_id uuid fk
  alumno_id         uuid
  intento           int               -- SIN tope: recuperación de vida reintenta libremente
  nota              int               -- 0-100
  aprobado          boolean
  corrector         enum              -- AUTOMATICO | LLM | HUMANO
  estado            enum              -- PENDIENTE | FINAL
  corregida_por     uuid null         -- profesor, si corrector = HUMANO
  corregida_en      timestamp null

correccion_historial      -- D-08: criterio de release 13
  id                uuid pk
  correccion_id     uuid fk
  nota_anterior     int null
  nota_nueva        int
  motivo            text              -- obligatorio
  profesor_id       uuid
  registrado_en     timestamp
```

**`intento` no tiene tope.** El desafío de recuperación de vida (RF-REC-04/06) se reintenta
un número indefinido de veces: no le aplica el límite de RF-DES-07. Si modeláramos el intento
con un máximo de 4, ese tipo de desafío rompería el modelo.

**Anonimización (D-11).** Al vencer PAR-16 y decidir el ADMIN, `respuesta.alumno_id` y
`correccion.alumno_id` se reemplazan por un subrogado aleatorio irreversible; la nota, el
contenido y las series estadísticas se conservan (RF-NFR-10). *Límite conocido: si un alumno
escribió su nombre dentro de una respuesta abierta, el texto libre no se anonimiza solo — se
declara como residual, igual que el PRD hace con sus propios residuales.*

`payload` y `criterio` en `jsonb` porque los 7 tipos de ítem tienen formas incompatibles entre sí. Siete tablas con columnas nulas sería peor.

`criterio` **nunca** viaja en la respuesta de un endpoint consumido por un alumno. Es la clave de corrección.

### 3.1b Los siete tipos de ítem no son homogéneos

| Tipo | Sprint 1 | Por qué |
|---|---|---|
| Opción múltiple | ✅ autocorregible | |
| Verdadero / falso | ✅ autocorregible | |
| Emparejar conceptos | ✅ autocorregible | |
| Ordenar secuencias | ✅ autocorregible | |
| Respuesta abierta | ✅ corrección humana | D-01; el LLM lo toma después como adaptador |
| **Conversación sobre el contenido** | ❌ diferido (D-09) | Es multi-turno: no encaja en "responder n ítems y enviar" |
| **Debate estructurado** | ❌ diferido (D-09) | Involucra a otros alumnos en simultáneo, o al Tema 07 |

Los dos diferidos no son un recorte de ambición: son dos formas de interacción que el modelo
de entrega única no soporta, y hacen falta decisiones de producto que todavía no existen
(¿quién arma las parejas de un debate? ¿cuándo termina una conversación?). El `payload jsonb`
admite una transcripción cuando se implementen.

### 3.2 Módulo `encuestas` — tres esquemas, tres roles

Este es el corazón del anonimato. **Tres esquemas más, en la misma base, con roles de PostgreSQL distintos y un origen de datos por rol. Ninguno tiene permisos sobre el esquema del otro, y ninguno de los tres los tiene sobre `teoricos`.**

```sql
-- esquema: cumplimiento          (rol: app_cumplimiento)
marcador
  alumno_id             uuid
  instrumento_id        uuid          -- la CAMPAÑA, no la encuesta individual
  curso_cohorte_id      uuid null     -- null en encuesta de plataforma
  estado                enum          -- PENDIENTE | CUMPLIDA
  actualizado_en        timestamp
  pk (alumno_id, instrumento_id)

-- esquema: catalogo               (rol: app_catalogo; SELECT para los otros dos)
instrumento               -- la CAMPAÑA. Compartida por todos los respondentes,
                          -- por eso no es un canal de correlación
                          -- D-10: el texto lo fija la plataforma, versionado
  id                    uuid pk
  dimension             enum          -- CURSO | CONTENIDO | PLATAFORMA
  version               int
  clave_texto           varchar       -- clave i18n, NO el literal (RF-NFR-07)
  curso_cohorte_id      uuid null     -- null en plataforma
  periodo               varchar
  abierto_desde         timestamp
  abierto_hasta         timestamp null

-- esquema: respuestas             (rol: app_respuestas)
respuesta_encuesta
  id                    uuid pk default gen_random_uuid()   -- v4: sin timestamp embebido
  instrumento_id        uuid
  curso_cohorte_id      uuid null     -- presente en curso/contenido, NULL en plataforma
  dimension             enum          -- CURSO | CONTENIDO | PLATAFORMA
  estrellas             int null      -- 1-5, null si se abstuvo
  abstencion            boolean
  comentario            text null
  periodo               varchar       -- '2026-2C'. NO hay created_at
```

**Por qué `instrumento_id` es la campaña y no la encuesta del alumno:** si fuera individual, un join entre los dos esquemas reconstruye el vínculo y todo el diseño es decorativo. Tiene que ser un valor que comparten todos los respondentes de la misma campaña.

**Por qué `curso_cohorte_id` va en el marcador pero no en la respuesta de plataforma:** el marcador es nominal por diseño, así que ahí el campo no cuesta nada y da toda la métrica de cobertura por curso. En la respuesta de plataforma, en cambio, un alumno puede cursar tres materias (¿cuál ponemos?) y cada dimensión extra en una fila anónima reduce el conjunto de posibles autores. En curso y contenido sí va, porque el KPI-02 es por curso y no hay alternativa; ahí el trabajo lo hace el umbral de PAR-18.

### 3.3 Los cinco canales de correlación, y cómo se cierra cada uno

| Canal | Cómo se cierra |
|---|---|
| Join por clave | Esquemas separados, roles separados, sin FK ni id compartido. Lo impide el motor, no el código |
| Correlación temporal | La respuesta no guarda `created_at`, solo `periodo` |
| Orden de inserción | La respuesta se encola y la escribe un consumidor con jitter aleatorio y flush por lotes. Nunca en la misma transacción ni en el mismo request que el marcador |
| Secuencias / PK | `gen_random_uuid()` (v4). Nunca autoincremental, nunca UUIDv7 ni ULID: codifican el instante |
| Logs y trazas | El `trace_id` del gateway no se persiste con la respuesta; el logging no registra el body del POST de encuesta |

**Residual declarado:** con `curso + período`, un curso donde responde una sola persona la identifica. Por eso existe PAR-18 (mínimo 5 respuestas) y la publicación recién al cierre. Es k-anonimato, y hay que decirlo, no taparlo.

**Prueba (criterio de release 15b):** un test de integración que intenta la reconstrucción por los cinco canales y **afirma que falla**. Incluye un intento de join con cada rol de base, verificando que PostgreSQL lo rechaza por permisos.

---

## 4. Contratos

### 4.1 El módulo `teoricos` expone

| Método | Ruta | Consumidor | Descripción |
|---|---|---|---|
| POST | `/teoricos/items` | Profesor | Alta de ítem en el banco |
| PUT | `/teoricos/items/{id}` | Profesor | Crea versión nueva; no toca las anteriores |
| GET | `/teoricos/items` | Profesor | Banco propio, filtrable por tipo |
| POST | `/teoricos/desafios` | Tema 03 | Compone ítems para un desafío. Congela `item_version_id` |
| GET | `/teoricos/desafios/{id}` | Alumno | Enunciados **sin** criterio de corrección |
| POST | `/teoricos/desafios/{id}/respuestas` | Alumno | Envía el intento completo |
| GET | `/teoricos/correcciones/{desafio}/{alumno}` | Tema 03 | Resultado |
| GET | `/teoricos/correcciones/pendientes` | Profesor | Cola de corrección manual |
| POST | `/teoricos/correcciones/{id}` | Profesor | Corrige a mano (MVP, D-01) |

Respuesta de corrección:

```json
{
  "nota": 85,
  "aprobado": true,
  "corrector": "HUMANO",
  "estado": "FINAL",
  "detalle": [ { "itemVersionId": "...", "puntaje": 10, "obtenido": 8 } ]
}
```

`estado: PENDIENTE` con `nota: null` es la respuesta normal cuando hay ítems abiertos sin corregir. **El 03 tiene que saber convivir con eso** — es el mismo patrón que el PRD ya define para el score de IA diferido (RF-IA-27).

### 4.2 El módulo `encuestas` expone

| Método | Ruta | Consumidor | Descripción |
|---|---|---|---|
| GET | `/encuestas/pendientes?alumno={id}` | Front | Encuestas bloqueantes pendientes |
| GET | `/encuestas/{instrumento}` | Alumno | Preguntas del instrumento |
| POST | `/encuestas/{instrumento}/respuesta` | Alumno | Responde o se abstiene. Escribe marcador + encola respuesta |
| GET | `/encuestas/cumplimiento?alumno={id}&instrumento={id}` | Tema 02/10 | `{cumplida: bool}` — el gate de RF-ENC-11 |
| GET | `/encuestas/kpi?curso={id}&periodo={p}` | Profesor, Tema 12 | Agregados. Bajo PAR-18 devuelve solo el conteo |
| GET | `/encuestas/kpi/plataforma?periodo={p}` | ADMIN, Tema 12 | Consolidado |

**Invariante de todos los endpoints de lectura:** ninguno expone una respuesta individual atribuible, ni la lista nominal de quién cumplió. Solo agregados (RF-ENC-12, RSK-13).

### 4.3 Consumimos

| De | Qué | Cómo |
|---|---|---|
| Tema 02 | Pertenencia a cohorte y rol | Sincrónico por gateway (a confirmar, pregunta 16) |
| Tema 11 | Moderación de comentarios | Sincrónico, bloqueante. **Stub tras feature flag** en sprint 1 |
| Tema 03 | Número de intento | En el request de respuestas |

### 4.4 Eventos que publicamos

El bus es **Apache Kafka**, en Docker como infraestructura independiente. Publicamos detrás de un
puerto propio, pero no para independizarnos de la tecnología —ya está decidida— sino para tener
un adaptador en memoria en los tests y no levantar el broker en cada build.

**Envelope estándar, obligatorio para todos los microservicios:**

```json
{
  "eventId":   "123e4567-e89b-12d3-a456-426614174000",
  "eventType": "TEORICO_CORREGIDO",
  "timestamp": "2026-09-06T19:30:00Z",
  "producer":  "tema-04-teoricos-encuestas",
  "payload":   { }
}
```

| Evento | Tópico | Consumidor | Payload |
|---|---|---|---|
| `TEORICO_CORREGIDO` | `desafios.resultados` | **Tema 03** | `desafioId, alumnoId, intento, nota, aprobado, estado` |
| `ENCUESTA_CUMPLIDA` | *a definir* — proponemos `encuestas.cumplimiento` | Tema 02 / 10 | `alumnoId, instrumentoId, cursoCohorteId` |
| `ENCUESTA_PERIODO_CERRADO` | *a definir* | Tema 12 | `instrumentoId, cursoCohorteId, nRespuestas` |

**No le hablamos al Tema 10.** Publicamos `TEORICO_CORREGIDO` en el tópico del dominio de
desafíos, el Tema 03 lo consume con su `groupId`, consolida el resultado del desafío —que puede
tener parte teórica y parte práctica— y emite `DESAFIO_RESUELTO`. El 10 y el 11 reaccionan a ese.

**Riesgo a acordar:** el 10 y el 11 escuchan `desafios.resultados`. Tienen que **filtrar por
`eventType`** y reaccionar solo a `DESAFIO_RESUELTO`. Si alguno escuchara el tópico entero, un
mismo intento aprobado sumaría XP dos veces: una por nuestro evento y otra por el del 03. Es el
tipo de falla que aparece en la demo y no en desarrollo.

**Falta un tópico.** Los cuatro acordados son `cursos.ciclo-vida`, `desafios.resultados`,
`sistema.notificaciones` y `sistema.moderacion`. Ninguno corresponde al cumplimiento de encuestas:
no es un resultado de desafío ni una notificación. Hay que resolverlo en la integración.

**Y un residual nuevo.** Kafka **retiene los mensajes**, y `ENCUESTA_CUMPLIDA` lleva `alumno_id`.
Anonimizar nuestra base a los cinco años (RF-NFR-10) no borra lo que quedó publicado en el bus.
O se acuerda una política de retención por tópico, o se declara como residual conocido. Sospechamos
que le pasa lo mismo a todos los temas que publiquen datos personales.

---

## 5. Supuestos vigentes

Avanzamos con estos hasta que la sesión de integración diga otra cosa. Están todos en `PREGUNTAS-INTEGRACION-G04.md`.

0. **El archivado del curso no espera a las encuestas pendientes** (pregunta 17b, D-07).
   Es el supuesto de mayor impacto: si el 02 lo lee al revés, ningún curso con un alumno
   que abandonó puede cerrarse nunca.
1. El 04 invoca al corrector él mismo por el gateway; el 03 no actúa de conducto (pregunta 2).
2. El 03 acepta correcciones en estado `PENDIENTE` (pregunta 4).
3. El 03 nos informa el número de intento (pregunta 5).
4. No bloqueamos al 03 ni al 05 por encuesta (pregunta 6, D-03).
5. La corrección semántica de abiertas no la toma el Tema 07 en el sprint 1 (pregunta 7).
6. No hay moderador disponible: stub que aprueba todo (pregunta 10).
7. La pertenencia a cohorte se consulta sincrónicamente al 02 (pregunta 16).
8. El bloqueo por encuesta pendiente lo aplica el front (pregunta 25).
9. El Tema 03 consume `TEORICO_CORREGIDO` y se hace cargo de emitir `DESAFIO_RESUELTO` hacia el 10.
10. El Tema 10 y el Tema 11 filtran por `eventType` y no reaccionan a nuestro evento intermedio.
11. Se habilita un tópico para los eventos de encuesta; proponemos `encuestas.cumplimiento`.

---

## 6. TO-DOs del Sprint 1

Dos frentes paralelos que no comparten código, dentro de un mismo servicio. La separación ya no la da el despliegue, así que la sostienen el test de arquitectura de C-08 y la convención de paquetes.

### Frente A — módulo `teoricos`

| # | Tarea | Depende de |
|---|---|---|
| A-01 | *(fusionado en C-00: el scaffolding es único)* | — |
| A-02 | Modelo de ítem + versionado inmutable (`item`, `item_version`) | A-01 |
| A-03 | CRUD del banco de ítems, con baja lógica | A-02 |
| A-04 | Payload y criterio por tipo: los 4 autocorregibles (MC, V/F, emparejar, ordenar) | A-02 |
| A-05 | Payload de los 3 tipos de corrección humana (abierta, conversación, debate) | A-02 |
| A-06 | Composición de desafío teórico + congelado de versiones | A-03 |
| A-07 | Endpoint de lectura para el alumno **sin criterio de corrección** | A-06 |
| A-08 | Recepción de respuestas de un intento | A-06 |
| A-09 | Corrector automático de los 4 tipos autocorregibles | A-04, A-08 |
| A-10 | Puerto de corrección con adaptadores `AUTOMATICO` / `HUMANO` (D-01) | A-09 |
| A-11 | Cola de corrección manual del profesor + endpoint de corrección | A-10 |
| A-12 | Corrección mixta: nota parcial + estado `PENDIENTE` hasta cerrar las abiertas | A-09, A-11 |
| A-13 | Publicación de `TEORICO_CORREGIDO` en `desafios.resultados`, para el Tema 03 | A-12, C-01 |
| A-14 | Autorización: el profesor solo ve su banco; el alumno solo sus respuestas (RF-USR-07/08) | A-03, A-08 |
| A-15 | Tests: corrección de los 4 tipos, inmutabilidad de versión, no fuga del criterio | A-09, A-12 |
| A-16 | Historial auditado de correcciones manuales, con motivo obligatorio (D-08) | A-11 |
| A-17 | Intento sin tope, para el desafío de recuperación de vida (RF-REC-04) | A-08 |
| A-18 | Camino de anonimización: subrogado irreversible conservando el registro (D-11) | A-02 |

### Frente B — módulo `encuestas`

| # | Tarea | Depende de |
|---|---|---|
| B-01 | *(fusionado en C-00: el scaffolding es único)* | — |
| B-02 | **Tres esquemas + tres roles de base sin permisos cruzados, un origen de datos por rol** | C-00 |
| B-03 | Modelo de instrumento y campaña (curso, contenido, plataforma) | B-02 |
| B-04 | Marcador de cumplimiento con `curso_cohorte_id` | B-02, B-03 |
| B-05 | Almacén de respuestas: UUIDv4, sin `created_at` | B-02, B-03 |
| B-06 | Escritura desacoplada: cola interna + consumidor con jitter y flush por lotes | B-04, B-05 |
| B-07 | Endpoint de respuesta con abstención explícita (RF-ENC-09) | B-06 |
| B-08 | Comentario obligatorio en 1 y 5, opcional en 2-4 (RF-ENC-05) | B-07 |
| B-09 | Puerto de moderación + stub tras feature flag (D-05) | B-07 |
| B-10 | Endpoint de pendientes para el bloqueo del front | B-04 |
| B-11 | Endpoint de cumplimiento — el gate de RF-ENC-11 | B-04 |
| B-12 | Cálculo de KPI-01/02: CSAT, % satisfechos, % detractores, tasa de abstención | B-05 |
| B-13 | Umbral PAR-18: bajo el mínimo, solo el conteo | B-12 |
| B-14 | Disparo de la encuesta de plataforma a los 30 días y una por período | B-03 |
| B-15 | Supresión de trazas: sin `trace_id` persistido, sin body en logs | B-06 |
| B-16 | **Test de reconstrucción** — los 5 canales, afirmando que fallan (criterio 15b) | B-06, B-15 |
| B-17 | Publicación de `ENCUESTA_CUMPLIDA` — falta acordar el tópico | B-04, C-01 |
| B-18 | Esquema de catálogo: instrumento versionado con clave i18n (D-10) | B-02 |
| B-19 | Anonimización del marcador de cumplimiento (D-11b). Las respuestas no la necesitan | B-04 |

### Frente C — transversal

| # | Tarea | Depende de |
|---|---|---|
| C-00 | Scaffolding único: Spring Boot, registro en Eureka, health, Postgres, migraciones por esquema | — |
| C-08 | **Test de arquitectura**: dependencias cruzadas entre módulos prohibidas y cada repositorio atado a su origen de datos | C-00 |
| C-01 | Productor de Kafka con el envelope estándar, detrás de un puerto con adaptador en memoria | C-00 |
| C-02 | Cliente hacia el Tema 02 para pertenencia a cohorte, con timeout y fallback | — |
| C-03 | Baja lógica transversal verificada en todas las entidades (RF-NFR-01) | A-02, B-03 |
| C-04 | Documento de contratos publicado para los otros grupos (OpenAPI) | A-*, B-* |
| C-05 | Llevar `PREGUNTAS-INTEGRACION-G04.md` a la sesión de integración | — |
| C-06 | Externalización de textos: cero literales embebidos, idioma por usuario (RF-NFR-07) | A-01, B-01 |
| C-07 | Respuesta explícita en móvil: *«esta sección requiere una computadora»*, sin degradar (D-12) | A-07, B-07 |

**Ruta crítica:** C-00 → A-02 → A-06 → A-08 → A-09 en teóricos, y C-00 → B-02 → B-06 en encuestas. **B-02 es lo que más conviene hacer primero** después del scaffolding: si los roles de base no quedan bien desde el arranque, todo lo que se escriba encima hay que revisarlo. Y con un único proceso, C-08 deja de ser opcional: es lo que impide que el aislamiento se rompa desde el código.

---

## 7. Backlog posterior (diseñado, no implementado)

Lo que el reparto pone en "para más adelante" y ya está contemplado en el modelo:

- **Corrección de abiertas por LLM** — es un adaptador nuevo del puerto de A-10. Ninguna otra pieza cambia.
- **Exposición de resultados de encuesta al profesor con umbral de 5** — B-13 ya lo implementa; falta la vista.
- **Tipos de ítem adicionales** — `payload` en `jsonb` los admite sin migración.
- **Agregados por cohorte** — B-12 ya calcula por curso.
- **Banco de ítems con etiquetado por tema** — una tabla de etiquetas sobre `item`.
- **Analítica de dificultad por ítem** — posible porque los ítems tienen identidad propia y versionada (D-04). Si los hubiéramos duplicado dentro de cada desafío, sería imposible.
- **Conversación y debate estructurado** — el `payload jsonb` guarda una transcripción sin migración; falta la decisión de producto, no el modelo (D-09).
- **Segundo idioma** — todo texto sale de una clave, incluido el de las preguntas de encuesta (C-06). Incorporar un idioma es contenido, no refactor (RF-NFR-07).
