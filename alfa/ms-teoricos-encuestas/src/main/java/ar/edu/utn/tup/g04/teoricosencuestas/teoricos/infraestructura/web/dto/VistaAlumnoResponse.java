package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.web.dto;

import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.TipoDeItem;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.payload.PayloadDeItem;

import java.util.List;
import java.util.UUID;

/**
 * Lo que ve el alumno. Es un DTO APARTE, no la vista del profesor filtrada por
 * rol (CI-17): es una frontera de seguridad, no de estilo.
 *
 * La clase NO TIENE un campo criterio. La garantia la da el tipo, no un
 * @JsonIgnore que alguien puede borrar sin darse cuenta de lo que rompe.
 *
 * El payload se re-serializa desde el record del dominio y no se reenvia el
 * jsonb crudo: asi, el dia que alguien meta un campo esCorrecta adentro de las
 * opciones para simplificar algo, no se filtra por este endpoint.
 */
public record VistaAlumnoResponse(
        UUID contenidoId,
        String titulo,
        int version,
        int puntajeTotal,
        /**
         * LIBRE | SECUENCIAL. El front sirve las consignas de a una y con esto
         * decide si habilita el boton de volver. Es una regla de la evaluacion
         * que eligio la profesora, no una preferencia del alumno, y por eso
         * viaja con el cuestionario y no vive en el front.
         *
         * NO es una barrera de seguridad: quien quiera volver a una consigna
         * que ya paso puede hacerlo con la consola del navegador, porque las
         * preguntas ya estan en su maquina. Impedirlo de verdad exigiria servir
         * de a una y guardar por donde va cada alumno, que es estado de lectura
         * —lo que CI-19 justamente dice que no tenemos—. Lo que si es firme: la
         * nota sale de lo que se entrega, no de como se navego.
         */
        String navegacion,
        List<ItemParaAlumno> items) {

    public record ItemParaAlumno(
            UUID itemVersionId,
            TipoDeItem tipo,
            String enunciado,
            int orden,
            int puntaje,
            PayloadDeItem payload) {
    }
}
