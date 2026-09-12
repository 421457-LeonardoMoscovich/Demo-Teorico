package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.web.dto;

/**
 * Una etiqueta del vocabulario del profesor, con cuantos items vigentes la usan.
 *
 * El conteo no es decoracion: sin el, el filtro del banco ofrece etiquetas que
 * no traen nada —quedaron de items dados de baja— y el profesor cree que el
 * filtro esta roto.
 */
public record EtiquetaResponse(String etiqueta, long cuantos) {
}
