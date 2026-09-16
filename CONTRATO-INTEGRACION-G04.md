# Grupo 04 — Contrato de integración con el resto de la plataforma

> **Qué es este documento.** El registro de las decisiones de integración que va tomando
> el G04, tomadas una por una recorriendo el camino del desafío teórico. Es la fuente de
> verdad de lo acordado; `DISENIO-G04-SPRINT1.md` sigue siendo la fuente de verdad del
> diseño interno del microservicio.
>
> **Estado del contexto (confirmado 2026-09-08):** ningún grupo escribió contratos
> todavía — somos el primer mover. Ningún microservicio ajeno va a estar levantado en el
> Sprint 1: todo se diseña asumiendo aislamiento total. El intercambio entre grupos va por
> repositorio compartido. **El front de la plataforma es un monolito único**, no un front
> por grupo.

---

## La forma exacta de cada mensaje

Este documento explica **por qué** cada decisión es como es, y es lo que hay que leer para
discutirla. La **forma** de cada mensaje —campos, tipos, códigos de error, las tres credenciales
y cuándo va cada una— está publicada como OpenAPI por la alfa ejecutable:

```
http://localhost:8081/swagger-ui/index.html    para leerlo y probarlo
http://localhost:8081/v3/api-docs              para generarse un cliente
```

Se levanta con `docker compose up` desde `alfa/`. Si alguna vez el spec y este documento se
contradicen, **manda este documento**: acá está el argumento, allá solamente la forma.

---

## 0. Estrategia general

**Regla de asimetría.** Con lo que **consumimos** somos tolerantes y degradables: toda
dependencia externa entra al código detrás de un puerto, con un adaptador stub tras
feature flag. Con lo que **producimos** somos estrictos y versionados.

**Somos el primer mover, con un límite.** Publicamos contrato definitivo donde somos
dueños (nuestros endpoints, nuestros eventos, nuestros payloads) y propuesta marcada como
supuesto donde no lo somos (envelope, nombres de tópicos, retención). El contrato de
eventos de plataforma es del Tema 11 por reparto; no se lo pisamos.

**Formato de publicación.** OpenAPI generado por springdoc para lo sincrónico. Para
eventos, un JSON Schema por evento **más un fixture de ejemplo** — un ejemplo ejecutable
lo puede correr el otro grupo contra su implementación; una tabla en Markdown no.

---

## 1. Decisiones cerradas

