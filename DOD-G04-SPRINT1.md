# Tema 04 — Teóricos y Encuestas
## Definition of Done — Sprint 1

**Grupo 4** · Programación IV · `ms-teoricos-encuestas`
Acompaña a `BACKLOG-G04-SPRINT1.md` y `PLAN-TECNICO-G04-SPRINT1.md`.

---

## Para qué sirve esto

Un Definition of Done es el acuerdo sobre qué significa que una historia esté terminada, para que
nadie tenga que preguntarlo y para que «terminada» quiera decir lo mismo en boca de los diez.

Este no es un DoD de manual. Está calibrado contra tres cosas concretas: **el tiempo que tenemos**,
**lo que el equipo ya sabe hacer** y **lo que la cátedra va a mirar**. Un DoD que no se cumple
es peor que no tener ninguno, porque enseña que las reglas del equipo son decorativas.

---

## El presupuesto real

| | |
|---|---|
| Personas | 10 |
| Duración del Sprint 1 | 2 semanas |
| Dedicación por persona | 3 a 4 horas semanales |
| **Capacidad total del equipo** | **≈ 70 horas** |
| Experiencia con Spring Boot, JPA, Docker, JUnit y tests contra base real | sí, hay gente que ya lo hizo |
| Flujo de trabajo | rama por historia, PR, revisión de otro integrante |
| Lo que mira la cátedra | tablero con historias cerradas, demo funcionando, repositorio y documentación |

**De ahí sale el criterio de diseño de este documento:** el equipo sabe hacer todo lo que el plan
técnico pide; lo que no tiene es tiempo. Entonces el DoD no baja la vara técnica —los tests contra
base real quedan, porque hay quien sabe escribirlos y son justo los que sostienen el anonimato—,
sino que **recorta el trabajo administrativo alrededor de cada historia**.

**El presupuesto concreto: cerrar una historia no puede costar más de 30 minutos** por encima de
escribir el código y sus tests. Todo lo que se propuso para este DoD y no entraba en ese
presupuesto quedó afuera, y está listado más abajo con su motivo.

---

## Definition of Ready — antes de tomar una historia

Con 3 horas por semana, empezar una historia que no estaba lista quema la semana entera de esa
persona. Tres condiciones, treinta segundos de verificación:

1. **Sus dependencias están mergeadas**, no «casi listas». El plan técnico dice, para cada
   historia, qué tiene que estar en verde antes de tomarla.
2. **Los criterios de aceptación se entienden sin preguntarle a nadie.** Si hay que preguntar,
   se pregunta en el grupo *antes* de tomarla, no a mitad de camino.
3. **Entra en una sesión de trabajo.** Si a ojo son más de 3 horas, se parte en dos historias
   antes de empezar. Una historia que cruza dos semanas no se termina nunca.

---

## Definition of Done — las ocho líneas

Aplica a **todas** las historias. Está ordenado por el momento en que ocurre cada cosa.

| # | Criterio | Costo |
|---|---|---|
| 1 | Los **criterios de aceptación del backlog están tildados uno por uno** en el tablero, por quien hizo la historia | 5 min |
| 2 | **Hay al menos un test automatizado por criterio de aceptación** | ya está en el código |
| 3 | **`mvn verify` pasa en limpio**, y lo corrió alguien que no es quien escribió la historia | 5 min |
| 4 | **Levanta desde cero:** `docker compose down -v && docker compose up` y la historia se puede ejercitar | 10 min |
| 5 | **Un PR aprobado por otra persona del grupo**, que corrió los puntos 3 y 4 | incluido arriba |
| 6 | **Sin literales de usuario** y **sin `TODO` huérfano**: un `TODO` lleva nombre e historia, o no va | 2 min |
| 7 | **Mergeado a la rama principal.** No existe «listo pero sin mergear» | 1 min |
| 8 | **Rama borrada y tarjeta movida** en el tablero | 1 min |

### Por qué cada uno está

**1 — Los CA tildados en el tablero.** Es lo primero que va a mirar la cátedra, y además es la
única forma de que «terminada» signifique lo mismo para los diez: la definición ya está escrita
en el backlog, historia por historia. Tildar es leer el CA y verificar que se cumple, no marcar
una casilla.

**2 — Un test por criterio de aceptación.** No pedimos «tests»: pedimos **uno por CA**, que es
verificable de un vistazo y no admite interpretación. Unitario cuando alcanza; contra base real
solo cuando el CA habla de la base, de los permisos o de un endpoint. El plan técnico ya nombra,
para cada historia, cuáles son.

Este es el punto donde un DoD genérico pondría «cobertura mínima del 80 %». No lo hacemos: la
cobertura se sube escribiendo tests de lo fácil, y lo que necesitamos cubierto es lo difícil.

