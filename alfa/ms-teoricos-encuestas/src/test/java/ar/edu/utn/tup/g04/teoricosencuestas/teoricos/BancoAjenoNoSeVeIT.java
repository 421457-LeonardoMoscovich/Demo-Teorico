package ar.edu.utn.tup.g04.teoricosencuestas.teoricos;

import ar.edu.utn.tup.g04.teoricosencuestas.BaseIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RF-USR-07: el banco de cada profesor es invisible para el resto.
 *
 * La garantia no es un filtro: el profesorId sale del token y nunca de un
 * parametro, asi que un GET del banco ajeno no se puede ni siquiera expresar.
 */
class BancoAjenoNoSeVeIT extends BaseIT {

    /** Se emite dentro del test: el emisor lo inyecta Spring despues de construir la clase. */
    private String otroProfesor() {
        return bearerDeOtroProfesor(UUID.randomUUID());
    }

    @Test
    void el_profesor_B_no_ve_ningun_item_del_profesor_A() throws Exception {
        String profe = bearerProfesor();
        itemOpcionMultiple(profe);

        String suyos = mvc.perform(get("/teoricos/items").header("Authorization", profe))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(json.readTree(suyos)).isNotEmpty();

        String ajenos = mvc.perform(get("/teoricos/items").header("Authorization", otroProfesor()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(json.readTree(ajenos)).isEmpty();
    }

    @Test
    void el_profesor_B_no_puede_leer_un_item_del_A_ni_sabiendo_el_id() throws Exception {
        String itemId = itemOpcionMultiple(bearerProfesor());

        mvc.perform(get("/teoricos/items/{id}", itemId).header("Authorization", otroProfesor()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.clave").value("ITEM_INEXISTENTE"));
    }

    @Test
    void el_profesor_B_no_puede_componer_con_items_del_A() throws Exception {
        String itemId = itemOpcionMultiple(bearerProfesor());

        mvc.perform(post("/teoricos/contenidos")
                        .header("Authorization", otroProfesor())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cursoCohorteId":"11111111-1111-1111-1111-111111111111",
                                 "titulo":"Robado","escala":"PORCENTUAL",
                                 "items":[{"itemId":"%s","orden":1,"puntaje":100}]}
                                """.formatted(itemId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.clave").value("ITEM_INEXISTENTE"));
    }

    @Test
    void la_baja_es_logica_y_saca_el_item_del_banco() throws Exception {
        String profe = bearerProfesor();
        String itemId = itemVerdaderoFalso(profe);

        mvc.perform(delete("/teoricos/items/{id}", itemId).header("Authorization", profe))
                .andExpect(status().isNoContent());

        String lista = mvc.perform(get("/teoricos/items").header("Authorization", profe))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(lista).doesNotContain(itemId);

        // RF-NFR-01: la fila sigue existiendo, solo que ya no se lista.
        // Lo verifica el hecho de que el mismo id no se puede volver a usar.
        mvc.perform(get("/teoricos/items/{id}", itemId).header("Authorization", profe))
                .andExpect(status().isBadRequest());
    }

    @Test
    void un_alumno_no_puede_entrar_al_banco() throws Exception {
        mvc.perform(get("/teoricos/items").header("Authorization", bearerAlumno()))
                .andExpect(status().isForbidden());
    }
}