| # | Decisión | Motivo |
|---|---|---|
| CI-01 | **Simetría 03 ↔ {04, 05}: una interfaz con dos implementaciones.** El Tema 03 nunca mira adentro del contenido de un desafío ni adentro de la respuesta de un alumno. Para él son cajas opacas | Lámina 3 del PDF de arquitectura: teórico y práctico son dos tipos de desafío, no dos cosas. Un servicio que transporta algo que no entiende no queda acoplado a eso. En cuanto el 03 tenga que saber qué es un ítem de opción múltiple, la simetría se rompe |
| CI-02 | **El catálogo del banco se sirve directo al front.** El 03 no agrega ni intermedia catálogos de contenido ajeno | Es la primera grieta por donde después se cuela el `if (tipo == ...)` adentro del 03 |
| CI-03 | **Composición: el front compone (alternativa B).** Primero el contenido en el 04 o el 05, que devuelve un identificador; después el desafío en el 03, con la referencia ya formada | Es la única opción donde **el contrato de composición entre el 03 y nosotros no existe**, y un mensaje que no existe es trivialmente idéntico para el 04 y el 05. Además falla bien en las dos direcciones: si el 03 está caído se puede armar contenido, y si nosotros estamos caídos el 03 sigue creando desafíos |
| CI-03b | Rechazada la **alternativa A** (03 primero, contenido después, reconciliación) | Tres llamadas y una transacción distribuida hecha a mano |
| CI-03c | Rechazada la **alternativa C** (el 03 orquesta y nos reenvía un blob) | Convierte al 03 en orquestador con transacción distribuida, y si nuestro servicio está caído el profesor no puede crear **ningún** desafío teórico |
| CI-04 | **`contenidoRef` tiene cinco campos** (ver §2) | Cada campo tiene un motivo explícito; ninguno filtra conocimiento de contenido al 03 |
| CI-05 | **Relación 1:1 entre contenido y desafío.** Un `contenidoId` no se reutiliza en varios desafíos. Reutilizar un ítem se hace clonándolo desde el banco al componer | *(Motivo reescrito en la revisión del 2026-09-08: el original invocaba el congelamiento, que CI-12 abolió.)* Como la referencia es **flotante** (CI-13), si un mismo contenido colgara de tres desafíos, editarlo cambiaría los tres a la vez en silencio — incluidos los de otros cursos o cohortes, cuyos profesores nunca lo pidieron. Con 1:1, editar afecta exactamente al desafío que el profesor tiene delante |
| CI-06 | **Nosotros hablamos en nota normalizada 0-100. El 03 la mapea a XP** | Si habláramos en XP, las reglas de la economía quedarían escritas en tres lugares. El 05 hace lo mismo desde "casos de prueba pasados" |
| CI-07 | **El modo de corrección es por ítem, es nuestro, y al 03 solo le llega la consecuencia:** `correccion: INMEDIATA \| DIFERIDA` | Para que el 03 guardara el modo tendría que saber qué es un ítem de opción múltiple y qué uno de respuesta abierta. Y el modo no le cambia nada: fechas, XP y reintentos son iguales. Lo único que sí le cambia la pantalla y la máquina de estados es si tiene que esperar el resultado |
| CI-08 | **Si el profesor eligió corrección por IA y el Tema 07 no responde, la corrección cae a la cola del profesor.** Nunca a nota neutra | Una nota neutra en una evaluación académica no es aceptable, aunque sea el patrón que el PRD usa para el score de uso de IA (RF-IA-27) |
| CI-09 | **La lista de documentos (`materialRefs`) se queda en nuestro registro de contenido y NO viaja al 03. Al 03 le va solo `unidadId`** | *(Corregido en la revisión del 2026-09-08. La versión anterior mandaba la lista al 03.)* El material del curso no es un bloque: son documentos con roles distintos (material teórico, guía de ejercicios, anexos) y el rol cambia por completo las preguntas que se generan — por eso es una lista y no un campo. Pero es **insumo de generación, que es asunto nuestro**: aplicada la pregunta de siempre, el 03 no hace nada con esa lista. Mismo caso que `corrector` y `detalle` (CI-38, CI-28). **Lo que sí es de ellos es `unidadId`:** un desafío pertenece a un nodo del roadmap y el 03 lo necesita para ubicarlo en el mapa del curso |
| CI-10 | **Generar contenido es siempre nuestro. Decidir que haya que generarlo a veces es del 03.** Un endpoint, dos llamadores posibles: el front cuando el profesor genera a mano, el 03 cuando un alumno pide un desafío personalizado | El reparto ya los separa: "generación asistida de ítems" está en nuestra columna, "desafíos personalizados por LLM" en la del 03. El 03 nunca sabe cómo se escribe un ítem |
| CI-11 | **`origen` NO viaja al 03.** Se queda en nuestro registro, para trazabilidad de IA | Revierte una propuesta anterior de esta misma sesión. Se había justificado por economía (los desafíos personalizados pagan distinto), pero el 03 ya lo sabe: el desafío personalizado lo crea él porque el alumno se lo pidió a él |
| CI-12 | **No existe un momento de congelamiento. Existe una regla permanente:** cada respuesta guarda qué versión de cada ítem vio el alumno. Editar siempre crea versión nueva y nunca recalcula lo ya corregido | Mismo criterio que el PRD ya usa para `rubric_version` (RF-IA-13) y para los parámetros de economía. Un instante de congelamiento habría requerido un mensaje del 03 avisándonos que publicó; la regla permanente no requiere ninguno |
| CI-13 | **La referencia al contenido es flotante, no pinneada.** El desafío sirve siempre la última versión; el alumno que todavía no respondió ve la corrección | RF-CUR-05 existe para que el profesor pueda arreglar un error mientras los alumnos trabajan. Con referencia pinneada, la corrección no surtiría efecto hasta que alguien subiera la versión del desafío a mano — y ese "subir la versión" sería un mensaje nuevo hacia el 03. **Consecuencia aceptada:** dos alumnos del mismo desafío pueden haber respondido enunciados distintos |
| CI-14 | **La ficha que guarda el 03 es una caché, y nosotros la invalidamos.** Cuando cambia algo que la afecta, publicamos `CONTENIDO_ACTUALIZADO` con `{ contenidoId, tipo, version, resumen, correccion }`. El endpoint de ficha queda como reconciliación | Detectado como bug del contrato del Paso 1: si el profesor agrega un ítem de respuesta abierta a un cuestionario que era todo V/F, el desafío pasa de `INMEDIATA` a `DIFERIDA` y el 03 seguiría prometiendo un resultado al instante que nunca llega. Se eligió empujar (evento) sobre consultar (endpoint) para preservar que **el 03 pueda dibujar su lista de desafíos con nuestro servicio apagado** |
| CI-15 | **El 03 no nos avisa que publicó un desafío.** No lo necesitamos | Publicar significa que los alumnos ya pueden llegar; cuando lleguen, la respuesta nos llega a nosotros con sus estampas de versión. Fechas, asignación y estado son del 03, y nosotros nunca validamos si la ventana está abierta (CI-01) |
| CI-16 | **El enunciado se lo servimos nosotros al front, directo.** El front le pide el desafío al 03 (título, fechas, intentos restantes, estado), ve que el tipo es `TEORICO` y viene a buscarnos el contenido | **Segunda aparición de la misma regla del Paso 0: la tabla de ruteo tipo → servicio la tiene el front, no el 03.** Que la misma forma gane en dos momentos distintos del ciclo indica que la frontera está bien puesta y no se está improvisando caso por caso. Rechazado que el 03 proxee: lo pone de conducto en el camino caliente —cada alumno abriendo cada desafío— y convierte nuestra caída en la suya |
| CI-17 | **Dos endpoints y dos proyecciones separadas**, no un endpoint con filtro por rol: `GET /teoricos/contenidos/{id}/vista-alumno` (sin nada de corrección) y `/vista-profesor` (completo). Con un test que afirma que la proyección del alumno **no contiene** los campos de corrección | Es una frontera de seguridad, no de estilo: si la respuesta correcta, el criterio, la lista de respuestas aceptadas del modo "texto exacto" o el prompt de la IA viajan en el payload, todo el trabajo de corrección vale cero — no importa que el front no los pinte. Filtrar campos por rol en un endpoint único funciona hasta que alguien agrega un campo y se olvida; dos proyecciones hacen la fuga **imposible por construcción y no por disciplina**. Es el mismo estilo de prueba que ya usamos para el anonimato de encuestas (un test que intenta la reconstrucción y afirma que falla) |
| CI-18 | **Autorización de lectura del contenido: vale de lectura firmado, emitido por el 03 al abrir el desafío.** Validamos la firma sin llamar a nadie. Además, pertenencia al curso | *(Cambiado en la revisión del 2026-09-08. Antes era "pertenencia ahora, vale como deuda".)* **El argumento de que era caro se cayó con CI-21:** el vale dejó de ser un mecanismo aparte y pasó a ser el valor de retorno de una llamada que el 03 ya hace igual. Ver §2b para las tres ventajas y para lo que se perdía sin él |
| CI-20 | **Hay dos validaciones, no una: al abrir ("¿puede empezar?") y al enviar ("¿sigue pudiendo?").** Las dos las hace el 03 | Validar solo al enviar significa que el alumno contesta veinte minutos y ahí se entera de que no tenía intentos. Validar solo al abrir ignora que el tiempo pasa: la ventana se puede cerrar mientras contesta, o puede haber abierto el desafío en dos pestañas. **El reparto le asigna al Tema 03, dentro del núcleo, "cierre evaluado al momento del envío"** — o sea que la validación de la fecha límite es explícitamente al enviar. La de apertura es una cortesía; la del envío es la que manda |
| CI-21 | **Al abrir, el 03 crea el intento y devuelve `{ entregaId, intento, contenidoRef, vale }`** | Consecuencia buena e inesperada de CI-20: **el vale de lectura de §2b deja de ser un mecanismo aparte y pasa a ser el valor de retorno natural de "empezar el desafío"**. El 03 ya está validando ahí y ya devuelve la referencia al contenido; firmar esa respuesta es un paso más, no una pieza nueva. Abarata la deuda declarada lo suficiente como para reconsiderarla. Efecto secundario: el `entregaId` existe desde la apertura, así que la clave de idempotencia está disponible antes del envío |
| CI-22 | **La entrega llega al 03 y el 03 nos despacha.** El mensaje de despacho es idéntico para el 04 y el 05 salvo el campo `respuesta`, que es opaco | Ver §2c para el argumento de defensa completo. **No contradice la Lámina 3:** su nota ① es sobre quién invoca al LLM (nosotros, directo), y eso no cambia. La lámina no tiene alumno ni front entre sus actores, así que no modela el envío |
| CI-23 | **Respondemos siempre `202 Aceptado` con `{ evaluacionId, estado, correccion }`. Nunca la nota**, aunque la corrección sea inmediata | "Inmediata" no significa "en la misma respuesta HTTP": significa "en segundos y sin que intervenga un humano". Si devolviéramos la nota cuando es inmediata y `202` cuando es diferida, el mismo endpoint tendría dos formas de respuesta y volvería a haber un `if` adentro del 03 — que es exactamente lo que este trabajo compra. Además el sandbox del 05 nunca va a poder responder sincrónico bajo el pico de cierre que el propio PDF anticipa |
| CI-24 | **El despacho es una llamada sincrónica, no un evento** | Regla del propio PDF: "si necesito la respuesta para continuar, es sincrónico". El 03 necesita un bit: **si aceptamos la entrega o no**. Si el bloque viene mal armado o el `contenidoId` no existe, el 03 no puede haberle dicho "entregado" al alumno y descubrirlo después. Con un evento la entrega desaparecería en silencio, que es la peor falla posible en un sistema académico. El costo —que el 03 reintente si estamos caídos— es trabajo que igual necesita para el 05 |
| CI-25 | **`entregaId` es nuestra clave de idempotencia.** Si el 03 reintenta el despacho, no corregimos dos veces ni emitimos dos eventos | Mismo criterio que le pedimos al 10 y al 11 con `eventId` |
| CI-26 | **Una revisión vigente a la vez por entrega, emitida cuando la nota está cerrada. No emitimos parciales.** *(Redacción corregida el 2026-09-08: decía "un solo evento por entrega", que contradice a CI-52 — el recálculo y la apelación emiten revisiones posteriores.)* | Si mandáramos primero un 40 y después un 85, el 03 tendría que manejar una nota que cambia y el 10 podría pagar XP dos veces. **Precio aceptado:** si el profesor nunca corrige, el 03 se queda esperando — por eso necesita un aviso por demora, que igual va a necesitar porque RF-CUR-08b no le deja archivar el curso con evaluaciones diferidas pendientes |
| CI-27 | **Las penalidades las aplica el 03 sobre el XP. Nosotros devolvemos siempre la nota académica limpia** | La entrega tardía descuenta 30% en la ventana de 48 h, pero si nosotros descontáramos, la economía quedaría escrita en dos lugares. Continuación de CI-06 |
| CI-28 | **El desglose por ítem no va en el evento de resultado.** Al 03 le va la nota; el desglose se lo servimos al front, directo | Cuánto sacó en cada pregunta y por qué es conocimiento de contenido, y al 03 no le hace falta: con la nota le alcanza para su XP. El alumno sí lo quiere ver, y se lo servimos igual que los enunciados del Paso 3. Misma regla de siempre, aplicada una vez más |
| CI-29 | **El 03 publica `DESAFIO_INVALIDADO`. Regla de consumo: usarlo para ahorrar trabajo, sí; para decidir si una entrega es válida, no** | Ver §2d. **El que filtra es el que tiene la verdad, no el que la escuchó por radio** |
| CI-30 | **Postura: invalidar un desafío no se aplica retroactivamente a los intentos ya iniciados.** El evento lleva `vigenciaIntentosEnCurso`: la fecha hasta la que el 03 sigue aceptando entregas de intentos abiertos | Mismo criterio que el PRD ya usa en RF-CUR-05 para las ediciones: lo que ya se hizo no se recalcula. Es la postura defendible académicamente |
| CI-31 | **El mensaje al Tema 07 no lleva `alumnoId`, `desafioId` ni `cursoCohorteId`.** Solo enunciado, respuesta del alumno, material de referencia, criterio y `rubricaId`. La correlación con la entrega la guardamos nosotros | La Lámina 3 lo dice: **el 07 no conoce desafíos, cursos ni alumnos**. Efecto secundario valioso: **la llamada al LLM queda anónima por construcción** — no es una precaución que agregamos, es consecuencia de respetar su frontera |
| CI-32 | **Adoptamos el campo `confianza` que devuelve el evaluador, en vez de definir uno propio** | RF-IA-17 ya obliga al evaluador a emitir un nivel de confianza de su propio scoring. Usamos el que la plataforma ya tiene |
| CI-33 | **Reusamos el mecanismo de revisión de RF-IA-17:** los casos de baja confianza más un muestreo aleatorio (PAR-10, default 10%) van a la cola del profesor. **Excepción: en desafíos con reintentos ilimitados no se aplica el muestreo** | Es gratis: la regla ya está escrita y el profesor ya va a tener esa pantalla por el otro motivo. Y nos evita discutir si nuestra corrección por IA es confiable — se audita con el mismo criterio que todo lo demás. **La excepción se agregó en la revisión del 2026-09-08:** CI-47 prohíbe la corrección humana en desafíos con reintentos ilimitados justamente para que la cola del profesor tenga techo, pero el muestreo la dejaba entrar por otra puerta — un 10% de infinito sigue siendo infinito. Se justifica sola: esos desafíos pagan 10/20/30 XP, así que auditarlos cuesta más de lo que valen. Misma lógica con la que el PRD les puso XP bajo |
| CI-34 | **Nuestra corrección de respuestas abiertas cae bajo RF-IA-25: un único modelo activo, sin pool ni ruteo**, con `model_id` y `model_version` registrados en cada corrección. **El plan B ante caída no es otro modelo, es el profesor** (CI-08) | Es más restrictivo que lo que el PRD nos exige —no figuramos en la lista de RF-IA-25 ni en la de RF-IA-26— y lo proponemos igual. **El argumento decisivo:** con un pool, dos alumnos que responden la misma pregunta pueden ser corregidos por modelos distintos; sus notas dejan de ser comparables dentro de la cohorte, y el ranking se calcula sobre esa cohorte. Es exactamente el "carácter académico" que invoca RF-IA-25. Y el costo de equivocarse es asimétrico: con modelo único de más, perdemos rendimiento; con pool de más, invalidamos las notas de una cohorte. **Objeción esperable y su respuesta:** RF-IA-27 dice que la caída de una dependencia nunca bloquea al alumno, y con un solo modelo no hay failover — pero nuestro plan B es humano, así que ya estamos cubiertos sin el pool. **Alcance, para que no se lea de más (aclarado el 2026-09-08): esto vale para la corrección, NO para la generación de ítems (CI-10)** — RF-IA-26 lista explícitamente al "generador" entre las funciones que sí admiten varios modelos en simultáneo |
| CI-35 | **Declaramos la apelación de la corrección**, con el mismo formato de auditoría de RF-IA-18: profesor, fecha, nota anterior y nueva, motivo | Nadie la pidió (ver H-13) y nos la sumamos solos. El argumento es más fuerte que el de RF-IA-18: **el score de uso de IA es un bonus, la corrección es la nota**. Es enteramente nuestro de implementar, no involucra a ningún otro grupo — pero si no lo declaramos, no existe |
| CI-36 | **Le exponemos al Tema 02 un endpoint de pendientes:** `GET /teoricos/pendientes?curso={id}` → `{ correccionesPendientes: 12 }`, para consultar antes de archivar | Cierra el agujero de H-14. El 02 lo consulta igual que consulta al 07 por los scores diferidos. **Es simétrico:** el 05 va a deber el mismo endpoint por sus ejecuciones encoladas. No estaba en `DISENIO-G04-SPRINT1.md` |
| CI-37 | **No notificamos al alumno que su resultado está listo.** Solo publicamos el hecho de que el profesor tiene cola de corrección | Nosotros emitimos `TEORICO_CORREGIDO`, el 03 consolida y emite `DESAFIO_RESUELTO`, y el 11 reacciona a ese. Si avisáramos nosotros, el alumno se enteraría de su nota **antes** de que el 03 le aplicara la penalidad por tardanza y el 10 el XP. Es la cadena de D-14 aplicada a las notificaciones, y vale escribirlo porque el reflejo natural es "ya tengo la nota, la anuncio" |
| CI-38 | **`corrector` sale del evento.** `AUTOMATICO \| LLM \| HUMANO` se lo servimos al front para que el alumno lo vea; al 03 no le va | La nota ③ de la Lámina 3 dice textualmente que al 03 *"no le importa cómo se produjo"* la nota. No le cambia el XP, ni las vidas, ni el estado. **Tercera aplicación de la misma regla**, y la primera vez que sirve para sacar algo que ya estaba escrito en vez de para no agregarlo. Beneficio extra: con la apelación de CI-35 el campo se complica —una respuesta corregida por LLM y después sobrescrita por el profesor tiene dos correctores— y esa complejidad queda enteramente de nuestro lado |
| CI-39 | **`aprobado` sale del evento. Devolvemos la nota 0-100 y nada más; aprobar o no lo decide el 03** | Ver §2e. En una frase: **el PRD no define en ningún lado el umbral de aprobación** (H-16), así que decirlo sería inventar una regla académica; y como el umbral maneja XP (PAR-01) y vidas (RF-DES-07), es economía, y la economía vive en un solo lugar |
| CI-40 | **`estado: FINAL \| SIN_CORRECCION` en el evento.** `EN_CURSO` existe en el endpoint de reconciliación pero **nunca viaja en un evento** | Con CI-26 solo emitimos cuando la nota está cerrada, así que `FINAL` sola sería una constante. `SIN_CORRECCION` cubre el caso que no teníamos: cuando no va a haber nota nunca (desafío invalidado, profesor que no corrigió y curso cerrado, contenido roto). Le dice al 03 dos cosas concretas: **no otorgues XP y no descuentes vida** |
| CI-41 | **`alumnoId` se queda en el evento, a propósito** | No contradice H-05: ahí el problema es que una encuesta **anónima** lleve identificador. Acá la nota de un alumno es un dato que el 03 conoce legítimamente. Se documenta para que no parezca un descuido cuando alguien compare los dos eventos |
| CI-42 | **Clave de partición `desafioId + alumnoId`, más el 03 descartando eventos con `intento` menor al ya registrado.** Se propone como **regla de plataforma**, no como acuerdo bilateral | Ver §2e. Kafka garantiza orden solo dentro de una partición: si los dos intentos de un alumno caen en particiones distintas, el 03 puede procesar el intento 2 antes que el 1 y quedarse con la nota vieja. Se hacen las dos cosas porque son baratas y la segunda no depende de que nadie configure bien el productor |
| CI-43 | **`GET /teoricos/evaluaciones/{entregaId}` devuelve el mismo payload que el evento**, más `EN_CURSO` si todavía no terminamos | Reconciliación para cuando el 03 perdió el evento. Mismo patrón que CI-14 |
| CI-44 | **Guardamos una corrección por `entregaId` y nunca la pisamos.** Todos los intentos se conservan | No es opinable: RF-NFR-01 prohíbe el borrado físico en toda la plataforma, y sobrescribir una corrección con una mejor es destruir un registro académico. Además la apelación de CI-35 necesita la corrección del intento 2 intacta si el alumno apela el intento 2, y la analítica de dificultad por ítem —que está en nuestra columna del reparto— necesita la progresión |
| CI-45 | **Cuál intento "cuenta" lo decide el 03, no nosotros. Emitimos un evento por intento, cada uno con su nota** | Determina el XP, y la Lámina 3 dice que la economía vive en un solo lugar. Si nosotros dijéramos "la mejor" y el 05 "la última", dos alumnos con trabajo equivalente tendrían tratos distintos. Argumento más fuerte: **un desafío puede tener parte teórica y parte práctica** —es la razón de ser de D-14— y si cada evaluador eligiera su intento, la consolidación del 03 sería incoherente. **Beneficio para nosotros:** si la cátedra cambia de "la mejor" a "la última", no tocamos una línea. **Nuestra recomendación, no decisión:** "la mejor" donde hay tope de reintentos, "la última" donde no lo hay (ver H-17) |
| CI-46 | **Corregimos todo lo que aceptamos**, aunque el alumno ya haya reintentado y el intento viejo quede superado | Caso detectado en el Paso 7: el alumno entrega el intento 1 con una respuesta abierta que queda esperando al profesor, y sin esperar reintenta. Con "la mejor" hay que corregir el 1; con "la última" es trabajo tirado. Como no conocemos la regla (CI-45) y no queremos inventar un mensaje nuevo para preguntarla, corregimos todo. El costo está acotado por el tope de reintentos y lo necesitamos igual para la apelación |
| CI-47 | **Un desafío con reintentos ilimitados no admite ítems de corrección humana.** Lo validamos nosotros al componer | Sale de CI-46: si corregimos todo y los reintentos no tienen techo, **la cola del profesor tampoco lo tiene** — un alumno puede generarle treinta correcciones manuales. Es una regla nuestra, la aplicamos solos, y protege al profesor de algo que ningún documento previó |
| CI-48 | **El reintento puede ver una versión más nueva del contenido**, si el profesor editó en el medio. Cada respuesta queda estampada con lo que vio | Consecuencia directa de CI-13 (referencia flotante). No hace falta nada nuevo. **Y el pool de RF-REC-04 no nos toca:** elegir al azar *qué desafío* del pool es del 03; a nosotros nos llega un despacho igual a cualquier otro |
| CI-49 | **No todas las ediciones son iguales.** Errata en el enunciado: inocua. Agregar o sacar un ítem: cambia `resumen`, puede cambiar `correccion` de `INMEDIATA` a `DIFERIDA` (el bug de CI-14) y hace que dos alumnos del mismo desafío sean evaluados sobre distinta cantidad de preguntas. Arreglar la clave de corrección: caso aparte, ver CI-50 | Veníamos tratando "editar" como una sola cosa. La consecuencia de la segunda ya está asumida por CI-13; se nombra para que nadie la descubra después |
| CI-50 | **Editar nunca recalcula (CI-12 se mantiene). Recalcular es una acción aparte, explícita y auditada, que pide el profesor** | CI-12 se pensó para el caso del enunciado —donde no recalcular es correcto, porque el alumno respondió a otra pregunta— y se aplicaba por igual a un caso donde no recalcular es incorrecto: **un profesor que arregla una clave mal cargada quiere recalcular, es el sentido mismo de arreglarla**. Sin esto, veinte alumnos quedan con una nota que el profesor sabe injusta y hay que corregirlos a mano de a uno. Rechazado el recálculo automático al cambiar el criterio: cambiaría notas ya emitidas sin que nadie lo decidiera, con el XP ya dado por el 03 y aplicado por el 10 |
| CI-51 | **El recálculo emite un segundo `TEORICO_CORREGIDO` sobre una entrega que el 03 ya cerró, y la nota puede bajar** | **No pedimos nada nuevo:** el reparto le asigna al Tema 10 "XP retroactivamente reducible", y el PDF aclara que *"el XP reducible impide modelar el progreso como contador incremental: se necesita historial"*. **Cómo plantearlo:** llegar con "necesitamos bajar una nota ya dada" suena a pedido raro; llegar con "somos el caso de uso para el que su XP reducible fue diseñado" suena a lo que es |
| CI-52 | **Campo `revision` en el payload del evento.** Revisión 1 es la corrección original; de 2 en adelante son recálculos. **Regla del 03: aceptar si la revisión es mayor a la guardada, ignorar si es igual o menor** | Resuelve el choque con CI-26 (un evento por entrega) y **tapa un agujero que habíamos dejado abierto: la apelación de CI-35 también produce un segundo evento** —el profesor sobrescribe la nota tras revisar— y no habíamos definido cómo viajaba. Un solo campo cubre los dos casos. **No contradice CI-25:** ahí `entregaId` es la clave de idempotencia del despacho **que nos entra**; `revision` ordena los eventos **que salen** |
| CI-53 | **La estampa de versión por ítem es lo autoritativo, no `contenidoRef.version`** | Si el alumno abrió en la v3 y el profesor editó antes del envío, el `contenidoRef.version` cacheado del 03 puede decir 4. No importa: corregimos por la estampa de cada ítem. Se deja escrito para que nadie intente "arreglar" esa diferencia. **Corolario:** no existe carrera entre el profesor editando y nosotros corrigiendo — CI-12 ya nos había inmunizado sin que nos diéramos cuenta |
| CI-54 | **No se edita contenido de un curso archivado** (RF-CUR-09 lo deja en modo lectura). Nos enteramos del estado del curso por `cursos.ciclo-vida` | **Sale gratis:** ya vamos a estar suscritos a ese tópico para el módulo de encuestas, que necesita el cierre de curso para disparar los instrumentos. Misma suscripción, dos usos, cero integración nueva |
| CI-19 | **La lectura del contenido no tiene estado.** No guardamos nada al servirla. Lo que sí hacemos, desde 2026-09-10, es **barajar las preguntas por alumno** | Revisado. La redacción original decía que barajar obligaba a persistir el orden **por alumno y por intento** —porque si no, el alumno recarga y ve otra cosa— y que por eso la lectura dejaba de ser pura. **Eso vale para un barajado al azar y no para uno derivado.** La permutación sale de `SHA-256(contenidoId + alumnoId)`: es la misma cada vez que se calcula, así que recargar da lo mismo, reconstruirla para el desglose da lo mismo, y no hay nada que guardar. El principio de CI-19 sobrevive intacto; lo que cambió es que la respuesta ahora depende de **quién** pregunta, y eso ya lo sabíamos porque el vale trae el `alumnoId`. La semilla **no** incluye el intento: el vale no lo lleva y meterlo cambiaría el contrato con el 03 por una ganancia discutible. **Nota de alcance:** el PRD (verificado 2026-09-08) no pide barajado — es una decisión de producto del Grupo 04, tomada a sabiendas. El cronómetro sigue afuera, y ahí el argumento no cambió: registrar "abrió a las 14:32" es del **03**, porque la entrega y sus estados son de ellos. Lo fija `BarajadoPorAlumnoIT` |
| CI-55 | **Opción múltiple admite puntaje parcial por opción.** Cada opción lleva un porcentaje del peso del ítem; los negativos descuentan. **Revierte el todo-o-nada** que este mismo documento daba por cerrado | *(Reversión del 2026-09-11.)* El argumento que sostenía el todo-o-nada estaba escrito en `CorrectorAutomatico`: repartir puntaje adentro de un ítem era *"inventar una regla académica que nadie pidió"*. **Ese argumento se cayó el día que vimos Moodle**: la cátedra lo usa, pondera cada opción por separado desde siempre, y valida que las positivas sumen 100% — no es una regla que inventamos, es la que el profesor ya tiene en la mano. **Por qué los negativos:** sin ellos, marcar todas las opciones garantiza el 100% y el ítem deja de evaluar. **Compatibilidad, que es la parte que importa:** el campo `pesos` es opcional, y sin él se corrige exactamente como antes. Los criterios ya guardados no tienen `pesos`, así que **ninguna nota ya emitida cambia de valor** — CI-12 y CI-44 quedan intactos sin migrar un solo registro. Lo fija `PuntajeParcialPorOpcionTest`, que prueba el caso viejo junto al nuevo. **Alcance:** solo opción múltiple. En *ordenar*, un elemento fuera de lugar corre a todos los demás y "cuántos acertó" no significa nada; en *emparejar* sería defendible, pero nadie lo pidió |
| CI-56 | **Rechazada la navegación secuencial** (una pregunta por página, sin volver atrás), que Moodle ofrece en *Esquema* | Obliga a guardar por dónde va cada alumno, y eso rompe CI-19 de frente: la lectura dejaría de no tener estado. Y el estado no sería gratis — habría que persistirlo por alumno **y por intento**, que es exactamente el costo que el barajado derivado evitó. **Lo que se pierde es poco:** en Moodle la secuencial existe para desalentar la copia en exámenes presenciales, y la copia acá se ataca por otro lado (barajado por alumno, CI-19). **Lo que se ganaría de romperla es menor que lo que se pierde**, así que no se rompe |
| CI-57 | **Rechazada la pregunta aleatoria del banco** (el cuestionario toma N ítems al azar de una categoría) | Es la más tentadora del lote y la más cara. CI-13 ya acepta que dos alumnos del mismo desafío vean **versiones** distintas del mismo enunciado; esto es otra cosa: verían **preguntas distintas**, de dificultad distinta, y sus notas dejarían de ser comparables dentro de la cohorte. Es el mismo argumento con el que CI-34 rechazó el pool de modelos, y ahí ya lo dimos por bueno: el ranking se calcula sobre la cohorte, así que la comparabilidad es parte de la nota. Rompe además la analítica de dificultad por ítem, que está en nuestra columna del reparto. **Si alguna vez entra**, tiene que entrar con las categorías del banco ya hechas y una regla explícita de equivalencia entre ítems — no como un `ORDER BY random()` |
| CI-58 | **La retroalimentación del ítem vive en una columna propia y se sirve RECORTADA: solo el texto general y el de las opciones que ese alumno marcó** | *(2026-09-11, del contraste contra Moodle — §5c.)* **Dos decisiones, no una.** La primera es dónde vive: columna `devolucion` aparte y no un campo más adentro de `criterio`, aunque el criterio ya tenga la garantía de no viajar al alumno. Son cosas distintas — el criterio dice **cómo se puntúa** y lo consume el corrector; la devolución dice **qué se le explica** y no la consume nadie más que la pantalla de resultado. Se nota en ABIERTA, donde el criterio es la rúbrica que guía a quien corrige: meter ahí el texto que lee el alumno sería juntar el instructivo del corrector con la devolución del corregido. La segunda es cuánta se sirve: **nunca la lista completa.** La devolución de una opción dice si esa opción estaba bien, así que mandarlas todas es servir la clave con otras palabras — misma regla por la que el desglose no lleva criterio (con reintentos ilimitados, RF-REC-04, regalar la respuesta convierte el reintento en copiar). Y tampoco sale mientras el ítem esté pendiente: explicarle al alumno por qué su respuesta está bien antes de que un humano la haya leído no significa nada. El recorte vive en el dominio (`Devolucion.paraLoMarcado`) y no en la pantalla, para que no dependa de que el front se acuerde de filtrar. Lo fija `DevolucionRecortadaTest`. **Pendiente relacionado:** Moodle controla esto fino con su matriz de *opciones de revisión* (qué ve el alumno, y en cuál de cuatro momentos). No la implementamos; si alguna vez entra, este recorte es su caso por defecto |
| CI-59 | **Los ítems tienen estado `BORRADOR \| LISTO`, y un borrador no se puede componer. El estado vive en `item`, no en `item_version`. Un ítem ya compuesto no puede volver a borrador** | *(2026-09-11, del contraste contra Moodle — §5c.)* Tapa un agujero que teníamos sin saberlo: **hoy un ítem a medio cargar entra a un cuestionario igual que uno terminado**, y el alumno se lo come. **Por qué en `item` y no en la versión:** una versión es inmutable por trigger (D-04), así que con el estado ahí, pasar de borrador a listo obligaría a publicar una versión nueva — y no lo es: la pregunta no cambió, cambió la decisión del profesor sobre si ya se puede usar. Versionar eso ensucia el historial que D-04 existe para mantener legible. **Por qué no se puede volver a borrador una vez compuesto:** con referencia flotante (CI-13) el cuestionario sirve siempre la última versión, así que ese borrador le llegaría al alumno igual — el estado sería invisible justo en el caso para el que existe. Rechazarlo es más honesto que fingir que lo detuvo. **Compatibilidad:** `DEFAULT 'LISTO'` en la migración y en el DTO, así que todo lo que ya existe y todo el que no mande el campo sigue funcionando igual. Lo fija `BorradorYVistaPreviaIT` |
| CI-60 | **La vista previa del ítem devuelve la MISMA clase que se le sirve al alumno** (`VistaAlumnoResponse.ItemParaAlumno`), no una maqueta propia | Es lo único que la hace valer como prueba y no como adorno. Si fuera un DTO aparte armado "para mostrar", podría estar limpia mientras el endpoint real filtra —o al revés— y el profesor vería una pantalla que no corresponde a lo que el alumno recibe. Compartiendo la clase, **no puede haber una vista previa limpia sobre un endpoint que no lo está**: es CI-17 otra vez, y otra vez resuelto por construcción y no por disciplina. Efecto lateral que conviene en la demo: abrir la vista previa de un ítem de respuesta abierta y mostrar que la rúbrica no está en el JSON es la forma más corta de explicar CI-17 sin hablar de arquitectura |

