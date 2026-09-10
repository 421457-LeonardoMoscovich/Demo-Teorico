# Tema 04 — Teóricos y Encuestas
## Plan técnico de las 46 historias — Sprint 1

**Grupo 4** · Programación IV · `ms-teoricos-encuestas`
Complemento de `BACKLOG-G04-SPRINT1.md`: el backlog dice **qué** y **por qué**; esto dice **cómo**.

---

## Cómo leer este documento

Cada historia tiene siempre las mismas cinco partes:

| Parte | Qué contiene |
|---|---|
| **Antes de empezar** | Qué historias tienen que estar en verde y qué artefactos ya deben existir. Si esto no se cumple, la historia no se toma. |
| **Archivos** | Los archivos que se crean o se tocan, con su ruta. Es la lista de lo que debería aparecer en el diff del PR. |
| **Pasos** | La secuencia de implementación. Cada paso es una unidad que compila y que se puede commitear. |
| **Tests que la cierran** | Los tests concretos que hacen verdes los CA del backlog. |
| **Listo cuando** | La condición de aceptación operativa: qué se corre y qué tiene que dar. |

Donde hay una **⚠ Trampa**, es un error que ya sabemos que se va a cometer si nadie lo advierte.

> Los nombres de clase son sugerencias fuertes, no dogma. Lo que **no** es negociable es
> el paquete donde vive cada cosa y el origen de datos que usa cada repositorio: de eso
> depende que el test de arquitectura de `G04-HU02` siga pasando.

---

## Convenciones del repositorio

### Stack fijado

| Pieza | Versión / elección |
|---|---|
| Java | 21 |
| Spring Boot | 3.3.x |
| Build | Maven, **un solo módulo** (la separación es por paquete, no por artefacto) |
| Base | PostgreSQL 16 |
| Migraciones | Flyway, cuatro configuraciones separadas |
| Bus | Apache Kafka (`spring-kafka`) |
| Tests | JUnit 5, Testcontainers para PostgreSQL y Kafka, ArchUnit para el test de arquitectura |
| Contrato | `springdoc-openapi` |

### Estructura de paquetes

```
ar.edu.utn.tup.g04.teoricosencuestas
│
├── comun/                        ← lo único que ambos módulos pueden importar
│   ├── config/                     datasources, flyway, seguridad, openapi
│   ├── error/                      ErrorResponse, RestExceptionHandler
│   ├── i18n/                       MessageSource, ClaveI18n
│   ├── eventos/                    PublicadorDeEventos (puerto), EventoSobre, adaptadores
│   └── web/                        DesktopOnlyInterceptor, resolución de identidad
│
├── teoricos/                     ← NO puede importar nada de encuestas/
│   ├── dominio/                    Item, ItemVersion, Correccion, reglas
│   ├── aplicacion/                 servicios de caso de uso
│   └── infraestructura/
│       ├── persistencia/           entidades JPA + repositorios (ds: dsTeoricos)
│       ├── web/                    controladores + DTO
│       └── cliente/                clientes hacia otros temas
│
└── encuestas/                    ← NO puede importar nada de teoricos/
    ├── catalogo/                   instrumento           (ds: dsCatalogo)
    ├── cumplimiento/               marcador              (ds: dsCumplimiento)
    ├── respuestas/                 respuesta anónima     (ds: dsRespuestas)
    └── kpi/                        agregados (lee solo de respuestas y catalogo)
```

**La regla que sostiene todo el diseño:** `teoricos` y `encuestas` no se importan entre sí, y
dentro de `encuestas`, `cumplimiento` y `respuestas` tampoco se importan entre sí. Solo `catalogo`
es legible desde ambos. Eso es exactamente lo que verifica `G04-HU02`.

### Los cinco orígenes de datos

| Bean | Rol de PostgreSQL | Esquema | Para qué |
|---|---|---|---|
| `dsMigraciones` | `app_owner` | los cuatro | **solo** DDL, en el arranque. Ningún repositorio lo usa |
| `dsTeoricos` | `app_teoricos` | `teoricos` | módulo teóricos |
| `dsCumplimiento` | `app_cumplimiento` | `cumplimiento` + `SELECT` en `catalogo` | marcadores |
| `dsCatalogo` | `app_catalogo` | `catalogo` | instrumentos |
| `dsRespuestas` | `app_respuestas` | `respuestas` + `SELECT` en `catalogo` | respuestas anónimas y KPIs |

Cada uno tiene su `EntityManagerFactory` y su `TransactionManager`, y cada
`@EnableJpaRepositories` apunta a **un solo** paquete. No hay un datasource «por defecto»:
si alguien inyecta el repositorio equivocado, no compila o falla al arrancar, no en producción.

### Convención de trabajo

- **Rama por historia:** `hu/G04-HU15-lectura-desafio-alumno`.
- **El PR referencia la historia** y su descripción lista los CA con una casilla por cada uno.
- **Un PR no mezcla módulos.** Si toca `teoricos/` y `encuestas/` a la vez, o es transversal
  (y va en `comun/`), o está mal cortado.
- **Definición de terminado, para todas:** los CA verdes, los escenarios BDD del backlog
  implementados como tests, sin literales de usuario en el código (`G04-HU04`), migraciones
  reversibles hacia adelante, y el test de arquitectura pasando.

### Orden de ataque

```
Día 1        HU01 ──┬── HU03
                    ├── HU06  ← lo más urgente después del scaffolding
                    └── HU04
Día 2-3      HU02, HU07 · HU10 · HU24
Día 4-6      HU11, HU12, HU13 · HU25, HU29 · HU37
Día 7-9      HU14, HU15, HU16 · HU30, HU31, HU32 · HU26, HU27
Día 10-13    HU18, HU19, HU20, HU21 · HU34, HU35, HU36 · HU38, HU39
Cierre       HU08, HU22, HU23, HU43, HU44, HU45, HU46, HU41, HU42
```

**Camino crítico:** `HU01 → HU06 → HU10 → HU14 → HU16 → HU18 → HU21 → HU38`.
Todo lo que no está en esa línea puede correr en paralelo desde el día 2.

**HU06 antes que nada.** Si los roles de base no quedan bien desde el arranque, cada línea de
persistencia escrita encima hay que revisarla de nuevo.

---

# E-01 · Cimientos del servicio

---

### ▸ G04-HU01 — Servicio operativo y descubrible
`5 pts · Must` · módulo: transversal

**Antes de empezar**
Nada. Es la primera historia del proyecto. Hace falta tener a mano la URL del Eureka Server de
la cátedra (o levantar el propio en `HU03`).

**Archivos**

| Archivo | Qué es |
|---|---|
| `pom.xml` | dependencias: web, data-jpa, flyway, postgresql, eureka-client, actuator, validation, security |
| `MsTeoricosEncuestasApplication.java` | `@SpringBootApplication` + `@EnableDiscoveryClient` |
| `comun/config/DatasourcesConfig.java` | los cinco `DataSource`, cada uno `@ConfigurationProperties` |
| `comun/config/MigracionesConfig.java` | cuatro beans `Flyway`, uno por esquema |
| `comun/config/JpaTeoricosConfig.java` … (uno por esquema) | EMF + TxManager + `@EnableJpaRepositories` acotado |
| `src/main/resources/application.yml` | perfiles `local`, `test`, `docker` |
| `src/main/resources/db/migration/{teoricos,cumplimiento,catalogo,respuestas}/` | carpetas vacías por ahora |

**Pasos**

1. **Generar el esqueleto** con Spring Initializr y fijar `spring.jpa.hibernate.ddl-auto: validate`.
   Nunca `update`: si Hibernate puede crear tablas, las migraciones dejan de ser la fuente de verdad.
2. **Desactivar el Flyway automático** (`spring.flyway.enabled: false`) y escribir `MigracionesConfig`
   con cuatro beans construidos a mano:
   ```java
   Flyway.configure()
         .dataSource(dsMigraciones)          // rol app_owner, el único con DDL
         .schemas("teoricos")
         .locations("classpath:db/migration/teoricos")
         .table("flyway_history")            // una historia por esquema
         .load().migrate();
   ```
   Los cuatro se ejecutan en un `@PostConstruct` ordenado: `catalogo` primero, porque los otros
   dos le piden `SELECT`.
3. **Configurar los cinco datasources.** En `application.yml`, cinco bloques bajo
   `app.datasource.*`, con usuario y contraseña por variable de entorno. Ninguno usa `postgres`.
4. **Un `JpaXxxConfig` por esquema.** Cada uno declara `entityManagerFactory` con
   `packagesToScan` limitado a **su** paquete y `@EnableJpaRepositories(basePackages = "…", entityManagerFactoryRef = …, transactionManagerRef = …)`.
   Marcar el de `teoricos` como `@Primary` únicamente para que Spring no se queje del arranque.
5. **Registro en Eureka:** `spring.application.name: ms-teoricos-encuestas`,
   `eureka.client.service-url.defaultZone` por variable, `prefer-ip-address: true`.
6. **Actuator:** exponer solo `health` e `info`. `management.endpoint.health.show-details: when-authorized`.
7. **Logging (CA3):** en `application.yml`, fijar explícitamente
   `logging.level.org.springframework.web.filter.CommonsRequestLoggingFilter: OFF` y **no** registrar
   ningún `RequestLoggingFilter`. Dejar un comentario en el archivo apuntando a `G04-HU07`,
   porque quien vuelva a activarlo va a romper el anonimato sin darse cuenta.

**Tests que la cierran**
- `ArranqueIT` — con Testcontainers PostgreSQL: la app levanta, `/actuator/health` da `UP`,
  los cuatro esquemas existen.
- `MigracionFallidaIT` — se agrega una migración inválida en un recurso de test y se afirma
  que el contexto **no** arranca (CA2).
- `LoggingSinBodyTest` — se inspecciona la configuración y se afirma que no hay ningún
  filtro de logging de payload registrado (CA3).

**Listo cuando** `mvn verify` pasa y, levantado contra el Eureka de la cátedra, el servicio
aparece en el dashboard en menos de 30 s.

**⚠ Trampa.** La tentación de arrancar con un solo datasource «y después lo partimos» hace
inviable `HU06`, `HU02` y `HU08` a la vez. Los cinco beans van desde el primer commit, aunque
tres apunten a esquemas todavía vacíos.

---

### ▸ G04-HU02 — Separación de módulos verificada por test de arquitectura
`5 pts · Must` · módulo: transversal

**Antes de empezar**
`HU01` en verde. Es la contraparte en código del aislamiento de base de `HU06`; las dos se
pueden hacer en paralelo, pero ninguna sola alcanza.

**Archivos**

| Archivo | Qué es |
|---|---|
| `pom.xml` | agregar `com.tngtech.archunit:archunit-junit5` |
| `test/…/arquitectura/ReglasDeModulosTest.java` | las reglas de import |
| `test/…/arquitectura/ReglasDePersistenciaTest.java` | repositorio ↔ origen de datos |
| `.github/workflows/ci.yml` | el job que los corre |

**Pasos**

1. **Regla de módulos (CA1).**
   ```java
   @AnalyzeClasses(packages = "ar.edu.utn.tup.g04.teoricosencuestas")
   class ReglasDeModulosTest {
       @ArchTest static final ArchRule teoricosNoConoceEncuestas =
           noClasses().that().resideInAPackage("..teoricos..")
               .should().dependOnClassesThat().resideInAPackage("..encuestas..");

       @ArchTest static final ArchRule encuestasNoConoceTeoricos = /* simétrica */;

       @ArchTest static final ArchRule cumplimientoNoConoceRespuestas =
           noClasses().that().resideInAPackage("..encuestas.cumplimiento..")
               .should().dependOnClassesThat().resideInAPackage("..encuestas.respuestas..");

       @ArchTest static final ArchRule respuestasNoConoceCumplimiento = /* simétrica */;
   }
   ```
2. **Regla de persistencia (CA2).** Recorrer las clases anotadas con `@EnableJpaRepositories`
   y afirmar que cada `basePackages` aparece en **una sola** configuración. Después, para cada
   interfaz que extienda `Repository`, afirmar que su paquete está cubierto por exactamente una.
   La forma barata y suficiente: una regla por paquete que diga *«todo repositorio bajo
   `..encuestas.respuestas..` reside en un paquete gestionado por `JpaRespuestasConfig`»*,
   verificada por convención de nombre de paquete.
3. **Regla extra que conviene agregar ahora:** ninguna clase fuera de `comun.config` puede
   depender de `javax.sql.DataSource`. Cierra el atajo de inyectar el datasource a mano.
4. **CI (CA3):** el workflow corre `mvn verify` en cada push y en cada PR. En el repositorio,
   marcar el job como *required* para poder mergear.

**Tests que la cierran** — la historia **es** los tests. La verificación es meta: agregar
deliberadamente un import cruzado en una rama descartable y comprobar que el build se pone rojo.

**Listo cuando** un import de `encuestas.respuestas` dentro de `encuestas.cumplimiento` hace
fallar `mvn verify` con un mensaje que nombra la clase infractora.

**⚠ Trampa.** ArchUnit analiza el bytecode compilado: si una regla apunta a un paquete que
todavía no tiene clases, **pasa vacía y en silencio**. Encadenar `.allowEmptyShould(false)`
para que una regla sin clases que evaluar sea un error y no un falso verde.

---

### ▸ G04-HU03 — Entorno local reproducible en un comando
`3 pts · Must` · módulo: transversal

**Antes de empezar**
`HU01` en verde y con `Dockerfile` funcionando.

**Archivos**

| Archivo | Qué es |
|---|---|
| `Dockerfile` | build multi-etapa |
| `docker-compose.yml` | postgres, eureka, gateway, kafka, kraft, el servicio |
| `.env.example` | todas las variables, con valores de ejemplo |
| `docker/postgres/00-roles.sql` | los cinco roles, para el arranque en frío del contenedor |
| `README.md` | requisitos, arranque, verificación |

**Pasos**

1. **`Dockerfile`** en dos etapas (`maven:3.9-eclipse-temurin-21` → `eclipse-temurin:21-jre`),
   con la capa de dependencias separada para que el rebuild no baje internet entera.
2. **`docker-compose.yml`** con cinco servicios:
   - `postgres:16` — monta `docker/postgres/` en `/docker-entrypoint-initdb.d`, que corre solo la
     primera vez y crea los roles antes de que arranque la app.
   - `kafka` en modo **KRaft** (sin ZooKeeper), con un `healthcheck` real sobre el puerto 9092.
   - `eureka` y `gateway` — las imágenes que provea la cátedra.
   - `ms-teoricos-encuestas` con `depends_on: {postgres: {condition: service_healthy}, kafka: …}`.
3. **`.env.example`** con las cinco credenciales de base, la URL de Eureka y el bootstrap de Kafka.
   El `.env` real va al `.gitignore` desde el primer commit.
4. **`README.md` (CA2)** con cuatro bloques: requisitos previos, `cp .env.example .env`,
   `docker compose up --build`, y **cómo verificar** — el dashboard de Eureka en `:8761` y
   `curl localhost:8080/ms-teoricos-encuestas/actuator/health` por el gateway.
5. **Prueba de humo del README (CA3):** que lo siga alguien del equipo que no escribió el compose,
   en una máquina limpia, cronómetro en mano. Lo que le falte al README aparece ahí y en ningún
   otro momento.

**Tests que la cierran** — no hay test unitario; el criterio es la prueba de humo del paso 5,
con constancia en el PR de quién la corrió y cuánto tardó.

**Listo cuando** `docker compose up` levanta los cinco contenedores y el health por gateway
responde `UP` sin ningún paso manual.

**⚠ Trampa.** Los scripts de `/docker-entrypoint-initdb.d` corren **solo si el volumen está
vacío**. Al cambiar `00-roles.sql` hay que `docker compose down -v`, y eso hay que decirlo en
el README o el equipo va a perder una tarde.

---

### ▸ G04-HU04 — Externalización de textos con claves i18n
`5 pts · Must` · módulo: transversal · **impacta a todas las historias posteriores**

**Antes de empezar**
`HU01` en verde. Conviene hacerla temprano: retrofitear claves i18n sobre veinte controladores
ya escritos cuesta el triple.

**Archivos**

| Archivo | Qué es |
|---|---|
| `comun/i18n/ClaveI18n.java` | catálogo de claves como constantes |
| `comun/error/ErrorResponse.java` | `record ErrorResponse(String clave, String campo, Map<String,Object> args)` |
| `comun/error/RestExceptionHandler.java` | `@RestControllerAdvice` |
| `comun/error/ExcepcionDeNegocio.java` | lleva clave + campo, nunca un mensaje literal |
| `resources/messages_es.properties` / `messages_en.properties` | los textos |
| `test/…/i18n/SinLiteralesTest.java` | el test del CA1 |

**Pasos**

1. **`ExcepcionDeNegocio`** con constructor `(ClaveI18n clave, String campo, Object... args)`.
   No tiene constructor que reciba `String message`: si no existe, nadie lo usa.
2. **`RestExceptionHandler`** traduce toda excepción a `ErrorResponse`, resolviendo el texto con
   `MessageSource` y el `Locale` de `Accept-Language`. Mapear también `MethodArgumentNotValidException`
   para que las validaciones de Bean Validation salgan con clave y con `campo` (CA2).
3. **Anotaciones de validación con clave:** `@NotNull(message = "{error.campo.requerido}")` —
   con llaves, para que Spring lo resuelva contra el `MessageSource` en vez de imprimir el literal.
4. **Los dos archivos de propiedades (CA3).** `messages_en.properties` puede estar incompleto:
   alcanza con que tenga las claves suficientes para que un test cambie el `Locale` y obtenga
   otro texto. Eso demuestra el mecanismo, que es lo que pide el CA.
5. **`SinLiteralesTest` (CA1).** Con ArchUnit: ninguna clase bajo `..web..` o `..dominio..` puede
   llamar a un constructor de excepción con un `String` que no sea una constante de `ClaveI18n`.
   Complementarlo con un test que recorra el `.jar` buscando cadenas con espacios y acentos en
   los paquetes de DTO. Es tosco, pero atrapa el 90 % de los descuidos.

**Tests que la cierran**
- `SinLiteralesTest` (CA1).
- `ErrorConClaveYCampoTest` — un POST inválido devuelve 400 con `clave` y `campo` poblados (CA2).
- `CambioDeIdiomaIT` — el mismo error con `Accept-Language: es` y `en` da dos textos distintos
  y **la misma** clave (CA3).

**Listo cuando** ningún texto dirigido al usuario está escrito en un `.java`.

**⚠ Trampa.** Los mensajes de las anotaciones de Bean Validation sin llaves (`message = "requerido"`)
no pasan por el `MessageSource` y el test del CA1 no los ve, porque son metadatos y no llamadas.
Agregar una regla específica para las anotaciones.

---

### ▸ G04-HU05 — Aviso explícito de que la sección requiere escritorio
`3 pts · Should` · módulo: transversal

**Antes de empezar**
`HU04` en verde, porque el aviso es una clave i18n.

**Archivos**

| Archivo | Qué es |
|---|---|
| `comun/web/RequiereEscritorio.java` | anotación de marca para el controlador o el método |
| `comun/web/DesktopOnlyInterceptor.java` | el interceptor |
| `comun/config/WebConfig.java` | lo registra |
| `resources/messages_*.properties` | clave `error.requiere.escritorio` |

