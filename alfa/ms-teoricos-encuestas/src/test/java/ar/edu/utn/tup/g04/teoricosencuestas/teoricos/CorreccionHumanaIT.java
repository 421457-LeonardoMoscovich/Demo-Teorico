package ar.edu.utn.tup.g04.teoricosencuestas.teoricos;

import ar.edu.utn.tup.g04.teoricosencuestas.BaseIT;
import ar.edu.utn.tup.g04.teoricosencuestas.comun.eventos.PublicadorEnMemoria;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La correccion humana (D-01), que es la que obliga a que exista un estado de
 * espera.
 *
 * El test que justifica la feature entera es
 * `el_evento_no_sale_hasta_que_el_profesor_pone_el_ultimo_puntaje`: prueba que
 * el contrato con el Tema 03 SIEMPRE fue asincronico, y que con los cuatro
 * tipos automaticos eso no se notaba porque el evento salia en el mismo
 * milisegundo que la entrega.
 */
class CorreccionHumanaIT extends BaseIT {

    @Autowired
    PublicadorEnMemoria publicador;

    @BeforeEach
    void limpiarEventos() {
        publicador.limpiar();
    }

    /**
     * Arma un cuestionario mixto, lo entrega, y devuelve la entregaId.
     *
     * La respuesta abierta lleva una MARCA unica por corrida. Sin eso hay que
     * buscar el pendiente con `cola.get(0)`, que devuelve el mas viejo de toda
     * la base — y estos tests corren contra una base que sobrevive entre
     * corridas, asi que cada test terminaria corrigiendo el sobrante de otro.
     */
    private UUID entregarMixto(String profe, String marca) throws Exception {
        String om = itemOpcionMultiple(profe);
        String abierta = itemAbierta(profe);

        JsonNode ficha = componer("Parcial mixto", "PORCENTUAL", """
                [{"itemId":"%s","orden":1,"puntaje":40},
                 {"itemId":"%s","orden":2,"puntaje":60}]
                """.formatted(om, abierta), profe);

        assertThat(ficha.get("correccion").asText())
                .as("CI-07: alcanza un item que espere a un humano para que sea DIFERIDA")
                .isEqualTo("DIFERIDA");

        UUID contenidoId = UUID.fromString(ficha.get("contenidoId").asText());
        JsonNode vista = vistaAlumno(contenidoId);
        // Por TIPO y no por posicion: las preguntas le llegan barajadas al alumno,
        // asi que `items[0]` no es necesariamente la de opcion multiple.
        String vOm = versionDelTipo(vista, "OPCION_MULTIPLE");
        String vAbierta = versionDelTipo(vista, "ABIERTA");

        UUID entregaId = UUID.randomUUID();
        mvc.perform(post("/teoricos/evaluaciones")
                        .header("X-Despacho-Key", CLAVE_DESPACHO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entregaId":"%s","desafioId":"%s","alumnoId":"%s",
                                 "cursoCohorteId":"11111111-1111-1111-1111-111111111111","intento":1,
                                 "contenidoBinding":{"contenidoId":"%s","version":1},
                                 "respuestas":[
                                   {"itemVersionId":"%s","contenido":{"seleccionadas":["a"]}},
                                   {"itemVersionId":"%s","contenido":{"texto":"[%s] Porque acopla los esquemas y ya no se puede desplegar por separado."}}]}
                                """.formatted(entregaId, UUID.randomUUID(), idAlumno,
                                contenidoId, vOm, vAbierta, marca)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.correccion").value("DIFERIDA"));
        return entregaId;
    }

    private String versionDelTipo(JsonNode vista, String tipo) {
        for (JsonNode i : vista.get("items")) {
            if (tipo.equals(i.get("tipo").asText())) {
                return i.get("itemVersionId").asText();
            }
        }
        throw new AssertionError("La vista no trajo ningun item de tipo " + tipo);
    }

    private JsonNode vistaAlumno(UUID contenidoId) throws Exception {
        return json.readTree(mvc.perform(get("/teoricos/contenidos/{id}/vista-alumno", contenidoId)
                        .header("Authorization", bearerAlumno())
                        .header("X-Vale-Lectura", vale(contenidoId, idAlumno, 15)))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private JsonNode cola(String bearer) throws Exception {
        return json.readTree(mvc.perform(get("/teoricos/correcciones/pendientes")
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    /** El pendiente de ESTA entrega, y no el primero que haya en la cola. */
    private JsonNode pendienteCon(String bearer, String marca) throws Exception {
        for (JsonNode p : cola(bearer)) {
            if (p.get("respuesta").asText().contains("[" + marca + "]")) {
                return p;
            }
        }
        throw new AssertionError("No quedo pendiente el item de la marca " + marca);
    }

    private String marca() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void la_entrega_queda_sin_nota_con_lo_automatico_ya_puntuado() throws Exception {
        UUID entregaId = entregarMixto(bearerProfesor(), marca());

        mvc.perform(get("/teoricos/evaluaciones/{id}", entregaId)
                        .header("Authorization", bearerAlumno()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_ESPERA"))
                // La nota es del cuestionario entero o no es: nada de notas parciales.
                .andExpect(jsonPath("$.nota").doesNotExist())
                // Lo automatico YA esta corregido, y el alumno lo puede ver.
                .andExpect(jsonPath("$.detalle[0].obtenido").value(40))
                .andExpect(jsonPath("$.detalle[0].pendiente").value(false))
                // Lo abierto es un hueco, no un 0: decirle 0 seria mentirle.
                .andExpect(jsonPath("$.detalle[1].obtenido").doesNotExist())
                .andExpect(jsonPath("$.detalle[1].correcto").doesNotExist())
                .andExpect(jsonPath("$.detalle[1].pendiente").value(true));
    }

    @Test
    void el_evento_no_sale_hasta_que_el_profesor_pone_el_ultimo_puntaje() throws Exception {
        String profe = bearerProfesor();
        String marca = marca();
        UUID entregaId = entregarMixto(profe, marca);

        assertThat(publicador.publicados())
                .as("con correccion pendiente el Tema 03 NO se entera: no hay nota que contarle")
                .isEmpty();

        String detalleId = pendienteCon(profe, marca).get("detalleId").asText();
        mvc.perform(post("/teoricos/correcciones/{id}", detalleId)
                        .header("Authorization", profe)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"obtenido\":45}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrada").value(true));

        assertThat(publicador.publicados())
                .as("recien cuando hay nota sale el evento, y sale UNO SOLO")
                .hasSize(1);

        var evento = publicador.publicados().get(0).evento();
        assertThat(evento.eventType()).isEqualTo("TEORICO_CORREGIDO");
        assertThat(evento.payload().get("entregaId")).hasToString(entregaId.toString());
        assertThat(evento.payload().get("nota")).isEqualTo(85);

        mvc.perform(get("/teoricos/evaluaciones/{id}", entregaId)
                        .header("Authorization", bearerAlumno()))
                .andExpect(jsonPath("$.estado").value("FINAL"))
                .andExpect(jsonPath("$.nota").value(85))
                // El cuestionario era mixto, pero la nota la termino de definir
                // un humano, y eso es lo que el corrector tiene que decir.
                .andExpect(jsonPath("$.corrector").value("HUMANO"));
    }

    @Test
    void la_rubrica_le_llega_al_profesor_y_nunca_al_alumno() throws Exception {
        String profe = bearerProfesor();
        String abierta = itemAbierta(profe);
        JsonNode ficha = componer("Solo abierta", "PORCENTUAL", """
                [{"itemId":"%s","orden":1,"puntaje":100}]
                """.formatted(abierta), profe);
        UUID contenidoId = UUID.fromString(ficha.get("contenidoId").asText());

        // Sobre el JSON CRUDO y no sobre el objeto: lo que importa es que la
        // rubrica no viaje por el cable, no que el DTO no la exponga.
        String crudo = mvc.perform(get("/teoricos/contenidos/{id}/vista-alumno", contenidoId)
                        .header("Authorization", bearerAlumno())
                        .header("X-Vale-Lectura", vale(contenidoId, idAlumno, 15)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(crudo).doesNotContain("rubrica");
        assertThat(crudo).doesNotContain("acoplamiento");
        assertThat(crudo).as("la consigna SI: es lo que el alumno tiene que leer")
                .contains("Desarrolla en no mas de 200 palabras");
    }

    @Test
    void un_profesor_no_ve_la_cola_de_otro() throws Exception {
        entregarMixto(bearerProfesor(), marca());

        JsonNode ajena = cola(bearerDeOtroProfesor(UUID.randomUUID()));
        assertThat(ajena).as("la cola se filtra por el dueno del contenido, igual que el banco")
                .isEmpty();
    }

    @Test
    void un_alumno_no_puede_entrar_a_la_cola() throws Exception {
        mvc.perform(get("/teoricos/correcciones/pendientes")
                        .header("Authorization", bearerAlumno()))
                .andExpect(status().isForbidden());
    }

    @Test
    void una_correccion_no_se_pisa() throws Exception {
        String profe = bearerProfesor();
        String marca = marca();
        entregarMixto(profe, marca);
        String detalleId = pendienteCon(profe, marca).get("detalleId").asText();

        mvc.perform(post("/teoricos/correcciones/{id}", detalleId)
                        .header("Authorization", profe)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"obtenido\":30}"))
                .andExpect(status().isOk());

        // CI-44: para cambiar una nota ya puesta existe el recalculo (CI-50),
        // que deja rastro. Volver a postear no puede ser el camino.
        mvc.perform(post("/teoricos/correcciones/{id}", detalleId)
                        .header("Authorization", profe)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"obtenido\":60}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.clave").value("DETALLE_YA_CORREGIDO"));
    }

    @Test
    void no_se_puede_poner_mas_puntaje_del_que_vale_el_item() throws Exception {
        String profe = bearerProfesor();
        String marca = marca();
        entregarMixto(profe, marca);
        JsonNode pendiente = pendienteCon(profe, marca);

        assertThat(pendiente.get("puntaje").asInt()).isEqualTo(60);
        assertThat(pendiente.get("rubrica").asText()).contains("acoplamiento");
        assertThat(pendiente.get("respuesta").asText()).contains("desplegar por separado");

        mvc.perform(post("/teoricos/correcciones/{id}", pendiente.get("detalleId").asText())
                        .header("Authorization", profe)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"obtenido\":61}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.clave").value("PUNTAJE_FUERA_DE_RANGO"))
                .andExpect(jsonPath("$.campo").value("obtenido"));
    }
}