---

## 2. El contrato de composición

Lo que el front le manda al Tema 03 al crear el desafío, además de sus propios campos
(título, fechas, dificultad, reintentos, obligatoriedad):

```json
{
  "unidadId": "u02",
  "contenidoRef": {
    "tipo":        "TEORICO",
    "contenidoId": "9f2c...",
    "version":     1,
    "resumen":     "10 preguntas · 100 puntos",
    "correccion":  "DIFERIDA"
  }
}
```

> **Ojo con el nombre — dos formas distintas, dos nombres distintos** *(aclarado en la
> revisión del 2026-09-08)*. Acá arriba va el **`contenidoRef` de composición, con cinco
> campos**. En el despacho del Paso 4 viaja el **`contenidoBinding`, con dos**:
> `{ contenidoId, version }`. Y tiene que ser así: `tipo` se consume en el ruteo del 03 y
> ya no necesita viajar, y `resumen` y `correccion` los produjimos nosotros — sería absurdo
> que nos vuelvan. Usar el mismo nombre para las dos formas garantiza que alguien lo
> implemente mal.
>
> **La lista de documentos del material NO está acá** (CI-09): se queda en nuestro registro
> de contenido. Al 03 le va `unidadId` y nada más, porque lo que él necesita es ubicar el
> desafío en el roadmap, no saber de qué PDF salieron las preguntas.

