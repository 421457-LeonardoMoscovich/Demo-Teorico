package ar.edu.utn.tup.g04.teoricosencuestas.teoricos;

import ar.edu.utn.tup.g04.teoricosencuestas.BaseIT;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El barajado por alumno, y la propiedad que lo hace compatible con CI-19.
 *
 * CI-19 decia que barajar obligaba a guardar el orden por alumno y por intento,
 * y que por eso la lectura dejaba de ser pura. Eso es cierto para un barajado
 * al azar; no lo es para uno DERIVADO de (contenidoId, alumnoId). Estos tests
 * fijan las tres propiedades de las que depende ese argumento:
 *
 *   1. dos alumnos ven ordenes distintos;
 *   2. el mismo alumno ve SIEMPRE el mismo orden, recargue cuando recargue;
 *   3. el desglose del resultado sale en el orden en que EL las vio.
 *
 * Si alguna de las tres se cae, hay que volver a guardar el orden y CI-19
 * recupera su forma original.
 */
class BarajadoPorAlumnoIT extends BaseIT {

    /** Un cuestionario de seis items, que es donde un orden distinto se nota. */
    private UUID cuestionarioDeSeis(String profe) throws Exception {
        List<String> ids = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            ids.add(crearItem("VERDADERO_FALSO", "Pregunta numero " + i,
                    """
                    {"afirmacion":"Afirmacion %d"}
                    """.formatted(i),
                    """
                    {"esVerdadero":true}
                    """, profe));
        }
        StringBuilder items = new StringBuilder("[");
        for (int i = 0; i < ids.size(); i++) {
            items.append(i > 0 ? "," : "")
                    .append("""
                            {"itemId":"%s","orden":%d,"puntaje":%d}
                            """.formatted(ids.get(i), i + 1, i == 5 ? 15 : 17));
        }
        items.append("]");

        JsonNode ficha = componer("Parcial de seis", "PORCENTUAL", items.toString(), profe);
        return UUID.fromString(ficha.get("contenidoId").asText());
    }

    private List<String> enunciadosQueVe(UUID contenidoId, UUID alumnoId) throws Exception {
        JsonNode vista = json.readTree(
                mvc.perform(get("/teoricos/contenidos/{id}/vista-alumno", contenidoId)
                                .header("Authorization", bearerAlumno())
                                .header("X-Vale-Lectura", vale(contenidoId, alumnoId, 15)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));

        List<String> enunciados = new ArrayList<>();
        for (JsonNode i : vista.get("items")) {
            enunciados.add(i.get("enunciado").asText());
        }
        return enunciados;
    }

    @Test
    void dos_alumnos_ven_las_mismas_preguntas_en_distinto_orden() throws Exception {
        UUID contenidoId = cuestionarioDeSeis(bearerProfesor());

        List<String> deUno = enunciadosQueVe(contenidoId, idAlumno);
        List<String> deOtro = enunciadosQueVe(contenidoId, UUID.randomUUID());

        assertThat(deUno)
                .as("las mismas seis preguntas, en otro orden")
                .containsExactlyInAnyOrderElementsOf(deOtro)
                .isNotEqualTo(deOtro);
    }

    @Test
    void el_mismo_alumno_ve_siempre_el_mismo_orden() throws Exception {
        UUID contenidoId = cuestionarioDeSeis(bearerProfesor());

        // Tres lecturas seguidas: es lo que pasa si el alumno recarga la pagina.
        // Sin esta propiedad habria que persistir el orden, que es exactamente lo
        // que CI-19 queria evitar.
        List<String> primera = enunciadosQueVe(contenidoId, idAlumno);
        assertThat(enunciadosQueVe(contenidoId, idAlumno)).isEqualTo(primera);
        assertThat(enunciadosQueVe(contenidoId, idAlumno)).isEqualTo(primera);
    }

    @Test
    void el_alumno_ve_su_numeracion_y_no_la_del_profesor() throws Exception {
        UUID contenidoId = cuestionarioDeSeis(bearerProfesor());

        JsonNode vista = json.readTree(
                mvc.perform(get("/teoricos/contenidos/{id}/vista-alumno", contenidoId)
                                .header("Authorization", bearerAlumno())
                                .header("X-Vale-Lectura", vale(contenidoId, idAlumno, 15)))
                        .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));

        List<Integer> ordenes = new ArrayList<>();
        for (JsonNode i : vista.get("items")) {
            ordenes.add(i.get("orden").asInt());
        }
        assertThat(ordenes)
                .as("para el alumno la primera que ve es la 1, sea cual sea la del profesor")
                .containsExactly(1, 2, 3, 4, 5, 6);
    }

    @Test
    void el_desglose_sale_en_el_orden_en_que_el_alumno_las_vio() throws Exception {
        String profe = bearerProfesor();
        UUID contenidoId = cuestionarioDeSeis(profe);

        JsonNode vista = json.readTree(
                mvc.perform(get("/teoricos/contenidos/{id}/vista-alumno", contenidoId)
                                .header("Authorization", bearerAlumno())
                                .header("X-Vale-Lectura", vale(contenidoId, idAlumno, 15)))
                        .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));

        StringBuilder respuestas = new StringBuilder("[");
        List<String> comoLasVio = new ArrayList<>();
        int n = 0;
        for (JsonNode i : vista.get("items")) {
            comoLasVio.add(i.get("enunciado").asText());
            respuestas.append(n++ > 0 ? "," : "")
                    .append("""
                            {"itemVersionId":"%s","contenido":{"valor":true}}
                            """.formatted(i.get("itemVersionId").asText()));
        }
        respuestas.append("]");

        UUID entregaId = UUID.randomUUID();
        mvc.perform(post("/teoricos/evaluaciones")
                        .header("X-Despacho-Key", CLAVE_DESPACHO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entregaId":"%s","desafioId":"%s","alumnoId":"%s",
                                 "cursoCohorteId":"11111111-1111-1111-1111-111111111111","intento":1,
                                 "contenidoBinding":{"contenidoId":"%s","version":1},
                                 "respuestas":%s}
                                """.formatted(entregaId, UUID.randomUUID(), idAlumno,
                                contenidoId, respuestas)))
                .andExpect(status().isAccepted());

        JsonNode resultado = json.readTree(
                mvc.perform(get("/teoricos/evaluaciones/{id}", entregaId)
                                .header("Authorization", bearerAlumno()))
                        .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));

        List<String> enElDesglose = new ArrayList<>();
        List<Integer> ordenes = new ArrayList<>();
        for (JsonNode d : resultado.get("detalle")) {
            enElDesglose.add(d.get("enunciado").asText());
            ordenes.add(d.get("orden").asInt());
        }

        assertThat(enElDesglose)
                .as("si contesto la que para el era la 3, el resultado tiene que decirle 3; "
                    + "la permutacion no esta guardada, se reconstruye con la misma semilla")
                .isEqualTo(comoLasVio);
        assertThat(ordenes).containsExactly(1, 2, 3, 4, 5, 6);
    }
}