**Pasos**

1. **Anotación `@RequiereEscritorio`** a nivel de método. Explícita y grepeable: se ve en el
   controlador qué endpoint está protegido, sin tener que leer una lista de rutas en otro archivo.
2. **Interceptor** en `preHandle`: si el handler tiene la anotación, el flag `desktop-only.enabled`
   está en `true` y el `User-Agent` matchea el patrón móvil, lanzar `ExcepcionDeNegocio` con
   `HttpStatus.UNPROCESSABLE_ENTITY` (422) y la clave del aviso.
   422 y no 403: no es una cuestión de permisos, es que el cliente no sirve para esta sección.
3. **Detección:** una regex configurable en `application.yml`
   (`desktop-only.patron-movil: "(?i).*(android|iphone|ipad|mobile).*"`). Configurable porque el
   User-Agent es un dato pobre y va a haber que ajustarla.
4. **Anotar los endpoints (CA2):** `GET /teoricos/desafios/{id}` y `GET /encuestas/{instrumento}`.
   Se anotan **al implementarse** en `HU15` y `HU31` — dejar la nota en esas historias.
5. **Plan de retiro (CA3):** documentar en el README que el flag se apaga cuando exista la versión
   móvil, y que el retiro implica borrar la anotación, no solo el flag.

**Tests que la cierran**
- `MovilRecibeAvisoTest` — `MockMvc` con `User-Agent` de iPhone: 422 + la clave, y `content` vacío (CA1).
- `MovilNoRecibeParcialTest` — el body de la respuesta no contiene ningún enunciado (CA2).
- `FlagApagadoTest` — con `desktop-only.enabled: false`, el mismo request pasa (CA3).

**Listo cuando** un móvil recibe el aviso y ningún contenido, y un escritorio no nota diferencia.

**⚠ Trampa.** El interceptor tiene que correr **antes** de que el controlador arme la respuesta.
Si se implementa como filtro de respuesta, el enunciado ya se serializó y se está confiando en
que nadie lo lea: eso es degradar en silencio, justo lo que D-12 prohíbe.

---
# E-02 · Aislamiento de datos para el anonimato

---

### ★ G04-HU06 — Esquemas y roles sin permisos cruzados
`8 pts · Must` · módulo: transversal · **es la historia más importante del sprint**

**Antes de empezar**
`HU01` en verde con los cinco datasources declarados, aunque tres apunten a esquemas vacíos.

**Archivos**

| Archivo | Qué es |
|---|---|
| `db/migration/teoricos/V1__esquema_y_permisos.sql` | `CREATE SCHEMA teoricos` + grants |
| `db/migration/catalogo/V1__esquema_y_permisos.sql` | ídem + `SELECT` para los otros dos roles |
| `db/migration/cumplimiento/V1__esquema_y_permisos.sql` | ídem |
| `db/migration/respuestas/V1__esquema_y_permisos.sql` | ídem |
| `docker/postgres/00-roles.sql` | `CREATE ROLE` de los cinco, para el arranque en frío |
| `test/…/aislamiento/SinFksCruzadasTest.java` | el test del CA3 |

**Pasos**

1. **Crear los roles** en `00-roles.sql` — fuera de Flyway, porque los roles son de la instancia
   y no del esquema, y porque `app_owner` tiene que existir antes de que corra la primera migración:
   ```sql
   CREATE ROLE app_owner        LOGIN PASSWORD :'owner_pwd';
   CREATE ROLE app_teoricos     LOGIN PASSWORD :'teoricos_pwd';
   CREATE ROLE app_cumplimiento LOGIN PASSWORD :'cumplimiento_pwd';
   CREATE ROLE app_catalogo     LOGIN PASSWORD :'catalogo_pwd';
   CREATE ROLE app_respuestas   LOGIN PASSWORD :'respuestas_pwd';
   REVOKE ALL ON SCHEMA public FROM PUBLIC;
   ```
   El `REVOKE` sobre `public` no es adorno: sin él, cualquier rol puede crear una tabla ahí y
   usarla de puente entre esquemas.
2. **Migración de `catalogo` primero** (los otros dos dependen de su `SELECT`):
   ```sql
   CREATE SCHEMA catalogo AUTHORIZATION app_owner;
   GRANT USAGE ON SCHEMA catalogo TO app_catalogo, app_cumplimiento, app_respuestas;
   GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA catalogo TO app_catalogo;
   GRANT SELECT                  ON ALL TABLES IN SCHEMA catalogo TO app_cumplimiento, app_respuestas;
   ALTER DEFAULT PRIVILEGES FOR ROLE app_owner IN SCHEMA catalogo
       GRANT SELECT ON TABLES TO app_cumplimiento, app_respuestas;
   ```
   El `ALTER DEFAULT PRIVILEGES` es lo que hace que las tablas creadas por migraciones futuras
   hereden los permisos. Sin eso, cada migración nueva rompe el permiso y nadie entiende por qué.
3. **Migraciones de `cumplimiento` y `respuestas`,** cada una con `USAGE` y CRUD **solo para su
   propio rol** y ni un `GRANT` hacia el otro (CA2). Lo mismo con `teoricos`.
4. **Extensión para los UUID:** `CREATE EXTENSION IF NOT EXISTS pgcrypto;` en la migración de
   `respuestas`, que es la que va a necesitar `gen_random_uuid()` en `HU29`.
5. **Ningún datasource con superusuario (Extras):** verificar que `dsMigraciones` usa `app_owner`
   y no `postgres`. `app_owner` puede crear objetos en los cuatro esquemas, y nada más.
6. **Test de FKs cruzadas (CA3):** consultar el catálogo del sistema y afirmar cero filas:
   ```sql
   SELECT con.conname, ns_o.nspname AS origen, ns_d.nspname AS destino
     FROM pg_constraint con
     JOIN pg_class      t_o ON t_o.oid = con.conrelid
     JOIN pg_namespace  ns_o ON ns_o.oid = t_o.relnamespace
     JOIN pg_class      t_d ON t_d.oid = con.confrelid
     JOIN pg_namespace  ns_d ON ns_d.oid = t_d.relnamespace
    WHERE con.contype = 'f' AND ns_o.nspname <> ns_d.nspname;
   ```
   Va como test de integración con Testcontainers, después de correr las migraciones. Falla el
   build si aparece una fila.

**Tests que la cierran**
- `EsquemasCreadosIT` — los cuatro existen tras el arranque (CA1).
- `PermisosCruzadosIT` — conectando como `app_cumplimiento`, un `SELECT` sobre
  `respuestas.respuesta_encuesta` lanza `SQLException` con `SQLState 42501` (*insufficient
  privilege*). Y la simétrica (CA2).
- `CatalogoLegibleDesdeAmbosIT` — los dos roles leen `catalogo.instrumento` correctamente.
- `SinFksCruzadasTest` (CA3).

**Listo cuando** los cuatro esquemas están migrados, los tests de permisos pasan **afirmando el
rechazo**, y ningún datasource se conecta con un superusuario.

**⚠ Trampa.** Un `SELECT` sin permiso puede fallar de dos maneras muy distintas: `42501` si el rol
no tiene privilegio, o `42P01` (*relation does not exist*) si ni siquiera tiene `USAGE` sobre el
esquema. **Las dos sirven, pero hay que afirmar el SQLState concreto.** Si el test solo espera
«una excepción», un día la tabla deja de existir por otro motivo y el test sigue verde mintiendo.

---

### ▸ G04-HU07 — Supresión de trazas y de body en logs
`5 pts · Must` · módulo: encuestas

**Antes de empezar**
`HU06` en verde. Se puede escribir antes de que exista la tabla de respuestas: es configuración
y test, no modelo.

**Archivos**

| Archivo | Qué es |
|---|---|
| `resources/logback-spring.xml` | configuración explícita de appenders |
| `comun/config/ObservabilidadConfig.java` | métricas sin correlación |
| `test/…/anonimato/SinBodyEnLogsTest.java` | el test del CA2 |
| `test/…/anonimato/SinTraceIdPersistidoTest.java` | el test del CA1 |

**Pasos**

1. **`logback-spring.xml` explícito.** Patrón sin `%X{traceId}` para los loggers de
   `..encuestas.respuestas..`. Definir un appender propio para ese paquete, así el día que
   alguien agregue MDC global no arrastra el trace al log de respuestas.
2. **Prohibir el logging de payload:** dejar comentado y explicado en el XML que no se registra
   `CommonsRequestLoggingFilter` ni ningún `AbstractRequestLoggingFilter`, con la razón.
3. **Métricas sin correlación (CA3):** instrumentar con Micrometer `respuestas.recibidas` (contador)
   y `respuestas.latencia` (timer), **sin ninguna etiqueta** que lleve `instrumento_id`, `curso` ni
   nada por fila. Volumen y latencia agregados, que es lo que pide el CA.
4. **Test del appender (CA2):** el más valioso de la historia. Adjuntar un `ListAppender<ILoggingEvent>`
   al logger raíz, ejecutar el `POST` de respuesta con un comentario que tenga una cadena centinela
   (`"XYZZY-CENTINELA"`), y afirmar que **ningún** evento capturado la contiene, en ningún nivel,
   incluido `DEBUG` con el logger raíz en `TRACE`.
5. **Test de columna (CA1):** consultar `information_schema.columns` de `respuestas.respuesta_encuesta`
   y afirmar que ninguna se llama `trace_id`, `request_id` ni `correlation_id`. Es un test barato
   que sobrevive a los refactors futuros y avisa si alguien agrega la columna «para debuggear».

**Tests que la cierran** — los del paso 4 y 5, más `MetricasSinEtiquetaIdentificanteTest`.

**Listo cuando** con el logger raíz en `TRACE` la cadena centinela no aparece ni una vez.

**⚠ Trampa.** El logger de Hibernate en `TRACE` imprime los parámetros de cada `INSERT`, comentario
incluido. El test del paso 4 tiene que subir el nivel de `org.hibernate.orm.jdbc.bind` para
detectarlo, y la configuración de producción tiene que fijarlo en `WARN` explícitamente.

---

### ★ G04-HU08 — Test de reconstrucción de los cinco canales
`8 pts · Must` · módulo: encuestas · **criterio de release 15b**

**Antes de empezar**
`HU06`, `HU07`, `HU29` y `HU30` en verde. Es la última historia del bloque de anonimato porque
prueba el conjunto; escribirla antes da falsos verdes por tablas vacías.

**Archivos**

| Archivo | Qué es |
|---|---|
| `test/…/anonimato/ReconstruccionIT.java` | los cinco canales, un `@Nested` por canal |
| `test/…/anonimato/SembradorDeDatos.java` | siembra determinista de marcadores y respuestas |
| `test/resources/application-anonimato.yml` | los cinco datasources apuntando a los roles reales |

**Pasos**

1. **Sembrador:** N alumnos responden un mismo instrumento, en un orden conocido por el test, a
   través del `POST` real. Nada de insertar filas a mano: si el test escribe directo en la base,
   no está probando el sistema.
2. **Canal 1 — join por clave.** Con `dsCumplimiento`, ejecutar
   `SELECT ... FROM cumplimiento.marcador m JOIN respuestas.respuesta_encuesta r ON ...`
   y afirmar `SQLState 42501` o `42P01` (CA2). Repetir con `dsRespuestas` en el sentido inverso.
   **Dos casos, uno por rol.**
3. **Canal 2 — correlación temporal.** Consultar `information_schema.columns` y afirmar que
   `respuesta_encuesta` no tiene ninguna columna de tipo `timestamp`. Es la versión estructural
   del canal: si no existe el dato, no hay correlación posible.
4. **Canal 3 — orden de inserción.** Sembrar 50 respuestas con marcadores en orden conocido,
   leer las respuestas con `SELECT * FROM respuestas.respuesta_encuesta` (sin `ORDER BY`, o sea
   orden físico) y calcular la **correlación de Spearman** entre ese orden y el orden de los
   marcadores. Afirmar que el coeficiente está por debajo de un umbral (`|ρ| < 0.5`).
   Comparar par por par sería frágil; el coeficiente mide lo que realmente importa.
5. **Canal 4 — secuencias y PK.** Tomar los `id` de las respuestas y afirmar que
   (a) el bit de versión del UUID es `4`, y (b) ordenarlos lexicográficamente no reproduce el
   orden de inserción. Esto es lo que atrapa a alguien que cambie a UUIDv7 «porque indexa mejor».
6. **Canal 5 — logs y trazas.** Reutilizar el `ListAppender` de `HU07` y afirmar que en el log
   generado durante la siembra no aparece ningún `alumno_id` junto a ningún contenido de respuesta.
7. **Caso adicional — el residual (Extras).** Un `@Test` llamado
   `residualDeKAnonimatoCursoConUnSoloRespondente` que **documenta el ataque y afirma que funciona**:
   con un solo respondente, curso + período identifica. El test no falla: verifica que el endpoint
   de KPI de `HU36` devuelve solo el conteo, que es la mitigación. Enlazarlo por nombre a `HU36`.
8. **CI (CA3):** correr en el mismo `mvn verify` que el resto. No en un perfil aparte que nadie ejecute.

**Tests que la cierran** — la historia es el test. Su valor está en que **afirma el fracaso**,
no el éxito.

**Listo cuando** los cinco canales tienen su caso, cada join se ejecuta con cada rol, y la falla
por permisos se distingue de un resultado vacío por el SQLState.

**⚠ Trampa.** Es fácil escribir un test que pasa porque la consulta devolvió cero filas —tablas
vacías, o un `WHERE` que no matchea— y creer que el aislamiento funciona. **Cada caso tiene que
afirmar la excepción y su SQLState**, y el sembrador tiene que garantizar que, con permisos, el
join *sí* devolvería filas. Vale la pena un caso de control que corra el mismo join con `app_owner`
y afirme que **sí** trae datos: eso demuestra que lo que bloquea es el permiso y no la falta de datos.

---

### ▸ G04-HU09 — Declaración documentada del residual de k-anonimato
`2 pts · Should` · módulo: encuestas

**Antes de empezar**
`HU08` en verde, con el caso del residual ya escrito. Esta historia lo pone en prosa y lo enlaza.

**Archivos**

| Archivo | Qué es |
|---|---|
| `db/migration/respuestas/V1__esquema_y_permisos.sql` | `COMMENT ON` de la tabla y las columnas |
| `docs/ANONIMATO.md` | la declaración del residual |
| `README.md` | enlace a `docs/ANONIMATO.md` |

**Pasos**

1. **Comentario en la base (CA1):** que la declaración viva junto al esquema y no solo en un `.md`.
   ```sql
   COMMENT ON TABLE respuestas.respuesta_encuesta IS
     'Sin autor y sin marca temporal por diseño. Residual conocido: curso + periodo con un
      unico respondente identifica. Mitigacion: umbral PAR-18 (G04-HU36). Ver docs/ANONIMATO.md';
   ```
2. **`docs/ANONIMATO.md`** con cuatro secciones: los cinco canales y cómo se cierra cada uno,
   el residual de k-anonimato, el residual de retención de Kafka (`ENCUESTA_CUMPLIDA` lleva
   `alumnoId` y el bus retiene), y la exención de las respuestas en la anonimización (D-11).
3. **Redacción del CA3, literal:** *«el umbral de cinco respuestas reduce la probabilidad de
   identificación; no la elimina. Con seis respondentes en un curso de seis, la publicación del
   agregado sigue acotando el valor individual de cada uno.»* Decirlo así, y no «queda mitigado».
4. **Enlace bidireccional:** el documento nombra el test `residualDeKAnonimatoCursoConUnSoloRespondente`
   y el test lleva un comentario que apunta al documento (CA2).

**Tests que la cierran** — ninguno propio; se apoya en el caso de `HU08`.

**Listo cuando** el residual está en el comentario de la tabla, en `docs/ANONIMATO.md` y en un
test con nombre propio, y el texto dice «umbral», no «eliminado».

---

# E-03 · Banco de ítems versionado

---

### ★ G04-HU10 — Ítems con identidad estable y contenido versionado
`8 pts · Must` · módulo: teoricos · **D-04**

**Antes de empezar**
`HU06` en verde (el esquema `teoricos` existe y `app_teoricos` tiene permisos).

**Archivos**

| Archivo | Qué es |
|---|---|
| `db/migration/teoricos/V2__item.sql` | `item` e `item_version` |
| `teoricos/dominio/TipoDeItem.java` | el enum de los siete tipos |
| `teoricos/infraestructura/persistencia/ItemEntity.java` | identidad estable |
| `teoricos/infraestructura/persistencia/ItemVersionEntity.java` | contenido inmutable |
| `teoricos/infraestructura/persistencia/ItemRepository.java` / `ItemVersionRepository.java` | |
| `teoricos/aplicacion/BancoDeItemsService.java` | `crear`, `publicarVersion` |

**Pasos**

1. **Migración `V2__item.sql`:**
   ```sql
   CREATE TABLE teoricos.item (
     id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
     profesor_id    uuid NOT NULL,
     tipo           varchar(24) NOT NULL,
     version_actual int  NOT NULL DEFAULT 0,
     baja_logica    timestamptz NULL
   );
   CREATE TABLE teoricos.item_version (
     id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
     item_id    uuid NOT NULL REFERENCES teoricos.item(id),
     version    int  NOT NULL,
     enunciado  text NOT NULL,
     payload    jsonb NOT NULL,
     criterio   jsonb NULL,
     creada_en  timestamptz NOT NULL DEFAULT now(),
     UNIQUE (item_id, version)
   );
   ```
   El `UNIQUE (item_id, version)` es lo que hace que dos publicaciones concurrentes no puedan
   generar dos «versión 3».
2. **`ItemVersionEntity` sin setters.** Todos los campos `final`-por-convención, constructor
   completo, y **ningún** método `setEnunciado` ni `setPayload`. La inmutabilidad se defiende
   primero por la forma de la clase; el CA2 es la segunda línea.
3. **`publicarVersion(itemId, contenido)`** en el servicio:
   - carga el `item` con `PESSIMISTIC_WRITE` (o confía en el `UNIQUE` y reintenta),
   - calcula `version = version_actual + 1`,
   - inserta la `item_version` nueva,
   - actualiza `item.version_actual` (CA3),
   - **no toca ninguna versión anterior**.
4. **Rechazo de mutación (CA2):** un método `editarVersion` no existe. Si alguien intenta
   `PUT /teoricos/items/{id}/versiones/{v}`, el controlador devuelve 405. Y por si acaso, un
   trigger de base:
   ```sql
   CREATE FUNCTION teoricos.f_item_version_inmutable() RETURNS trigger AS $$
   BEGIN RAISE EXCEPTION 'item_version es inmutable'; END $$ LANGUAGE plpgsql;
   CREATE TRIGGER t_item_version_no_update BEFORE UPDATE ON teoricos.item_version
     FOR EACH ROW EXECUTE FUNCTION teoricos.f_item_version_inmutable();
   ```
   El trigger es lo que hace que el CA2 lo garantice el motor y no la disciplina.
5. **Tipo inmutable entre versiones (Extras):** `tipo` vive en `item`, no en `item_version`.
   El modelo lo hace imposible de cambiar, que es mejor que validarlo.

**Tests que la cierran**
- `EditarCreaVersionNuevaTest` — publicar dos veces deja dos filas; se lee la v1 y su enunciado
  es el original, carácter por carácter (CA1).
