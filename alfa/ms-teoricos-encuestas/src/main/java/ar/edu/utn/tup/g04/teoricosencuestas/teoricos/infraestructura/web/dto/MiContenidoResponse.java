package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Los cuestionarios del profesor, para que pueda elegir uno y reutilizarlo.
 *
 * NO es la ficha, y por eso es un DTO aparte. La ficha tiene cinco campos
 * porque cada campo de mas seria conocimiento de contenido filtrandose al Tema
 * 03; esta lista no cruza ninguna frontera —es el profesor mirando lo suyo— asi
 * que puede tener el titulo, que es justamente lo que le falta a la ficha para
 * que un humano distinga un cuestionario de otro.
 *
 * La ficha viaja adentro, entera, para que el front la pase tal cual al 03 al
 * crear el desafio: no se rearma en el front ni se le agrega nada.
 */
public record MiContenidoResponse(
        UUID contenidoId,
        String titulo,
        UUID cursoCohorteId,
        String escala,
        Instant creadoEn,
        ContenidoRefResponse ficha) {

    // NO hay un campo "cuantos desafios la usan", y no es un olvido: NO PODEMOS
    // SABERLO. Los desafios viven en el Tema 03; contarlos seria preguntarle a
    // otro grupo de forma sincronica cada vez que el profesor abre esta lista, y
    // convertir su caida en la nuestra por un numero decorativo.
    //
    // Es un buen recordatorio de hasta donde llega nuestra caja: sabemos que un
    // contenido existe, no cuantas veces lo colgaron de un desafio.
}
