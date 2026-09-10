package ar.edu.utn.tup.g04.teoricosencuestas.comun.eventos;

/**
 * El puerto de publicacion. El bus decidido es Kafka; el puerto existe para
 * tener un adaptador en memoria en los tests y en la alfa, y no levantar el
 * broker en cada build.
 */
public interface PublicadorDeEventos {
    void publicar(String topico, String clave, EventoSobre evento);
}
