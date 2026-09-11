package ar.edu.utn.tup.g04.teoricosencuestas.teoricos;

import ar.edu.utn.tup.g04.teoricosencuestas.BaseIT;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Borrador / listo y la vista previa del item (CI-59).
 *
 * Las dos cosas de la misma tanda, y el test las junta porque prueban la misma
 * idea desde dos lados: que el profesor pueda ver y frenar un item ANTES de que
 * un alumno se lo coma.
 */
class BorradorYVistaPreviaIT extends BaseIT {

    private String crearConEstado(String bearer, String estado) throws Exception {
        String cuerpo = """
                {"tipo":"OPCION_MULTIPLE","enunciado":"Una pregunta a medio escribir",
                 "payload":{"opciones":[{"id":"a","texto":"Una"},{"id":"b","texto":"Otra"}],"multiple":false},
                 "criterio":{"correctas":["a"]},
                 "estado":"%s"}
                """.formatted(estado);
        String r = mvc.perform(post("/teoricos/items")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(r).get("id").asText();
    }

    private int componerCon(String itemId, String bearer) throws Exception {
        return mvc.perform(post("/teoricos/contenidos")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Un cuestionario","escala":"PORCENTUAL",
                                 "cursoCohorteId":"11111111-1111-1111-1111-111111111111","unidadId":"u01",
                                 "items":[{"itemId":"%s","orden":1,"puntaje":100}]}
                                """.formatted(itemId)))
                .andReturn().getResponse().getStatus();
    }

    @Test
    void el_item_que_no_dice_nada_nace_listo() throws Exception {
        // La compatibilidad: todo lo que ya existe en la base, y todo lo que
        // manden los clientes viejos, sigue siendo usable.
        String bearer = bearerProfesor();
        String id = itemOpcionMultiple(bearer);

        mvc.perform(get("/teoricos/items/{id}", id).header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("LISTO"));
    }

    @Test
    void un_borrador_no_entra_a_un_cuestionario() throws Exception {
        String bearer = bearerProfesor();
        String borrador = crearConEstado(bearer, "BORRADOR");

        mvc.perform(post("/teoricos/contenidos")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Un cuestionario","escala":"PORCENTUAL",
                                 "cursoCohorteId":"11111111-1111-1111-1111-111111111111","unidadId":"u01",
                                 "items":[{"itemId":"%s","orden":1,"puntaje":100}]}
                                """.formatted(borrador)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.clave").value("ITEM_EN_BORRADOR"))
                // Campo navegable: el front marca la linea exacta del armado.
                .andExpect(jsonPath("$.campo").value("items[0].itemId"));
    }

    @Test
    void marcarlo_listo_lo_habilita() throws Exception {
        String bearer = bearerProfesor();
        String id = crearConEstado(bearer, "BORRADOR");

        mvc.perform(put("/teoricos/items/{id}", id)
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"OPCION_MULTIPLE","enunciado":"Ahora si esta terminada",
                                 "payload":{"opciones":[{"id":"a","texto":"Una"},{"id":"b","texto":"Otra"}],"multiple":false},
                                 "criterio":{"correctas":["a"]},
                                 "estado":"LISTO"}
                                """))
                .andExpect(status().isOk());

        assertThat(componerCon(id, bearer)).isEqualTo(201);
    }

    @Test
    void un_item_ya_compuesto_no_puede_volver_a_borrador() throws Exception {
        // El agujero que la regla tapa: con referencia flotante (CI-13) el
        // cuestionario sirve siempre la ultima version, asi que un borrador
        // sobre un item ya compuesto le llegaria al alumno igual — el estado
        // seria invisible justo en el caso para el que existe.
        String bearer = bearerProfesor();
        String id = itemOpcionMultiple(bearer);
        assertThat(componerCon(id, bearer)).isEqualTo(201);

        mvc.perform(put("/teoricos/items/{id}", id)
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"OPCION_MULTIPLE","enunciado":"Cual es la capital de Francia?",
                                 "payload":{"opciones":[{"id":"a","texto":"Paris"},{"id":"b","texto":"Roma"}],"multiple":false},
                                 "criterio":{"correctas":["a"]},
                                 "estado":"BORRADOR"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.clave").value("ITEM_YA_COMPUESTO"));
    }

    @Test
    void la_vista_previa_no_trae_la_clave_de_correccion() throws Exception {
        // Lo que hace valer a la vista previa: devuelve la MISMA clase que el
        // endpoint del alumno, asi que no puede estar limpia si aquel no lo
        // esta. Es el mismo test que VistaAlumnoSinCriterioIT hace sobre el
        // cuestionario, aplicado al item suelto.
        String bearer = bearerProfesor();
        String id = itemOpcionMultiple(bearer);

        String cuerpo = mvc.perform(get("/teoricos/items/{id}/vista-previa", id)
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enunciado").value("Cual es la capital de Francia?"))
                .andExpect(jsonPath("$.payload.opciones[0].texto").value("Paris"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(cuerpo).doesNotContain("criterio").doesNotContain("correctas");

        JsonNode vista = json.readTree(cuerpo);
        assertThat(vista.has("criterio")).isFalse();
        // Orden y puntaje los decide el profesor al componer, y todavia no compuso.
        assertThat(vista.get("orden").asInt()).isZero();
        assertThat(vista.get("puntaje").asInt()).isZero();
    }

    @Test
    void la_vista_previa_de_un_item_ajeno_no_existe() throws Exception {
        String id = itemOpcionMultiple(bearerProfesor());

        mvc.perform(get("/teoricos/items/{id}/vista-previa", id)
                        .header("Authorization", bearerDeOtroProfesor(java.util.UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.clave").value("ITEM_INEXISTENTE"));
    }
}
