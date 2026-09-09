# Grupo 04 — Teóricos y Encuestas
## Preguntas para la sesión de integración

> Cada pregunta indica a qué grupo va dirigida, por qué nos bloquea y, donde
> corresponde, cuál es nuestra propuesta. Si no hay respuesta, avanzamos con la
> propuesta y queda asentado como supuesto nuestro.

---

## Para el Grupo 03 — Motor de Desafíos

**1. Frontera de propiedad: ¿quién guarda qué?**
Nuestra lectura: el 03 es dueño del desafío, la entrega, el estado, las fechas y el
versionado. El 04 es dueño del ítem teórico, de la respuesta del alumno a cada ítem y
de la corrección. ¿Lo comparten? Concretamente: cuando un alumno responde 10 ítems,
¿el 03 guarda una entrega con un puntero al resultado del 04, o espera recibir las
respuestas ítem por ítem?

**2. ¿Quién invoca al corrector?**
Dos opciones sobre la mesa:
- (a) El 04 le manda las respuestas al 03, el 03 las rutea al corrector y devuelve el resultado.
- (b) El 04 invoca al corrector por el gateway y le entrega al 03 un resultado ya cerrado.

Nuestra preferencia es la (b): la opción (a) convierte al 03 en conducto (la propuesta
arquitectónica advierte contra eso) y lo obliga a entender de ítems y rúbricas, que es
dominio nuestro. ¿Están de acuerdo?

**3. Contrato de resultado.**
Proponemos devolver `{ nota: 0-100, aprobado: bool, corrector: AUTOMATICO|LLM|HUMANO,
estado: FINAL|PENDIENTE }`. ¿Les sirve ese shape? ¿Necesitan el desglose por ítem o
solo el agregado?

**4. Corrección diferida.**
Con corrección humana o por LLM, el resultado NO está disponible al momento del envío.
¿Cómo modelan un desafío entregado con corrección pendiente? ¿Tienen un estado
`EN_CORRECCION`? Es el mismo patrón que el PRD ya define para el score de IA diferido
(RF-IA-27), así que conviene resolverlo igual.

**4b. Desafío de recuperación de vida (RF-REC-04/06).**
El profesor carga un **pool** por curso, el sistema elige uno al azar priorizando los que el
alumno no resolvió antes, y se puede **reintentar sin límite** — no aplica el tope de
RF-DES-07. Si esos desafíos son teóricos, nos toca a nosotros.
- ¿Quién es dueño del pool y de la selección al azar: ustedes como tipo de desafío, o nosotros?
- Confirmamos que nuestro modelo de intento **no puede asumir un máximo**: para estos
  desafíos el contador es abierto.

**5. Reintentos (RF-DES-07).**
Nosotros necesitamos saber qué número de intento es cada respuesta, para no mezclar
correcciones. ¿Nos lo mandan en el request, o lo consultamos? ¿Quién valida que al
alumno le queden intentos: ustedes antes de llamarnos, o nosotros?

**6. NO los bloqueamos por desafío.**
El reparto dice que "03 y 05 no muestran nota hasta que el 04 confirme". Descartamos hacer
una encuesta por desafío (no está en el PRD y genera fatiga de respuesta), así que **no
queda ningún instrumento exigible a nivel desafío**: pueden revelar la nota de cada desafío
sin consultarnos. El único gate de encuesta es al cierre de curso, sobre el resultado
académico final, y ahí el interlocutor es el 02/10, no ustedes. ¿Lo confirman?

---

## Para el Grupo 07 — Evaluación LLM

**7. ¿Corrigen respuestas abiertas, o solo evalúan el uso de IA?**
El reparto los define como el evaluador de la rúbrica de uso de IA (30/25/20/15/10 sobre
la transcripción alumno-tutor). La corrección semántica de una respuesta teórica abierta
es otra cosa y **no está asignada a nadie en el reparto**. ¿La toman ustedes, o la
tomamos nosotros con nuestra propia invocación a LLM?

