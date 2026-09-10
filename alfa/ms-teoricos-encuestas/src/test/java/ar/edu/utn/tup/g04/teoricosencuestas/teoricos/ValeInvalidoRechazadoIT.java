package ar.edu.utn.tup.g04.teoricosencuestas.teoricos;

import ar.edu.utn.tup.g04.teoricosencuestas.BaseIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El vale de lectura de CI-18, por sus tres formas de fallar.
 *
 * Sin esto, un alumno que consiga el contenidoId puede leer las preguntas antes
 * de que el desafio abra: las ventanas de apertura son del Tema 03 y decidimos
 * no mirarlas (CI-01), asi que la unica barrera es la firma.
 */
class ValeInvalidoRechazadoIT extends BaseIT {

    private UUID contenidoId;

    @BeforeEach
    void armarCuestionario() throws Exception {
        String profe = bearerProfesor();
        String om = itemOpcionMultiple(profe);
        var ficha = componer("Parcial", "PORCENTUAL", """
                [{"itemId":"%s","orden":1,"puntaje":100}]
                """.formatted(om), profe);
        contenidoId = UUID.fromString(ficha.get("contenidoId").asText());
    }

    @Test
    void sin_vale() throws Exception {
        mvc.perform(get("/teoricos/contenidos/{id}/vista-alumno", contenidoId)
                        .header("Authorization", bearerAlumno()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.clave").value("VALE_REQUERIDO"));
    }

    @Test
    void con_la_firma_cambiada() throws Exception {
        String bueno = vale(contenidoId, idAlumno, 15);
        String falsificado = bueno.substring(0, bueno.length() - 4) + "AAAA";

        mvc.perform(get("/teoricos/contenidos/{id}/vista-alumno", contenidoId)
                        .header("Authorization", bearerAlumno())
                        .header("X-Vale-Lectura", falsificado))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.clave").value("VALE_INVALIDO"));
    }

    @Test
    void vencido() throws Exception {
        mvc.perform(get("/teoricos/contenidos/{id}/vista-alumno", contenidoId)
                        .header("Authorization", bearerAlumno())
                        .header("X-Vale-Lectura", vale(contenidoId, idAlumno, -5)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.clave").value("VALE_VENCIDO"));
    }

    @Test
    void un_vale_valido_de_OTRO_cuestionario_no_sirve() throws Exception {
        // El caso que se escapa si solo se verifica la firma: el vale es
        // autentico, pero habilita otra cosa.
        UUID otro = UUID.randomUUID();
        mvc.perform(get("/teoricos/contenidos/{id}/vista-alumno", contenidoId)
                        .header("Authorization", bearerAlumno())
                        .header("X-Vale-Lectura", vale(otro, idAlumno, 15)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.clave").value("VALE_DE_OTRO_CONTENIDO"));
    }
}