- `VersionRespondidaNoMutaIT` — con una respuesta asociada, un `UPDATE` directo por JDBC lanza
  la excepción del trigger (CA2).
- `VersionActualApuntaALaUltimaTest` (CA3).
- `TipoNoCambiaEntreVersionesTest` (Extras).

**Listo cuando** publicar tres versiones deja tres filas intactas y `version_actual` en 3.

**⚠ Trampa.** El `@OneToMany` de `ItemEntity` hacia sus versiones con `cascade = ALL` y
`orphanRemoval = true` **borra versiones anteriores** cuando alguien manipula la colección.
No mapear la colección: navegar por `ItemVersionRepository.findByItemId(...)`.

---

### ▸ G04-HU11 — ABM del banco de ítems con baja lógica
`5 pts · Must` · módulo: teoricos

**Antes de empezar**
`HU10` en verde. Idealmente `HU43` avanzada, o al menos la resolución de identidad del token.

**Archivos**

| Archivo | Qué es |
|---|---|
| `teoricos/infraestructura/web/ItemController.java` | `POST`, `GET`, `PUT`, `DELETE` |
| `teoricos/infraestructura/web/dto/` | `CrearItemRequest`, `ItemResponse`, `ItemResumenResponse` |
| `teoricos/aplicacion/BancoDeItemsService.java` | se le agregan `listar` y `darDeBaja` |
| `db/migration/teoricos/V3__item_indices.sql` | índice por `(profesor_id, tipo)` |

**Pasos**

1. **`POST /teoricos/items`** — crea `item` + `item_version` v1 en una transacción. Devuelve 201
   con `Location`.
2. **`PUT /teoricos/items/{id}`** — **no edita**: llama a `publicarVersion` de `HU10`. El verbo
   es `PUT` porque así está en el contrato del diseño, pero el efecto es crear. Documentarlo en
   el OpenAPI para que el Tema 03 no se confunda.
3. **`GET /teoricos/items?tipo=OPCION_MULTIPLE`** — filtrable (CA1). Devuelve `ItemResumenResponse`:
   id, tipo, enunciado de la versión actual y número de versión. **No devuelve `criterio`** —
   ni siquiera al profesor dueño hace falta, y así el DTO no puede filtrarlo por accidente
   en otro endpoint.
4. **Baja lógica (CA2):** `DELETE /teoricos/items/{id}` hace `UPDATE ... SET baja_logica = now()`.
   El repositorio expone **solo** métodos con `WHERE baja_logica IS NULL`. No declarar `deleteById`:
   como `JpaRepository` lo trae heredado, la forma limpia es no extender `JpaRepository` sino
   `Repository<ItemEntity, UUID>` y declarar a mano los métodos que sí se permiten.
5. **Banco propio (CA3):** todas las consultas filtran por `profesor_id` tomado del token, **nunca**
   de un parámetro. Un `GET` con el id de otro profesor no puede ni siquiera expresarse.

**Tests que la cierran**
- `AbmDeItemsIT` — el ciclo completo con filtro por tipo (CA1).
- `BajaEsLogicaIT` — tras el `DELETE`, la fila sigue en la base con `baja_logica` poblado y el
  `GET` no la lista (CA2).
- `BancoAjenoNoSeVeTest` — el profesor B no ve ningún ítem del profesor A (CA3).

**Listo cuando** ningún camino de la API ejecuta un `DELETE` físico y el banco de cada profesor
es invisible para el resto.

**⚠ Trampa.** Extender `JpaRepository` y confiar en «no llamamos a `delete`» hace fallar el test
de `HU46`, que verifica que **ningún repositorio expone** borrado físico. Resolverlo acá y no
al final del sprint.

---

### ★ G04-HU12 — Los cuatro tipos autocorregibles con su clave de corrección
`8 pts · Must` · módulo: teoricos

**Antes de empezar**
`HU10` en verde. Esta historia define el contrato de `payload` y `criterio` que después consume
el corrector de `HU18`: conviene escribirla con quien vaya a tomar `HU18`.

**Archivos**

| Archivo | Qué es |
|---|---|
| `teoricos/dominio/payload/PayloadDeItem.java` | interfaz sellada |
| `teoricos/dominio/payload/{OpcionMultiple,VerdaderoFalso,Emparejar,Ordenar}Payload.java` | uno por tipo |
| `teoricos/dominio/payload/CriterioDeCorreccion.java` | jerarquía paralela |
| `teoricos/dominio/payload/ValidadorDePayload.java` | despacho por tipo |
| `teoricos/infraestructura/persistencia/PayloadJsonConverter.java` | `AttributeConverter` jsonb ↔ objeto |

**Pasos**

1. **Interfaz sellada** para que el compilador exija tratar los siete casos:
   ```java
   public sealed interface PayloadDeItem
       permits OpcionMultiplePayload, VerdaderoFalsoPayload, EmparejarPayload,
               OrdenarPayload, AbiertaPayload, ConversacionPayload, DebatePayload { }
   ```
   Los dos diferidos existen como tipo pero su validador los rechaza (`HU13`).
2. **Las cuatro formas:**

   | Tipo | `payload` | `criterio` |
   |---|---|---|
   | Opción múltiple | `{opciones: [{id, texto}], multiple: bool}` | `{correctas: [id]}` |
   | Verdadero / falso | `{afirmacion: string}` | `{esVerdadero: bool}` |
   | Emparejar | `{izquierda: [{id,texto}], derecha: [{id,texto}]}` | `{pares: [[idIzq, idDer]]}` |
   | Ordenar | `{elementos: [{id, texto}]}` | `{secuencia: [id]}` |

3. **`ValidadorDePayload`** con un `switch` sobre el tipo sellado. Cada rama valida forma y
   coherencia entre `payload` y `criterio`, y lanza `ExcepcionDeNegocio` con la clave i18n y el
   **nombre del campo** (CA2). El campo tiene que ser navegable: `criterio.correctas[0]`, no `criterio`.
4. **Las validaciones que piden los CA, explícitas:**
   - opción múltiple sin ninguna correcta → rechazo (CA3),
   - opción múltiple con `multiple: false` y más de una correcta → rechazo,
   - una `correcta` que no existe en `opciones` → rechazo,
   - emparejar con un `idIzq` repetido en dos pares → rechazo,
   - ordenar cuya `secuencia` no sea una permutación exacta de `elementos` —repetido o faltante—
     → rechazo (Extras). La validación es un solo `equals` entre dos conjuntos ordenados por id.
5. **`AttributeConverter`** con Jackson y `@JdbcTypeCode(SqlTypes.JSON)` para mapear a `jsonb`.
   Registrar los subtipos con `@JsonSubTypes` para que la deserialización sepa qué clase construir.

**Tests que la cierran**
- `AltaDeLosCuatroTiposTest` — un caso feliz por tipo (CA1).
- `PayloadMalArmadoTest` — parametrizado: ocho casos inválidos, cada uno afirmando el `campo`
  exacto en la respuesta (CA2).
- `OpcionMultipleSinCorrectaTest` (CA3).
- `OrdenarConSecuenciaInvalidaTest` — repetido y salteado (Extras).

**Listo cuando** los cuatro tipos se dan de alta y cada forma inválida devuelve 400 nombrando
su campo.

**⚠ Trampa.** Validar el `payload` solo en el DTO con Bean Validation deja el `criterio` sin
verificar contra él: se puede crear un ítem cuyo `criterio` apunte a una opción inexistente y el
corrector de `HU18` lo va a puntuar mal en silencio. **La coherencia entre los dos es una regla
de dominio**, y va en `ValidadorDePayload`, no en anotaciones.

---

### ▸ G04-HU13 — Ítems de respuesta abierta y rechazo explícito de los tipos diferidos
`3 pts · Must` · módulo: teoricos · **D-09**

**Antes de empezar**
`HU10` en verde. Se hace junto con `HU12`: es la misma jerarquía sellada.

**Archivos**

| Archivo | Qué es |
|---|---|
| `teoricos/dominio/payload/AbiertaPayload.java` | `{consigna: string, extensionMaxima: int}` |
| `teoricos/dominio/payload/{Conversacion,Debate}Payload.java` | existen, pero se rechazan |
| `teoricos/dominio/TipoDeItem.java` | se le agrega `esAutocorregible()` y `estaDiferido()` |
| `resources/messages_*.properties` | clave `error.tipo.diferido` |

**Pasos**

1. **`AbiertaPayload`** con `consigna` y `extensionMaxima` opcional. `criterio` **nullable**:
   si viene, es una rúbrica en texto que el corrector humano ve en la cola de `HU20` (CA3).
2. **Marcar el ítem como de corrección humana (CA1):** no hace falta una columna nueva —
   `TipoDeItem.esAutocorregible()` lo deriva del enum. Menos estado que mantener sincronizado.
3. **Rechazo de los diferidos (CA2):** en `ValidadorDePayload`, las ramas de `CONVERSACION` y
   `DEBATE` lanzan `ExcepcionDeNegocio(ClaveI18n.TIPO_DIFERIDO, "tipo")` con **422**, no 400.
   El mensaje, en `messages_es.properties`:
   > `error.tipo.diferido=El tipo de ítem «{0}» está previsto en el modelo pero no implementado en esta versión. Ver D-09.`

   Que diga *diferido* y no *inválido*: quien lo reciba tiene que entender que el tipo existe y
   que va a llegar, no que se equivocó de nombre.
4. **Dejar el `payload` preparado.** Los dos records existen con un campo `transcripcion` sin usar.
   Cuando se implementen, no hace falta migración: el `jsonb` ya lo admite.

**Tests que la cierran**
- `AltaDeAbiertaTest` — se crea, queda no-autocorregible, `criterio` nulo aceptado (CA1, CA3).
- `TipoDiferidoRechazadoTest` — parametrizado con los dos tipos: 422 y la clave `error.tipo.diferido`,
  y se afirma que el mensaje resuelto contiene la palabra «diferido» (CA2).

**Listo cuando** un `POST` de tipo `DEBATE` devuelve 422 con un mensaje que explica que está
diferido, y no un 400 genérico.

---
# E-04 · Composición y entrega del desafío teórico

> ## ⛔ Corrección aplicada el 2026-09-09 — leer antes de tomar HU14, HU15 o HU16
>
> Las tres historias de esta épica se escribieron sobre `DISENIO-G04-SPRINT1.md`, **antes** de que
> `CONTRATO-INTEGRACION-G04.md` cerrara las decisiones CI-01 a CI-54. Tres de esas decisiones
> cambian lo que dicen los pasos de abajo. El contrato manda; los pasos originales se conservan
> tachados en lo que corresponde para que se vea qué cambió y por qué.
>
> **1 · No hay congelamiento de versión (CI-12, CI-13).**
> La referencia del contenido al ítem es **flotante**: `contenido_item` guarda `item_id` y el
> desafío sirve **siempre la última versión**. Quien estampa la versión es **la respuesta del
> alumno** (`respuesta.item_version_id`), no la composición. La `⚠ Trampa` original de HU14 decía
> exactamente lo contrario y **quedó invertida**. El motivo del cambio es RF-CUR-05: el profesor
> tiene que poder arreglar una errata y que surta efecto en los alumnos que todavía no respondieron.
> Corolario (CI-53): lo autoritativo al corregir es la estampa de cada respuesta, nunca
> `contenidoRef.version`.
>
> **2 · `POST /teoricos/desafios` lo llama el front, no el Tema 03 (CI-03), y se renombra.**
> El front compone: primero el contenido acá, que devuelve un `contenidoId`, y después el desafío
> en el 03 con la referencia ya formada. La entidad deja de llamarse `desafio_teorico` y pasa a ser
> **`contenido`**; el endpoint es **`POST /teoricos/contenidos`**. No existe ningún contrato de
> composición entre el 03 y nosotros — ese es el punto de CI-03.
>
> **3 · La entrega del alumno no nos llega a nosotros (CI-22).**
> Llega al Tema 03, que valida, registra y **nos despacha** con
> `{ entregaId, desafioId, alumnoId, cursoCohorteId, intento, contenidoBinding, respuesta }`.
> Nuestro endpoint de recepción es **`POST /teoricos/evaluaciones`**, lo consume un servicio y no
> un alumno, responde **siempre `202` con `{ evaluacionId, estado, correccion }` y nunca la nota**
> (CI-23, CI-24), y `entregaId` es la clave de idempotencia (CI-25). En consecuencia, en HU16 el
> `alumnoId` **sí** viene en el body —lo manda el 03, que es su dueño— y el paso 3 original
> («resolverlo del token, el DTO no tiene el campo») ya no aplica: lo que reemplaza a esa garantía
> es la validación de la firma del despacho.
>
> **4 · La lectura del alumno exige un vale firmado (CI-18, CI-21).**
> En HU15, la pertenencia a cohorte deja de ser la única autorización: el 03 emite un vale de
> lectura de corta vida al abrir el desafío y nosotros validamos la firma sin llamar a nadie.
> Y son **dos endpoints con dos proyecciones separadas** —`/vista-alumno` y `/vista-profesor`—,
> no uno filtrado por rol (CI-17).
>
> Lo que **no** cambió y sigue valiendo palabra por palabra: el test de no-fuga de criterio sobre el
> JSON crudo (HU15 paso 5), la inmutabilidad de `item_version` (HU10) y toda la épica E-03.

---

### ▸ G04-HU14 — Composición del desafío teórico con congelado de versión
`5 pts · Must` · módulo: teoricos · **dependencia dura con el Tema 03**

**Antes de empezar**
`HU11` en verde. Hace falta saber del Tema 03 qué `desafioId` nos manda y con qué autenticación
nos llama: si todavía no está, se implementa contra el contrato del diseño y se marca como supuesto.

**Archivos**

| Archivo | Qué es |
|---|---|
| `db/migration/teoricos/V4__desafio_teorico.sql` | `desafio_teorico` + `desafio_teorico_item` |
| `teoricos/infraestructura/persistencia/DesafioTeoricoEntity.java` | |
| `teoricos/infraestructura/persistencia/DesafioTeoricoItemEntity.java` | con `item_version_id` |
| `teoricos/aplicacion/ComposicionService.java` | `componer(...)` |
| `teoricos/infraestructura/web/DesafioController.java` | `POST /teoricos/desafios` |

**Pasos**

1. **Migración:**
   ```sql
   CREATE TABLE teoricos.desafio_teorico (
     id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
     desafio_id        uuid NOT NULL UNIQUE,   -- el id que nos da el Tema 03
     curso_cohorte_id  uuid NOT NULL,
     puntaje_total     int  NOT NULL,
     baja_logica       timestamptz NULL
   );
   CREATE TABLE teoricos.desafio_teorico_item (
     desafio_teorico_id uuid NOT NULL REFERENCES teoricos.desafio_teorico(id),
     item_version_id    uuid NOT NULL REFERENCES teoricos.item_version(id),
     orden              int  NOT NULL,
     puntaje            int  NOT NULL,
     PRIMARY KEY (desafio_teorico_id, item_version_id)
   );
   ```
   El `UNIQUE` en `desafio_id` evita que dos llamadas del 03 compongan dos veces el mismo desafío.
2. **`componer(desafioId, cursoCohorteId, List<ItemEnDesafio>)`**. El request trae **`itemId`**,
   no `itemVersionId`: el profesor elige un ítem del banco, no una versión. El servicio resuelve
   `version_actual` en ese instante y guarda el `item_version_id` resultante (CA1). **Ese es el
   congelado**, y ocurre una sola vez.
3. **Validaciones de composición:**
   - todos los ítems existen, están vigentes (`baja_logica IS NULL`) y son del profesor,
   - `orden` sin repetidos y consecutivo desde 1,
   - `puntaje > 0` por ítem.
4. **`puntaje_total` calculado y persistido (CA3):** se guarda porque el Tema 03 lo va a pedir en
   cada lectura y recalcularlo cada vez es un join innecesario. Se valida contra la suma en cada
   escritura, así no puede desincronizarse.
5. **Respuesta 201** con el `desafioTeoricoId`, el `puntajeTotal` y la lista de ítems con su
   `orden` y `puntaje` — sin enunciados y sin `criterio`.

**Tests que la cierran**
- `ComposicionCongelaVersionIT` — componer, publicar una versión nueva del ítem, releer el
  desafío: sigue apuntando al `item_version_id` viejo y el enunciado es el original (CA1, CA2).
  **Este es el test que justifica toda la épica E-03.**
- `PuntajeTotalEsLaSumaTest` (CA3).
- `ComposicionDuplicadaRechazadaTest` — dos `POST` con el mismo `desafioId` → 409.

**Listo cuando** editar un ítem del banco no altera ningún desafío ya compuesto.

**⚠ Trampa** *(invertida por CI-12 y CI-13 — ver la corrección al inicio de la épica)*.
Guardar `item_version_id` en la composición **pinnea** la referencia y rompe RF-CUR-05: el profesor
arregla una errata y ningún alumno la ve hasta que alguien suba la versión del desafío a mano.
La columna es **`item_id`**, la versión se resuelve en cada lectura, y la FK a `item_version(id)`
va en **`respuesta`**, que es donde la estampa tiene sentido: dice qué vio ese alumno.
Lo que D-04 protege —que un examen ya rendido no cambie— lo garantiza la estampa de la respuesta,
no el pinneo de la composición.

---

### ★ G04-HU15 — Lectura del desafío por el alumno sin la clave de corrección
`5 pts · Must` · módulo: teoricos

**Antes de empezar**
`HU14` en verde. `HU40` (cliente hacia el Tema 02) para el CA3; si no está, se implementa con el
fallback de `HU40` y se marca. `HU05` para el aviso de escritorio.

**Archivos**

| Archivo | Qué es |
|---|---|
| `teoricos/infraestructura/web/DesafioController.java` | `GET /teoricos/desafios/{id}` |
| `teoricos/infraestructura/web/dto/DesafioParaAlumnoResponse.java` | **sin** `criterio` |
| `teoricos/infraestructura/web/dto/ItemParaAlumnoResponse.java` | ídem |
| `teoricos/aplicacion/LecturaDeDesafioService.java` | resuelve pertenencia |
| `test/…/teoricos/SinFugaDeCriterioTest.java` | el test del CA2 |

**Pasos**

1. **DTO dedicado, no la entidad.** `ItemParaAlumnoResponse(UUID itemVersionId, String enunciado,
   JsonNode payload, int orden, int puntaje)`. **La clase no tiene campo `criterio`**: la garantía
   del CA2 la da el tipo, no un `@JsonIgnore` que alguien puede borrar.
2. **Limpiar el `payload` también.** El `payload` de opción múltiple lleva las opciones, que sí van;
   pero conviene pasar por un `mapper` explícito por tipo en vez de reenviar el `jsonb` crudo,
   porque el día que alguien meta un campo `esCorrecta` dentro de `opciones` para simplificar,
   se filtra la clave sin tocar el DTO.
3. **Pertenencia a cohorte (CA3):** `LecturaDeDesafioService` llama al cliente del Tema 02 con el
   `alumnoId` del token y el `cursoCohorteId` del desafío. Si no pertenece → 403 **con body vacío**.
   No devolver «no pertenecés al curso X»: eso confirma que el desafío existe.