**8. Si la toman ustedes: contrato y calibración.**
Necesitaríamos enviarles `{ enunciado, respuesta_alumno, material_de_referencia,
criterio_de_correccion }` y recibir `{ nota, justificacion, confianza }`. ¿Es viable?
¿Esa función entra bajo la restricción de modelo único activo de RF-IA-25, o admite pool
como las demás funciones de RF-IA-26?

**9. Reutilización de infraestructura.**
Si la corrección abierta queda de nuestro lado, igual queremos reutilizar su mecanismo de
calibración y golden set en vez de inventar otro. ¿Está pensado como algo genérico o está
atado a la rúbrica de uso de IA?

---

## Para el Grupo 11 — Social y Notificaciones

**10. Moderación de comentarios de encuesta (RF-ENC-07).**
Los comentarios de texto libre deben pasar por el agente moderador antes de almacenarse,
con las mismas categorías y severidades del chat. Está en su columna "para más adelante".
- ¿Existe en el sprint 1?
- ¿Cuál es el contrato? Proponemos sincrónico: `POST /moderacion/evaluar` →
  `{ permitido: bool, severidad: BAJA|MEDIA|ALTA }`.
- Es sincrónico y bloqueante: si no responde, el comentario no se guarda y el alumno debe
  poder reescribirlo en el momento (RF-ENC-07 no admite apelación, porque apelar exigiría
  identificar al autor y rompería el anonimato).

Mientras no exista, vamos con un stub que aprueba todo, detrás de un feature flag.

**11. Contrato de eventos.**
Ustedes definen el contrato de eventos de la plataforma. Vamos a publicar al menos:
`encuesta.cumplida`, `teorico.corregido`, `encuesta.periodo_cerrado`.
¿Qué formato de envelope usamos (nombre del evento, versión, id de correlación,
curso_cohorte_id, timestamp)? ¿Y qué tecnología de bus?

**12. Notificaciones que necesitamos disparar.**
Encuesta pendiente, corrección disponible, resultado de desafío teórico publicado.
¿Publicamos un evento y ustedes deciden la notificación, o llamamos a un endpoint suyo?

---

## Para el Grupo 12 — Backoffice

**13. Qué les exponemos en el sprint 1.**
Los KPI-01 y KPI-02 salen de nosotros. Proponemos exponer, siempre agregado y nunca
atribuible:
- `GET /encuestas/kpi?curso={id}&periodo={p}` → `{ csat, pct_satisfechos, pct_detractores, n_respuestas, tasa_abstencion }`
- `GET /encuestas/kpi/plataforma?periodo={p}` → lo mismo, consolidado

**14. Restricción no negociable sobre esos contratos.**
Ninguna lectura nuestra expone una respuesta individual ni permite reconstruir quién
respondió qué (RF-ENC-04/12). Por debajo del umbral de PAR-18 (5 respuestas), el endpoint
por curso devuelve **solo el conteo**, sin puntajes ni comentarios, y eso vale también
para ustedes. El consolidado de plataforma no tiene esa restricción porque el volumen
agregado la vuelve inaplicable.

**15. Frescura.**
Su columna pide 15 minutos de frescura máxima. ¿Consultan on-demand o quieren que
publiquemos un evento de agregados recalculados?

---

## Para el Grupo 02 — Cursos y Matrícula

**16. ¿Cómo sabemos quién está en la cohorte?**
Necesitamos saber si un alumno pertenece al curso y con qué rol, en cada operación.
¿Consulta sincrónica por el gateway, o publican eventos de inscripción y mantenemos una
proyección local? La proyección local escala mejor pero introduce consistencia eventual
en una decisión de autorización.

