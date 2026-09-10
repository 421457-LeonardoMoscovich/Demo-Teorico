package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.web.dto;

import java.util.UUID;

/**
 * CI-23: respondemos SIEMPRE 202 con estos tres campos, y NUNCA la nota,
 * aunque la correccion sea inmediata.
 *
 * "Inmediata" no significa "en la misma respuesta HTTP": significa "en segundos
 * y sin que intervenga un humano". Si devolvieramos la nota cuando es inmediata
 * y un 202 pelado cuando es diferida, el mismo endpoint tendria dos formas de
 * respuesta y el 03 volveria a tener un if adentro sobre nuestro dominio.
 */
public record AcuseResponse(UUID evaluacionId, String estado, String correccion) {

    public static final String ACEPTADA = "ACEPTADA";
}
