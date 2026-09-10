package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio;

import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.payload.CriterioDeCorreccion;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.payload.RespuestaDeItem;

/**
 * El puerto de correccion (D-01).
 *
 * La alfa registra un solo adaptador, el automatico. El corrector humano y el
 * del LLM entran despues como adaptadores nuevos, sin tocar una linea del que
 * ya esta: ese es todo el punto de haber puesto un puerto aca en vez de un if
 * sobre el tipo de item adentro del servicio.
 */
public interface Corrector {

    boolean atiende(TipoDeItem tipo);

    /** Cuanto obtuvo el alumno, entre 0 y el puntaje del item. */
    int corregir(TipoDeItem tipo, CriterioDeCorreccion criterio,
                 RespuestaDeItem respuesta, int puntaje);

    /** AUTOMATICO | LLM | HUMANO. Se lo servimos al front; al 03 no le va (CI-38). */
    String nombre();
}
