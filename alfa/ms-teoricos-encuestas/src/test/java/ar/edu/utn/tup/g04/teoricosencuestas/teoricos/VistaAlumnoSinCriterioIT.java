package ar.edu.utn.tup.g04.teoricosencuestas.teoricos;

import ar.edu.utn.tup.g04.teoricosencuestas.BaseIT;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La frontera de seguridad de CI-17.
 *
 * La afirmacion es sobre el JSON CRUDO y no sobre el DTO: lo que se filtra es
 * lo serializado, y un anidamiento inesperado —un campo esCorrecta metido
 * adentro de las opciones "para simplificar"— no lo ve un test que mire tipos.
 */
class VistaAlumnoSinCriterioIT extends BaseIT {

    private static final String[] CLAVES_DE_CORRECCION = {
            "criterio", "correctas", "esVerdadero", "pares", "secuencia"
    };

    @Test
    void la_vista_del_alumno_no_contiene_ninguna_clave_de_correccion() throws Exception {
        String profe = bearerProfesor();
        String om = itemOpcionMultiple(profe);
        String vf = itemVerdaderoFalso(profe);

        var ficha = componer("Parcial", "PORCENTUAL", """
                [{"itemId":"%s","orden":1,"puntaje":60},
                 {"itemId":"%s","orden":2,"puntaje":40}]
                """.formatted(om, vf), profe);

        UUID contenidoId = UUID.fromString(ficha.get("contenidoId").asText());

        String crudo = mvc.perform(get("/teoricos/contenidos/{id}/vista-alumno", contenidoId)
                        .header("Authorization", bearerAlumno())
                        .header("X-Vale-Lectura", vale(contenidoId, idAlumno, 15)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        for (String clave : CLAVES_DE_CORRECCION) {
            assertThat(crudo)
                    .as("la vista del alumno no puede contener la clave %s", clave)
                    .doesNotContain(clave);
        }
        assertThat(crudo).contains("Cual es la capital de Francia?");
        assertThat(crudo).contains("Paris");
    }

    @Test
    void la_vista_del_profesor_si_lleva_el_criterio() throws Exception {
        String profe = bearerProfesor();
        String om = itemOpcionMultiple(profe);
        var ficha = componer("Parcial", "PORCENTUAL", """
                [{"itemId":"%s","orden":1,"puntaje":100}]
                """.formatted(om), profe);

        String crudo = mvc.perform(
                        get("/teoricos/contenidos/{id}/vista-profesor",
                                ficha.get("contenidoId").asText())
                                .header("Authorization", profe))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(crudo).contains("criterio").contains("correctas");
    }

    @Test
    void un_alumno_no_puede_entrar_a_la_vista_del_profesor() throws Exception {
        String profe = bearerProfesor();
        String om = itemOpcionMultiple(profe);
        var ficha = componer("Parcial", "PORCENTUAL", """
                [{"itemId":"%s","orden":1,"puntaje":100}]
                """.formatted(om), profe);

        mvc.perform(get("/teoricos/contenidos/{id}/vista-profesor",
                        ficha.get("contenidoId").asText())
                        .header("Authorization", bearerAlumno()))
                .andExpect(status().isForbidden());
    }
}