| Campo | Dueño | Qué hace el 03 con él |
|---|---|---|
| `tipo` | **Tema 03** — el enum es de ellos, extensible | Lo interpreta. Es lo único de la ficha que interpreta |
| `contenidoId` | Tema 04 / Tema 05 | Lo guarda. No lo parsea, no lo compone, no le saca información |
| `version` | Tema 04 / Tema 05 | Lo guarda. Habilita que editar el contenido no toque lo ya respondido |
| `resumen` | Tema 04 / Tema 05 | **Lo pinta en pantalla, no lo interpreta.** Existe para que nadie termine pidiendo `cantidadDeItems` y `puntajeTotal`, que sí serían conocimiento de contenido. El 05 mandaría `"3 casos de prueba · Python"` |
| `correccion` | Tema 04 / Tema 05 — **calculado**, no elegido | Decide si muestra el resultado al instante o un estado `EN_CORRECCION`. Nosotros lo derivamos: si todos los ítems se corrigen solos → `INMEDIATA`; si hay al menos uno que espera al profesor o a la IA → `DIFERIDA`. El 05 lo deriva de si el sandbox responde sincrónico o encolado |

### Modo de corrección, por ítem (interno nuestro)

| Tipo de ítem | Qué se le ofrece al profesor |
|---|---|
| V/F, opción múltiple, emparejar conceptos, ordenar secuencias | Nada. Es automático y no se muestra la opción |
| Respuesta abierta | **Texto exacto** (carga la lista de respuestas aceptadas) · **Corrige el profesor** · **Corrige la IA** |