**3 — `mvn verify` en la máquina de otro.** «En la mía andaba» es el problema que más horas
cuesta en un equipo de diez con poco tiempo. Que lo corra el revisor cierra eso por dos pesos.

**4 — Levanta desde cero.** Es el punto más caro del DoD y el que igual dejamos, porque **la
cátedra pide demo**. Una demo se arma con lo que levanta desde cero; si cada historia se probó
sobre una base que ya tenía datos de la anterior, la demo se rompe en vivo. El `down -v` es
literal: borra los volúmenes.

**5 — Revisión de otro.** Ya trabajan así, y en este proyecto hay tres historias donde la
revisión es lo único que atrapa el error —`HU06`, `HU29` y `HU44`—, porque el código compila,
los tests pasan y la garantía de anonimato igual se perdió.

**6 — Sin literales ni `TODO` huérfano.** `HU04` ya pone un test que atrapa los literales, así
que esto es casi gratis. El `TODO` con nombre e historia (`// TODO(Sofía, G04-HU33): …`) es la
diferencia entre deuda declarada y deuda perdida.

**7 y 8 — Mergeado y movido.** Una historia sin mergear no existe: no está en la demo, no está
en el repositorio y no se puede construir encima. Y con diez personas sobre un repo, una rama
que sobrevive una semana es un conflicto garantizado.

---

## DoD reforzado — nueve historias

Nueve historias tienen tres requisitos extra, porque son las que sostienen garantías que no se
ven fallar: el código compila y los tests pasan igual cuando la garantía ya se perdió.

**Cuáles son:** `HU06`, `HU07`, `HU08`, `HU10`, `HU18`, `HU21`, `HU22`, `HU29`, `HU44`.

| # | Criterio extra | Costo |
|---|---|---|
| 9 | **El test afirma el mecanismo, no el síntoma.** Un rechazo por permisos se verifica por su `SQLState`, no por «lanzó una excepción»; una inmutabilidad se verifica intentando mutar por JDBC, no solo por la API | ya está en el plan técnico |
| 10 | **Lo revisa alguien del otro frente.** Quien viene de teóricos mira la de encuestas, y al revés. Una mirada de afuera es lo que ve el supuesto que el autor da por obvio | 10 min |
| 11 | **Queda una línea en `docs/ANONIMATO.md` o `docs/DECISIONES.md`** diciendo qué garantiza y qué no | 5 min |

**Por qué solo estas nueve.** Aplicar esto a las 46 costaría unas 11 horas de las 70 que tenemos,
un 15 % de la capacidad del equipo en revisión cruzada. En estas nueve el costo se justifica; en
`HU03` o `HU26`, si algo está mal, se ve enseguida.

---

## Definition of Done del Sprint

Además de las historias, el sprint tiene su propio cierre. Son las tres cosas que mira la cátedra,
y ninguna se improvisa el día anterior.

1. **La demo está guionada y ensayada una vez.** Un documento de una carilla: qué se muestra, en
   qué orden, quién habla. Ensayada de verdad, contra el compose levantado desde cero.
   Se hace el anteúltimo día, no el último.
2. **El tablero está al día**, con las historias cerradas cerradas y las no tomadas en el backlog.
   Una historia a medias **no** se muestra como cerrada: eso es lo único que no se puede arreglar
   después.
3. **El README lo siguió alguien que no lo escribió**, en una máquina limpia, y funcionó.
   Ya es el CA3 de `HU03`; acá se repite porque es lo que hace creíble al repositorio.
4. **Los supuestos vigentes están actualizados** con lo que haya salido de la sesión de integración,
   o marcados como todavía sin respuesta. Un supuesto sin estado es peor que uno pendiente.
5. **Lo que no se terminó está declarado**, con una línea por historia diciendo hasta dónde se
   llegó. Se entrega igual: lo que hunde una entrega no es lo que falta, es lo que falta y no se
   dijo.

### Una decisión del día 1

**Poner a andar la integración continua el primer día**, aunque cueste media hora. Con 3 horas
semanales por persona, nadie quiere gastar 20 minutos verificando a mano el PR de otro. Con
`mvn verify` corriendo solo en cada PR, los puntos 3 y 6 del DoD se verifican sin que nadie los
mire, y el revisor puede dedicar su tiempo a lo que una máquina no ve.

Es la única inversión de este documento que se paga sola dentro del mismo sprint.

---

## Lo que deliberadamente NO está en el DoD

Un DoD se define tanto por lo que exige como por lo que decide no exigir. Esto se dejó afuera a
propósito, y no por olvido:

