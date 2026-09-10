package ar.edu.utn.tup.g04.teoricosencuestas;

import ar.edu.utn.tup.g04.teoricosencuestas.comun.identidad.EmisorDeTokens;
import ar.edu.utn.tup.g04.teoricosencuestas.comun.identidad.Rol;
import ar.edu.utn.tup.g04.teoricosencuestas.comun.identidad.Usuario;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base de los tests de integracion.
 *
 * El contenedor es UNO SOLO para toda la suite (patron singleton container):
 * arrancar PostgreSQL por clase de test agrega minutos al build sin agregar
 * ninguna garantia.
 *
 * Se conecta con los roles REALES, app_owner y app_teoricos, y no con el
 * superusuario: si un permiso falta, queremos que falle en el build y no en la
 * demo. Es la version reducida de lo que G04-HU06 hace con los cuatro esquemas.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class BaseIT {

    protected static final String SECRETO_SESION = "secreto-de-sesion-para-los-tests-1234567890";
    protected static final String SECRETO_VALE = "secreto-del-vale-para-los-tests-1234567890";
    protected static final String CLAVE_DESPACHO = "clave-de-despacho-de-test";

    /**
     * Por defecto los tests levantan su propio PostgreSQL con Testcontainers y no
     * dependen de nada externo.
     *
     * La salida de emergencia es la variable ALFA_DB_URL: si esta puesta, se usa
     * esa base en vez de arrancar un contenedor. Existe porque hay entornos donde
     * Testcontainers no puede hablar con el demonio de Docker aunque el CLI
     * funcione — por ejemplo Docker Desktop en Windows sirviendo el engine por
     * npipe. Ver el README: se corre con
     *
     *     ALFA_DB_URL=jdbc:postgresql://localhost:5432/g04_test mvn verify
     *
     * y esa base tiene que tener los roles de docker/postgres/00-roles.sql.
     */
    private static final String URL_EXTERNA = System.getenv("ALFA_DB_URL");

    static final PostgreSQLContainer<?> POSTGRES;

    static {
        if (URL_EXTERNA == null) {
            POSTGRES = new PostgreSQLContainer<>("postgres:16").withInitScript("roles-test.sql");
            POSTGRES.start();
        } else {
            POSTGRES = null;
        }
    }

    private static String urlDeLaBase() {
        return URL_EXTERNA != null ? URL_EXTERNA : POSTGRES.getJdbcUrl();
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        registro.add("app.datasource.migraciones.url", BaseIT::urlDeLaBase);
        registro.add("app.datasource.migraciones.username", () -> "app_owner");
        registro.add("app.datasource.migraciones.password", () -> "owner");
        registro.add("app.datasource.teoricos.url", BaseIT::urlDeLaBase);
        registro.add("app.datasource.teoricos.username", () -> "app_teoricos");
        registro.add("app.datasource.teoricos.password", () -> "teoricos");
        registro.add("app.seguridad.secreto-sesion", () -> SECRETO_SESION);
        registro.add("app.seguridad.secreto-vale", () -> SECRETO_VALE);
        registro.add("app.seguridad.clave-despacho", () -> CLAVE_DESPACHO);
        // Sin esto el sembrador de la demo llenaria el banco y BancoAjenoNoSeVeIT
        // dejaria de estar afirmando sobre lo que el propio test creo.
        registro.add("app.demo.sembrar", () -> "false");
    }

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    @Autowired
    protected EmisorDeTokens emisor;

    protected UUID idProfesor = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    protected UUID idAlumno = UUID.fromString("00000000-0000-0000-0000-0000000000b1");

    protected String bearerProfesor() {
        return bearer(new Usuario(idProfesor, "profe", "Ana Profesora", Rol.PROFESOR));
    }

    protected String bearerAlumno() {
        return bearer(new Usuario(idAlumno, "alumno", "Luis Alumno", Rol.ALUMNO));
    }

    protected String bearerDeOtroProfesor(UUID id) {
        return bearer(new Usuario(id, "otro", "Otro Profesor", Rol.PROFESOR));
    }

    private String bearer(Usuario usuario) {
        return "Bearer " + emisor.emitir(usuario);
    }

    // ---- atajos para armar el escenario ----

    /** Crea un item del banco del profesor y devuelve su id. */
    protected String crearItem(String tipo, String enunciado, String payload, String criterio,
                               String bearer) throws Exception {
        String cuerpo = """
                {"tipo":"%s","enunciado":"%s","payload":%s,"criterio":%s}
                """.formatted(tipo, enunciado, payload, criterio);
        String respuesta = mvc.perform(post("/teoricos/items")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(respuesta).get("id").asText();
    }

    protected String itemOpcionMultiple(String bearer) throws Exception {
        return crearItem("OPCION_MULTIPLE", "Cual es la capital de Francia?",
                """
                {"opciones":[{"id":"a","texto":"Paris"},{"id":"b","texto":"Roma"}],"multiple":false}
                """,
                """
                {"correctas":["a"]}
                """, bearer);
    }

    protected String itemVerdaderoFalso(String bearer) throws Exception {
        return crearItem("VERDADERO_FALSO", "Un microservicio puede tener su propia base.",
                """
                {"afirmacion":"Un microservicio puede tener su propia base."}
                """,
                """
                {"esVerdadero":true}
                """, bearer);
    }

    /** Compone un cuestionario y devuelve la ficha de cinco campos. */
    protected JsonNode componer(String titulo, String escala, String itemsJson, String bearer)
            throws Exception {
        String cuerpo = """
                {"cursoCohorteId":"11111111-1111-1111-1111-111111111111",
                 "titulo":"%s","escala":"%s","items":%s}
                """.formatted(titulo, escala, itemsJson);
        String respuesta = mvc.perform(post("/teoricos/contenidos")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(respuesta);
    }

    /** Emula lo que firma el Tema 03 al abrir el desafio (CI-21). */
    protected String vale(UUID contenidoId, UUID alumnoId, long minutos) {
        try {
            Instant ahora = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(alumnoId.toString())
                    .claim("contenidoId", contenidoId.toString())
                    .claim("alumnoId", alumnoId.toString())
                    .issueTime(Date.from(ahora.minus(1, ChronoUnit.MINUTES)))
                    .expirationTime(Date.from(ahora.plus(minutos, ChronoUnit.MINUTES)))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(SECRETO_VALE.getBytes(StandardCharsets.UTF_8)));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
