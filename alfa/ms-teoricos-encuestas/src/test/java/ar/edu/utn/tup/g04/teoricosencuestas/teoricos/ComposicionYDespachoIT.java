package ar.edu.utn.tup.g04.teoricosencuestas.teoricos;

import ar.edu.utn.tup.g04.teoricosencuestas.BaseIT;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ComposicionYDespachoIT extends BaseIT {

    @Test
    void los_pesos_que_no_suman_100_se_rechazan_nombrando_el_campo() throws Exception {
        String profe = bearerProfesor();
        String om = itemOpcionMultiple(profe);
        String vf = itemVerdaderoFalso(profe);

        mvc.perform(post("/teoricos/contenidos")
                        .header("Authorization", profe)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cursoCohorteId":"11111111-1111-1111-1111-111111111111",
                                 "titulo":"Parcial","escala":"PORCENTUAL",
                                 "items":[{"itemId":"%s","orden":1,"puntaje":40},
                                          {"itemId":"%s","orden":2,"puntaje":40}]}
                                """.formatted(om, vf)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.clave").value("PESOS_NO_SUMAN_100"))
                .andExpect(jsonPath("$.campo").value("items"));
    }

    @Test
    void con_escala_libre_los_pesos_no_tienen_que_sumar_100() throws Exception {
        // La regla de los 100 es de la escala, no del dominio: el desafio de
        // recuperacion de vida (RF-REC-04) usa otras escalas.
        String profe = bearerProfesor();
        String om = itemOpcionMultiple(profe);

        JsonNode ficha = componer("Recuperacion", "LIBRE", """
                [{"itemId":"%s","orden":1,"puntaje":7}]
                """.formatted(om), profe);

        assertThat(ficha.get("resumen").asText()).isEqualTo("1 pregunta · 7 puntos");
    }

    @Test
    void la_ficha_deriva_el_modo_de_correccion_y_no_lo_elige_el_profesor() throws Exception {
        String profe = bearerProfesor();
        String om = itemOpcionMultiple(profe);
        String vf = itemVerdaderoFalso(profe);

        JsonNode ficha = componer("Parcial", "PORCENTUAL", """
                [{"itemId":"%s","orden":1,"puntaje":60},
                 {"itemId":"%s","orden":2,"puntaje":40}]
                """.formatted(om, vf), profe);

        assertThat(ficha.get("tipo").asText()).isEqualTo("TEORICO");
        assertThat(ficha.get("version").asInt()).isEqualTo(1);
        assertThat(ficha.get("resumen").asText()).isEqualTo("2 preguntas · 100 puntos");
        assertThat(ficha.get("correccion").asText()).isEqualTo("INMEDIATA");
        // La ficha tiene cinco campos y ni uno mas: cada campo de mas es
        // conocimiento de contenido que se le filtra al Tema 03.
        assertThat(ficha.size()).isEqualTo(5);
    }

    @Test
    void el_orden_tiene_que_ser_consecutivo_desde_1() throws Exception {
        String profe = bearerProfesor();
        String om = itemOpcionMultiple(profe);
        String vf = itemVerdaderoFalso(profe);

        mvc.perform(post("/teoricos/contenidos")
                        .header("Authorization", profe)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cursoCohorteId":"11111111-1111-1111-1111-111111111111",
                                 "titulo":"Parcial","escala":"PORCENTUAL",
                                 "items":[{"itemId":"%s","orden":1,"puntaje":50},
                                          {"itemId":"%s","orden":7,"puntaje":50}]}
                                """.formatted(om, vf)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.clave").value("ORDEN_NO_CONSECUTIVO"));
    }

    @Test
    void el_despacho_es_idempotente_por_entregaId() throws Exception {
        String profe = bearerProfesor();
        String om = itemOpcionMultiple(profe);
        JsonNode ficha = componer("Parcial", "PORCENTUAL", """
                [{"itemId":"%s","orden":1,"puntaje":100}]
                """.formatted(om), profe);
        UUID contenidoId = UUID.fromString(ficha.get("contenidoId").asText());

        JsonNode vista = json.readTree(
                mvc.perform(get("/teoricos/contenidos/{id}/vista-alumno", contenidoId)
                                .header("Authorization", bearerAlumno())
                                .header("X-Vale-Lectura", vale(contenidoId, idAlumno, 15)))
                        .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        String versionId = vista.get("items").get(0).get("itemVersionId").asText();

        UUID entregaId = UUID.randomUUID();
        String cuerpo = """
                {"entregaId":"%s","desafioId":"%s","alumnoId":"%s",
                 "cursoCohorteId":"11111111-1111-1111-1111-111111111111","intento":1,
                 "contenidoBinding":{"contenidoId":"%s","version":1},
                 "respuestas":[{"itemVersionId":"%s","contenido":{"seleccionadas":["a"]}}]}
                """.formatted(entregaId, UUID.randomUUID(), idAlumno, contenidoId, versionId);

        String primera = mvc.perform(post("/teoricos/evaluaciones")
                        .header("X-Despacho-Key", CLAVE_DESPACHO)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        String segunda = mvc.perform(post("/teoricos/evaluaciones")
                        .header("X-Despacho-Key", CLAVE_DESPACHO)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(json.readTree(segunda).get("evaluacionId").asText())
                .as("reintentar el despacho no crea una evaluacion nueva (CI-25)")
                .isEqualTo(json.readTree(primera).get("evaluacionId").asText());
    }

    @Test
    void el_acuse_nunca_lleva_la_nota() throws Exception {
        String profe = bearerProfesor();
        String om = itemOpcionMultiple(profe);
        JsonNode ficha = componer("Parcial", "PORCENTUAL", """
                [{"itemId":"%s","orden":1,"puntaje":100}]
                """.formatted(om), profe);
        UUID contenidoId = UUID.fromString(ficha.get("contenidoId").asText());

        JsonNode vista = json.readTree(
                mvc.perform(get("/teoricos/contenidos/{id}/vista-alumno", contenidoId)
                                .header("Authorization", bearerAlumno())
                                .header("X-Vale-Lectura", vale(contenidoId, idAlumno, 15)))
                        .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));

        // CI-23: siempre 202 y siempre la misma forma, aunque la correccion sea
        // inmediata. Si devolvieramos la nota cuando es inmediata, el Tema 03
        // volveria a tener un if sobre nuestro dominio.
        mvc.perform(post("/teoricos/evaluaciones")
                        .header("X-Despacho-Key", CLAVE_DESPACHO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entregaId":"%s","desafioId":"%s","alumnoId":"%s",
                                 "cursoCohorteId":"11111111-1111-1111-1111-111111111111","intento":1,
                                 "contenidoBinding":{"contenidoId":"%s","version":1},
                                 "respuestas":[{"itemVersionId":"%s","contenido":{"seleccionadas":["a"]}}]}
                                """.formatted(UUID.randomUUID(), UUID.randomUUID(), idAlumno,
                                contenidoId, vista.get("items").get(0).get("itemVersionId").asText())))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.estado").value("ACEPTADA"))
                .andExpect(jsonPath("$.correccion").value("INMEDIATA"))
                .andExpect(jsonPath("$.nota").doesNotExist());
    }

    @Test
    void el_despacho_sin_la_credencial_de_servicio_no_pasa() throws Exception {
        mvc.perform(post("/teoricos/evaluaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void un_alumno_no_puede_despachar_una_entrega() throws Exception {
        // El despacho lo hace el Tema 03, no el navegador del alumno (CI-22).
        mvc.perform(post("/teoricos/evaluaciones")
                        .header("Authorization", bearerAlumno())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