4. **Anotar con `@RequiereEscritorio`** (Extras, de `HU05`).
5. **Test de no-fuga (CA2), el importante:** serializar la respuesta completa con el
   `ObjectMapper` real y afirmar sobre el **string JSON**:
   ```java
   String json = mockMvc.perform(get(...)).andReturn().getResponse().getContentAsString();
   assertThat(json).doesNotContain("criterio")
                   .doesNotContain("correctas").doesNotContain("esVerdadero")
                   .doesNotContain("secuencia").doesNotContain("pares");
   ```
   Buscar las claves de los cuatro criterios y no solo la palabra `criterio`: un anidamiento
   inesperado se escapa del primer chequeo.

**Tests que la cierran**
- `LecturaDevuelveEnunciadosTest` (CA1).
- `SinFugaDeCriterioTest` (CA2) — y se reutiliza en `HU23`.
- `AlumnoAjenoRecibe403Test` (CA3).
- `MovilRecibeAvisoTest` reutilizado de `HU05` (Extras).

**Listo cuando** el JSON serializado completo no contiene ninguna de las cinco claves de criterio.

**⚠ Trampa.** Reutilizar la entidad JPA como respuesta «para ir rápido» expone `criterio` el día
que alguien agregue el campo al mapeo. Los DTO de alumno viven en un paquete propio
(`web.dto.alumno`) y una regla de ArchUnit prohíbe que dependan de `..persistencia..`.

---

### ★ G04-HU16 — Envío del intento completo
`5 pts · Must` · módulo: teoricos

**Antes de empezar**
`HU14` en verde. Hace falta el número de intento del Tema 03 (supuesto S-03): mientras no esté,
se acepta en el body y se documenta.

**Archivos**

| Archivo | Qué es |
|---|---|
| `db/migration/teoricos/V5__respuesta.sql` | tabla `respuesta` |
| `teoricos/infraestructura/persistencia/RespuestaEntity.java` | |
| `teoricos/aplicacion/EnvioDeIntentoService.java` | la transacción |
| `teoricos/infraestructura/web/dto/EnviarIntentoRequest.java` | |

**Pasos**

1. **Migración:**
   ```sql
   CREATE TABLE teoricos.respuesta (
     id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
     desafio_teorico_id uuid NOT NULL REFERENCES teoricos.desafio_teorico(id),
     item_version_id    uuid NOT NULL REFERENCES teoricos.item_version(id),
     alumno_id          uuid NOT NULL,
     intento            int  NOT NULL,
     contenido          jsonb NOT NULL,
     respondida_en      timestamptz NOT NULL DEFAULT now(),
     UNIQUE (desafio_teorico_id, item_version_id, alumno_id, intento)
   );
   ```
   Acá el `timestamp` **sí** va: el registro académico es nominal por diseño. La ausencia de marca
   temporal es una regla del módulo `encuestas`, no de este.
2. **`@Transactional` sobre el caso de uso completo (CA1).** Validar los N ítems primero,
   acumulando los errores, y recién después persistir con `saveAll`. Si algo falla, la excepción
   revierte todo (CA2) y el error lista **cuál** ítem falló, por `itemVersionId`.
3. **Identidad desde el token (CA3):** `alumnoId` se resuelve del `JwtAuthenticationToken`.
   El DTO `EnviarIntentoRequest` **no tiene campo `alumnoId`**: si no existe, no se puede confundir.
   Es la misma estrategia que en `HU15` con `criterio`.
4. **Cobertura completa del desafío:** validar que llegan respuestas para **todos** los ítems del
   desafío. Las que el alumno dejó en blanco vienen con `contenido: {}` y se persisten como fila
   (Extras): así el corrector de `HU18` puntúa 0 explícitamente y no tiene que distinguir
   «no contestó» de «no llegó el dato».
5. **Validación del `contenido` por tipo:** despachar sobre el tipo del ítem —una respuesta de
   opción múltiple tiene que traer ids de opciones que existan en el `payload` congelado—.
   Reutiliza la jerarquía sellada de `HU12`.

**Tests que la cierran**
- `EnvioCompletoPersisteTodoIT` (CA1).
- `UnaRespuestaInvalidaRevierteTodoIT` — se afirma cero filas en la tabla y el error nombra el
  ítem culpable (CA2).
- `AlumnoIdDelBodySeIgnoraTest` — se manda un `alumnoId` ajeno en el JSON y la fila queda con el
  del token (CA3).
- `ItemEnBlancoSePersisteTest` (Extras).

**Listo cuando** un intento con un ítem inválido no deja ni una fila en la base.

**⚠ Trampa.** El `UNIQUE` del paso 1 hace que reenviar el mismo intento explote con violación de
constraint en vez de un 409 legible. Capturar `DataIntegrityViolationException` y traducirla a
`ExcepcionDeNegocio(INTENTO_YA_ENVIADO)` con 409.

---

### ★ G04-HU17 — Reintento sin tope para el desafío de recuperación de vida
`3 pts · Must` · módulo: teoricos · **RF-REC-04/06**

**Antes de empezar**
`HU16` en verde. Es más una historia de *no hacer* que de hacer: consiste en garantizar que
nadie introdujo un límite.

**Archivos**

| Archivo | Qué es |
|---|---|
| `teoricos/infraestructura/web/dto/EnviarIntentoRequest.java` | revisar que `intento` no tenga `@Max` |
| `test/…/teoricos/ReintentoSinTopeIT.java` | el test central |
| `docs/DECISIONES.md` | la nota que explica por qué no hay tope |

**Pasos**

1. **Auditar el modelo (CA1):** `respuesta.intento` y `correccion.intento` son `int NOT NULL` sin
   `CHECK`. En el DTO, `@Positive` sí, `@Max(4)` **no**. Dejar un comentario en el record:
   ```java
   // Sin @Max: RF-REC-04, el desafío de recuperación de vida se reintenta sin límite.
   // El tope de RF-DES-07 lo aplica el Tema 03, que es el dueño del ciclo de vida del desafío.
   ```
   El comentario es la mitad del valor de esta historia: sin él, alguien va a «arreglar» la
   validación faltante en el sprint 2.
2. **Corrección independiente por intento (CA3):** la clave de `correccion` es
   `(desafio_teorico_id, alumno_id, intento)`. Verificar que ningún `save` hace *upsert* sobre
   `(desafio, alumno)` ignorando el intento — es el error que sobrescribiría el historial.
3. **Test de las diez pasadas (CA2):** un `@Test` que envía diez intentos y afirma diez
   correcciones, cada una con su nota, y que la nota del intento 1 sigue siendo la original.
4. **Documentar** en `docs/DECISIONES.md` que el límite de intentos, cuando exista, es del Tema 03
   y lo aplica antes de llamarnos. Nosotros no lo replicamos: dos lugares con la misma regla
   terminan discrepando.

**Tests que la cierran**
- `ReintentoSinTopeIT` (CA1, CA2).
- `CorreccionDeIntentoNoPisaLaAnteriorTest` (CA3).

**Listo cuando** diez intentos dejan diez correcciones y la primera está intacta.

---

# E-05 · Motor de corrección

---

### ★ G04-HU18 — Corrección automática de los cuatro tipos objetivos
`8 pts · Must` · módulo: teoricos

**Antes de empezar**
`HU12` (los criterios) y `HU16` (las respuestas) en verde. Es el corazón del módulo `teoricos`.

**Archivos**

| Archivo | Qué es |
|---|---|
| `db/migration/teoricos/V6__correccion.sql` | tabla `correccion` |
| `teoricos/dominio/correccion/CorrectorDeItem.java` | interfaz, una por tipo |
| `teoricos/dominio/correccion/{OpcionMultiple,VerdaderoFalso,Emparejar,Ordenar}Corrector.java` | |
| `teoricos/dominio/correccion/CalculadoraDeNota.java` | agrega los puntajes a 0-100 |
| `teoricos/aplicacion/CorreccionAutomaticaService.java` | orquesta |
| `teoricos/infraestructura/web/dto/CorreccionResponse.java` | con `detalle` |

**Pasos**

1. **Migración:**
   ```sql
   CREATE TABLE teoricos.correccion (
     id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
     desafio_teorico_id uuid NOT NULL REFERENCES teoricos.desafio_teorico(id),
     alumno_id          uuid NOT NULL,
     intento            int  NOT NULL,
     nota               int  NULL CHECK (nota BETWEEN 0 AND 100),
     aprobado           boolean NULL,
     corrector          varchar(16) NOT NULL,
     estado             varchar(16) NOT NULL,
     corregida_por      uuid NULL,
     corregida_en       timestamptz NULL,
     UNIQUE (desafio_teorico_id, alumno_id, intento)
   );
   CREATE TABLE teoricos.correccion_detalle (
     correccion_id   uuid NOT NULL REFERENCES teoricos.correccion(id),
     item_version_id uuid NOT NULL,
     puntaje         int NOT NULL,
     obtenido        int NOT NULL,
     PRIMARY KEY (correccion_id, item_version_id)
   );
   ```
   `nota` y `aprobado` son **nullable**: en `PENDIENTE` no hay nota, y eso es normal (`HU21`).
2. **Un corrector por tipo,** cada uno puro: `(payload, criterio, contenido, puntaje) → obtenido`.
   Sin base, sin Spring, testeables como funciones.

   | Tipo | Regla de puntaje |
   |---|---|
   | Opción múltiple | todo o nada si `multiple: false`; parcial proporcional a aciertos menos errores, con piso en 0, si `multiple: true` |
   | Verdadero / falso | todo o nada |
   | Emparejar | proporcional: `pares correctos / total de pares` |
   | Ordenar | proporcional a las posiciones acertadas |

   **Fijar esto ahora y escribirlo en el OpenAPI.** Es la clase de decisión que si no se documenta,
   cada uno la implementa distinto y el alumno se entera en el examen.
3. **`CalculadoraDeNota` (CA2):** `nota = round(100 * Σobtenido / Σpuntaje)`.
   `aprobado = nota >= umbral`, con `teoricos.correccion.umbral-aprobacion: 60` en
   `application.yml`. **El umbral nunca llega del cliente**; el DTO de entrada no lo tiene.
4. **Ítem objetivo sin `criterio` (Extras):** no puntúa 0 — se marca ese ítem como pendiente de
   corrección humana y arrastra la corrección a `PENDIENTE`. Es el caso raro de un ítem mal
   cargado, y puntuar 0 automáticamente perjudicaría al alumno por un error del profesor.
5. **`detalle` en la respuesta (CA3):** `[{itemVersionId, puntaje, obtenido}]`, del `correccion_detalle`.
6. **Disparo:** por ahora, al final de `EnvioDeIntentoService` de forma sincrónica. `HU19` lo
   convierte en un puerto asincrónico; no adelantarlo acá.

**Tests que la cierran**
- `CorrectorPorTipoTest` — parametrizado, con acierto total, parcial y nulo para cada uno de los
  cuatro. Doce casos (CA1). Es la base de `HU23`.
- `NotaEnEscalaCentesimalTest` y `AprobadoNoLlegaDelClienteTest` (CA2).
- `DetallePorItemTest` (CA3).
- `ItemObjetivoSinCriterioVaAPendienteTest` (Extras).

**Listo cuando** los doce casos del corrector están verdes y la nota se calcula sin que el
cliente pueda influir en `aprobado`.

**⚠ Trampa.** La corrección parcial de opción múltiple sin piso en 0 da notas negativas cuando
el alumno marca todo. `Math.max(0, aciertos - errores)` por ítem, antes de agregar.

---

### ▸ G04-HU19 — Puerto de corrección con adaptadores intercambiables
`5 pts · Must` · módulo: teoricos · **D-01, prepara el LLM**

**Antes de empezar**
`HU18` en verde. Esta historia **refactoriza** lo que `HU18` dejó sincrónico; hacerlas en ese
orden y no al revés.

**Archivos**

| Archivo | Qué es |
|---|---|
| `teoricos/dominio/correccion/PuertoDeCorreccion.java` | la interfaz |
| `teoricos/infraestructura/correccion/AdaptadorAutomatico.java` | envuelve `HU18` |
| `teoricos/infraestructura/correccion/AdaptadorHumano.java` | encola para `HU20` |
| `teoricos/aplicacion/ResultadoDeCorreccionHandler.java` | recibe el resultado *después* |
| `test/…/correccion/AdaptadorFicticio.java` | el del CA2 |

**Pasos**

1. **La interfaz, con la forma que importa (CA3):**
   ```java
   public interface PuertoDeCorreccion {
       TipoDeCorrector tipo();
       boolean puedeCorregir(TipoDeItem tipo);
       void solicitarCorreccion(SolicitudDeCorreccion solicitud);  // void, no devuelve nota
   }
   ```
   **`void` es la decisión de diseño de la historia.** Si devolviera `ResultadoDeCorreccion`, el
   adaptador humano tendría que bloquear hasta que el profesor corrija, y el del LLM tampoco
   podría implementarla. El resultado vuelve por `ResultadoDeCorreccionHandler.recibir(...)`.
2. **`AdaptadorAutomatico`** llama al corrector de `HU18` y publica el resultado en el handler
   **en el mismo hilo**. Es asincrónico en la forma y sincrónico en el tiempo: sigue siendo
   inmediato para el alumno, pero el código que lo invoca ya no lo sabe.
3. **`AdaptadorHumano`** deja la corrección en `PENDIENTE` y no hace nada más. El resultado llega
   cuando el profesor usa el endpoint de `HU20`, que también entra por el handler.
4. **Selección por configuración (CA1):** un `Map<TipoDeItem, PuertoDeCorreccion>` armado en un
   `@Configuration` a partir de `teoricos.correccion.adaptador.<tipo>: AUTOMATICO|HUMANO`.
   Con valores por defecto sensatos: los cuatro objetivos en `AUTOMATICO`, `ABIERTA` en `HUMANO`.
5. **`AdaptadorFicticio` en los tests (CA2):** un tercer adaptador que puntúa siempre 42, registrado
   solo en el contexto de test. El test afirma que **ninguna clase de `aplicacion` cambió** para
   soportarlo. Ese es el CA: no es que funcione, es que no hubo que tocar nada.

**Tests que la cierran**
- `SeleccionPorConfiguracionTest` (CA1).
- `AdaptadorFicticioSinTocarCodigoTest` (CA2).
- `ResultadoLlegaDespuesTest` — el flujo funciona cuando el resultado entra por el handler
  minutos después de la solicitud (CA3).

**Listo cuando** agregar un adaptador es agregar una clase y una línea de configuración.

**Cuando llegue el LLM (Sprint 2+).** Un `AdaptadorLlm` que publica una solicitud en Kafka y un
`@KafkaListener` que llama a `ResultadoDeCorreccionHandler.recibir(...)`. Ni `HU18`, ni `HU20`,
ni `HU21` se tocan. Si esta historia queda bien, ese cambio es de un día.

---

### ★ G04-HU20 — Cola de corrección manual del profesor
`5 pts · Must` · módulo: teoricos · **D-01**

**Antes de empezar**
`HU19` en verde. `HU40` para saber qué cursos dicta el profesor (CA3).

**Archivos**

| Archivo | Qué es |
|---|---|
| `teoricos/infraestructura/web/CorreccionController.java` | `GET /pendientes`, `POST /{id}` |
| `teoricos/infraestructura/web/dto/PendienteResponse.java` | |
| `teoricos/aplicacion/CorreccionManualService.java` | |
| `db/migration/teoricos/V7__correccion_indices.sql` | índice para la cola |

**Pasos**

1. **`GET /teoricos/correcciones/pendientes` (CA1).** La consulta: correcciones en `PENDIENTE`
   cuyos ítems abiertos todavía no tienen `correccion_detalle`, de desafíos de cursos que dicta
   el profesor autenticado, **ordenadas por `respondida_en` ascendente** — lo más viejo primero.
   Índice: `(estado, desafio_teorico_id)` sobre `correccion`.
2. **La respuesta trae lo que hace falta para corregir sin otro request:** enunciado, consigna,
   la rúbrica del `criterio` si existe (`HU13`), el texto que escribió el alumno, y el puntaje
   máximo del ítem. **No trae `alumno_id`** — corregir a ciegas es mejor práctica y, además,
   evita el sesgo. Sí un identificador opaco para poder asentar la nota.
3. **Conteo por curso (Extras):** un campo `porCurso: [{cursoCohorteId, pendientes}]` en la misma
   respuesta, para que el front pueda ordenar el trabajo.
4. **`POST /teoricos/correcciones/{id}`** con `{itemVersionId, obtenido, motivo}`.
   - valida `0 <= obtenido <= puntaje`,
   - escribe/actualiza `correccion_detalle`,
   - registra el historial de `HU22` — **la escritura del historial va en la misma transacción**,
   - llama a `ResultadoDeCorreccionHandler`, que recalcula la nota (CA2) y decide si pasa a
     `FINAL` (`HU21`).
5. **Autorización (CA3):** un interceptor de dominio verifica que el `cursoCohorteId` del desafío
   esté en la lista de cursos del profesor. Si no, 403 **antes** de tocar nada.

**Tests que la cierran**
- `ColaSoloDelProfesorTest` — el profesor B no ve pendientes de cursos de A; el orden es por
  antigüedad (CA1).
- `CorreccionRecalculaNotaIT` (CA2).
- `CorreccionDeCursoAjeno403Test` — y se afirma que la nota **no** cambió (CA3).
- `ConteoPorCursoTest` (Extras).

**Listo cuando** el profesor ve su cola ordenada, corrige un ítem abierto y la nota del desafío
se recalcula sola.

---

### ★ G04-HU21 — Corrección parcial en estado PENDIENTE
`5 pts · Must` · módulo: teoricos · **espeja RF-IA-27**

**Antes de empezar**
`HU18` y `HU20` en verde. `HU37` (el productor de Kafka) para el CA3, aunque se puede implementar
contra el adaptador en memoria y conectar después.

**Archivos**

| Archivo | Qué es |
|---|---|
| `teoricos/dominio/correccion/EstadoDeCorreccion.java` | enum `PENDIENTE`, `FINAL` |
| `teoricos/aplicacion/ResultadoDeCorreccionHandler.java` | la máquina de estados |
| `teoricos/infraestructura/web/dto/CorreccionResponse.java` | `nota` nullable |

**Pasos**

1. **La regla, en un solo lugar:**
   ```java
   boolean estaCompleta = detalles.size() == itemsDelDesafio.size();
   correccion.setEstado(estaCompleta ? FINAL : PENDIENTE);
   correccion.setNota(estaCompleta ? calculadora.calcular(detalles) : null);
   correccion.setAprobado(estaCompleta ? nota >= umbral : null);
   ```
   Que viva **solo** en `ResultadoDeCorreccionHandler`. Si la transición se decide en dos lugares
   —el corrector automático y el manual— van a divergir.
2. **`PENDIENTE` devuelve `nota: null` y el detalle de lo ya corregido (CA1).** Serializar el
   `null` explícitamente: `@JsonInclude(ALWAYS)` en ese campo. Si Jackson lo omite, el Tema 03
   no puede distinguir «pendiente» de «campo que no llegó».
3. **Transición a `FINAL` (CA2):** ocurre cuando entra el último detalle faltante, venga del
   corrector automático o del profesor. Es el mismo camino de código.