El modo elegido se congela junto con la versión del ítem.

---

## 2b. Deuda declarada — el vale de lectura del Tema 03

> Esto **no es un olvido, es una decisión con fecha**. Se documenta con su ventaja para que
> cuando alguien pregunte "¿por qué no lo hicimos bien de entrada?" haya una respuesta.

### El problema

Nuestro endpoint de contenido recibe un `contenidoId`, no un `desafioId`. Eso es
exactamente lo que nos mantiene libres del Tema 03 — pero deja una pregunta sin dueño:
**¿quién verifica que este alumno tiene derecho a leer este contenido ahora?**

Podemos verificar que pertenece al curso (preguntándole al Tema 02). Lo que **no** podemos
verificar es que el desafío esté abierto, porque las ventanas de apertura y cierre son del
03 y decidimos no mirarlas (CI-01). Un alumno que consiga el `contenidoId` puede leer las
preguntas antes de que el desafío abra.

### Lo que se descartó, y por qué se descartó

**Verificar solo la pertenencia al curso.** Era lo único sostenible sin acordar nada con
nadie, y estuvo vigente como deuda declarada hasta la revisión del 2026-09-08.

**Lo que se perdía:** si un profesor preguntaba *"¿un alumno puede ver el examen antes de
que abra?"*, la respuesta honesta era **"sí, si se esfuerza"**. Insuficiente justamente
contra el atacante que importa: el compañero de curso es el que tiene el incentivo para
adelantarse, y la pertenencia al curso no lo filtra.

**Por qué dejó de ser deuda:** CI-21 estableció que al abrir el desafío el 03 crea el
intento y devuelve la referencia al contenido. El vale pasó a ser **el valor de retorno de
una llamada que el 03 ya hace igual**, no una pieza aparte. El único argumento en contra
—"es caro, hay que acordar firma con otro equipo"— se cayó.

### La solución adoptada (CI-18)

**El 03 emite un vale de lectura de corta vida.** Cuando el alumno abre el desafío, el 03
le entrega al front un vale firmado que dice *"este alumno puede leer el contenido X hasta
las 14:47"*. Nosotros lo validamos con la firma, sin llamar a nadie.

Las tres ventajas, en orden de importancia:

1. **Cierra el agujero de verdad.** La autorización deja de apoyarse en que el front se
   porte bien o en que el UUID sea difícil de adivinar.
2. **No nos acopla.** Es la única salida que resuelve el problema **sin** meter una llamada
   sincrónica al 03 en el camino caliente. La alternativa obvia —preguntarle al 03 "¿está
   abierto?" en cada apertura— convierte su caída en nuestra caída y duplica su tráfico.
3. **Es simétrica.** El Tema 05 necesita exactamente lo mismo para proteger sus consignas.
   No es un pedido nuestro: es una pieza de plataforma que sirve a los dos evaluadores.

**Lo único que hay que acordar con el 03:** el esquema de firma y el secreto compartido, y
que el vale viaje en la respuesta de `POST /desafios/{id}/intentos`. Es el único acuerdo
criptográfico de todo nuestro contrato, así que conviene plantearlo temprano y en conjunto
con el 05, que necesita exactamente lo mismo.

---

## 2c. Cómo defender que la entrega pase primero por el 03

> Material de defensa. La objeción esperable es *"¿para qué se mete el 03 en el medio?"*.

### Argumento 1 — validar y registrar tienen que ser un solo acto

**Este es el argumento fuerte y va primero.** El de "el navegador es el alumno" (argumento 2)
es más fácil de contar, pero también más fácil de discutir.

Validar que el alumno puede entregar y anotar que entregó **no pueden ser dos pasos
separados**, porque entre uno y otro el mundo cambia. La única forma de que sean un solo
acto es que los haga el dueño de la entrega, en su base, en una transacción.

**Alternativa A — el 04 acepta y después le avisa al 03.** Guardamos una respuesta sin
autoridad para aceptarla. Si el 03 después dice "no le quedaban intentos", ya tenemos la
respuesta guardada — y RF-NFR-01 prohíbe el borrado físico, así que queda un registro
inválido que hay que arrastrar para siempre. Peor: si nos caemos entre guardar y avisar,
**el alumno ve "entregado" y el desafío figura sin entregar.**

**Alternativa B — el 04 le pregunta al 03 antes de aceptar.** Es la que suena razonable.
Mirá el reloj:

```
23:58:41   el front le manda las respuestas al 04
23:58:42   el 04 le pregunta al 03: "¿este alumno puede entregar?"
23:58:59   el 03 responde: sí, faltan 60 segundos      ← era verdad
23:59:02   el 04 guarda la entrega                     ← ya venció
```

**El 04 guardó una entrega fuera de plazo con el visto bueno del 03, y el 03 no mintió:**
el sí era correcto cuando lo dijo. Diecisiete segundos de red, una pausa de GC o un pico en
el cierre abren esa ventana. Y **no se puede cerrar desde afuera** — por más corta que se
haga, existe. Solo desaparece si validar y registrar ocurren en la misma transacción.

**Lo que elegimos:**

```
23:58:41   el front le manda las respuestas al 03
23:58:41   el 03, en una transacción: valida ventana → valida intentos
                                      → crea la entrega → asigna el número
23:58:42   el 03 despacha al 04
```

Sin ventana entre validar y registrar, porque son la misma operación.

### El costo, dicho de frente

**Un salto más**, y **el 03 tiene que estar vivo para que alguien entregue**. Eso último
suena a acoplamiento nuevo, y no lo es: el 03 es el dueño de la fecha de cierre y del
contador de intentos, así que **no existe ningún diseño en el que un alumno entregue con el
03 caído y el resultado sea confiable**. La dependencia ya estaba; lo que hicimos fue
volverla explícita y atómica en vez de implícita y con carreras adentro.

### Por qué las dos validaciones no se pisan

Objeción relacionada: *"si validan al abrir, ¿para qué validar de nuevo al enviar?"*.
Porque entre los dos momentos pasa el tiempo:

| Al abrir | Al enviar |
|---|---|
| La ventana estaba abierta | **Se cerró.** Abrió 23:50, cierre 23:59, entrega 00:07 |
| Le quedaban 2 intentos | **Le queda 1**, porque envió desde otra pestaña |
| El desafío estaba publicado | **El profesor lo cerró** mientras contestaba |
| Abrió el desafío | **Volvió tres días después** con el mismo `entregaId` en el navegador |
| Estaba en el curso | **Lo desmatricularon** |

> **La validación al abrir es una predicción. La del envío es un hecho.**

Dicho por el lado de a quién protege cada una: **al abrir se protege al alumno** —si no
puede, se entera antes de escribir una letra—; **al enviar se protege al sistema**. Sacar
la primera significa alumnos trabajando al pedo; sacar la segunda, aceptar entregas
inválidas.

### Argumento 2 — la frase con la que se abre

> **El 03 es el que convierte "lo que el alumno dice" en "lo que la plataforma sabe".**

### El argumento

Si el navegador del alumno nos mandara las respuestas directo, también tendría que contarnos
el resto: *"soy el alumno 1177, este es mi intento número 2, y el desafío está abierto"*.

**Y el navegador es el alumno.** Cualquiera que abra la consola y cambie `"intento": 2` por
`"intento": 1` se acaba de regalar intentos infinitos. Nosotros no tenemos con qué
desmentirlo, porque los intentos no son nuestros.

