# Alfa del desafío teórico — Grupo 4

Un *vertical slice* del módulo `teoricos`: la profesora arma un cuestionario, el alumno lo
responde, el sistema lo corrige solo y le devuelve la nota con el desglose.

No es una demo decorativa. Es el camino crítico del Sprint 1 —banco → composición → lectura →
despacho → corrección— hecho antes y en chico, para que las decisiones de
`CONTRATO-INTEGRACION-G04.md` se prueben ahora y no en la semana de la entrega.

---

## Qué levanta

| Proceso | Puerto | Qué es |
|---|---|---|
| `postgres` | 5432 | Un solo esquema, `teoricos`, con los roles `app_owner` y `app_teoricos` |
| `ms-teoricos-encuestas` | 8081 | **Nuestro microservicio.** Banco de ítems, composición, lectura, corrección |
| `stub-tema-03` | 8082 | **Stub de otros grupos.** `CursoController` (Tema 02: cursos, cohortes, roadmap) y `DesafioController` (Tema 03: desafíos, intentos, entregas) |
| `front-alfa` | 4200 | Angular standalone |

**El stub es un proceso separado a propósito.** Si viviera adentro de nuestro servicio,
cualquier problema de integración se podría "resolver" inyectando un bean del otro lado, y la
demo no probaría nada. Lo único que lo une al nuestro son los dos contratos que acordamos: el
vale firmado y el mensaje de despacho.

Adentro del stub, **cada grupo tiene su controlador**: en la plataforma real, cursos y desafíos
son dos microservicios de dos equipos distintos. Comparten proceso sólo porque es un stub.

**Qué es de quién**, que es lo que la demo tiene que dejar claro:

| Concepto | Dueño |
|---|---|
| Curso y cohorte | Tema 02 |
| Roadmap y unidades | Tema 02 (RF-CUR-01/06) |
| Desafío dentro de la unidad | Tema 03 |
| El cuestionario adentro del desafío | **nosotros** |

---

## Arrancar

```bash
cp .env.example .env
docker compose up --build
```

Verificar:

```bash
curl localhost:8081/actuator/health     # {"status":"UP"}
open http://localhost:4200
```

Dos usuarios, clave igual al usuario: **`profe`** y **`alumno`**.

**El banco viene sembrado**: al arrancar, si está vacío, se cargan 8 ítems de los cuatro tipos
con contenido real de la materia (`SembradorDeDemo`). Es andamiaje de la demo — se apaga con
`DEMO_SEMBRAR=false` y los tests lo apagan solos. Como sólo siembra sobre un banco vacío, para
volver al estado inicial hay que borrar el volumen: `docker compose down -v`.

> **Ojo con los roles de base.** `docker/postgres/00-roles.sql` corre **solo si el volumen está
> vacío**. Si lo cambiás: `docker compose down -v` y volver a levantar. Si no, el equipo pierde
> una tarde buscando por qué el permiso nuevo no aparece.

### Sin Docker, para desarrollar

```bash
docker compose up -d postgres
cd ms-teoricos-encuestas && mvn spring-boot:run    # 8081
cd stub-tema-03         && mvn spring-boot:run     # 8082
cd front-alfa           && npx ng serve            # 4200
```

---

## El recorrido de la demo

1. Entrar como **profesora** → **Cursos** → *Programación IV* → el **roadmap de unidades**.
   Esa pantalla la llenan dos grupos y ninguno es el nuestro: las unidades las sirve el Tema 02
   y los desafíos el Tema 03.
2. En una unidad, **+ Crear desafío** → elegir el tipo. Se ofrecen `Teórico` y
   `Práctico · Tema 05` **deshabilitado**: la tabla de tipo → servicio la tiene el front, no el
   Tema 03 (CI-02 y CI-16).
3. **Armar el cuestionario**: agregar ítems del banco, repartir los pesos hasta 100 →
   **Publicar**. Aparece la **ficha de cinco campos**, que es literalmente todo lo que viaja al
   Tema 03, y el único campo que él interpreta es `tipo`.
4. Volver al roadmap: el desafío quedó **dentro de esa unidad**.
5. **Salir** → entrar como **alumno** → su curso → el mismo roadmap → **Empezar**.
   Ahí el Tema 03 crea el intento y firma el vale de lectura.
6. Responder → **Entregar**. El botón va al **Tema 03**, no a nosotros.
7. Ver la nota y el desglose por pregunta.

### El cierre, si querés mostrar lo más fino del diseño

Con el alumno en la pantalla del cuestionario, en otra pestaña como profesora: **Banco** →
**Editar** una de las preguntas → **Publicar versión nueva**. Volvés a la pestaña del alumno y
entregás. Se corrige **contra la versión que él vio**, y el desglose muestra el enunciado viejo;
el siguiente alumno que entre ya ve la nueva. Eso es CI-12, CI-13 y CI-53 en vivo — y es lo que
prueba `EstampaDeVersionIT`.