4. **El evento, solo en la transición (CA3).** Publicar **después** del commit, no dentro de la
   transacción:
   ```java
   @TransactionalEventListener(phase = AFTER_COMMIT)
   public void alPasarAFinal(CorreccionFinalizada e) { publicador.publicar(...); }
   ```
   Si se publica dentro y la transacción se revierte, el Tema 03 consolida un desafío que en
   nuestra base nunca pasó a `FINAL`.
5. **Documentar en el OpenAPI (Extras):** `estado: PENDIENTE` con `nota: null` es un **200**, no
   un 202 ni un error. Con un `example` en el esquema, porque el Tema 03 lo va a leer de ahí.

**Tests que la cierran**
- `MixtoQuedaPendienteTest` — desafío con tres objetivos y una abierta: `PENDIENTE`, `nota: null`,
  detalle con tres entradas (CA1).
- `UltimoItemCierraAFinalIT` (CA2).
- `NoSePublicaEnPendienteTest` — con el adaptador en memoria: cero eventos mientras hay pendientes,
  exactamente uno al pasar a `FINAL` (CA3).
- `RollbackNoPublicaIT` — se fuerza una excepción después del cambio de estado y se afirma cero
  eventos (paso 4).

**Listo cuando** el evento sale una sola vez, después del commit, en el momento exacto en que la
corrección pasa a `FINAL`.

**⚠ Trampa.** Publicar en el `@Transactional` es el bug clásico de esta historia, y no aparece
en desarrollo porque las transacciones de test siempre commitean. El test `RollbackNoPublicaIT`
existe justamente para eso.

---

### ★ G04-HU22 — Historial auditado de correcciones manuales
`5 pts · Should` · módulo: teoricos · **D-08, criterio de release 13**

**Antes de empezar**
`HU20` en verde.

**Archivos**

| Archivo | Qué es |
|---|---|
| `db/migration/teoricos/V8__correccion_historial.sql` | tabla + triggers |
| `teoricos/infraestructura/persistencia/CorreccionHistorialEntity.java` | sin setters |
| `teoricos/infraestructura/persistencia/CorreccionHistorialRepository.java` | solo `save` y `findBy` |
| `teoricos/infraestructura/web/CorreccionController.java` | `GET /{id}/historial` |

**Pasos**

1. **Migración con las defensas incluidas:**
   ```sql
   CREATE TABLE teoricos.correccion_historial (
     id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
     correccion_id uuid NOT NULL REFERENCES teoricos.correccion(id),
     nota_anterior int NULL,
     nota_nueva    int NOT NULL,
     motivo        text NOT NULL CHECK (length(trim(motivo)) >= 10),
     profesor_id   uuid NOT NULL,
     registrado_en timestamptz NOT NULL DEFAULT now()
   );
   REVOKE UPDATE, DELETE ON teoricos.correccion_historial FROM app_teoricos;
   ```
   El `REVOKE` es lo que hace que el CA3 sea una garantía del motor: aunque alguien escriba el
   código para editar una fila, PostgreSQL lo rechaza. Ojo: en `HU06` se otorgó CRUD sobre todo
   el esquema, así que este `REVOKE` tiene que ir **después**, en esta migración.
2. **`nota_anterior` nullable:** la primera corrección de un ítem no tiene anterior. `nota_nueva`
   no es nullable nunca.
3. **Motivo obligatorio (CA2):** el `CHECK` de base es el piso; la validación de negocio devuelve
   400 con clave i18n y `campo: "motivo"` antes de llegar ahí. **La regla completa:** si la
   corrección modifica una nota que ya existía, `motivo` es obligatorio; si es la primera, es
   opcional. El `CHECK` sobre `length >= 10` solo aplica cuando hay motivo — modelarlo como
   `motivo IS NULL OR length(trim(motivo)) >= 10` y hacer obligatoria la presencia en el servicio.
4. **Entidad sin setters y repositorio recortado (CA3):** el repositorio extiende
   `Repository<...>` y declara únicamente `save` y `findByCorreccionIdOrderByRegistradoEnAsc`
   (Extras). Nada de `JpaRepository`.
5. **Escritura en la misma transacción** que el cambio de nota de `HU20`: una nota modificada sin
   su fila de historial es exactamente lo que el criterio 13 prohíbe.

**Tests que la cierran**
- `TodaCorreccionManualDejaHistorialIT` — se afirman los cinco campos (CA1).
- `SinMotivoSeRechazaIT` — 400, y se afirma que la nota **sigue siendo la anterior** (CA2).
- `HistorialInmutableIT` — un `UPDATE` por JDBC con el rol `app_teoricos` lanza `SQLState 42501` (CA3).
- `HistorialCompletoOrdenadoTest` (Extras).

**Listo cuando** ninguna nota puede cambiar sin dejar rastro, y el rastro no se puede borrar.

---

### ▸ G04-HU23 — Batería de tests del motor de corrección
`5 pts · Must` · módulo: teoricos · **línea base de regresión del Sprint 2**

**Antes de empezar**
`HU18` y `HU21` en verde. Esta historia consolida lo que las anteriores dejaron disperso.

**Archivos**

| Archivo | Qué es |
|---|---|
| `test/…/teoricos/correccion/CorreccionSuite.java` | agrupa la batería |
| `test/resources/fixtures/items/*.json` | los casos de cada tipo, en archivos |
| `test/…/teoricos/InmutabilidadDeVersionIT.java` | CA2 |
| `test/…/teoricos/SinFugaDeCriterioEnTodaLaApiTest.java` | CA3 |

**Pasos**

1. **Sacar los casos a fixtures.** Un JSON por tipo con `payload`, `criterio`, y tres
   `contenido` — total, parcial y nulo—. Doce casos que se leen y se mantienen sin recompilar
   la lógica del test (CA1).
2. **`@ParameterizedTest` con `@MethodSource`** que recorre los fixtures. Agregar un tipo nuevo
   en el sprint 2 es agregar un archivo.
3. **Inmutabilidad (CA2):** recoger acá el test de `HU10`, pero end-to-end: componer un desafío,
   responderlo, y después intentar mutar la `item_version` por API y por JDBC. Ambos fallan.
4. **No fuga del criterio, ampliado (CA3).** El test de `HU15` mira un endpoint; este mira
   **todos** los endpoints consumidos por el alumno. Enumerarlos por reflexión: buscar los métodos
   de controlador anotados con la marca de rol alumno, invocarlos con `MockMvc` y afirmar sobre
   cada JSON. Así, un endpoint nuevo queda cubierto sin que nadie se acuerde de agregarlo.
5. **Cobertura como piso, no como objetivo:** exigir en el `pom.xml` (jacoco) un mínimo sobre el
   paquete `..teoricos.dominio.correccion..`, que es el que no puede tener huecos.

**Tests que la cierran** — la historia son los tests. La verificación es que la suite corre en
CI y que sacarle una rama al corrector la pone roja.

**Listo cuando** los doce casos, el de inmutabilidad y el de no-fuga corren en `mvn verify`, y
un endpoint nuevo para alumno queda cubierto automáticamente por el test del paso 4.

---
# E-06 · Instrumentos de encuesta y cumplimiento

---

### ▸ G04-HU24 — Catálogo de instrumentos versionado con clave i18n
`5 pts · Must` · módulo: encuestas / catalogo · **D-10**

**Antes de empezar**
`HU06` en verde (esquema `catalogo` con `SELECT` para los otros dos roles). `HU04` para las claves.

**Archivos**

| Archivo | Qué es |
|---|---|
| `db/migration/catalogo/V2__instrumento.sql` | tabla `instrumento` |
| `encuestas/catalogo/Instrumento.java` | entidad (ds: `dsCatalogo`) |
| `encuestas/catalogo/InstrumentoRepository.java` | |
| `encuestas/catalogo/CatalogoService.java` | `publicarVersion` |
| `resources/messages_*.properties` | los textos de las preguntas de encuesta |

**Pasos**

1. **Migración:**
   ```sql
   CREATE TABLE catalogo.instrumento (
     id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
     dimension        varchar(16) NOT NULL CHECK (dimension IN ('CURSO','CONTENIDO','PLATAFORMA')),
     version          int NOT NULL,
     clave_texto      varchar(120) NOT NULL,
     curso_cohorte_id uuid NULL,
     periodo          varchar(16) NOT NULL,
     abierto_desde    timestamptz NOT NULL,
     abierto_hasta    timestamptz NULL,
     baja_logica      timestamptz NULL,
     CHECK ((dimension = 'PLATAFORMA') = (curso_cohorte_id IS NULL))
   );
   ```
   El último `CHECK` es la mitad de la garantía del CA3 de `HU28`: la plataforma nunca lleva
   curso y las otras dos siempre lo llevan, y lo impone el motor.
2. **`clave_texto`, no el literal (CA2):** la columna guarda `encuesta.curso.satisfaccion.v2`.
   El texto vive en `messages_es.properties`. Un `CHECK` que rechace espacios en `clave_texto`
   es barato y atrapa al que pegue el enunciado ahí.
3. **Versionado (CA3), mismo criterio que los ítems teóricos:** editar un instrumento en uso es
   `publicarVersion`, que inserta una fila nueva con `version + 1` y una `clave_texto` nueva.
   `UNIQUE (dimension, curso_cohorte_id, periodo, version)`.
   **Un instrumento con respuestas no se toca nunca:** si se editara, las respuestas viejas
   quedarían contestando una pregunta que ya no existe y el KPI mezclaría dos cosas distintas.
4. **`SELECT` desde los otros roles:** verificar que `app_cumplimiento` y `app_respuestas` leen
   la tabla — el `ALTER DEFAULT PRIVILEGES` de `HU06` debería haberlo resuelto, pero conviene
   confirmarlo en el test, porque es el punto donde ese mecanismo falla en silencio.
5. **Semilla mínima:** una migración `R__instrumentos_base.sql` (repetible) con la encuesta de
   plataforma del período actual, para que `HU28` y los KPIs tengan con qué trabajar.

**Tests que la cierran**
- `EsquemaCatalogoConLasTresDimensionesIT` (CA1).
- `ClaveNoLiteralTest` — `clave_texto` matchea `^[a-z0-9.]+$` (CA2).
- `InstrumentoEnUsoNoSeEditaIT` — con respuestas asociadas, el `PUT` devuelve 409 y sugiere
  publicar versión (CA3).
- `CatalogoLegibleConLosTresRolesIT` (paso 4).

**Listo cuando** el catálogo existe, guarda claves y no permite editar un instrumento con respuestas.

---

### ★ G04-HU25 — Marcador de cumplimiento sin contenido de respuesta
`5 pts · Must` · módulo: encuestas / cumplimiento · **RF-ENC-12**

**Antes de empezar**
`HU06` y `HU24` en verde.

**Archivos**

| Archivo | Qué es |
|---|---|
| `db/migration/cumplimiento/V2__marcador.sql` | tabla `marcador` |
| `encuestas/cumplimiento/Marcador.java` | entidad (ds: `dsCumplimiento`) |
| `encuestas/cumplimiento/MarcadorRepository.java` | |
| `encuestas/cumplimiento/CumplimientoService.java` | `marcarCumplida` |
| `test/…/anonimato/MarcadorSinContenidoTest.java` | el test del CA2 |

**Pasos**

1. **Migración, y mirar bien lo que *no* tiene:**
   ```sql
   CREATE TABLE cumplimiento.marcador (
     alumno_id        uuid NOT NULL,
     instrumento_id   uuid NOT NULL,      -- la CAMPAÑA, no la encuesta del alumno
     curso_cohorte_id uuid NULL,
     estado           varchar(16) NOT NULL DEFAULT 'PENDIENTE'
                      CHECK (estado IN ('PENDIENTE','CUMPLIDA')),
     actualizado_en   timestamptz NOT NULL DEFAULT now(),
     baja_logica      timestamptz NULL,
     PRIMARY KEY (alumno_id, instrumento_id)
   );
   ```
   **Sin FK hacia `catalogo.instrumento`** aunque sea tentador: la regla de `HU06` es cero FK
   entre esquemas, sin excepciones. La integridad la valida el servicio leyendo el catálogo.
2. **`instrumento_id` es la campaña.** Escribirlo como comentario en la tabla:
   ```sql
   COMMENT ON COLUMN cumplimiento.marcador.instrumento_id IS
     'ID de la CAMPANIA, compartido por todos los respondentes. Si fuera individual, un join
      entre esquemas reconstruiria el vinculo y todo el diseno de anonimato seria decorativo.';
   ```
   Es la clase de cosa que alguien «normaliza» en el sprint 3 sin entender por qué estaba así.
3. **Transición unidireccional (CA3):** `marcarCumplida` hace
   `UPDATE ... SET estado='CUMPLIDA' WHERE alumno_id=? AND instrumento_id=? AND estado='PENDIENTE'`.
   Idempotente: llamarlo dos veces afecta cero filas la segunda y no es error. Un método
   `marcarPendiente` **no existe** en el servicio ni en el repositorio.
4. **Abstención cuenta como cumplimiento (Extras):** lo resuelve `HU31` llamando al mismo método.
   No hay una rama distinta acá, y eso es deliberado: el marcador no sabe qué respondió el alumno.
5. **Alta de marcadores:** al crearse una campaña (`HU28`), se generan los marcadores en
   `PENDIENTE` para los alumnos alcanzados. Ese `INSERT` masivo va con `ON CONFLICT DO NOTHING`
   para que el job sea reejecutable (`HU28` CA2).

**Tests que la cierran**
- `MarcadorConPkCompuestaIT` (CA1).
- `MarcadorSinContenidoTest` — se consulta `information_schema.columns` y se afirma que no hay
  ninguna columna llamada `estrellas`, `comentario`, `respuesta` ni `abstencion`, y que no hay
  ninguna FK que salga del esquema (CA2).
- `RetrocesoDeEstadoRechazadoTest` (CA3).

**Listo cuando** la tabla tiene seis columnas, ninguna de contenido, y el estado no retrocede.

**⚠ Trampa.** La comodidad de guardar `estrellas` acá «para el KPI y evitar el join» destruye
todo el diseño de anonimato en una línea. El test del CA2 existe exactamente para eso, y por eso
mira el catálogo del sistema y no el código.

---

### ▸ G04-HU26 — Consulta de encuestas pendientes del alumno
`3 pts · Must` · módulo: encuestas / cumplimiento · **D-06**

**Antes de empezar**
`HU25` en verde.

**Archivos**

| Archivo | Qué es |
|---|---|
| `encuestas/cumplimiento/web/PendientesController.java` | `GET /encuestas/pendientes` |
| `encuestas/cumplimiento/web/dto/PendienteResponse.java` | |

**Pasos**

1. **`GET /encuestas/pendientes?alumno={id}` (CA1).** Marcadores en `PENDIENTE` del alumno,
   enriquecidos con el catálogo (`clave_texto`, `abierto_hasta`) leído con `dsCumplimiento`,
   que tiene `SELECT` sobre `catalogo`.
   **Es el único punto del sistema donde `cumplimiento` lee `catalogo`,** y está permitido.
2. **Autorización (CA2):** el parámetro `alumno` existe porque está en el contrato, pero el
   servicio compara contra el token: si no coinciden y el rol no es ADMIN → 403.
   Alternativa mejor si se puede negociar con el front: ignorar el parámetro y usar siempre el
   token. Dejarlo anotado como propuesta para la sesión de integración.
3. **La respuesta (CA3):** `[{instrumentoId, claveTexto, dimension, cursoCohorteId, abiertoHasta}]`.
   Con `claveTexto` el front arma el mensaje del bloqueo en el idioma del usuario, sin que
   nosotros mandemos texto. Con `abiertoHasta` puede decir cuánto falta.
4. **Filtrar las vencidas:** una campaña con `abierto_hasta` pasado no bloquea. Si bloqueara,
   un alumno quedaría trabado para siempre por una encuesta que ya no puede responder.
   No está en los CA, pero sin esto el gate de `HU27` genera un deadlock.

**Tests que la cierran**
- `PendientesDelAlumnoTest` (CA1).
- `PendientesAjenas403Test` (CA2).
- `RespuestaTraeClaveYFechaTest` (CA3).
- `CampaniaVencidaNoBloqueaTest` (paso 4).

**Listo cuando** el front puede explicar el bloqueo sin recibir de nosotros ni una palabra de texto.

---

### ★ G04-HU27 — Gate de cumplimiento para el cierre de curso
`3 pts · Must` · módulo: encuestas / cumplimiento · **RF-ENC-11, D-03, D-07**

**Antes de empezar**
`HU25` en verde. **Depende del supuesto S-01** (el archivado del curso no espera a las encuestas
pendientes): por eso lleva flag.

**Archivos**

| Archivo | Qué es |
|---|---|
| `encuestas/cumplimiento/web/GateController.java` | `GET /encuestas/cumplimiento` |
| `encuestas/cumplimiento/GateService.java` | |
| `comun/config/MetricasConfig.java` | contador de anomalías |

**Pasos**

1. **`GET /encuestas/cumplimiento?alumno={id}&instrumento={id}` → `{"cumplida": true}` (CA1).**
   **Un solo campo.** Nada de agregar `fecha` o `estado` «por si sirve»: cada campo extra es una
   filtración potencial y este endpoint lo consumen dos temas ajenos.
2. **Ningún endpoint nominal (CA2).** Es una restricción sobre todo el módulo, no sobre esta
   historia. Escribir un test de arquitectura: ningún método de controlador bajo
   `..encuestas.cumplimiento..` puede devolver una `Collection` que contenga `alumnoId`.
   Ese test es lo que impide que en el sprint 3 alguien agregue el listado «para el profesor».
3. **Fail-open ante lo desconocido (CA3):** instrumento inexistente o no aplicable al alumno →
   `cumplida: true` + `meterRegistry.counter("encuestas.gate.anomalia").increment()`.
   Es la decisión correcta: bloquear el cierre académico de un alumno por un dato que nosotros
   no tenemos es peor que dejarlo pasar. Pero **tiene que quedar registrado**, porque si el
   contador sube, hay un bug de integración.
4. **Flag `encuesta.gate.enabled` (Extras):** en `false`, siempre `cumplida: true`.
   Por defecto en `true`; se apaga si S-01 sale al revés en la integración.
5. **Documentar en el OpenAPI** que el `true` puede venir de tres orígenes distintos —cumplió,
   anomalía, gate apagado— y que el consumidor no puede distinguirlos. Es intencional.

**Tests que la cierran**
- `GateDevuelveSoloCumplidaTest` — se afirma que el JSON tiene exactamente una clave (CA1).
- `SinEndpointNominalArchTest` (CA2).
- `InstrumentoInexistenteDaTrueYCuentaTest` (CA3).
- `GateApagadoSiempreTrueTest` (Extras).

**Listo cuando** el endpoint devuelve un booleano y nada más, y toda anomalía queda en una métrica.

---

### ▸ G04-HU28 — Disparo automático de la encuesta de plataforma
`3 pts · Should` · módulo: encuestas · **RF-ENC-02**

**Antes de empezar**
`HU24` y `HU25` en verde. Hace falta del Tema 02 la fecha de alta del alumno.

**Archivos**

| Archivo | Qué es |
|---|---|
| `encuestas/catalogo/DisparoDePlataformaJob.java` | `@Scheduled` |
| `db/migration/catalogo/V3__campania_ejecucion.sql` | control de idempotencia |

**Pasos**

