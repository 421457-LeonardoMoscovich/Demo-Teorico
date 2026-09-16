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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Preguntas al azar por etiqueta (V10).
 *
 * Lo que estos tests fijan es la propiedad que hace que el sorteo no rompa
 * CI-19: el subconjunto de cada alumno se DERIVA de (contenidoId, alumnoId) y
 * no se guarda en ningun lado. De ahi salen las dos afirmaciones que hay que
 * poder sostener frente a un reclamo: dos alumnos reciben examenes distintos, y
 * el mismo alumno recibe siempre el suyo, recargue cuando recargue.
 */
class SorteoPorEtiquetaIT extends BaseIT {

    private static final UUID OTRO_ALUMNO = UUID.fromString("00000000-0000-0000-0000-0000000000b2");

    @Test
    void dos_alumnos_reciben_preguntas_distintas_del_mismo_cuestionario() throws Exception {
        String profe = bearerProfesor();
        sembrar(profe, 8, "microservicios");
        UUID contenidoId = componerConSorteo(profe, "microservicios", 3, 33, "[]");

        List<String> unas = enunciadosDe(contenidoId, idAlumno);
        List<String> otras = enunciadosDe(contenidoId, OTRO_ALUMNO);

        assertThat(unas).hasSize(3);
        assertThat(otras).hasSize(3);
        // Con 8 candidatas y 3 sorteadas, que a los dos les toque exactamente el
        // mismo trio es posible pero improbable (1 en 56). Si esto empieza a
        // fallar, el sorteo dejo de depender de quien pregunta.
        assertThat(unas).isNotEqualTo(otras);
    }

    @Test
    void el_mismo_alumno_recibe_siempre_las_mismas() throws Exception {
        String profe = bearerProfesor();
        sembrar(profe, 8, "microservicios");
        UUID contenidoId = componerConSorteo(profe, "microservicios", 3, 33, "[]");

        // Recargar es volver a pedir la vista. Que devuelva lo mismo es lo que
        // permite que no haya una tabla de "que le toco a quien": el sorteo se
        // recalcula igual cada vez.
        assertThat(enunciadosDe(contenidoId, idAlumno))
                .isEqualTo(enunciadosDe(contenidoId, idAlumno));
    }

    @Test
    void el_mixto_le_da_las_fijas_a_todos_y_sortea_el_resto() throws Exception {
        String profe = bearerProfesor();
        String fija = itemVerdaderoFalso(profe);
        sembrar(profe, 6, "datos");

        UUID contenidoId = componerConSorteo(profe, "datos", 2, 30, lineaFija(fija, 40));

        List<String> unas = enunciadosDe(contenidoId, idAlumno);
        List<String> otras = enunciadosDe(contenidoId, OTRO_ALUMNO);
        String laFija = "Un microservicio puede tener su propia base.";

        assertThat(unas).hasSize(3).contains(laFija);
        assertThat(otras).hasSize(3).contains(laFija);
    }

    @Test
    void una_pregunta_fija_no_puede_volver_a_salir_sorteada() throws Exception {
        // Que la misma pregunta aparezca dos veces en el mismo examen es el
        // error que mas rapido se ve y peor queda.
        String profe = bearerProfesor();
        List<String> ids = sembrar(profe, 3, "chica");
        UUID contenidoId = componerConSorteo(profe, "chica", 2, 30, lineaFija(ids.get(0), 40));

        List<String> vistas = enunciadosDe(contenidoId, idAlumno);
        assertThat(vistas).hasSize(3).doesNotHaveDuplicates();
    }

    @Test
    void la_ficha_cuenta_las_sorteadas_sin_nombrarlas() throws Exception {
        String profe = bearerProfesor();
        sembrar(profe, 5, "microservicios");
        JsonNode ficha = componer(profe, "PORCENTUAL", "[]", "microservicios", 4, 25);

        // El 03 recibe "4 preguntas · 100 puntos" y sigue sin saber cuales son,
        // ni que algunas se sortean: adentro de la caja no mira.
        assertThat(ficha.get("resumen").asText()).isEqualTo("4 preguntas · 100 puntos");
        assertThat(ficha.size()).isEqualTo(5);
    }

    @Test
    void alcanza_una_abierta_en_la_bolsa_para_que_la_correccion_sea_diferida() throws Exception {
        // El Tema 03 recibe UN modo de correccion, no uno por alumno: si alguna
        // de las que pueden salir espera a un humano, el cuestionario entero es
        // DIFERIDO aunque a la mayoria le toquen automaticas.
        String profe = bearerProfesor();
        sembrar(profe, 4, "mixta");
        crearEtiquetado(profe, "ABIERTA", "Explica el acoplamiento de esquema.",
                "{\"consigna\":\"Desarrolla.\",\"extensionMaxima\":200}",
                "{\"rubrica\":\"Completo si nombra una consecuencia sobre el despliegue.\"}",
                "mixta");

        JsonNode ficha = componer(profe, "PORCENTUAL", "[]", "mixta", 2, 50);
        assertThat(ficha.get("correccion").asText()).isEqualTo("DIFERIDA");
    }

