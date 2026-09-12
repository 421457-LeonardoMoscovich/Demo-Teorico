package ar.edu.utn.tup.g04.teoricosencuestas.teoricos;

import ar.edu.utn.tup.g04.teoricosencuestas.BaseIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Las etiquetas del banco.
 *
 * Lo que prueba, mas alla del alta y el filtro: que la etiqueta NO sea una
 * puerta al banco ajeno, y que reetiquetar no publique una version nueva —que es
 * lo que pasaria si las etiquetas vivieran en `item_version`—.
 */
class EtiquetasDelBancoIT extends BaseIT {

    private String crearEtiquetado(String bearer, String enunciado, String etiquetas)
            throws Exception {
        String cuerpo = """
                {"tipo":"VERDADERO_FALSO","enunciado":"%s",
                 "payload":{"afirmacion":"%s"},
                 "criterio":{"esVerdadero":true},
                 "etiquetas":%s}
                """.formatted(enunciado, enunciado, etiquetas);
        String r = mvc.perform(post("/teoricos/items")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(r).get("id").asText();
    }

    @Test
    void se_guardan_normalizadas_y_ordenadas() throws Exception {
        // "  Microservicios " y "MICROSERVICIOS" son la misma etiqueta: sin
        // normalizar, el filtro deja de servir justo cuando el banco crece.
        String bearer = bearerProfesor();
        String id = crearEtiquetado(bearer, "Una pregunta",
                "[\"  Microservicios \",\"Concurrencia\",\"MICROSERVICIOS\"]");

        mvc.perform(get("/teoricos/items/{id}", id).header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etiquetas.length()").value(2))
                .andExpect(jsonPath("$.etiquetas[0]").value("concurrencia"))
                .andExpect(jsonPath("$.etiquetas[1]").value("microservicios"));
    }

    @Test
    void el_acento_no_hace_dos_etiquetas() throws Exception {
        String bearer = bearerProfesor();
        String id = crearEtiquetado(bearer, "Otra pregunta", "[\"prácticos\",\"practicos\"]");

        mvc.perform(get("/teoricos/items/{id}", id).header("Authorization", bearer))
                .andExpect(jsonPath("$.etiquetas.length()").value(1))
                .andExpect(jsonPath("$.etiquetas[0]").value("practicos"));
    }

    @Test
    void el_filtro_del_banco_trae_solo_las_de_esa_etiqueta() throws Exception {
        // Profesor propio del test: contar sobre el banco compartido de
        // bearerProfesor() hace que el resultado dependa de que otros tests
        // corrieron antes, que es la receta del test que falla los martes.
        String bearer = bearerDeOtroProfesor(UUID.randomUUID());
        crearEtiquetado(bearer, "De kafka", "[\"kafka\"]");
        crearEtiquetado(bearer, "De sagas", "[\"sagas\"]");

        mvc.perform(get("/teoricos/items").param("etiqueta", "kafka")
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].enunciado").value("De kafka"));

        // Y se busca igual que se guarda: con acento, en mayuscula, da lo mismo.
        mvc.perform(get("/teoricos/items").param("etiqueta", "KAFKA")
                        .header("Authorization", bearer))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void el_filtro_por_tipo_y_por_etiqueta_se_combinan_con_y() throws Exception {
        String bearer = bearerDeOtroProfesor(UUID.randomUUID());
        crearEtiquetado(bearer, "Un vf de kafka", "[\"kafka\"]");
        // El de opcion multiple NO lleva la etiqueta: el filtro tiene que
        // descartarlo por tipo Y el vf ajeno por etiqueta.
        crearItem("OPCION_MULTIPLE", "Un om de kafka",
                """
                {"opciones":[{"id":"a","texto":"Una"},{"id":"b","texto":"Otra"}],"multiple":false}
                """,
                """
                {"correctas":["a"]}
                """, bearer);

        mvc.perform(get("/teoricos/items")
                        .param("tipo", "VERDADERO_FALSO").param("etiqueta", "kafka")
                        .header("Authorization", bearer))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].enunciado").value("Un vf de kafka"));
    }

    @Test
    void reetiquetar_no_publica_una_version_nueva() throws Exception {
        // Las etiquetas cuelgan del item y no de la version (ver V8): cambiar
        // una no es un cambio de contenido, y versionarlo ensuciaria el
        // historial que D-04 existe para mantener legible.
        String bearer = bearerProfesor();
        String id = crearEtiquetado(bearer, "Una pregunta", "[\"vieja\"]");

        mvc.perform(get("/teoricos/items/{id}", id).header("Authorization", bearer))
                .andExpect(jsonPath("$.version").value(1));

        mvc.perform(put("/teoricos/items/{id}", id)
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"VERDADERO_FALSO","enunciado":"Una pregunta",
                                 "payload":{"afirmacion":"Una pregunta"},
                                 "criterio":{"esVerdadero":true},
                                 "etiquetas":["nueva"]}
                                """))
                .andExpect(status().isOk())
                // Sube a 2 porque un PUT SIEMPRE publica version (D-04): lo que
                // se prueba es que las etiquetas son las nuevas y no se
                // acumularon con las viejas.
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.etiquetas.length()").value(1))
                .andExpect(jsonPath("$.etiquetas[0]").value("nueva"));
    }

    @Test
    void no_mandar_el_campo_deja_las_etiquetas_que_ya_tenia() throws Exception {
        // La compatibilidad: un cliente que no sabe de etiquetas no puede
        // borrarlas sin querer. Ausente y vacia NO son lo mismo.
        String bearer = bearerProfesor();
        String id = crearEtiquetado(bearer, "Una pregunta", "[\"queda\"]");

        mvc.perform(put("/teoricos/items/{id}", id)
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"VERDADERO_FALSO","enunciado":"Una pregunta",
                                 "payload":{"afirmacion":"Una pregunta"},
                                 "criterio":{"esVerdadero":true}}
                                """))
                .andExpect(jsonPath("$.etiquetas[0]").value("queda"));
    }