1. **El job, diario:** `@Scheduled(cron = "0 0 3 * * *")`. Busca alumnos con 30 días o más de
   antigüedad que no tengan marcador para la campaña de plataforma del período vigente, y
   les crea el marcador en `PENDIENTE` (CA1).
2. **Idempotencia por dos vías (CA2):**
   - una tabla `catalogo.campania_ejecucion (periodo, dimension, ejecutado_en)` con
     `UNIQUE (periodo, dimension)`, que impide crear dos campañas del mismo período;
   - el `INSERT` de marcadores con `ON CONFLICT (alumno_id, instrumento_id) DO NOTHING`,
     que hace inocuo correr el job dos veces el mismo día.

   **Las dos**, porque protegen cosas distintas: la campaña y el marcador.
3. **Una por período (CA1):** el período (`2026-2C`) sale de configuración, no de la fecha
   calculada. Calcularlo a partir del mes es la clase de lógica que falla en enero.
4. **Sin `curso_cohorte_id` (CA3):** el `CHECK` de `HU24` ya lo impone en el catálogo; acá hay que
   asegurar que el marcador de plataforma también lo deja en `NULL`, y que `HU29` no lo copia a
   la respuesta.
5. **Un solo nodo:** si el servicio escala, dos instancias corren el job a la vez. Con la
   idempotencia del paso 2 no rompe nada, pero conviene un `SELECT ... FOR UPDATE SKIP LOCKED`
   sobre `campania_ejecucion` para que solo uno haga el trabajo.

**Tests que la cierran**
- `DisparoALos30DiasTest` — con reloj fijo: 29 días no dispara, 30 sí (CA1).
- `JobIdempotenteIT` — se ejecuta dos veces y se afirman los mismos conteos (CA2).
- `PlataformaSinCursoTest` (CA3).

**Listo cuando** correr el job cinco veces seguidas deja el mismo estado que correrlo una vez.

---

# E-07 · Respuesta anónima de encuesta

---

### ★ G04-HU29 — Almacén de respuestas sin marca temporal
`5 pts · Must` · módulo: encuestas / respuestas · **el corazón del anonimato**

**Antes de empezar**
`HU06` y `HU24` en verde. `pgcrypto` instalada.

**Archivos**

| Archivo | Qué es |
|---|---|
| `db/migration/respuestas/V2__respuesta_encuesta.sql` | la tabla |
| `encuestas/respuestas/RespuestaEncuesta.java` | entidad (ds: `dsRespuestas`) |
| `encuestas/respuestas/RespuestaEncuestaRepository.java` | |
| `test/…/anonimato/RespuestaSinTiempoTest.java` | el test del CA2 |

**Pasos**

1. **Migración. Lo importante de esta tabla es lo que no tiene:**
   ```sql
   CREATE TABLE respuestas.respuesta_encuesta (
     id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),   -- v4, NUNCA v7 ni ULID
     instrumento_id   uuid NOT NULL,
     curso_cohorte_id uuid NULL,
     dimension        varchar(16) NOT NULL,
     estrellas        int NULL CHECK (estrellas BETWEEN 1 AND 5),
     abstencion       boolean NOT NULL DEFAULT false,
     comentario       text NULL,
     periodo          varchar(16) NOT NULL,
     CHECK ((abstencion = true) = (estrellas IS NULL)),
     CHECK ((dimension = 'PLATAFORMA') = (curso_cohorte_id IS NULL))
   );
   ```
   **No hay `created_at`, no hay `alumno_id`, no hay FK, no hay secuencia.**
2. **Comentarios en la base**, para que la próxima persona no lo «arregle»:
   ```sql
   COMMENT ON COLUMN respuestas.respuesta_encuesta.id IS
     'gen_random_uuid() = UUIDv4. NO cambiar a v7 ni ULID: codifican el instante de creacion
      y reabren el canal de correlacion temporal. Ver docs/ANONIMATO.md y G04-HU08.';
   ```
3. **La entidad sin `@CreatedDate` ni `@EntityListeners(AuditingEntityListener.class)`,** y
   **sin habilitar `@EnableJpaAuditing` a nivel global** en la aplicación. Si se habilita para
   `teoricos`, hay que acotarlo a ese `EntityManagerFactory`, o Spring va a agregar la columna
   que estamos evitando.
4. **Sin `alumno_id` ni FK (CA3):** el repositorio no tiene ningún método que reciba un
   `alumnoId`; no compila si alguien lo intenta.
5. **`curso_cohorte_id` nulo en plataforma (Extras):** ya lo impone el `CHECK`. Un alumno cursa
   varias materias y no habría cuál elegir; además, cada dimensión extra en una fila anónima
   achica el conjunto de posibles autores.

**Tests que la cierran**
- `TablaExisteConUuidV4IT` — se insertan 100 filas y se afirma que el nibble de versión de cada
  UUID es `4` (CA1).
- `RespuestaSinTiempoTest` — `information_schema.columns` no devuelve ninguna columna de tipo
  `timestamp*` ni `date` (CA2).
- `SinAlumnoNiFkTest` — ninguna columna se llama `alumno_id` y `pg_constraint` no tiene FK
  saliendo de esta tabla (CA3).
- `PlataformaSinCursoTest` (Extras).

**Listo cuando** la tabla no tiene ninguna columna temporal ni ninguna referencia al alumno, y
un test lo afirma leyendo el catálogo del sistema, no el código.

**⚠ Trampa.** Hibernate con `@EnableJpaAuditing` global agrega `created_at` sin que nadie escriba
la columna, la migración `validate` falla y la reacción natural es «agregar la columna que falta».
Ese es el momento exacto en que se pierde el anonimato. Por eso el test del CA2 mira la base.

---

### ★ G04-HU30 — Escritura desacoplada de marcador y respuesta
`8 pts · Must` · módulo: encuestas · **canal 3 de correlación**

**Antes de empezar**
`HU25` y `HU29` en verde.

**Archivos**

| Archivo | Qué es |
|---|---|
| `encuestas/respuestas/ColaDeRespuestas.java` | la cola en memoria |
| `encuestas/respuestas/ConsumidorDeRespuestas.java` | el flusher con jitter |
| `encuestas/respuestas/web/RespuestaController.java` | el `POST` |
| `encuestas/EncuestaFachada.java` | orquesta marcador y encolado **sin** unir los módulos |

**Pasos**

1. **El problema de diseño primero.** El `POST` tiene que hacer dos cosas en dos módulos que no
   se pueden importar entre sí. La solución: una fachada en `encuestas/` (nivel padre) que
   depende de `cumplimiento` y de `respuestas`, pero **ninguno de los dos depende del otro**.
   Ajustar la regla de ArchUnit de `HU02` para permitir exactamente eso y nada más.
2. **El flujo del `POST`:**
   ```
   1. validar (HU31, HU32)
   2. @Transactional(dsCumplimiento) → marcarCumplida()   ← commit
   3. cola.offer(respuestaPendiente)                      ← en memoria, sin transacción
   4. responder 202 al alumno                             ← Extras: no espera
   ```
   Dos transacciones distintas en dos momentos distintos (CA1). El paso 3 **nunca** dentro de la
   transacción del paso 2.
3. **La cola:** `ArrayBlockingQueue<RespuestaPendiente>` con capacidad configurable (5000).
   Si se llena, `offer` devuelve `false` → log de error sin el body, métrica, y **el marcador
   igual quedó**: se pierde la respuesta, no el cumplimiento. Es la degradación correcta.
4. **El consumidor (CA2):**
   ```java
   @Scheduled(fixedDelayString = "${encuestas.flush.periodo-ms:5000}")
   public void flush() {
       List<RespuestaPendiente> lote = new ArrayList<>();
       cola.drainTo(lote, loteMaximo);            // p.ej. 50
       if (lote.isEmpty()) return;
       Collections.shuffle(lote, random);          // rompe el orden dentro del lote
       Thread.sleep(random.nextInt(jitterMaxMs));  // rompe el alineamiento entre lotes
       repository.saveAll(lote);
   }
   ```
   **Las tres piezas son necesarias y hacen cosas distintas:** el `shuffle` rompe el orden dentro
   del lote, el jitter rompe la correspondencia entre el instante del marcador y el de la
   respuesta, y el lote hace que las fronteras no coincidan con las de llegada.
5. **`saveAll` sin `flush` por fila:** con `hibernate.jdbc.batch_size` configurado, el `INSERT`
   sale como lote y el orden físico dentro del lote depende del driver, lo que suma ruido.
6. **El riesgo, dicho:** una cola en memoria pierde lo encolado si el proceso muere. Con el
   volumen de una cursada es aceptable y es lo que hacemos en el Sprint 1. La alternativa
   —una tabla *outbox*— reintroduce un orden de inserción persistido, que es justo el canal que
   estamos cerrando. **Anotarlo en `docs/ANONIMATO.md`** como decisión consciente, no como olvido.

**Tests que la cierran**
- `DosTransaccionesDistintasIT` — se afirma que el marcador está commiteado mientras la respuesta
  todavía no existe (CA1).
- `OrdenNoSeReproduceIT` — 50 respuestas concurrentes; se calcula la correlación de Spearman
  entre el orden de marcadores y el orden físico de las respuestas y se afirma `|ρ| < 0.5` (CA2, CA3).
  Correrlo con semilla fija para que no sea intermitente.
- `PostRespondeSinEsperarTest` — el `POST` devuelve 202 con la cola no vacía (Extras).

**Listo cuando** con 50 respuestas concurrentes la correlación entre los dos órdenes es
estadísticamente indistinguible de cero.

**⚠ Trampa.** Un test que compara los órdenes par por par va a fallar de forma intermitente:
por azar, algunos pares van a coincidir siempre. Medir la correlación del conjunto, no las
coincidencias individuales.

---

### ★ G04-HU31 — Respuesta con abstención explícita
`3 pts · Must` · módulo: encuestas / respuestas · **RF-ENC-09**

**Antes de empezar**
`HU30` en verde.

**Archivos**

| Archivo | Qué es |
|---|---|
| `encuestas/respuestas/web/dto/ResponderRequest.java` | con la validación cruzada |
| `encuestas/respuestas/web/RespuestaController.java` | `POST /encuestas/{instrumento}/respuesta` |
| `comun/config/MetricasConfig.java` | contador de abstenciones |

**Pasos**

1. **El DTO con validación de clase, no de campo:**
   ```java
   public record ResponderRequest(Integer estrellas, boolean abstencion, String comentario) { }
   ```
   La regla «`abstencion` XOR `estrellas`» es cruzada, así que va en un
   `@AbstencionCoherente` (`ConstraintValidator` a nivel de clase) que devuelve el `campo`
   correcto en el error (CA3). Es el mismo `CHECK` que ya está en la tabla de `HU29`: doble
   defensa, y el 400 llega antes de tocar la base.
2. **Persistencia (CA1):** `abstencion: true` → fila con `estrellas: null`, `abstencion: true`.
   La abstención **es una respuesta**, no una ausencia de fila: si no se guardara, no se podría
   calcular la tasa.
3. **Cumplimiento igual (CA2):** la fachada de `HU30` llama a `marcarCumplida` en los dos casos.
   No hay una rama distinta — y eso es lo que garantiza que el marcador no sepa qué respondió.
4. **Anotar con `@RequiereEscritorio`** (`HU05`).
5. **Métrica y KPI separado (Extras):** `meterRegistry.counter("encuestas.abstenciones")`, y
   en `HU34` la tasa se informa como indicador propio. **Las abstenciones no entran en el
   denominador del promedio** — eso se implementa en `HU34`, pero se decide acá.

**Tests que la cierran**
- `AbstencionSePersisteTest` (CA1).
- `AbstencionMarcaCumplimientoIT` (CA2).
- `AbstencionConEstrellasRechazadaTest` — 400, clave i18n, `campo` nombrado (CA3).

**Listo cuando** abstenerse cumple la encuesta, deja fila, y no ensucia el promedio.

---

### ★ G04-HU32 — Comentario obligatorio en los extremos de la escala
`3 pts · Must` · módulo: encuestas / respuestas · **RF-ENC-05**

**Antes de empezar**
`HU31` en verde.

**Archivos**

| Archivo | Qué es |
|---|---|
| `encuestas/respuestas/ComentarioRequeridoValidator.java` | la regla |
| `resources/messages_*.properties` | `error.comentario.requerido`, `error.comentario.corto` |
| `application.yml` | `encuestas.comentario.longitud-minima: 15` |

**Pasos**

1. **La regla, completa:**

   | Caso | Comentario |
   |---|---|
   | 1 estrella | obligatorio |
   | 2, 3, 4 estrellas | opcional |
   | 5 estrellas | obligatorio |
   | abstención | opcional (Extras) |

   Un 1 y un 5 son los datos accionables de la encuesta y sin el porqué no sirven de nada;
   un 3 sin comentario es información suficiente.
2. **Longitud mínima configurable (CA3):** 15 caracteres **después de `trim()`**, y rechazando
   cadenas de un solo carácter repetido (`"..............."` tiene 15 y no dice nada).
   Una regex `^(?!(.)\1+$).{15,}$` sobre el texto trimmed lo cubre.
3. **Dos claves distintas (CA1):** «falta el comentario» y «el comentario es demasiado corto» son
   errores diferentes para quien está respondiendo. Ambos con `campo: "comentario"`.
4. **Validador de clase**, junto al de `HU31`, porque la regla depende de `estrellas` y de
   `abstencion` a la vez.
5. **Longitud máxima también:** un `text` sin tope invita a que alguien pegue un libro.
   `encuestas.comentario.longitud-maxima: 2000`, y `CHECK` correspondiente en la tabla.

**Tests que la cierran**
- `ExtremosSinComentarioRechazadosTest` — parametrizado con 1 y 5 (CA1).
- `MediosSinComentarioAceptadosTest` — parametrizado con 2, 3, 4, con y sin comentario (CA2).
- `ComentarioDemasiadoCortoTest` — un carácter, 14 caracteres, y 15 caracteres repetidos (CA3).
- `AbstencionSinComentarioAceptadaTest` (Extras).

**Listo cuando** los extremos exigen un porqué con sustancia y el medio no molesta a nadie.

---

### ▸ G04-HU33 — Moderación asincrónica del comentario
`5 pts · Should` · módulo: encuestas / respuestas · **D-15, revisión de D-05 con Kafka**

**Antes de empezar**
`HU32` y `HU37` en verde. **Dependencia externa con el Tema 11** para el contrato del tópico
`sistema.moderacion`; mientras no exista, se trabaja con el stub.

**Archivos**

| Archivo | Qué es |
|---|---|
| `db/migration/respuestas/V3__estado_moderacion.sql` | columna `estado_moderacion` |
| `encuestas/respuestas/moderacion/PuertoDeModeracion.java` | el puerto |
| `encuestas/respuestas/moderacion/StubDeModeracion.java` | aprueba todo, tras flag |
| `encuestas/respuestas/moderacion/ConsumidorDeModeracion.java` | `@KafkaListener`, para después |

**Pasos**

1. **Columna de estado (CA1):**
   ```sql
   ALTER TABLE respuestas.respuesta_encuesta
     ADD COLUMN estado_moderacion varchar(16) NOT NULL DEFAULT 'APROBADO'
       CHECK (estado_moderacion IN ('PENDIENTE','APROBADO','RECHAZADO'));
   ```
   `APROBADO` por defecto para que las filas sin comentario no queden trabadas. Solo las que
   traen texto nacen en `PENDIENTE`.
2. **El puerto, asincrónico como el de corrección:**
   ```java
   public interface PuertoDeModeracion { void solicitar(UUID respuestaId, String texto); }
   ```
   El resultado vuelve por `ResultadoDeModeracionHandler.resolver(respuestaId, aprobado)`.
   Misma forma que `HU19`: es el patrón del proyecto para toda dependencia externa que puede
   tardar.
3. **Stub tras flag (CA2):** `moderacion.stub.enabled`, **`false` en el perfil `prod`** y `true`
   en `local` y `test`. Con un `TODO` visible en la clase que nombre al Tema 11.
   Cuando el flag está apagado y no hay adaptador real, la respuesta queda en `PENDIENTE`:
   eso es correcto, no es un bug.
4. **Publicación en `sistema.moderacion` (CA3):** el adaptador real publica una solicitud con el
   envelope de `HU37` y un `@KafkaListener` con nuestro `groupId` recibe el veredicto.
   El código que llama al puerto no distingue uno de otro.
5. **Rechazo conserva la nota (Extras):**
   ```sql
   UPDATE respuestas.respuesta_encuesta
      SET comentario = NULL, estado_moderacion = 'RECHAZADO' WHERE id = ?;
   ```
   Se descarta el texto, **no la fila**. Las estrellas son un dato legítimo que no se modera.
6. **Consecuencia para los KPIs, que hay que implementar en `HU34`:** un comentario en
   `PENDIENTE` no aparece en los agregados; el puntaje sí cuenta desde el primer momento.
   Dejarlo escrito en las dos historias.

**Tests que la cierran**
- `ComentarioNaceEnPendienteIT` (CA1).
- `StubApruebaTodoTest` y `StubApagadoEnProdTest` (CA2).
- `AdaptadorIntercambiableTest` — se sustituye el stub por un adaptador de Kafka falso sin tocar
  el llamador (CA3).
- `RechazoConservaEstrellasIT` (Extras).

**Listo cuando** un comentario no se expone hasta ser aprobado y un rechazo no borra el puntaje.

---
# E-08 · KPIs y umbral de publicación

---

### ★ G04-HU34 — CSAT por curso con su desglose
`5 pts · Must` · módulo: encuestas / kpi · **KPI-01, KPI-02**

**Antes de empezar**
`HU29`, `HU31` y `HU32` en verde (hace falta que existan respuestas con estrellas, abstenciones
y comentarios para que los indicadores tengan sentido).

**Archivos**

| Archivo | Qué es |
|---|---|
| `encuestas/kpi/KpiService.java` | los cuatro indicadores |
| `encuestas/kpi/AgregadoRepository.java` | la consulta (ds: `dsRespuestas`) |
| `encuestas/kpi/web/KpiController.java` | `GET /encuestas/kpi` |
| `encuestas/kpi/web/dto/KpiResponse.java` | |
| `docs/KPIS.md` | las definiciones, para el OpenAPI |

**Pasos**

1. **Fijar las definiciones antes de escribir SQL.** Sin esto, cada integrante calcula distinto
   y el Tema 12 recibe números que no cierran:

   | Indicador | Definición |
   |---|---|
   | `csat` | promedio de `estrellas`, **excluyendo abstenciones**, redondeado a dos decimales |
   | `porcentajeSatisfechos` | respuestas con 4 o 5 sobre el total **con puntaje** |
   | `porcentajeDetractores` | respuestas con 1 o 2 sobre el total **con puntaje** |
   | `tasaAbstencion` | abstenciones sobre el total de respuestas, **incluyéndolas** |
   | `n` | total de respuestas, abstenciones incluidas |

   Los tres primeros tienen el mismo denominador; la tasa de abstención tiene otro. Es
   deliberado y va documentado, porque es exactamente donde alguien se va a equivocar.
