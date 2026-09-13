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

/**
 * Como recorre el alumno el cuestionario: de a una consigna, y si puede volver.
 *
 * Lo que estos tests fijan, y es lo discutible: la navegacion la elige la
 * profesora y viaja con el CONTENIDO, pero NO entra en la ficha que sale al
 * Tema 03. Es la frontera funcionando: el 03 no necesita saber como se recorre
 * algo que nunca abre.
 */
class NavegacionDelCuestionarioIT extends BaseIT {

    @Test
    void sin_elegir_nada_el_cuestionario_es_de_navegacion_libre() throws Exception {
        // El helper `componer` NO manda el campo, asi que este test prueba de
        // paso lo que le importa a los otros grupos: un cliente escrito antes de
        // que esto existiera sigue componiendo y obtiene lo que ya tenia.
        String profe = bearerProfesor();
        JsonNode ficha = componer("Parcial", "PORCENTUAL", """
                [{"itemId":"%s","orden":1,"puntaje":100}]
                """.formatted(itemOpcionMultiple(profe)), profe);

        assertThat(vistaDelAlumno(ficha).get("navegacion").asText()).isEqualTo("LIBRE");
    }

    @Test
    void la_navegacion_que_eligio_la_profesora_le_llega_al_alumno() throws Exception {
        String profe = bearerProfesor();
        JsonNode ficha = componerCon("SECUENCIAL", itemOpcionMultiple(profe), profe);

        assertThat(vistaDelAlumno(ficha).get("navegacion").asText()).isEqualTo("SECUENCIAL");
    }

    @Test
    void una_navegacion_que_no_existe_se_rechaza_nombrando_el_campo() throws Exception {
        String profe = bearerProfesor();
        String om = itemOpcionMultiple(profe);

        mvc.perform(post("/teoricos/contenidos")
                        .header("Authorization", profe)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cursoCohorteId":"11111111-1111-1111-1111-111111111111",
                                 "titulo":"Parcial","escala":"PORCENTUAL","navegacion":"A_DEDO",
                                 "items":[{"itemId":"%s","orden":1,"puntaje":100}]}
                                """.formatted(om)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.clave").value("NAVEGACION_INVALIDA"))
                .andExpect(jsonPath("$.campo").value("navegacion"));
    }

    @Test
    void la_navegacion_no_entra_en_la_ficha_que_sale_al_tema_03() throws Exception {
        String profe = bearerProfesor();
        JsonNode ficha = componerCon("SECUENCIAL", itemOpcionMultiple(profe), profe);

        // Sigue teniendo cinco campos. Agregarle este seria pedirle al 03 que
        // sepa como se recorre adentro de una caja que no abre (CI-04).
        assertThat(ficha.size()).isEqualTo(5);
        assertThat(ficha.has("navegacion")).isFalse();
    }

    @Test
    void el_profesor_ve_la_navegacion_de_cada_cuestionario_que_armo() throws Exception {
        String profe = bearerProfesor();
        JsonNode ficha = componerCon("SECUENCIAL", itemOpcionMultiple(profe), profe);
        String contenidoId = ficha.get("contenidoId").asText();

        JsonNode mios = json.readTree(
                mvc.perform(get("/teoricos/contenidos").header("Authorization", profe))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));

        JsonNode elMio = null;
        for (JsonNode c : mios) {
            if (c.get("contenidoId").asText().equals(contenidoId)) elMio = c;
        }
        assertThat(elMio).isNotNull();
        assertThat(elMio.get("navegacion").asText()).isEqualTo("SECUENCIAL");
    }

    private JsonNode componerCon(String navegacion, String itemId, String bearer) throws Exception {
        String respuesta = mvc.perform(post("/teoricos/contenidos")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cursoCohorteId":"11111111-1111-1111-1111-111111111111",
                                 "titulo":"Parcial","escala":"PORCENTUAL","navegacion":"%s",
                                 "items":[{"itemId":"%s","orden":1,"puntaje":100}]}
                                """.formatted(navegacion, itemId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(respuesta);
    }

    private JsonNode vistaDelAlumno(JsonNode ficha) throws Exception {
        UUID contenidoId = UUID.fromString(ficha.get("contenidoId").asText());
        return json.readTree(
                mvc.perform(get("/teoricos/contenidos/{id}/vista-alumno", contenidoId)
                                .header("Authorization", bearerAlumno())
                                .header("X-Vale-Lectura", vale(contenidoId, idAlumno, 15)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
