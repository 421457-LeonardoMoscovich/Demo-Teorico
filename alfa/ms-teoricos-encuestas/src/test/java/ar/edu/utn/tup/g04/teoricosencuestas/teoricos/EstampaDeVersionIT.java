package ar.edu.utn.tup.g04.teoricosencuestas.teoricos;

import ar.edu.utn.tup.g04.teoricosencuestas.BaseIT;
import ar.edu.utn.tup.g04.teoricosencuestas.comun.eventos.PublicadorEnMemoria;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * EL TEST QUE JUSTIFICA TODO EL MODELO (CI-12, CI-13, CI-53).
 *
 * El escenario: el alumno abre el cuestionario, el profesor publica una version
 * nueva del item mientras el alumno contesta, y el alumno entrega.
 *
 * Lo que tiene que pasar:
 *   - se lo corrige contra LA VERSION QUE VIO, no contra la vigente;
 *   - la respuesta queda estampada con ese itemVersionId;
 *   - el desglose le muestra el enunciado y el payload de ESA version;
 *   - el siguiente alumno que abra ya ve la version nueva.
 *
 * Esas tres cosas juntas son las que hacen que no haga falta congelar la
 * composicion, y las que hacen que RF-CUR-05 —el profesor arregla una errata
 * mientras los alumnos trabajan— funcione de verdad.
 */
class EstampaDeVersionIT extends BaseIT {

    @Autowired
    PublicadorEnMemoria eventos;

    @Test
    void se_corrige_por_lo_que_el_alumno_vio_y_el_siguiente_ve_la_version_nueva() throws Exception {
        String profe = bearerProfesor();
        String itemId = crearItem("VERDADERO_FALSO", "Version 1 del enunciado",
                """
                {"afirmacion":"Afirmacion de la version 1"}
                """,
                """
                {"esVerdadero":true}
                """, profe);

        JsonNode ficha = componer("Parcial", "PORCENTUAL", """
                [{"itemId":"%s","orden":1,"puntaje":100}]
                """.formatted(itemId), profe);
        UUID contenidoId = UUID.fromString(ficha.get("contenidoId").asText());

        // El alumno abre y ve la v1.
        JsonNode vistaV1 = json.readTree(
                mvc.perform(get("/teoricos/contenidos/{id}/vista-alumno", contenidoId)
                                .header("Authorization", bearerAlumno())
                                .header("X-Vale-Lectura", vale(contenidoId, idAlumno, 15)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));

        String versionQueVio = vistaV1.get("items").get(0).get("itemVersionId").asText();
        assertThat(vistaV1.get("items").get(0).get("enunciado").asText())
                .isEqualTo("Version 1 del enunciado");

        // El profesor corrige el item MIENTRAS el alumno contesta, y de paso
        // invierte la clave: en la v2 la respuesta correcta es "falso".
        mvc.perform(put("/teoricos/items/{id}", itemId)
                        .header("Authorization", profe)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"VERDADERO_FALSO","enunciado":"Version 2 del enunciado",
                                 "payload":{"afirmacion":"Afirmacion corregida"},
                                 "criterio":{"esVerdadero":false}}
                                """))
                .andExpect(status().isOk());

        // El alumno entrega lo que vio: contesta "verdadero", que era lo correcto
        // en la v1 y ya no lo es en la v2.
        UUID entregaId = UUID.randomUUID();
        despachar(entregaId, contenidoId, versionQueVio, "{\"valor\":true}");

        JsonNode resultado = json.readTree(
                mvc.perform(get("/teoricos/evaluaciones/{id}", entregaId)
                                .header("Authorization", bearerAlumno()))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertThat(resultado.get("nota").asInt())
                .as("se corrige por la estampa: en la v1 verdadero era correcto")
                .isEqualTo(100);

        JsonNode detalle = resultado.get("detalle").get(0);
        assertThat(detalle.get("itemVersionId").asText())
                .as("la respuesta quedo estampada con la version que vio el alumno")
                .isEqualTo(versionQueVio);
        assertThat(detalle.get("enunciado").asText())
                .as("el desglose muestra el enunciado que el alumno leyo, no el de hoy")
                .isEqualTo("Version 1 del enunciado");
        assertThat(detalle.get("payload").get("afirmacion").asText())
                .as("y el payload tambien sale de la estampa: es lo que permite mostrar "
                    + "\"Contestaste: Verdadero\" sobre la afirmacion que el alumno leyo")
                .isEqualTo("Afirmacion de la version 1");
        assertThat(detalle.has("criterio"))
                .as("pero NUNCA el criterio: con reintentos ilimitados eso es copiar")
                .isFalse();

        // Y el siguiente alumno que abra el MISMO cuestionario ya ve la v2:
        // la referencia es flotante, no pinneada.
        JsonNode vistaV2 = json.readTree(
                mvc.perform(get("/teoricos/contenidos/{id}/vista-alumno", contenidoId)
                                .header("Authorization", bearerAlumno())
                                .header("X-Vale-Lectura", vale(contenidoId, idAlumno, 15)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertThat(vistaV2.get("items").get(0).get("enunciado").asText())
                .isEqualTo("Version 2 del enunciado");
        assertThat(vistaV2.get("items").get(0).get("itemVersionId").asText())
                .isNotEqualTo(versionQueVio);
    }

    @Test
    void el_evento_lleva_lo_acordado_y_nada_mas() throws Exception {
        eventos.limpiar();

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
        despachar(entregaId, contenidoId, versionId, "{\"seleccionadas\":[\"a\"]}");

        assertThat(eventos.publicados()).hasSize(1);
        var publicado = eventos.publicados().get(0);

        assertThat(publicado.topico()).isEqualTo("desafios.resultados");
        assertThat(publicado.evento().eventType()).isEqualTo("TEORICO_CORREGIDO");
        assertThat(publicado.evento().producer()).isEqualTo("tema-04-teoricos-encuestas");

        var payload = publicado.evento().payload();
        assertThat(payload).containsKeys("entregaId", "desafioId", "alumnoId",
                "cursoCohorteId", "intento", "nota", "estado", "revision");
        // CI-38, CI-39, CI-28: estos tres NO viajan al Tema 03.
        assertThat(payload).doesNotContainKeys("aprobado", "corrector", "detalle");
    }

    private void despachar(UUID entregaId, UUID contenidoId, String itemVersionId,
                           String contenido) throws Exception {
        mvc.perform(post("/teoricos/evaluaciones")
                        .header("X-Despacho-Key", CLAVE_DESPACHO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entregaId":"%s","desafioId":"%s","alumnoId":"%s",
                                 "cursoCohorteId":"11111111-1111-1111-1111-111111111111",
                                 "intento":1,
                                 "contenidoBinding":{"contenidoId":"%s","version":1},
                                 "respuestas":[{"itemVersionId":"%s","contenido":%s}]}
                                """.formatted(entregaId, UUID.randomUUID(), idAlumno,
                                contenidoId, itemVersionId, contenido)))
                .andExpect(status().isAccepted());
    }
}