2. **Una sola consulta agregada,** no traer filas a memoria:
   ```sql
   SELECT count(*)                                                   AS n,
          count(*) FILTER (WHERE NOT abstencion)                      AS n_con_puntaje,
          avg(estrellas) FILTER (WHERE NOT abstencion)                AS csat,
          count(*) FILTER (WHERE estrellas >= 4)                      AS satisfechos,
          count(*) FILTER (WHERE estrellas <= 2)                      AS detractores,
          count(*) FILTER (WHERE abstencion)                          AS abstenciones
     FROM respuestas.respuesta_encuesta
    WHERE curso_cohorte_id = ? AND periodo = ? AND dimension <> 'PLATAFORMA';
   ```
   Con `dsRespuestas`, que no tiene acceso a `cumplimiento`: el aislamiento hace que este
   endpoint no *pueda* filtrar por alumno aunque alguien lo intentara.
3. **Abstención como indicador propio (CA2):** ya está en el paso 1. Verificarlo con un test que
   compare el CSAT de un conjunto con y sin abstenciones y afirme que es idéntico.
4. **Solo agregados (CA3):** `KpiResponse` no tiene ninguna colección de respuestas individuales.
   Los comentarios, cuando se expongan (backlog posterior), van en un endpoint aparte y también
   bajo umbral.
5. **Comentarios pendientes de moderación (de `HU33`):** el conteo de comentarios, si se agrega,
   filtra `estado_moderacion = 'APROBADO'`. Las estrellas cuentan siempre.
6. **Autorización:** el profesor solo consulta sus cursos, resuelto contra el Tema 02 con el
   cliente de `HU40`. El Tema 12 entra con su propio rol de servicio.
7. **Documentar en el OpenAPI (Extras):** las definiciones del paso 1 como `description` de cada
   campo del esquema. No solo en `docs/KPIS.md`, porque el Tema 12 va a leer el OpenAPI.

**Tests que la cierran**
- `IndicadoresConDatosConocidosTest` — un conjunto armado a mano con los cuatro valores esperados
  calculados aparte (CA1).
- `AbstencionNoAlteraPromedioTest` (CA2).
- `KpiNoDevuelveIndividualesTest` — se afirma que el JSON no contiene ningún campo de tipo array
  con contenido de respuesta (CA3).

**Listo cuando** los cuatro indicadores dan los valores calculados a mano sobre un conjunto conocido.

**⚠ Trampa.** `avg(estrellas)` en PostgreSQL ignora los `NULL` por sí solo, así que el `FILTER`
del CSAT parece redundante. **Dejarlo igual**, explícito: el día que `estrellas` deje de ser
nulo en las abstenciones, el promedio cambia solo y nadie se entera.

---

### ▸ G04-HU35 — Consolidado de plataforma por período
`3 pts · Should` · módulo: encuestas / kpi

**Antes de empezar**
`HU34` en verde. Es la misma maquinaria con otro filtro.

**Archivos**

| Archivo | Qué es |
|---|---|
| `encuestas/kpi/web/KpiController.java` | `GET /encuestas/kpi/plataforma` |
| `encuestas/kpi/KpiService.java` | `consolidadoDePlataforma(periodo)` |

**Pasos**

1. **Reutilizar la consulta de `HU34`** cambiando el `WHERE` a
   `dimension = 'PLATAFORMA' AND periodo = ?`. Extraer el agregado a un método común: son los
   mismos cinco indicadores y no deben poder divergir (CA1).
2. **Sin desagregación por curso (CA2):** el endpoint **no acepta** un parámetro `curso`.
   No es que lo ignore: no está en la firma. La respuesta de plataforma no lleva curso por diseño
   (`HU29`), así que desagregar es imposible, y el contrato lo tiene que reflejar.
3. **Solo ADMIN (CA3):** `@PreAuthorize("hasRole('ADMIN')")` más un test de acceso denegado para
   profesor y para alumno, como pide `HU43`.
4. **El umbral de `HU36` también aplica acá.** Es fácil olvidarlo porque «es el consolidado
   global y siempre hay muchas respuestas». En el primer período no las hay.

**Tests que la cierran**
- `ConsolidadoDePlataformaTest` (CA1).
- `SinParametroCursoTest` — un `?curso=...` no cambia el resultado (CA2).
- `SoloAdminTest` — profesor y alumno reciben 403 (CA3).
- `ConsolidadoBajoUmbralTest` (paso 4).

**Listo cuando** devuelve los mismos cinco indicadores sobre la dimensión plataforma, sin
desagregar y con el umbral aplicado.

---

### ★ G04-HU36 — Umbral mínimo de publicación
`3 pts · Must` · módulo: encuestas / kpi · **PAR-18, mitigación del residual de `HU09`**

**Antes de empezar**
`HU34` en verde. `HU35` también, si ya existe: el umbral se aplica en los dos.

**Archivos**

| Archivo | Qué es |
|---|---|
| `encuestas/kpi/UmbralDePublicacion.java` | la decisión, en un solo lugar |
| `encuestas/kpi/web/dto/KpiResponse.java` | se parte en dos formas |
| `application.yml` | `encuestas.kpi.umbral-minimo: 5` |
| `test/…/kpi/UmbralArchTest.java` | la regla que impide saltearlo |

**Pasos**

1. **Dos formas de respuesta, no campos nulos:**
   ```java
   public sealed interface KpiResponse permits KpiCompleto, KpiBajoUmbral { }
   public record KpiBajoUmbral(int n, String clave) implements KpiResponse { }
   ```
   Con `clave = "encuestas.kpi.muestra.insuficiente"`. **`KpiBajoUmbral` no tiene ningún campo
   de promedio** (CA2): la garantía la da el tipo, igual que con `criterio` en `HU15`.
   Un DTO con `csat: null` invita a que alguien lo complete «porque queda feo».
2. **La decisión, en un solo lugar (CA3):**
   ```java
   public KpiResponse aplicar(int n, Supplier<KpiCompleto> completo) {
       return n < umbral ? new KpiBajoUmbral(n, CLAVE) : completo.get();
   }
   ```
   Todo endpoint de KPI pasa por acá. **Sin excepción por rol** — el ADMIN tampoco lo evade,
   porque el umbral protege al respondente, no al dato.
3. **Configurable hacia arriba, nunca desactivable (Extras):** validar al arrancar que
   `umbral-minimo >= 5` y fallar el contexto si no. Un `@Min(5)` sobre la propiedad.
   Que no exista el valor `0`.
4. **Regla de arquitectura (paso clave):** ningún método de `..encuestas.kpi.web..` puede devolver
   `KpiCompleto` directamente; todos devuelven `KpiResponse`. ArchUnit lo verifica, y así un
   endpoint de KPI nuevo no puede saltearse el umbral por olvido.
5. **Enlazar con `HU09`:** en el javadoc de `UmbralDePublicacion`, nombrar el residual que mitiga
   y apuntar a `docs/ANONIMATO.md`. La clase existe por ese motivo y conviene que se lea.

**Tests que la cierran**
- `BajoUmbralSoloConteoTest` — con 4 respuestas: la respuesta tiene exactamente dos campos (CA1).
- `BajoUmbralSinNingunDerivadoTest` — se afirma que el JSON no contiene «csat», «porcentaje»,
  «comentario» ni «promedio» (CA2).
- `AdminNoEvadeElUmbralTest` (CA3).
- `UmbralMenorA5NoArrancaTest` y `UmbralArchTest` (Extras, paso 4).

**Listo cuando** con cuatro respuestas no sale ni un número derivado, para nadie.

---

# E-09 · Integración con los otros temas

---

### ▸ G04-HU37 — Productor de Kafka con el envelope estándar
`5 pts · Must` · módulo: comun · **contrato de plataforma**

**Antes de empezar**
`HU03` con Kafka en el compose. Es una de las primeras historias transversales que conviene
tener: `HU21`, `HU33`, `HU38` y `HU39` dependen de ella.

**Archivos**

| Archivo | Qué es |
|---|---|
| `comun/eventos/EventoSobre.java` | el envelope |
| `comun/eventos/PublicadorDeEventos.java` | el puerto |
| `comun/eventos/PublicadorKafka.java` | adaptador real |
| `comun/eventos/PublicadorEnMemoria.java` | adaptador de test |
| `comun/config/KafkaConfig.java` | serializador, tópicos, `groupId` |

**Pasos**

1. **El envelope, exacto (CA1).** Es el contrato de toda la plataforma; no admite variantes:
   ```java
   public record EventoSobre(
       UUID   eventId,
       String eventType,
       String timestamp,   // ISO 8601 UTC, con 'Z'
       String producer,    // siempre "tema-04-teoricos-encuestas"
       Object payload) { }
   ```
   `timestamp` como `String` y no como `Instant` para que la serialización no dependa de la
   configuración de Jackson de cada consumidor. Formatearlo con
   `DateTimeFormatter.ISO_INSTANT` sobre `Instant.now()` — siempre UTC.
2. **`producer` fijado en constante,** no configurable. Si es una propiedad, alguien lo cambia
   en un `.env` y el consumidor deja de reconocernos.
3. **`eventId` estable en la republicación (CA2).** Es el CA más fácil de leer mal: **no** es un
   UUID nuevo por llamada a `publicar`, es un UUID por **hecho**. Para `TEORICO_CORREGIDO`,
   derivarlo determinísticamente:
   ```java
   UUID eventId = UUID.nameUUIDFromBytes(
       (eventType + ":" + desafioId + ":" + alumnoId + ":" + intento).getBytes(UTF_8));
   ```
   Así, si republicamos el mismo hecho tras una caída, el consumidor puede deduplicar.
   Documentar la regla de derivación en el OpenAPI, porque el Tema 03 la va a necesitar.
4. **El puerto y los dos adaptadores (CA3):**
   ```java
   public interface PublicadorDeEventos { void publicar(String topico, String clave, EventoSobre evento); }
   ```
   - `PublicadorKafka` — `@Profile("!test")`, con `KafkaTemplate<String,String>` y el JSON
     serializado con un `ObjectMapper` propio configurado para no omitir nulos.
   - `PublicadorEnMemoria` — `@Profile("test")`, guarda en una `List` consultable. Es lo que
     permite que `HU21`, `HU38` y `HU39` se testeen sin broker.
5. **Clave de partición:** para `TEORICO_CORREGIDO`, `desafioId`; para los de encuesta,
   `instrumentoId`. Así los eventos del mismo desafío llegan ordenados al Tema 03.
6. **Confiabilidad mínima:** `acks=all`, `enable.idempotence=true`, `retries` alto.
   Sin esto, un rebalanceo del broker pierde una corrección y el alumno se queda sin XP.

**Tests que la cierran**
- `EnvelopeCompletoTest` — se afirman los cinco campos, `producer` exacto y `timestamp` matcheando
  `^\d{4}-\d{2}-\d{2}T.*Z$` (CA1).
- `EventIdEstablePorHechoTest` — dos publicaciones del mismo hecho dan el mismo `eventId`; dos
  hechos distintos, distinto (CA2).
- `AdaptadorEnMemoriaTest` — la publicación funciona sin broker y sin tocar el llamador (CA3).
- `PublicacionRealIT` — con Testcontainers Kafka, un consumidor de prueba deserializa el envelope
  sin ajustes. **Este es el que demuestra que el contrato sirve para otro equipo.**

**Listo cuando** un consumidor externo puede leer nuestro evento sin que le expliquemos nada.

---

### ▸ G04-HU38 — Publicación de `TEORICO_CORREGIDO`
`3 pts · Must` · módulo: teoricos · **D-14: va al Tema 03, no al 10**

**Antes de empezar**
`HU21` y `HU37` en verde.

**Archivos**

| Archivo | Qué es |
|---|---|
| `teoricos/infraestructura/eventos/PublicadorDeCorrecciones.java` | el listener de `AFTER_COMMIT` |
| `teoricos/infraestructura/eventos/dto/TeoricoCorregidoPayload.java` | los seis campos |

**Pasos**

1. **El payload, exactamente los seis campos del contrato (CA1):**
   ```java
   record TeoricoCorregidoPayload(UUID desafioId, UUID alumnoId, int intento,
                                  int nota, boolean aprobado, String estado) { }
   ```
   `desafioId` es **el del Tema 03**, no nuestro `desafio_teorico.id`. Es el error de integración
   más probable de todo el sprint: nosotros trabajamos con el nuestro y el 03 no lo conoce.
2. **Tópico `desafios.resultados`,** clave de partición `desafioId`.
3. **Solo en la transición a `FINAL` (CA2):** engancharse al evento de dominio de `HU21` con
   `@TransactionalEventListener(phase = AFTER_COMMIT)`. No hay otro punto de publicación.
4. **Un evento por intento (CA3):** el `eventId` derivado incluye `intento` (paso 3 de `HU37`),
   así que el reintento produce un `eventId` distinto y el anterior sigue siendo válido.
5. **Recordar el riesgo a los otros equipos.** El Tema 10 y el 11 escuchan el mismo tópico y
   tienen que **filtrar por `eventType`**: si alguno consume el tópico entero, un intento aprobado
   suma XP dos veces —por nuestro evento y por el `DESAFIO_RESUELTO` del 03—. Es el punto que
   `HU42` tiene que dejar acordado por escrito, con el 03, el 10 y el 11 presentes.

**Tests que la cierran**
- `PayloadConLosSeisCamposTest` — y se afirma que `desafioId` es el del Tema 03, no el interno (CA1).
- `NoSePublicaEnPendienteTest` — reutilizado de `HU21` (CA2).
- `ReintentoPublicaEventoPropioTest` — dos intentos, dos `eventId` distintos (CA3).

**Listo cuando** el Tema 03 recibe un evento por corrección finalizada, con su `desafioId`.

---

### ▸ G04-HU39 — Publicación de los eventos de encuesta
`3 pts · Must` · módulo: encuestas · **el tópico está pendiente de acuerdo (S-11)**

**Antes de empezar**
`HU25` y `HU37` en verde. **El tópico no está acordado:** ninguno de los cuatro existentes
corresponde al cumplimiento de encuestas. Proponemos `encuestas.cumplimiento`.

**Archivos**

| Archivo | Qué es |
|---|---|
| `encuestas/cumplimiento/eventos/PublicadorDeCumplimiento.java` | |
| `encuestas/kpi/eventos/PublicadorDeCierre.java` | |
| `application.yml` | `encuestas.eventos.topico: encuestas.cumplimiento` |

**Pasos**

1. **Tópico por configuración,** no constante. Es lo único que va a cambiar cuando la integración
   decida, y así el cambio es una variable de entorno.
2. **`ENCUESTA_CUMPLIDA` (CA1):** payload `{alumnoId, instrumentoId, cursoCohorteId}`.
   Se publica desde `cumplimiento`, con `@TransactionalEventListener(AFTER_COMMIT)` sobre el
   marcado. **Nunca desde `respuestas`** — ese módulo no conoce al alumno y no debe conocerlo.
3. **`ENCUESTA_PERIODO_CERRADO` (CA2):** payload `{instrumentoId, cursoCohorteId, nRespuestas}`.
   Se dispara al pasar `abierto_hasta`, desde un job diario junto al de `HU28`.
   `nRespuestas` es el conteo, que es público: el umbral de `HU36` restringe los derivados,
   no el `n`.
4. **Ningún contenido de respuesta (CA3), y este es el test que importa:** un test que serialice
   los dos payloads y afirme que el string **no contiene** `estrellas`, `comentario`,
   `abstencion` ni `csat`. Sobre el JSON serializado, no sobre los campos del record: si mañana
   alguien anida un objeto, el test lo ve igual.
5. **El residual, dicho otra vez donde corresponde.** `ENCUESTA_CUMPLIDA` lleva `alumnoId` y
   **Kafka retiene los mensajes**: anonimizar nuestra base a los cinco años (`HU44`) no borra lo
   que quedó en el bus. O se acuerda retención por tópico, o queda declarado.
   Va en `docs/ANONIMATO.md` y en la agenda de `HU42`.

**Tests que la cierran**
- `EncuestaCumplidaPayloadTest` (CA1).
- `PeriodoCerradoPayloadTest` (CA2).
- `EventosSinContenidoDeRespuestaTest` (CA3).

**Listo cuando** los dos eventos salen y ninguno transporta una estrella ni una letra de comentario.

---

### ▸ G04-HU40 — Cliente resiliente hacia el Tema 02
`5 pts · Must` · módulo: comun · **S-07**

**Antes de empezar**
Nada interno. **Depende del Tema 02**; mientras no exista, se implementa contra el contrato
acordado y se testea con un servidor simulado.

**Archivos**

| Archivo | Qué es |
|---|---|
| `comun/cliente/ClienteTema02.java` | la interfaz |
| `comun/cliente/ClienteTema02Http.java` | `RestClient` con timeout |
| `comun/cliente/PoliticaAnteFalla.java` | qué hacer cuando no responde |
| `comun/config/ClientesConfig.java` | timeouts, resiliencia |

**Pasos**

1. **Timeouts explícitos (CA1):** conexión 2 s, lectura 3 s, por configuración.
   Nunca los del `RestClient` por defecto, que son infinitos: una llamada colgada al Tema 02
   agota el pool de hilos y nos tira el servicio entero por un problema ajeno.
2. **Decidir la política ante falla, por caso de uso (CA2).** Esto es una decisión de producto,
   no un detalle técnico:

   | Consulta | Ante timeout | Por qué |
   |---|---|---|
   | Pertenencia a cohorte para leer un desafío (`HU15`) | **denegar** (403) | fail-closed: mostrar un examen a quien no corresponde es peor que un error transitorio |
   | Cursos que dicta el profesor (`HU20`, `HU34`) | **denegar** | mismo criterio |
   | Fecha de alta del alumno (`HU28`) | **reintentar mañana** | es un job, no hay usuario esperando |

   Escribirlo en `docs/DECISIONES.md`, no solo en el código.
3. **Circuit breaker** con Resilience4j: si el Tema 02 falla sostenidamente, abrir el circuito y
   responder rápido en vez de acumular hilos esperando. `slidingWindow` de 20 llamadas,
   umbral de 50 %.
4. **Caché corta** de pertenencia a cohorte —60 s, Caffeine—. La pertenencia no cambia en un
   minuto y esto baja mucho el acoplamiento en el flujo de lectura.
5. **Métricas y alerta (CA3):** contador `integracion.tema02.fallas` con etiqueta por tipo
   (timeout, 5xx, circuito abierto), y un `HealthIndicator` que se degrada si el circuito lleva
   más de N minutos abierto. La alerta la configura la cátedra si hay tablero; si no, el health
   degradado alcanza.

**Tests que la cierran**
- `TimeoutConfiguradoTest` (CA1).
- `PoliticaAnteTimeoutIT` — con un `MockWebServer` que demora más que el timeout, un caso por
  cada fila de la tabla del paso 2 (CA2).
- `FallasCuentanMetricaTest` y `CircuitoAbiertoDegradaHealthTest` (CA3).

**Listo cuando** el Tema 02 caído nos degrada de forma prevista y probada, y no nos arrastra.

---

### ▸ G04-HU41 — Publicación del contrato OpenAPI
`3 pts · Must` · módulo: comun

**Antes de empezar**
Las historias de `E-04` a `E-08` en verde: el contrato documenta lo que existe, no lo que se
planea.

**Archivos**

| Archivo | Qué es |
|---|---|
| `comun/config/OpenApiConfig.java` | metadatos, servidores, seguridad |
| `docs/openapi.yaml` | el archivo generado, versionado en el repo |
| `.github/workflows/ci.yml` | el paso que lo regenera y detecta drift |

**Pasos**