**17. Disparo de encuesta al cierre.**
La encuesta de curso y la de contenido se disparan al cierre (RF-ENC-03), y deben
completarse **antes** de que el alumno vea su resultado académico final (RF-ENC-11).
¿Nos avisan con un evento `curso.cierre_iniciado`? ¿Y quién bloquea la revelación del
resultado: ustedes consultándonos, o el Grupo 10?

**17b. ⚠ El cierre de curso puede quedar en deadlock. Es la pregunta más urgente que les tenemos.**
RF-ENC-11 dice que el alumno responde la encuesta antes de ver su resultado final.
RF-RNK-10 dice que el profesor debe confirmar el estado final de **todos** los alumnos para
archivar. Un alumno con estado `abandonó` no entra a la plataforma hace meses y **nunca va a
responder**. Si ustedes implementan "no archivo hasta que todos cumplan", el curso no cierra
nunca.

Nuestra resolución propuesta: **el gate es sobre el alumno viendo su resultado, no sobre el
profesor archivando.** El curso archiva con encuestas pendientes; el alumno que no respondió
se encuentra la encuesta cuando entre a ver su nota, y el curso archivado sigue siendo
visible en modo lectura (RF-CUR-09), así que el circuito cierra igual.

Las dos lecturas son defendibles leyendo el PRD y **solo una funciona**. Necesitamos
acordarla antes de que cada uno implemente la suya.

**18. Desmatriculación a mitad de cuatrimestre.**
Está listado como decisión abierta. Nos afecta: un alumno que se va del curso, ¿debe
igual responder la encuesta? ¿Sus respuestas ya enviadas se conservan? Nuestra postura:
se conservan (ya son anónimas, no hay nada que desvincular) y no se le exige nada más.

---

## Para el Grupo 01 — Identidad y Usuarios (dueños del API Gateway)

**25. ¿Quién aplica el bloqueo por encuesta pendiente?**
RF-ENC-09: responder es obligatorio para avanzar. La encuesta de plataforma bloquea el uso
de la plataforma hasta responder o abstenerse. La pregunta es dónde se aplica ese bloqueo:

- **Opción A (nuestra propuesta para el sprint 1):** lo aplica el front, con nuestro
  endpoint `GET /encuestas/pendientes?alumno={id}` como fuente de verdad. Simple, pero
  quien llame a la API directamente lo saltea.
- **Opción B:** filtro en el gateway. Antes de rutear, consulta si el usuario tiene
  encuestas bloqueantes pendientes y rechaza todo salvo las rutas de la encuesta. Es el
  único bloqueo real, y les toca a ustedes.

Proponemos A para el sprint 1 con B diseñado y documentado como paso siguiente. ¿Están de
acuerdo, o prefieren tomar B desde el arranque?

**26. Claims del token.**
¿Qué claims trae el token y con qué vigencia? Necesitamos al menos el id de usuario y el
rol. La pertenencia a la cohorte la responde el 02 (no el token), según el reparto — lo
confirmamos para no duplicar la fuente.

---

## Para el Grupo 10 — Roadmap y Progreso

**19. Nosotros no otorgamos XP.**
Confirmamos que el 04 no otorga XP ni monedas: emitimos el hecho de la corrección y
ustedes deciden el efecto. ¿Reciben ese hecho del 03 o directamente de nosotros?

**20. Responder encuestas no paga nada (RF-ENC-06).**
Ni XP, ni monedas, ni insignias. Lo dejamos asentado para que no aparezca después como
"mecánica de enganche".

---

## Para la cátedra / Product Owner

**21. El gate de encuesta es al cierre de curso, no por desafío.**
La propuesta arquitectónica dice que "03 y 05 no muestran nota hasta que el 04 confirme",
pero el PRD define tres instrumentos y tres disparos, ninguno por desafío. Descartamos
crear una microencuesta por desafío: no está en el PRD, y con abstención obligatoria en
cada desafío la fatiga de respuesta dispararía la tasa de abstención — que es exactamente
el argumento que RF-ENC-02 usa para no agregarle una cuarta pregunta a la encuesta de
cierre.