Mientras tanto, en el log de `ms-teoricos-encuestas` aparece el evento que iría a Kafka:

```
[EVENTO -> desafios.resultados | key=<desafioId>:<alumnoId>] {"eventId":…,"eventType":"TEORICO_CORREGIDO",…}
```

### Tres cosas para mirar en la demo

- **El alumno ve la nota, no si aprobó.** No es un olvido: el PRD no define en ningún lado el
  umbral de aprobación (H-16), y como el umbral maneja XP y vidas, es economía. La decide el
  Tema 03 (CI-39).
- **La ficha tiene cinco campos y ni uno más.** Cada campo de más sería conocimiento de
  contenido filtrándose al Tema 03.
- **Editar un ítem no cambia lo ya corregido, pero sí lo que ve el próximo alumno.** No hay
  congelamiento: hay estampa (CI-12, CI-13). El test `EstampaDeVersionIT` es el que lo prueba.

---

## Tests

```bash
cd ms-teoricos-encuestas
mvn verify
```

52 tests: 30 de dominio puro (corrección de los cuatro tipos, validaciones de payload) y 22 de
integración contra PostgreSQL real, conectándose con los **roles reales** y no con el
superusuario — si falta un permiso, queremos que falle en el build y no en la demo.

Los que valen la pena leer:

| Test | Qué prueba |
|---|---|
| `EstampaDeVersionIT` | **El que justifica todo el modelo.** El profesor edita mientras el alumno contesta |
| `VistaAlumnoSinCriterioIT` | Que ninguna clave de corrección aparezca en el **JSON crudo** del alumno |
| `ValeInvalidoRechazadoIT` | Sin vale, con la firma cambiada, vencido, y de otro cuestionario |
| `ComposicionYDespachoIT` | Pesos, idempotencia del despacho, y que el acuse nunca lleve la nota |
| `BancoAjenoNoSeVeIT` | El banco de cada profesor es invisible para el resto |

### Si Testcontainers no encuentra Docker

En algunas máquinas —Docker Desktop en Windows sirviendo el engine por *named pipe*—
Testcontainers no logra hablar con el demonio aunque el CLI funcione. La salida es correr los
tests contra una base ya levantada:

```bash
docker compose up -d postgres
ALFA_DB_URL=jdbc:postgresql://localhost:5432/g04_test mvn verify
```

`g04_test` la crea `docker/postgres/00-roles.sql` al inicializar el volumen.

---

## Lo que la alfa NO tiene

Está afuera a propósito, y conviene decirlo antes de que alguien lo busque:

- **El módulo `encuestas` entero**, con sus tres esquemas, sus tres roles y el test de
  reconstrucción de los cinco canales (`G04-HU06`, `G04-HU08`). Es la parte más importante del
  sprint y no arrancó.
- **Kafka.** El evento se publica contra el puerto `PublicadorDeEventos` con un adaptador en
  memoria que escribe el envelope en el log. El adaptador de Kafka entra sin tocar quién publica.
- **Respuestas abiertas** y la cola de corrección del profesor (D-01), la corrección por LLM,
  la apelación (CI-35) y el recálculo (CI-50).
- **Eureka y el gateway.** Los tres procesos se hablan por URL directa.
- **i18n** (`G04-HU04`): los textos de error viven en `ClaveError`, con la clave ya separada del
  mensaje, pero todavía no salen de un `messages_*.properties`.
- **Anonimización** (D-11).

Y una regla que está declarada pero no puede rechazar nada todavía: **CI-47** —un desafío con
reintentos ilimitados no admite ítems de corrección humana— está como `TODO` en
`ComposicionService`, porque en la alfa no hay ítems de corrección humana.

---

## Dónde está cada cosa

```
alfa/
├── ms-teoricos-encuestas/          nuestro microservicio
│   └── src/main/java/…/
│       ├── comun/                  config, errores, identidad, eventos
│       └── teoricos/
│           ├── dominio/            TipoDeItem, payloads sellados, correctores
│           ├── aplicacion/         banco, composición, evaluación, vale
│           └── infraestructura/    JPA y controladores
├── stub-tema-03/                   ~200 líneas, todo en memoria
├── front-alfa/                     Angular standalone
└── docker/postgres/00-roles.sql    los roles, fuera de Flyway
```

La regla que sostiene el diseño y que hay que respetar al agregar código: `teoricos` y
`encuestas` no se importan entre sí, y cada repositorio está atado a **un solo** origen de
datos. Hoy lo sostiene la convención; a partir de `G04-HU02` lo verifica ArchUnit.