    @Test
    void pedir_mas_preguntas_de_las_que_hay_se_rechaza_nombrando_el_campo() throws Exception {
        String profe = bearerProfesor();
        sembrar(profe, 2, "escasa");

        mvc.perform(post("/teoricos/contenidos")
                        .header("Authorization", profe)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Imposible", "PORCENTUAL", "[]", "escasa", 5, 20)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.clave").value("SORTEO_SIN_CANDIDATOS"))
                .andExpect(jsonPath("$.campo").value("regla.cuantos"));
    }

    @Test
    void los_pesos_cierran_en_100_contando_lo_que_aporta_el_sorteo() throws Exception {
        String profe = bearerProfesor();
        sembrar(profe, 5, "pesos");

        mvc.perform(post("/teoricos/contenidos")
                        .header("Authorization", profe)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("No cierra", "PORCENTUAL", "[]", "pesos", 3, 25)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.clave").value("PESOS_NO_SUMAN_100"));
    }

    @Test
    void el_alumno_entrega_y_se_corrige_sobre_las_que_le_tocaron_a_el() throws Exception {
        String profe = bearerProfesor();
        sembrar(profe, 5, "correccion");
        UUID contenidoId = componerConSorteo(profe, "correccion", 2, 50, "[]");

        StringBuilder respuestas = new StringBuilder();
        for (String version : versionesDe(contenidoId, idAlumno)) {
            if (respuestas.length() > 0) {
                respuestas.append(",");
            }
            respuestas.append("{\"itemVersionId\":\"").append(version)
                    .append("\",\"contenido\":{\"seleccionadas\":[\"a\"]}}");
        }

        UUID entregaId = UUID.randomUUID();
        mvc.perform(post("/teoricos/evaluaciones")
                        .header("X-Despacho-Key", CLAVE_DESPACHO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(despacho(entregaId, contenidoId, idAlumno, respuestas.toString())))
                .andExpect(status().isAccepted());

        // Contesto bien las dos que le tocaron y saca 100: el corrector
        // reconstruyo el subconjunto de ESTE alumno y no el del cuestionario en
        // abstracto —que, con sorteo, no existe—.
        mvc.perform(get("/teoricos/evaluaciones/{entregaId}", entregaId)
                        .header("Authorization", bearerAlumno(idAlumno)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FINAL"))
                .andExpect(jsonPath("$.nota").value(100))
                .andExpect(jsonPath("$.detalle.length()").value(2));
    }

    @Test
    void contestar_una_que_no_le_toco_se_rechaza() throws Exception {
        String profe = bearerProfesor();
        sembrar(profe, 6, "ajena");
        UUID contenidoId = componerConSorteo(profe, "ajena", 2, 50, "[]");

        List<String> mias = versionesDe(contenidoId, idAlumno);
        List<String> ajenas = new ArrayList<>(versionesDe(contenidoId, OTRO_ALUMNO));
        ajenas.removeAll(mias);
        assertThat(ajenas).isNotEmpty();

        String respuestas = "{\"itemVersionId\":\"" + mias.get(0)
                + "\",\"contenido\":{\"seleccionadas\":[\"a\"]}},"
                + "{\"itemVersionId\":\"" + ajenas.get(0)
                + "\",\"contenido\":{\"seleccionadas\":[\"a\"]}}";

        mvc.perform(post("/teoricos/evaluaciones")
                        .header("X-Despacho-Key", CLAVE_DESPACHO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(despacho(UUID.randomUUID(), contenidoId, idAlumno, respuestas)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.clave").value("RESPUESTA_DE_ITEM_AJENO"));
    }

    @Test
    void la_profesora_ve_la_regla_y_cuantas_candidatas_hay_hoy() throws Exception {
        String profe = bearerProfesor();
        sembrar(profe, 7, "vista");
        UUID contenidoId = componerConSorteo(profe, "vista", 4, 25, "[]");

        mvc.perform(get("/teoricos/contenidos/{id}/vista-profesor", contenidoId)
                        .header("Authorization", profe))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regla.etiqueta").value("vista"))
                .andExpect(jsonPath("$.regla.cuantos").value(4))
                .andExpect(jsonPath("$.regla.candidatos").value(7))
                // Las sorteadas NO se listan: no existen hasta que hay un alumno.
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.puntajeTotal").value(100));
    }

    @Test
    void el_banco_ajeno_no_entra_al_sorteo() throws Exception {
        // La etiqueta no puede ser una puerta al banco de otro profesor: es el
        // mismo limite que fija EtiquetasDelBancoIT, ahora para el sorteo.
        String profe = bearerProfesor();
        String otro = bearerDeOtroProfesor(UUID.randomUUID());
        sembrar(profe, 2, "compartida");
        sembrar(otro, 6, "compartida");

        mvc.perform(post("/teoricos/contenidos")
                        .header("Authorization", profe)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Del banco ajeno", "LIBRE", "[]", "compartida", 4, 25)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.clave").value("SORTEO_SIN_CANDIDATOS"));
    }

    // ------------------------------------------------------------------ ayuda -

    private List<String> sembrar(String bearer, int cuantos, String etiqueta) throws Exception {
        List<String> ids = new ArrayList<>();
        for (int i = 1; i <= cuantos; i++) {
            ids.add(crearEtiquetado(bearer, "OPCION_MULTIPLE",
                    "Pregunta de " + etiqueta + " numero " + i,
                    "{\"opciones\":[{\"id\":\"a\",\"texto\":\"Si\"},"
                            + "{\"id\":\"b\",\"texto\":\"No\"}],\"multiple\":false}",
                    "{\"correctas\":[\"a\"]}", etiqueta));
        }
        return ids;
    }

    private String crearEtiquetado(String bearer, String tipo, String enunciado, String payload,
                                   String criterio, String etiqueta) throws Exception {
        String cuerpo = "{\"tipo\":\"" + tipo + "\",\"enunciado\":\"" + enunciado
                + "\",\"payload\":" + payload + ",\"criterio\":" + criterio
                + ",\"etiquetas\":[\"" + etiqueta + "\"]}";
        String respuesta = mvc.perform(post("/teoricos/items")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(respuesta).get("id").asText();
    }

    private String lineaFija(String itemId, int puntaje) {
        return "[{\"itemId\":\"" + itemId + "\",\"orden\":1,\"puntaje\":" + puntaje + "}]";
    }

    private String cuerpo(String titulo, String escala, String itemsJson, String etiqueta,
                          int cuantos, int puntaje) {
        return "{\"cursoCohorteId\":\"11111111-1111-1111-1111-111111111111\","
                + "\"titulo\":\"" + titulo + "\",\"escala\":\"" + escala + "\","
                + "\"items\":" + itemsJson + ","
                + "\"regla\":{\"etiqueta\":\"" + etiqueta + "\",\"cuantos\":" + cuantos
                + ",\"puntaje\":" + puntaje + "}}";
    }

    private JsonNode componer(String bearer, String escala, String itemsJson, String etiqueta,
                              int cuantos, int puntaje) throws Exception {
        String respuesta = mvc.perform(post("/teoricos/contenidos")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Parcial sorteado", escala, itemsJson, etiqueta,
                                cuantos, puntaje)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(respuesta);
    }

    private UUID componerConSorteo(String bearer, String etiqueta, int cuantos, int puntaje,
                                   String itemsJson) throws Exception {
        return UUID.fromString(componer(bearer, "LIBRE", itemsJson, etiqueta, cuantos, puntaje)
                .get("contenidoId").asText());
    }

    private String despacho(UUID entregaId, UUID contenidoId, UUID alumnoId, String respuestas) {
        return "{\"entregaId\":\"" + entregaId + "\",\"desafioId\":\"" + UUID.randomUUID()
                + "\",\"alumnoId\":\"" + alumnoId
                + "\",\"cursoCohorteId\":\"11111111-1111-1111-1111-111111111111\",\"intento\":1,"
                + "\"contenidoBinding\":{\"contenidoId\":\"" + contenidoId + "\",\"version\":1},"
                + "\"respuestas\":[" + respuestas + "]}";
    }

    private JsonNode vistaDe(UUID contenidoId, UUID alumnoId) throws Exception {
        return json.readTree(
                mvc.perform(get("/teoricos/contenidos/{id}/vista-alumno", contenidoId)
                                .header("Authorization", bearerAlumno(alumnoId))
                                .header("X-Vale-Lectura", vale(contenidoId, alumnoId, 15)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private List<String> enunciadosDe(UUID contenidoId, UUID alumnoId) throws Exception {
        List<String> enunciados = new ArrayList<>();
        for (JsonNode i : vistaDe(contenidoId, alumnoId).get("items")) {
            enunciados.add(i.get("enunciado").asText());
        }
        return enunciados;
    }

    private List<String> versionesDe(UUID contenidoId, UUID alumnoId) throws Exception {
        List<String> versiones = new ArrayList<>();
        for (JsonNode i : vistaDe(contenidoId, alumnoId).get("items")) {
            versiones.add(i.get("itemVersionId").asText());
        }
        return versiones;
    }
}