Consecuencia: **el 04 no bloquea al 03 ni al 05**. El único gate es RF-ENC-11, sobre el
**resultado académico final** al cierre (RF-RNK-10), y ahí el interlocutor es el 02/10.
¿Se confirma esa lectura de la frase del reparto?

**22. Encuesta de plataforma: alcance, disparo y bloqueo.**
No pertenece a ningún curso, lo que la vuelve la excepción a la regla de que toda entidad
se acota por curso-cohorte. Nuestras decisiones, a confirmar:
- La **fila de respuesta** de la encuesta de plataforma va **sin** `curso_cohorte_id`
  (el alumno puede cursar varias materias, y cada dimensión extra en la fila anónima
  reduce el conjunto de posibles autores). El `curso_cohorte_id` **sí** va en el marcador
  de cumplimiento, que es nominal por diseño: de ahí sale toda la métrica de cobertura
  por curso, sin costo de anonimato.
- Una instancia por alumno **por período** (RF-ENC-03: a los 30 días y luego una vez por
  período), no una sola vez en la vida del alumno.
- Bloquea el uso de la plataforma hasta responder o abstenerse explícitamente. Ver
  pregunta 25: quién aplica ese bloqueo es una decisión que involucra al Grupo 01.

**22b. Límite de exposición del marcador.**
RF-ENC-12 establece que el profesor pierde la capacidad de saber quién no respondió y solo
ve el conteo agregado (aceptado como precio del anonimato, RSK-13). Por eso el marcador es
per-alumno **internamente** —lo necesitamos para el gate— pero se expone **siempre
agregado**, a profesor y a Backoffice. Si expusiéramos la lista nominal, en un curso chico
el profesor deduce por descarte quién escribió cada comentario, y el criterio de release
15b no se cumple.

**23. Corrección de respuestas abiertas: alcance del MVP.**
Definimos que en el sprint 1 la corrige el **profesor**, con el contrato diseñado para que
el LLM la tome después sin refactor. ¿Se valida?

**23b. ¿Quién redacta las preguntas de la encuesta?**
El PRD define las tres dimensiones (curso, contenido, plataforma) y la escala de 5 estrellas,
pero no el **texto de las preguntas**. Nuestra postura: **lo fija la plataforma y es
versionado**, no lo redacta cada profesor. Si cada uno escribe la suya, el KPI-02 deja de ser
comparable entre cursos — el mismo argumento que RF-CFG-05 usa para que el profesor no toque
los parámetros globales. ¿Se valida?

Derivado: ¿"curso" y "contenido" son **dos instrumentos separados** o uno solo con dos
preguntas? Afecta cómo se cuentan los denominadores de KPI-01 y KPI-02.

**23c. Tipos de ítem que quedan fuera del Sprint 1.**
De los siete tipos de RF-DES §8.2, dos no encajan en el modelo de "responder y enviar":
- **Conversación sobre el contenido** — es multi-turno, no un envío único.
- **Debate estructurado** (defensa de postura frente a compañeros o IA) — involucra a otros
  alumnos en simultáneo, o al Tema 07.

Los declaramos **fuera del Sprint 1**, con el modelo preparado para guardar una transcripción
cuando se implementen. Los otros cinco tipos entran completos. ¿Se acepta?

**24. Edición de ítems ya respondidos.**
RF-CUR-05 permite editar desafíos publicados y en uso. Si un profesor corrige el enunciado
de un ítem que 20 alumnos ya respondieron, ¿qué pasa con esas correcciones? Nuestra
propuesta: los ítems se versionan y la respuesta guarda `item_version_id`; editar crea una
versión nueva y no recalcula lo ya corregido. Es el mismo criterio que el PRD ya usa para
`rubric_version` (RF-IA-13) y para los parámetros de economía (RF-CFG-06).
