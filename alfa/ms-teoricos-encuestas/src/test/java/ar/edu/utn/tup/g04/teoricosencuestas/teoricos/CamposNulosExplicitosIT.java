package ar.edu.utn.tup.g04.teoricosencuestas.teoricos;

import ar.edu.utn.tup.g04.teoricosencuestas.BaseIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Mandar {@code "devolucion": null} tiene que valer lo mismo que no mandar el
 * campo (CI-58).
 *
 * No es purismo de contrato: es lo que hace cualquier cliente que arma el
 * cuerpo desde un objeto —el front lo hacia— y Jackson convierte ese null en un
 * NullNode, que no es null y se cuela por todos los {@code == null} del
 * servicio. El sintoma era un PAYLOAD_INVALIDO al crear cualquier item sin
 * retroalimentacion; el dano silencioso, un {@code "null"} guardado en el jsonb
 * que recien reventaba al corregir.
 */
class CamposNulosExplicitosIT extends BaseIT {

    private static final String CON_NULOS = """
            {"tipo":"OPCION_MULTIPLE","enunciado":"¿Cuál de estas es la correcta?",
             "payload":{"opciones":[{"id":"a","texto":"Una"},{"id":"b","texto":"Otra"}],"multiple":false},
             "criterio":{"correctas":["a"],"pesos":null},
             "devolucion":null,
             "estado":"LISTO"}
            """;

    @Test
    void un_item_con_devolucion_nula_explicita_se_crea_igual() throws Exception {
        String bearer = bearerProfesor();

        String r = mvc.perform(post("/teoricos/items")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CON_NULOS))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

        String id = json.readTree(r).get("id").asText();

        // Y queda sin devolucion, no con una vacia: la ausencia se conserva.
        mvc.perform(get("/teoricos/items/{id}", id).header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.devolucion").value((Object) null));
    }
}