### Lo que el 03 agrega al pasar

El alumno manda **un campo**; nosotros recibimos **seis**. Los cinco de diferencia son la
contribución del 03, y ninguno se lo preguntó al navegador:

| Lo que agrega | Qué pasaría si no estuviera |
|---|---|
| `entregaId` | Esta entrega no tendría identidad. Doble clic del alumno = corregimos dos veces; y el 03 no sabría a qué entrega corresponde la nota que le mandamos |
| `intento` | El alumno se autodeclara el número. Adiós al límite de reintentos y a la vida que se descuenta al agotarlos (RF-DES-07) |
| `contenidoRef` con la versión | El alumno podría declarar que contestó una versión vieja del cuestionario, la que le conviene |
| `alumnoId` y `cursoCohorteId` | Tendríamos que reconstruir a qué curso pertenece por nuestra cuenta, duplicando lo que el 02 y el 03 ya saben |

Y aparte de los campos, agrega algo que no se ve en el JSON: **el permiso**. Antes de armar
ese mensaje, el 03 chequeó ventana e intentos. Si algo falla, el mensaje nunca se arma.

### Argumento 3 — el 03 se entera de lo suyo por sí mismo

La entrega es su entidad. Si nos llegara a nosotros primero, el 03 se enteraría de que
existe una entrega suya porque se lo contamos nosotros. Está al revés.

Y si el front lo partiera en dos —"primero preguntale al 03 si puede, después mandale al
04"—, entre esas dos llamadas se puede cortar la luz: quedan **intentos quemados sin
respuestas guardadas**, que con una vida en juego (RF-DES-07) es un perjuicio académico
concreto.

### El cierre

**El 03 no abre `respuesta` ni una sola vez en todo el recorrido.** No es un intermediario
que estorba: agrega cinco datos que solo él tiene y no toca el que no es suyo. Y como los
cinco son iguales para un teórico y para un práctico, el mensaje que nos llega y el que le
llega al 05 son idénticos — que era todo el objetivo.

### El contraargumento que nos van a hacer