| Lo que no exigimos | Por qué | Cuándo sí |
|---|---|---|
| **Cobertura mínima de tests (%)** | se sube cubriendo lo fácil; lo que necesitamos cubierto es el motor de corrección y el aislamiento, y eso lo garantiza el punto 2 | Sprint 2, y solo sobre `..teoricos.dominio.correccion..` |
| **OpenAPI actualizado por historia** | son 46 actualizaciones de un documento que se genera solo. `HU41` lo hace una vez, bien | está en `HU41` |
| **Documentación por historia** | escribir un `.md` por historia cuesta más que la historia | solo en las nueve reforzadas, una línea |
| **Pruebas de rendimiento o de carga** | no hay requisito de carga en el Sprint 1 y no tenemos con qué compararlo | cuando haya un número que cumplir |
| **Accesibilidad** | somos back end; no entregamos interfaz | cuando haya front |
| **Aprobación del Product Owner por historia** | agrega un cuello de botella de una persona sobre 46 historias, con 3 horas semanales | la aprobación es del sprint completo, en la demo |
| **Traducción completa al segundo idioma** | `HU04` pide que el mecanismo funcione, no que el catálogo esté completo. Traducir es contenido, no código | cuando exista el requisito de un segundo idioma real |
| **Refactor de lo que quedó feo** | con 70 horas, refactorizar es tiempo que sale de una historia sin hacer | se anota como `TODO` con nombre, que el punto 6 obliga |

---

## Cuando una historia no llega

Va a pasar, y conviene tener la regla escrita antes de que pase, cuando todavía no duele.

- **No existe la historia «al 90 %».** Si al cierre del sprint no cumple el DoD, vuelve al backlog
  **entera** y se declara en el punto 5 del cierre. No se muestra como cerrada en el tablero.
- **El código a medias se mergea igual si no rompe nada**, detrás de un flag apagado o sin
  exponerse. Es preferible a una rama de dos semanas que después nadie puede integrar.
- **Una historia en curso por persona.** Con 3 horas semanales, tener dos abiertas significa
  terminar cero.
- **Si una historia se traba más de una semana, se avisa al grupo.** No para que alguien la
  rescate, sino para que las que dependían de ella se reordenen a tiempo.

---

## Cómo se ve esto en un PR

Para que el DoD se cumpla sin tener que recordarlo, va como plantilla del pull request. Tildar
seis casillas cuesta menos que discutir si algo estaba terminado:

```markdown
## G04-HUxx — <título de la historia>

### Criterios de aceptación
- [ ] CA1 — <texto del backlog>       · test: `NombreDelTest`
- [ ] CA2 — <texto del backlog>       · test: `NombreDelTest`
- [ ] CA3 — <texto del backlog>       · test: `NombreDelTest`

### Definition of Done
- [ ] `mvn verify` pasa
- [ ] `docker compose down -v && docker compose up` y la historia se ejercita
- [ ] sin literales de usuario, sin TODO sin dueño
- [ ] tablero actualizado

### Para el revisor
<qué mirar con atención, en una línea>
```

La línea **«para el revisor»** es la más útil de la plantilla: con poco tiempo, dirigir la
atención vale más que pedir una revisión completa.

---

## El número que no cierra

Esto no es parte del DoD, pero un DoD sobre un alcance imposible no se cumple igual, así que
conviene decirlo:

**70 horas de equipo contra 209 puntos y 46 historias.** Aun con estimaciones optimistas —una hora
y media por punto—, el backlog completo son unas 300 horas. **Entra alrededor de un quinto.**

No es un problema del backlog: fue escrito como el trabajo del tema, no como el trabajo de dos
semanas. Lo que hay que hacer es elegir qué entra en el Sprint 1 y declarar el resto, en vez de
tomar 46 historias y terminar 9 sin haber decidido cuáles.

**Un recorte posible, de 44 puntos, que da una demo mostrable:**

| Bloque | Historias | Puntos |
|---|---|---|
| Cimientos | `HU01`, `HU03` | 8 |
| Aislamiento de datos | `HU06` | 8 |
| Banco de ítems | `HU10`, `HU11` | 13 |
| Catálogo y cumplimiento | `HU24`, `HU25` | 10 |
| Almacén anónimo | `HU29` | 5 |
| | **Total** | **44** |

**Qué se puede demostrar con eso:** el servicio levanta desde cero y aparece en Eureka; un
profesor da de alta un ítem, lo edita y se ve que la versión anterior quedó intacta; y —lo más
vistoso— se muestra en vivo que PostgreSQL **rechaza** el join entre el marcador de cumplimiento
y la respuesta de encuesta, que es la garantía central del tema y no se parece a lo que va a
mostrar ningún otro grupo.

Quedan afuera del Sprint 1 el motor de corrección entero (`E-05`), los KPIs (`E-08`) y los eventos
de Kafka (`E-09`). Es mucho, y está bien que se vea: es preferible a una demo donde nada anda del
todo.

**Esto es una propuesta, no una decisión.** La decisión de alcance es del equipo, y conviene
tomarla en la primera reunión del sprint, no descubrirla en la última.