    @Test
    void la_lista_vacia_si_las_saca_todas() throws Exception {
        String bearer = bearerProfesor();
        String id = crearEtiquetado(bearer, "Una pregunta", "[\"se\",\"va\"]");

        mvc.perform(put("/teoricos/items/{id}", id)
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"VERDADERO_FALSO","enunciado":"Una pregunta",
                                 "payload":{"afirmacion":"Una pregunta"},
                                 "criterio":{"esVerdadero":true},
                                 "etiquetas":[]}
                                """))
                .andExpect(jsonPath("$.etiquetas.length()").value(0));
    }

    @Test
    void mas_de_ocho_se_rechaza() throws Exception {
        String bearer = bearerProfesor();
        mvc.perform(post("/teoricos/items")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"VERDADERO_FALSO","enunciado":"Una pregunta",
                                 "payload":{"afirmacion":"Una pregunta"},
                                 "criterio":{"esVerdadero":true},
                                 "etiquetas":["a","b","c","d","e","f","g","h","i"]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.clave").value("DEMASIADAS_ETIQUETAS"));
    }

    @Test
    void el_vocabulario_es_de_cada_profesor_y_no_se_mezcla() throws Exception {
        // La prueba que importa: la etiqueta no puede ser una puerta al banco
        // ajeno (RF-USR-07). Aunque los dos usen la misma palabra, cada uno ve
        // el conteo de lo suyo.
        String mio = bearerDeOtroProfesor(UUID.randomUUID());
        String ajeno = bearerDeOtroProfesor(UUID.randomUUID());

        crearEtiquetado(mio, "Mi pregunta", "[\"compartida\"]");
        crearEtiquetado(mio, "Mi otra pregunta", "[\"compartida\"]");
        crearEtiquetado(ajeno, "Su pregunta", "[\"compartida\"]");

        mvc.perform(get("/teoricos/items/etiquetas").header("Authorization", ajeno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.etiqueta == 'compartida')].cuantos").value(1));

        mvc.perform(get("/teoricos/items").param("etiqueta", "compartida")
                        .header("Authorization", ajeno))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].enunciado").value("Su pregunta"));
    }

    @Test
    void un_item_dado_de_baja_no_cuenta_en_el_vocabulario() throws Exception {
        // Si contara, el filtro ofreceria una etiqueta que no trae nada y el
        // profesor creeria que el filtro esta roto.
        String bearer = bearerDeOtroProfesor(UUID.randomUUID());
        String id = crearEtiquetado(bearer, "Se va de baja", "[\"efimera\"]");

        mvc.perform(get("/teoricos/items/etiquetas").header("Authorization", bearer))
                .andExpect(jsonPath("$[?(@.etiqueta == 'efimera')].cuantos").value(1));

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/teoricos/items/{id}", id).header("Authorization", bearer))
                .andExpect(status().isNoContent());

        mvc.perform(get("/teoricos/items/etiquetas").header("Authorization", bearer))
                .andExpect(jsonPath("$[?(@.etiqueta == 'efimera')]").isEmpty());
    }
}
