# Grupo 04 — Tema 04: Teóricos y Encuestas

Trabajo Integrador · Programación IV · TUP UTN FRC

---

## Por dónde empezar

| Si querés… | Abrí |
|---|---|
| Saber qué decidimos hacia afuera y por qué | `CONTRATO-INTEGRACION-G04.md` |
| Entender una decisión sin leer prosa | `laminas/` |
| Ver el alcance comprometido del Sprint 1 | `sprint-1/` |
| Preparar una reunión con otro grupo | `integracion/` |
| Consultar la letra de la cátedra | `catedra/` |

---

## `CONTRATO-INTEGRACION-G04.md` — la fuente de verdad

Vive en la raíz a propósito: es el único documento **vivo**. Contiene las decisiones
`CI-01`…`CI-54`, los hallazgos `H-01`…, y el recorrido del desafío teórico paso por paso.

Dos secciones que conviene conocer antes de leer cualquier otro archivo:

- **§5 — Correcciones a documentos previos.** Varios archivos de `sprint-1/` tienen
  supuestos que ya se revirtieron. La §5 dice cuáles, archivo por archivo y sección por
  sección. **Leerla antes de tomar algo de `sprint-1/` como vigente.**
- **§5b — Revisión de consistencia.** Qué cambió en la pasada del 2026-09-08 y por qué,
  para que nadie reinstale sin querer una versión anterior de una decisión.

**La regla que gobierna todo el contrato:** el Tema 03 nunca mira adentro del contenido de
un desafío ni de la respuesta de un alumno. Recibe una ficha de cinco campos y la guarda
sin abrirla. Ante una duda nueva de integración, la pregunta es si la respuesta obliga al
03 a entender algo de nuestro dominio; si obliga, está mal planteada.

---

## `laminas/` — las decisiones, en visual

Explicaciones de una sola decisión cada una, pensadas para defender en voz alta. Se abren
con doble click, no necesitan servidor.

| Lámina | Qué momento del ciclo cubre |
|---|---|
| `MAPA-DECISIONES-G04.html` | El panorama general |
| `QUIEN-ARMA-EL-DESAFIO.html` | Quién compone el cuestionario |
| `POR-DONDE-ENTRA-LA-ENTREGA.html` | El envío: por qué pasa primero por el Tema 03 |
| `LAS-DOS-VALIDACIONES.html` | El envío: por qué se valida al abrir **y** al enviar |

Las dos últimas se leen en ese orden — la segunda referencia a la primera.

---

## `sprint-1/` — los entregables de la cátedra

Épicas, backlog, diseño, plan técnico y Definition of Done. Cada uno en `.md` (la fuente)
y `.html` (la versión presentable).

> ⚠️ **`DISENIO-G04-SPRINT1.md` y `PLAN-TECNICO-G04-SPRINT1.md` contienen supuestos
> superados.** No se borran porque la §5 del contrato los corrige señalándolos por sección:
> si desaparece el archivo corregido, la corrección deja de tener sentido. Consultá la §5
> antes de usarlos.

---

## `integracion/` — lo que va hacia los otros grupos

- `PREGUNTAS-INTEGRACION-G04.md` — las 26 preguntas por grupo. Siguen vigentes, pero la §5
  del contrato pide **darles vuelta la forma**: de pregunta abierta a *contrato declarado +
  supuesto vigente*, para que el silencio de un grupo confirme nuestra versión en vez de
  bloquearnos. La pregunta 2 además hay que reescribirla (`CI-22` abrió una tercera opción
  que no estaba en la lista).
- `GUIA-G04.html` — versión visual de esas preguntas, del 31/08. **Su sección de decisiones
  quedó superada** por las 54 decisiones CI. Se conserva como registro de qué creíamos
  antes del contrato.

---

## `catedra/` — material fuente

Los dos PDF originales y su extracción a `.txt`. Los `.txt` no son basura: son para poder
buscar con `grep` sin abrir el PDF, y se usan seguido.

**No se editan.**

---

## `front-end/`

Material de diseño de la interfaz. Por ahora, referencias de paleta de colores.

---

## Pendientes anotados

- Corregir **CI-19**: daba por verificado que el PRD no tiene cronómetro para desafíos
  teóricos. Es cierto (el único "límite de tiempo" del PRD está en §8.3, para hackathons
  **prácticos**), pero el equipo propuso agregar uno, así que el supuesto queda con
  asterisco.
- Hallazgo nuevo para el **Tema 03**: si se agrega cronómetro, la duración es un atributo
  del desafío, no del cuestionario — así el 03 no aprende nada de nuestro dominio.
- Hallazgo nuevo para el **Tema 02**: el bloqueo de sesión única es suyo, no nuestro.