1. **`springdoc-openapi`** con la UI en `/swagger-ui.html` y el JSON en `/v3/api-docs`.
   Agrupar por módulo con `GroupedOpenApi`: `teoricos` y `encuestas` como dos grupos, para que
   se lea la separación (CA1).
2. **Documentar `PENDIENTE` como respuesta normal (CA2).** Con un `@Schema` y un `example`
   explícito:
   ```yaml
   estado: PENDIENTE
   nota: null
   detalle: [ { itemVersionId: "...", puntaje: 10, obtenido: 8 } ]
   ```
   Y una `description` que diga, con todas las letras, que **es un 200 y no una condición de
   error**, y que el Tema 03 tiene que saber convivir con ella. Es el mismo patrón que el PRD
   define para el score de IA diferido (RF-IA-27), y conviene citarlo ahí.
3. **Documentar la respuesta bajo umbral (CA3)** con `oneOf` entre `KpiCompleto` y `KpiBajoUmbral`,
   y una `description` que explique el porqué —PAR-18— y no solo el qué.
4. **Documentar también** el envelope de los tres eventos que publicamos, con su tópico, su
   `eventType`, su `payload` y la regla de derivación del `eventId`. El OpenAPI no cubre eventos,
   así que va como una sección de `docs/CONTRATOS.md` enlazada desde la descripción del servicio.
5. **Versionar y detectar drift:** un paso de CI que regenera el YAML y falla si difiere del
   commiteado. Así el contrato no se desactualiza en silencio, que es la forma normal en que
   estos documentos mueren.
6. **Avisar a los otros grupos** con el enlace y un resumen de cinco líneas de lo que consumen
   de nosotros. Un OpenAPI que nadie sabe que existe no cumple el CA1.

**Tests que la cierran**
- `OpenApiTieneLosDosGruposIT` (CA1).
- `PendienteDocumentadoTest` — se busca el `example` en el JSON generado (CA2).
- `BajoUmbralDocumentadoTest` (CA3).
- El paso de drift del CI (paso 5).

**Listo cuando** otro grupo puede integrarse leyendo el OpenAPI, sin preguntarnos nada.

---

### ▸ G04-HU42 — Cierre de los supuestos en la sesión de integración
`2 pts · Must` · módulo: — · **no es técnica, y es de las más importantes**

**Antes de empezar**
Nada técnico. Requiere que la sesión exista y que estén el 03, el 10, el 11 y el 02.

**Archivos**

| Archivo | Qué es |
|---|---|
| `PREGUNTAS-INTEGRACION-G04.md` | se actualiza con las respuestas |
| `DISENIO-G04-SPRINT1.md` | se ajusta la sección 5 |
| `docs/ACTA-INTEGRACION.md` | quién respondió qué, con fecha |

**Pasos**

1. **Repasar los once supuestos** antes de la sesión y marcar los tres que más nos exponen:

   | # | Supuesto | Si sale al revés |
   |---|---|---|
   | S-01 | el archivado del curso no espera encuestas pendientes | un alumno que abandonó traba el cierre del curso para siempre. **El de mayor impacto** |
   | S-09 / S-10 | el 03 consolida y el 10/11 filtran por `eventType` | XP duplicada por cada intento aprobado, y aparece recién en la demo |
   | S-11 | hace falta un tópico para los eventos de encuesta | `HU39` no tiene dónde publicar |

2. **Llevar una pregunta por supuesto, cerrada.** No «¿cómo lo ven?», sino «asumimos X, ¿confirman?».
   Una pregunta abierta en una reunión de doce grupos no se responde.
3. **Registrar quién respondió (CA2).** `docs/ACTA-INTEGRACION.md` con una fila por supuesto:
   número, texto, respuesta, grupo que respondió, fecha. La constancia importa cuando en el
   sprint 3 alguien diga que nunca se acordó.
4. **Actualizar el diseño y ajustar las historias afectadas (CA3)** el mismo día. Un acuerdo que
   no baja al backlog en 24 horas se pierde.
5. **Las tres preguntas nuevas que no estaban en el archivo original** y hay que agregar:
   el ruteo por el Tema 03, el filtrado por `eventType`, y el tópico de encuestas.
   Sumar la **retención de Kafka frente al derecho de supresión** (`HU39`, `HU44`): sospechamos
   que afecta a todos los temas que publiquen datos personales, así que conviene plantearlo como
   problema de plataforma y no como duda nuestra.

**Tests que la cierran** — ninguno. El entregable es el acta.

**Listo cuando** los once supuestos están confirmados o corregidos, con nombre y fecha, y el
diseño refleja lo acordado.

---

# E-10 · Privacidad, autorización y normativa

---

### ▸ G04-HU43 — Autorización por rol y por pertenencia
`5 pts · Must` · módulo: transversal · **RF-USR-07/08**

**Antes de empezar**
`HU11` y `HU16` en verde. `HU40` para la pertenencia. Tema 01 como fuente de identidad.

**Archivos**

| Archivo | Qué es |
|---|---|
| `comun/config/SeguridadConfig.java` | resource server JWT |
| `comun/seguridad/IdentidadActual.java` | `alumnoId()`, `profesorId()`, `roles()` |
| `comun/seguridad/PertenenciaService.java` | las reglas de pertenencia |
| `test/…/seguridad/AccesoDenegadoSuite.java` | **un test por regla** |

**Pasos**

1. **Resource server con el JWT del Tema 01.** Validar firma, `exp` e `iss`. El `sub` es el
   id de usuario y los roles vienen en un claim; acordar cuál con el Tema 01 —si no está,
   asumir `roles` y documentarlo como supuesto.
2. **`IdentidadActual` como único acceso al contexto.** Ningún controlador lee el
   `SecurityContextHolder` directamente. Una regla de ArchUnit lo impone, y eso hace que el día
   que cambie el claim se toque un solo archivo.
3. **Las reglas, enumeradas (CA1, CA2):**

   | Regla | Dónde se aplica |
   |---|---|
   | el profesor solo ve su banco | `HU11`, filtro por `profesor_id` del token |
   | el profesor solo corrige sus cursos | `HU20`, contra el Tema 02 |
   | el profesor solo ve KPIs de sus cursos | `HU34` |
   | el alumno solo lee desafíos de su cohorte | `HU15` |
   | el alumno solo ve sus respuestas y correcciones | `HU16` y la lectura de corrección |
   | el alumno solo consulta sus encuestas pendientes | `HU26` |
   | solo ADMIN ve el consolidado de plataforma | `HU35` |
   | solo ADMIN dispara la anonimización | `HU44` |

4. **Pertenencia distinta de rol.** Tener rol `PROFESOR` no da acceso al curso de otro profesor.
   La segunda verificación va en el servicio, con los datos del Tema 02, y es la que más se olvida.
5. **Un test de acceso denegado por cada regla (CA3).** Ocho reglas, dieciséis tests: permitido y
   denegado. Agruparlos en una suite con nombre visible, porque es la evidencia de la épica.
   Afirmar además que la respuesta 403 **no filtra existencia**: mismo cuerpo vacío tanto si el
   recurso existe como si no.

**Tests que la cierran** — `AccesoDenegadoSuite`, con los dieciséis casos.

**Listo cuando** cada una de las ocho reglas tiene su par de tests y ninguna 403 revela si el
recurso existía.

---

### ★ G04-HU44 — Anonimización del registro académico
`8 pts · Should` · módulo: teoricos + encuestas · **RF-NFR-10, D-11**

**Antes de empezar**
`HU16` y `HU18` en verde (hay `alumno_id` que anonimizar). `HU43` para el rol ADMIN.

**Archivos**

| Archivo | Qué es |
|---|---|
| `db/migration/teoricos/V9__anonimizacion_ejecucion.sql` | bitácora de ejecuciones |
| `comun/privacidad/AnonimizacionService.java` | el proceso |
| `comun/privacidad/web/AnonimizacionController.java` | `POST /admin/anonimizacion` |
| `docs/ANONIMATO.md` | se le agrega la sección del proceso |

**Pasos**

1. **El algoritmo, y por qué así:**
   ```
   1. reunir los alumno_id alcanzados (por antigüedad o por pedido puntual)
   2. construir en MEMORIA un Map<UUID original, UUID subrogado> con UUID.randomUUID()
   3. UPDATE teoricos.respuesta   SET alumno_id = subrogado WHERE alumno_id = original
   4. UPDATE teoricos.correccion  SET alumno_id = subrogado WHERE alumno_id = original
   5. UPDATE teoricos.correccion_historial: el profesor_id NO se toca (es el auditor, no el titular)
   6. HU45 hace lo propio con cumplimiento.marcador
   7. descartar el Map
   8. registrar la ejecución: operador, fecha, cantidad — NUNCA los ids
   ```
   **Un subrogado por alumno y no uno por fila:** si cada fila recibiera un UUID distinto, se
   perdería la noción de «las respuestas de una misma persona» y con ella toda la serie académica
   que RF-NFR-10 manda conservar.
2. **Aleatorio, no un hash (CA3).** `UUID.randomUUID()`, nunca `sha256(alumnoId + sal)`: un hash
   con sal es reversible por fuerza bruta sobre el universo de alumnos, que son unos pocos miles.
   Dejarlo escrito en el javadoc, porque «hasheamos, total es irreversible» es exactamente el
   razonamiento que va a aparecer en el code review.
3. **Nada que permita recuperar (CA3):** el `Map` es una variable local. **No hay** tabla de
   correspondencia, ni columna `alumno_id_original`, ni un log que imprima el par. Un test de
   `information_schema` afirma que ninguna columna del esquema se llama así.
4. **Bitácora sin ids (Extras):**
   ```sql
   CREATE TABLE teoricos.anonimizacion_ejecucion (
     id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
     operador_id  uuid NOT NULL,          -- el ADMIN que la ejecutó
     ejecutada_en timestamptz NOT NULL DEFAULT now(),
     alcance      text NOT NULL,          -- el criterio, no la lista
     filas_afectadas int NOT NULL
   );
   ```
5. **Todo en una transacción,** con los `UPDATE` en orden fijo para no deadlockear contra el
   tráfico normal. En volumen grande, por lotes de alumnos, con la bitácora por lote.
6. **Las respuestas de encuesta están exentas (D-11),** y no es un olvido: nacieron sin PII, no hay
   vínculo que cortar. Documentarlo en `docs/ANONIMATO.md` con el argumento, porque un auditor
   que vea «anonimización» y encuentre una tabla sin tocar va a preguntar.
7. **El residual del texto libre, declarado:** si un alumno escribió su nombre dentro de una
   respuesta abierta, eso no se anonimiza solo. Se declara, con el mismo criterio con que el PRD
   declara los suyos.
8. **El residual de Kafka, declarado:** `ENCUESTA_CUMPLIDA` lleva `alumnoId` y el bus retiene.
   Va a `HU42`.

**Tests que la cierran**
- `AnonimizacionReemplazaAlumnoIdIT` (CA1).
- `NotasYAgregadosIntactosIT` — se calculan los agregados antes y después y se afirma que son
  idénticos, campo por campo (CA2). **El test que demuestra que conservamos el registro académico.**
- `MismoAlumnoMismoSubrogadoTest` (paso 1).
- `SinTablaDeCorrespondenciaTest` — se recorre `information_schema` buscando cualquier columna
  con `original` o `previo` en el nombre (CA3).
- `EjecucionQuedaRegistradaSinIdsTest` (Extras).

**Listo cuando** los agregados dan idénticos antes y después, y no existe ningún camino para
volver del subrogado al original.

**⚠ Trampa.** Una tabla de correspondencia «temporal, para poder revertir si sale mal» convierte
el proceso entero en decorativo. Si hay que poder revertir, la respuesta es un backup previo
bajo control del ADMIN, no una tabla dentro del sistema.

---

### ▸ G04-HU45 — Anonimización del marcador de cumplimiento
`3 pts · Should` · módulo: encuestas / cumplimiento · **D-11b**

**Antes de empezar**
`HU25` y `HU44` en verde.

**Archivos**

| Archivo | Qué es |
|---|---|
| `encuestas/cumplimiento/AnonimizadorDeMarcadores.java` | la parte de este módulo |
| `comun/privacidad/AnonimizacionService.java` | lo invoca |
| `docs/ANONIMATO.md` | la exención de las respuestas |

**Pasos**

1. **El marcador entra en el mismo proceso (CA1),** con el **mismo** subrogado que usó `HU44`
   para ese alumno. Si usara otro, se perdería la trazabilidad interna entre el registro
   académico y el cumplimiento del mismo titular anónimo.
   El `AnonimizacionService` le pasa el `Map` como parámetro: el módulo `cumplimiento` no lo
   construye ni lo guarda.
2. **La `PRIMARY KEY (alumno_id, instrumento_id)` cambia.** Un `UPDATE` sobre una columna de PK
   funciona en PostgreSQL, pero hay que verificar que el subrogado no colisione con un
   `alumno_id` existente. Con UUIDv4 la probabilidad es despreciable, pero el `UPDATE` tiene que
   ir con manejo de `unique_violation` igual, porque un fallo silencioso acá corrompe marcadores.
3. **Por qué el marcador sí y las respuestas no (CA2).** Es el punto que hay que dejar
   documentado, porque parece una inconsistencia y no lo es:
   > El marcador guarda `alumno_id`: dice «María cumplió la encuesta del curso X». No es una
   > respuesta, pero es dato personal. La respuesta de encuesta, en cambio, nació sin vínculo con
   > su autor: no hay nada que cortar. El mapa de PII del PRD no contempla el marcador —
   > probablemente porque fue escrito pensando en repositorios de contenido—. Lo resolvemos
   > nosotros en vez de dejarlo en el limbo.
4. **Cobertura conservada (CA3):** el conteo de cumplimiento por curso e instrumento tiene que dar
   idéntico antes y después. Es el equivalente al CA2 de `HU44`.

**Tests que la cierran**
- `MarcadorSeAnonimizaConElMismoSubrogadoIT` (CA1, paso 1).
- `RespuestasDeEncuestaNoSeTocanIT` — se afirma que ninguna fila de `respuesta_encuesta` cambió (CA2).
- `CoberturaPorCursoIntactaIT` (CA3).

**Listo cuando** el marcador queda anónimo, la cobertura por curso no se mueve, y la exención
de las respuestas está escrita con su porqué.

---

### ▸ G04-HU46 — Baja lógica verificada en todas las entidades
`3 pts · Must` · módulo: transversal · **RF-NFR-01**

**Antes de empezar**
`HU10` y `HU24` en verde. Se cierra al final del sprint, cuando ya existen todas las entidades.

**Archivos**

| Archivo | Qué es |
|---|---|
| `comun/persistencia/ConBajaLogica.java` | interfaz de marca |
| `db/migration/*/V*__baja_logica.sql` | las columnas que falten |
| `test/…/arquitectura/BajaLogicaTest.java` | el test del CA3 |

**Pasos**

1. **Interfaz de marca:**
   ```java
   public interface ConBajaLogica { Instant getBajaLogica(); }
   ```
   La implementan todas las entidades **salvo las excepciones justificadas**, que hay que
   enumerar explícitamente:

   | Entidad | Baja lógica | Por qué |
   |---|---|---|
   | `item`, `desafio_teorico`, `instrumento`, `marcador` | **sí** | son gestionables |
   | `item_version` | **no** | es inmutable por D-04; darla de baja rompería desafíos compuestos |
   | `respuesta`, `correccion` | **no** | son el registro académico; se anonimizan (`HU44`), no se dan de baja |
   | `correccion_historial` | **no** | es auditoría; el CA3 de `HU22` prohíbe borrarlo |
   | `respuesta_encuesta` | **no** | anónima; no hay titular que pida la baja |

   **La lista de excepciones es el entregable principal de la historia.** Un test que exija
   `baja_logica` en todo obligaría a agregar la columna a `respuesta_encuesta`, y eso rompería
   `HU29`. Las excepciones van en el test, con su motivo como comentario.
2. **Agregar las columnas faltantes** en las cuatro que sí la llevan, con migraciones por esquema.
3. **Ningún borrado físico (CA2):** ningún repositorio extiende `JpaRepository` ni `CrudRepository`;
   todos extienden `Repository<T, ID>` y declaran los métodos permitidos. Ya se resolvió así en
   `HU11` y `HU22`; acá se verifica en todo el proyecto.
4. **El test (CA3), con ArchUnit:**
   ```java
   @ArchTest static final ArchRule sinBorradoFisico =
       noClasses().that().resideInAPackage("..persistencia..")
           .should().beAssignableTo(CrudRepository.class);

   @ArchTest static final ArchRule entidadesConBaja =
       classes().that().areAnnotatedWith(Entity.class)
           .and().doNotHaveSimpleName("ItemVersionEntity")   // D-04, inmutable
           .and()./* … el resto de las excepciones, cada una con su comentario … */
           .should().implement(ConBajaLogica.class);
   ```
5. **Filtro por defecto:** las consultas de las entidades con baja lógica llevan
   `WHERE baja_logica IS NULL`. Un `@Where` de Hibernate lo aplica solo, pero conviene
   **no** usarlo: esconde el filtro y complica los casos donde sí hay que ver lo dado de baja.
   Explícito en cada método del repositorio.

**Tests que la cierran** — `BajaLogicaTest`, con las dos reglas del paso 4.

**Listo cuando** agregar una entidad sin baja lógica y sin justificarla en la lista de excepciones
pone el build en rojo.

---

# Cierre

## Lo que hay que decidir en equipo antes del día 1

Cinco cosas que este documento fija por defecto y conviene confirmar entre todos, porque
cambiarlas después cuesta:

1. **El umbral de aprobación** de los desafíos teóricos (`HU18`, puesto en 60).
2. **La regla de puntaje parcial** de cada tipo de ítem (`HU18`, paso 2). Es lo que ve el alumno.
3. **La longitud mínima del comentario** obligatorio (`HU32`, puesta en 15).
4. **La política ante caída del Tema 02** (`HU40`, paso 2): proponemos *fail-closed* en lectura.
5. **La ventana de flush y el jitter** de la escritura desacoplada (`HU30`): 5 s y lote de 50.
   Más agresivo protege más el anonimato y demora más el dato.

## Las cinco historias donde se juega el sprint

| Historia | Por qué |
|---|---|
| `HU06` | si los roles no quedan bien al arranque, hay que revisar todo lo escrito encima |
| `HU10` | el versionado inmutable es la base de `HU14`, `HU16`, `HU18` y `HU23` |
| `HU19` | si el puerto de corrección queda mal, el LLM del sprint 2 es un refactor y no un adaptador |
| `HU30` | es la única historia donde el anonimato depende de nuestro código y no del motor |
| `HU42` | tres supuestos sin confirmar pueden invalidar trabajo ya hecho |

## Lo que este plan no cubre

- **La interfaz de usuario.** Somos back end; el front del bloqueo por encuesta (D-06) y las
  vistas del profesor están fuera.
- **El adaptador real de moderación** (`HU33`) y el **corrector por LLM** (`HU19`): los puertos
  quedan listos, los adaptadores no.
- **Conversación y debate estructurado** (D-09): el `payload jsonb` los admite, falta la decisión
  de producto.
- **Las respuestas de los supuestos.** Once historias asumen algo que `HU42` tiene que confirmar.
  Si alguna sale al revés, el ajuste vuelve al backlog antes de volver al código.