**En la Lámina 3 no hay flecha 03 → 04.** Alguien puede leerlo como "el 03 nunca llama al
04". La respuesta: el diagrama no tiene alumno ni front entre sus actores, así que no puede
estar modelando el envío; y su nota ③ ("el 03 recibe una nota ya formada y no le importa
cómo se produjo") es justamente lo que nosotros sostenemos. Conviene tenerlo anticipado y
no descubrirlo en vivo.

---

## 2d. La invalidación de un desafío a mitad de intento

Tres preguntas distintas que se confunden fácil, y cada una necesita algo diferente:

**(a) ¿Quién impide que una entrega inválida se procese?** El 03, con un `if` local contra
su propia base. **No hace falta ningún evento**: el 03 invalidó el desafío, así que ya lo
sabe. Un evento sería el 03 contándose algo a sí mismo.

> Se evaluó y descartó consumir un evento de Kafka para validar esto. Tres razones:
> Kafka no se consulta —se consumen eventos y se arma una copia local, o sea duplicar el
> estado del 03—; la consistencia eventual no sirve para una decisión de autorización (si
> el evento tarda dos segundos, aceptamos una entrega que ya no valía); y sobre todo, con
> el flujo de CI-22 **el botón "Entregar" le llega al 03**, que es el dueño del dato.
> Nota: ese aparato **sí** haría falta si la entrega nos llegara a nosotros primero. Que sea
> necesario en el otro diseño es, en sí mismo, otro argumento a favor de CI-22.

**(b) ¿Cómo se entera el alumno antes de perder veinte minutos?** Acá sí va el evento, y
hoy **no lo resuelve nadie**. El consumidor principal es el **Tema 11**, que avisa a todo
alumno con un intento abierto. Nosotros y el 05 lo consumimos solo para limpiar la cola de
corrección — nunca para validar (CI-29).

**(c) ¿Qué pasa con lo que el alumno ya escribió?** Ver H-12: nadie lo contestó, y es la
más importante de las tres.

**Red de seguridad del front:** si el envío vuelve rechazado, muestra el motivo y **no le
borra al alumno lo que escribió**. Cuesta nada, pero si no está escrito no lo hace nadie.

---

## 2e. Cómo defender que devolvemos menos de lo que teníamos escrito

> El Paso 6 fue de resta: sacamos dos campos que ya estaban en `DISENIO-G04-SPRINT1.md`.
> La objeción esperable del 03 es *"si ustedes ya saben la nota, díganme también si aprobó"*.

### Por qué no mandamos `aprobado`

**Primero: nadie definió el umbral.** Busqué en el PRD y no está. Hay 24 parámetros
globales —máximo de reintentos, vidas iniciales, umbral de similitud, muestreo de
auditoría— y ninguno es el umbral de aprobación, aunque PAR-01 pague XP "por desafío
superado". Si nosotros dijéramos `aprobado`, estaríamos **inventando una regla académica
que nadie escribió**, y el 05 inventaría la suya por su lado.

**Segundo, y es el argumento con el que se cierra la discusión:**

> Si el 04 dijera `aprobado`, la regla de aprobación quedaría escrita en dos lugares: en el
> 04 para los teóricos y en el 05 para los prácticos. **Es exactamente el problema que la
> Lámina 3 dice que hay que evitar con el XP** — "si el 04 o el 05 pudieran otorgar XP por
> su cuenta, las reglas de la economía quedarían escritas en tres lugares".

Y aprobar **es** economía: el umbral decide el XP de PAR-01 y decide si se descuenta una
vida (RF-DES-07). No es una opinión nuestra sobre la respuesta del alumno.

### Por qué no mandamos `corrector`

Acá alcanza con citarlos a ellos mismos. La nota ③ de la Lámina 3 dice: *"El 03 recibe una
nota ya formada y no le importa cómo se produjo."* No estamos proponiendo nada nuevo:
estamos tomándolos al pie de la letra.

### Por qué hace falta `SIN_CORRECCION`

El argumento se ve mejor al revés — **qué pasa si no está**. Un desafío que se invalidó a
mitad de camino le queda al 03 como una entrega esperando para siempre. Y cuando la cierre
por vencimiento, ¿qué asume? Lo más probable es que la cuente como fallada, y **fallar
descuenta una vida** (RF-DES-07). O sea: al alumno le cuesta una vida un desafío que la
plataforma anuló.

La alternativa —que el 03 lo resuelva con un timeout— les exige construir el timeout **y
además adivinar el desenlace correcto**. Un campo se los ahorra.

### Por qué la clave de partición es regla de plataforma y no acuerdo con nosotros

Dos motivos:

1. **La falla es invisible en desarrollo y aparece en la demo.** Con poco volumen todo cae
   en la misma partición y anda perfecto. Con varias particiones o un reintento, el
   resultado del intento 1 puede llegar después del intento 2 y pisar la nota buena.
2. **No es un problema nuestro, es de cualquiera que emita más de un evento sobre la misma
   entidad.** El 03 mismo emite un `DESAFIO_RESUELTO` por intento; el 08 emite movimientos
   sobre el mismo saldo; el 10 emite cambios sobre el mismo XP. Si se acuerda de a pares,
   se va a resolver bien en un lugar y mal en los otros cuatro.

Es el mismo tipo de regla que el filtrado por `eventType` de H-06: chica, aburrida, y la
diferencia entre una demo que anda y una que no.

---

## 3. Hallazgos para llevar a la sesión de integración

Ninguno es una decisión nuestra: son huecos o contradicciones de los documentos de cátedra
que nos afectan a nosotros y a otros equipos a la vez.

| # | Hallazgo | A quién |
|---|---|---|
| H-01 | **El PRD y el PDF de arquitectura se contradicen sobre los desafíos personalizados.** RF-DES-05 + PAR-02 dicen que otorgan **solo XP (10/20/30), sin monedas**; el PDF dice "sin XP ni monedas". Gana el PRD (el propio PDF aclara que es una propuesta hecha con IA y puede tener imprecisiones), pero el 10 y el 08 van a implementar el que hayan leído | Cátedra / 08 / 10 |
| H-02 | **El material de la unidad no tiene dueño asignado.** RF-NFR-08 confirma que el profesor carga materiales, pero el PDF le da el "grafo de contenidos" al Tema 10 mientras que el PRD mete el roadmap y sus secciones en la sección de Cursos (RF-CUR-01/06), que es del 02. Lo necesitamos nosotros **y lo va a necesitar el 05** para generar consignas sobre la misma unidad | 02 / 10 |
| H-03 | **Quién invoca al LLM para generar.** El Tema 12 administra el proveedor en exclusiva y el Tema 07 tiene "invocación del modelo", pero el alcance del 07 es *evaluar*, no generar. Si cada grupo trae su propia clave se rompe la regla de modelo único activo (RF-IA-25). Nos afecta a nosotros, al 03 y al 05 | 07 / 12 |
| H-04 | **Falta un tópico para el cumplimiento de encuestas.** Los cuatro acordados (`cursos.ciclo-vida`, `desafios.resultados`, `sistema.notificaciones`, `sistema.moderacion`) no lo cubren. Proponemos `encuestas.cumplimiento` | 11 |
| H-05 | **Retención de PII en el bus.** `ENCUESTA_CUMPLIDA` lleva `alumnoId` y Kafka retiene. Anonimizar nuestra base a los 5 años (RF-NFR-10) no borra lo publicado. Le pasa a todo grupo que publique datos personales | 11 / cátedra |
| H-06 | **Doble XP por filtrado.** El 10 y el 11 escuchan `desafios.resultados`. Tienen que filtrar por `eventType` y reaccionar solo a `DESAFIO_RESUELTO`; si escucharan el tópico entero, un mismo intento aprobado pagaría dos veces. Es regla de plataforma, no acuerdo bilateral | 10 / 11 |
| H-07 | **Nadie nos pasa el padrón del curso al cierre.** El marcador de cumplimiento necesita un denominador. Proponemos que el payload de `CURSO_CERRADO` traiga `alumnosIds[]`: el cierre es el único momento en que esa lista nos importa, y así evitamos una segunda llamada | 02 |
| H-08 | **PAR-18 lo aplicamos nosotros y no está en la lista.** El PDF dice que los parámetros del 12 los aplican el 03, 05, 08 y 10 — pero el umbral de 5 respuestas de RF-ENC-13 lo aplicamos nosotros. O lo leemos del 12 con un default local, o queda declarado como desvío | 12 |
| H-10 | **Falta un tópico para cambios de contenido.** `CONTENIDO_ACTUALIZADO` (CI-14) no es un resultado de desafío ni una notificación. Es el **segundo** hueco del mismo tipo que encontramos — el otro es el de cumplimiento de encuestas (H-04) — así que ya no es casualidad: al catálogo de cuatro tópicos le falta una categoría entera | 11 |
| H-11 | **Intentos abandonados.** Si abrir el desafío crea el intento (CI-21), hay que decidir qué pasa cuando el alumno abre y nunca envía: ¿se quema el intento, se libera al cerrar la ventana, se vence solo? No nos afecta —solo recibimos lo que llega— pero define cuántas veces puede intentar un alumno, y probablemente no lo pensaron | 03 |
| H-12 | **Qué pasa con lo ya escrito cuando se invalida un desafío.** El profesor cierra un desafío con treinta alumnos contestando: ¿pierden todo? No está en ningún documento. **Nuestra postura propuesta (CI-30):** la invalidación no se aplica retroactivamente a los intentos ya iniciados; hay ventana de gracia para entregar, y el que no arrancó no puede arrancar | 03 / cátedra |
| H-13 | **No existe apelación de la corrección académica.** RF-IA-18 le da al alumno derecho a pedir revisión humana de su **score de uso de IA**, pero no hay nada equivalente para la corrección de una respuesta teórica — y ahí el argumento es más fuerte, porque el score de IA es un bonus y la corrección es la nota. **Lo declaramos nosotros (CI-35)**, pero conviene que la cátedra sepa que el PRD no lo pide | Cátedra |
| H-14 | **Un curso puede archivarse con respuestas abiertas sin corregir.** RF-IA-34 bloquea el archivado solo mientras existan **scores de uso de IA** pendientes, y explica el motivo: *"un score diferido que se aplica después del cierre modifica el XP con el ranking ya sellado"*. Ese motivo aplica idéntico a una corrección nuestra pendiente, pero el texto no la cubre. **Lo cerramos con CI-36**, pero el 02 tiene que consultarlo | 02 / cátedra |
| H-15 | **¿PAR-10 se comparte?** Está definido para el muestreo del scoring de uso de IA. Si lo reusamos para el muestreo de nuestra corrección (CI-33), ¿es el mismo parámetro o necesitamos uno propio? No nos bloquea, pero conviene preguntarlo antes de hardcodear un 10% | 12 |
| H-16 | **Nadie define el umbral de aprobación de un desafío.** Hay 24 parámetros globales y ninguno es ese, aunque PAR-01 pague XP "por desafío superado" y RF-DES-07 descuente una vida al fallar tras agotar los reintentos. Sin umbral definido, el 04 y el 05 inventarían cada uno el suyo. **Afecta al 03, 05, 08 y 10 a la vez.** Nuestra postura (CI-39): lo define el 03 por desafío, partiendo de un parámetro global que hay que crear | Cátedra / 03 / 12 |
| H-17 | **"La mejor nota" más reintentos ilimitados es una máquina de farmear.** La regla razonable —que cuente el mejor intento— es segura mientras haya tope (PAR-13, máximo 3), pero hay **dos casos donde no lo hay**: los desafíos personalizados por LLM (RF-DES-07: *"pueden reintentarse libremente sin límite"*) y los de recuperación de vida (RF-REC-04/06). Ahí el alumno reintenta hasta sacar 100. El daño está acotado porque esos pagan 10/20/30 XP contra 100/250/500, pero **la regla no debería depender de que el monto sea chico** — es exactamente lo que el PRD dice querer evitar cuando explica por qué PAR-02 es tan bajo. Propuesta: "la mejor" con tope, "la última" sin tope | 03 / cátedra |
| H-18 | **El circuito de la apelación no cierra.** CI-35 nos da la apelación y CI-52 la hace viajar como revisión 2, pero CI-37 dice que nosotros no le avisamos al alumno. Entonces el alumno apela, el profesor le da la razón, y el alumno se entera **solo si el 03 vuelve a emitir `DESAFIO_RESUELTO` sobre la revisión nueva**. Hay que confirmarlo con ellos: sin eso, la apelación se resuelve y nadie la comunica | 03 |
| H-09 | **La frase del reparto sobre bloquear al 03 y al 05.** El PDF dice que "03 y 05 no muestran nota hasta que el 04 confirme"; nuestra lectura del PRD es que el único gate es RF-ENC-11 al cierre de curso. Define si somos un servicio bloqueante para dos equipos. **Es el hallazgo de mayor impacto del mapa** | Cátedra |

### Evidencia nueva a favor nuestro

**RF-CUR-08b enumera las condiciones bloqueantes del archivado de un curso:** estado
académico final confirmado para todos los alumnos (RF-RNK-10) y sin scores de IA
diferidos pendientes (RF-IA-34). **Las encuestas no están en esa lista.**

Eso convierte nuestra postura sobre el deadlock de cierre (pregunta 17b de
`PREGUNTAS-INTEGRACION-G04.md`) de interpretación defendible en lectura literal del PRD:
el curso archiva con encuestas pendientes, y el gate de RF-ENC-11 es sobre el alumno
viendo su resultado, no sobre el profesor archivando.

---

## 4. El recorrido del desafío teórico

Los momentos donde el 03 y nosotros nos tocamos. Se recorren de a uno.

| Paso | Estado |
|---|---|
| 0 · Autoría — el profesor arma el banco | ✅ Cerrado (CI-02). No nos tocamos |
| 1 · Composición — el profesor arma un desafío | ✅ Cerrado (CI-03 a CI-11) |
| 2 · Publicación y congelamiento de versión | ✅ Cerrado (CI-12 a CI-15). No nos tocamos: no hay congelamiento, y el 03 no nos avisa que publicó |
| 3 · El alumno abre el desafío — quién le sirve el enunciado | ✅ Cerrado (CI-16 a CI-19), con la deuda declarada de §2b |
| 4 · El alumno envía — el paso crítico | ✅ Cerrado (CI-20 a CI-30), con la defensa en §2c y la invalidación en §2d. **Objetivo cumplido: el mensaje que recibimos y el que recibe el 05 son idénticos salvo el campo opaco `respuesta`** |
| 5 · Evaluación — inmediata o diferida | ✅ Cerrado (CI-31 a CI-37). Destapó dos agujeros del PRD: H-13 (no hay apelación de la corrección) y H-14 (el curso se puede archivar con abiertas sin corregir) |
| 6 · Resultado — qué vuelve y por qué canal | ✅ Cerrado (CI-38 a CI-43), con la defensa en §2e. **Fue un paso de resta:** salieron `corrector` y `aprobado`, entró `estado: FINAL \| SIN_CORRECCION`. Destapó H-16 |
| 7 · Reintento — quién cuenta y qué se repite | ✅ Cerrado (CI-44 a CI-48). Separó **guardar** (todas, siempre, es nuestro) de **contar** (cuál vale, es del 03). Destapó H-17 y produjo la restricción CI-47 |
| 8 · Edición del contenido ya respondido | ✅ Cerrado (CI-49 a CI-54). La pasada de control destapó un caso no cubierto —el error en la clave de corrección— y el campo `revision` que salió de ahí terminó tapando también la apelación de CI-35, que había quedado a medio especificar |

**El recorrido del desafío teórico está completo.** De los nueve momentos, en **cinco no
existe ningún mensaje** entre el 03 y nosotros —autoría, composición, publicación,
evaluación y reintento— y en los otros el mensaje es idéntico al que intercambian con el
Tema 05 salvo un campo opaco.

*(Corregido el 2026-09-08: esta línea decía "cuatro (0, 2, 3 y 8)". Estaba mal en dos: el
momento 3 pasó a tener el vale de lectura cuando cambió CI-18, y el 8 tiene
`CONTENIDO_ACTUALIZADO` desde CI-14.)*

### Todo lo que cruza entre el 03 y el 04, en una lista

| # | Momento | Dirección | Qué viaja |
|---|---|---|---|
| 1 | El alumno abre el desafío | front → 03 → front | `{ entregaId, intento, contenidoRef, vale }` (CI-21) |
| 2 | El alumno envía | 03 → 04 | `{ entregaId, desafioId, alumnoId, cursoCohorteId, intento, contenidoBinding, respuesta }` (CI-22) |
| 3 | Acuse | 04 → 03 | `202` con `{ evaluacionId, estado, correccion }` (CI-23) |
| 4 | Resultado | 04 → 03 | Evento `TEORICO_CORREGIDO` con `{ entregaId, desafioId, alumnoId, cursoCohorteId, intento, nota, estado, revision }` (CI-38 a CI-43, CI-52) |
| 5 | Cambió el contenido | 04 → 03 | Evento `CONTENIDO_ACTUALIZADO` con la ficha nueva (CI-14) |
| 6 | Se invalidó un desafío | 03 → 04 | Evento `DESAFIO_INVALIDADO`. **Solo para limpiar cola, nunca para validar** (CI-29) |
| 7 | Reconciliación | 03 → 04 | `GET /teoricos/evaluaciones/{entregaId}` (CI-43) |

Siete intercambios en total, de los cuales dos son endpoints de rescate que en el camino
feliz no se usan.

---

## 5. Correcciones a documentos previos

Lo que cambió respecto de lo que está escrito en los otros archivos de la carpeta:

- **`DISENIO-G04-SPRINT1.md`, supuesto 1** ("el 04 invoca al corrector él mismo por el
  gateway; el 03 no actúa de conducto") **queda intacto**. Nosotros seguimos invocando al
  Tema 07 directamente y el 03 nunca rutea nada hacia el corrector. Coincide con la nota ①
  de la Lámina 3.
- **`PREGUNTAS-INTEGRACION-G04.md`, pregunta 2** hay que reescribirla. Sus dos opciones
  —"(a) el 04 le manda las respuestas al 03 y el 03 las rutea al corrector" y "(b) el 04
  invoca al corrector y le entrega al 03 un resultado cerrado"— **dan por sentado que la
  entrega del alumno llega al 04 primero**. Nunca se preguntó si podía llegar al 03. CI-22
  es una tercera opción que no estaba en la lista: la entrega llega al 03, el 03 nos
  despacha, y de ahí en adelante funciona como la (b).
- **`DISENIO-G04-SPRINT1.md`, §4.1** — `POST /teoricos/desafios` figura con consumidor
  "Tema 03". Con CI-03 el consumidor pasa a ser **el front**, y el 03 deja de llamarlo.
- **`DISENIO-G04-SPRINT1.md`, §4.1** — la respuesta de corrección tiene
  `{ nota, aprobado, corrector, estado, detalle }`. **De esos cinco, al 03 solo le van
  `nota` y `estado`.** `aprobado` se cae (CI-39), `corrector` se cae (CI-38) y `detalle` se
  cae (CI-28); los tres se sirven al front. Y `estado` cambia de
  `FINAL | PENDIENTE` a `FINAL | SIN_CORRECCION` (CI-40), porque con un solo evento por
  entrega ya no existe un resultado pendiente viajando.
- **`DISENIO-G04-SPRINT1.md`, §4.4** — el payload de `TEORICO_CORREGIDO` figura como
  `desafioId, alumnoId, intento, nota, aprobado, estado`. Sacar `aprobado` y agregar
  `entregaId` y `cursoCohorteId`.
- Las 26 preguntas de `PREGUNTAS-INTEGRACION-G04.md` se mantienen, pero hay que darles
  vuelta la forma: de pregunta abierta a **contrato declarado más supuesto vigente**, para
  que el silencio de un grupo confirme nuestra versión en vez de bloquearnos.

---

## 5b. Revisión de consistencia — 2026-09-08

Pasada completa sobre las 54 decisiones, buscando contradicciones entre pasos. Encontró
ocho cosas; todas quedaron aplicadas. Se registra qué cambió, para que nadie reinstale sin
querer una versión anterior.

**Cambios de contrato (2):**

1. **CI-09** — `materialRefs` dejó de viajar al 03; ahora se queda en nuestro registro y al
   03 le va solo `unidadId`. Aplicada la pregunta de siempre, el 03 no hacía nada con esa
   lista: mismo caso que `corrector` y `detalle`.
2. **CI-33** — se agregó la excepción de muestreo. CI-47 tapaba la puerta de la cola
   infinita del profesor y el muestreo de auditoría la dejaba entrar por la ventana: un 10%
   de infinito sigue siendo infinito.

**Textos que habían quedado viejos (3):**

3. **CI-05** — su motivo invocaba el congelamiento, que CI-12 abolió. Reescrito sobre la
   referencia flotante. La decisión no cambió, solo el argumento.
4. **CI-26** — decía "un solo evento por entrega", que contradice a CI-52. Corregido a "una
   revisión vigente a la vez".
5. **§2** — `contenidoRef` nombraba dos formas distintas (cinco campos en composición, dos
   en el despacho). Se separó en `contenidoRef` y `contenidoBinding`.

**Cabos sueltos (3):**

6. **CI-18** — el vale de lectura estaba como deuda declarada, pero CI-21 le había sacado
   el costo y nunca lo reconsideramos. **Pasó a ser la solución adoptada.** Era una decisión
   pendiente disfrazada de decisión tomada.
7. **H-18** — el circuito de la apelación no cerraba: se resuelve y nadie se lo comunica al
   alumno. Va como pregunta al 03.
8. **CI-34** — se le acotó el alcance: el modelo único vale para la corrección, no para la
   generación, que RF-IA-26 permite con pool.

**Conclusión metodológica:** las dos pasadas de control (el Paso 8 y esta) encontraron
cosas que los pasos individuales no. Conviene repetirla al terminar el recorrido de
encuestas, antes de publicar nada a los otros grupos.

---

## 5c. Contraste contra Moodle — 2026-09-11

El profesor nos pasó veinte capturas del armado de un cuestionario en Moodle, para que
viéramos cómo funciona su parte. No es una lista de requisitos: es **el estándar contra el
que va a leer nuestro trabajo**, y por eso conviene tener dicho, función por función, por qué
la tenemos o por qué no.

La pasada completa dio cuatro grupos:

**1. Lo que ya teníamos, y no sabíamos que era un punto a favor (5).** Versionado de ítems
con "última versión" visible, barajado de respuestas, multi-respuesta, pesos por pregunta con
totalizador, escala del cuestionario. Todo esto Moodle lo tiene y nosotros también. No hay
trabajo que hacer; hay que **saber señalarlo**.

**2. Lo que tomamos (1 en esta tanda).** El puntaje parcial por opción → **CI-55**, que
revierte una decisión que este documento daba por cerrada. Es el hallazgo más valioso de las
veinte capturas, y no por la función: por el argumento. El todo-o-nada se defendía diciendo
que el parcial era *"una regla académica que nadie pidió"*, y resultó que sí la pedía el
sistema que la cátedra usa todos los días. **Una premisa falsa sostenía una decisión
correcta-por-casualidad**, y eso es exactamente lo que una revisión tiene que encontrar.

**3. Lo que rechazamos con fundamento (2).** Navegación secuencial → **CI-56**. Pregunta
aleatoria del banco → **CI-57**. Las dos son implementables y las dos rompen algo ya
decidido. Se escriben como rechazos, igual que CI-03b y CI-03c, porque **un rechazo
argumentado vale más en la defensa que la función implementada**: demuestra que el contrato
se usa para decidir y no para decorar.

**4. Lo que es de otro grupo (4 pantallas enteras).** Y esta es la parte que más conviene
mostrar, porque son cuatro ausencias que **el contrato ya explicaba antes de que las
viéramos**:

| Pantalla de Moodle | De quién es | Dónde ya estaba dicho |
|---|---|---|
| Temporalización: abrir, cerrar, límite de tiempo | Tema 03 | CI-15, CI-20; *"el cronómetro sigue afuera"* en CI-19 |
| Intentos permitidos y método de calificación (más alta / promedio / primero / último) | Tema 03 | CI-45, textual: *"cuál intento cuenta lo decide el 03"* |
| Calificación para aprobar | Tema 03 | CI-39: el umbral es economía, y la economía vive en un solo lugar |
| Restricciones de acceso (fecha, grupo, calificación, perfil) | Temas 02 y 03 | CI-01: para el 03 somos una caja opaca, y el curso es del 02 |

**Cómo se presenta esto:** no como "Moodle tiene cuatro cosas que nosotros no". Como
**cuatro pantallas que un sistema monolítico mete en el mismo formulario y una plataforma de
microservicios reparte entre tres equipos** — y el reparto estaba escrito de antemano, con
su motivo, en decisiones que no se tomaron mirando estas capturas. Que la frontera aguante un
contraste que no fue diseñada para aguantar es la mejor evidencia de que está bien puesta.

**Tomada después:** la retroalimentación por opción y general → **CI-58**, con un giro que no
estaba en Moodle: allá el alumno ve el feedback de todas las opciones, y acá solo el de las que
marcó. No es una limitación nuestra, es una consecuencia de CI-28 — si el desglose no lleva la
clave de corrección, la devolución tampoco puede llevarla disfrazada de texto.

**Tomadas después, en la misma tanda:** la vista previa del ítem → **CI-60**, y el estado
borrador/listo → **CI-59**. La segunda no salió de querer parecerse a Moodle: salió de que al
mirar su campo *"Estado de pregunta"* nos dimos cuenta de que nosotros **no teníamos ninguna
forma de frenar un ítem a medio escribir**. El contraste sirvió para encontrar un agujero
propio, que es más de lo que se le pide a un contraste.

**Pendiente de esta pasada:** las categorías del banco y la matriz de *opciones de revisión*
(qué ve el alumno, y en cuál de cuatro momentos). Las dos son nuestras y ninguna choca con
nada; la segunda es la que le pondría control fino al recorte que CI-58 hace por defecto.

---

## 6. Material de presentación

- `QUIEN-ARMA-EL-DESAFIO.html` — la propuesta de CI-03/CI-04 escrita para el Grupo 03,
  sin vocabulario nuestro. Publicado en:
  https://claude.ai/code/artifact/2aa525f9-871e-4e80-b237-b46276a3fdf9
