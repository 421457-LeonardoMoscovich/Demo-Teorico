package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.web.dto;

import java.util.UUID;

/**
 * La ficha de cinco campos que el front le pasa al Tema 03 al crear el desafio
 * (CI-04). Cada campo tiene un motivo y ninguno filtra conocimiento de
 * contenido:
 *
 *   tipo        el enum es del 03, extensible. Es lo UNICO que interpreta.
 *   contenidoId lo guarda. No lo parsea ni le saca informacion.
 *   version     lo guarda. Habilita que editar el contenido no toque lo ya
 *               respondido.
 *   resumen     lo PINTA en pantalla, no lo interpreta.
 *   correccion  decide si muestra el resultado al instante o un estado de
 *               espera. Lo derivamos nosotros (CI-07).
 *
 * OJO: esta es la forma de COMPOSICION, con cinco campos. En el despacho del
 * Paso 4 viaja el contenidoBinding, con dos. Usar el mismo nombre para las dos
 * formas garantiza que alguien lo implemente mal.
 */
public record ContenidoRefResponse(
        String tipo,
        UUID contenidoId,
        int version,
        String resumen,
        String correccion) {

    public static final String TIPO_TEORICO = "TEORICO";
}
