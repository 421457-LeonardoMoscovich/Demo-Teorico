package ar.edu.utn.tup.g04.teoricosencuestas.comun.eventos;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ADAPTADOR DE ALFA. Deja el envelope en el log y lo guarda en memoria para que
 * los tests lo puedan afirmar. El adaptador de Kafka (G04-HU38) entra despues
 * sin tocar una sola linea de quien publica.
 */
@Component
public class PublicadorEnMemoria implements PublicadorDeEventos {

    private static final Logger log = LoggerFactory.getLogger(PublicadorEnMemoria.class);

    private final ObjectMapper mapper;
    private final List<Publicado> publicados = new CopyOnWriteArrayList<>();

    public record Publicado(String topico, String clave, EventoSobre evento) {}

    public PublicadorEnMemoria(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void publicar(String topico, String clave, EventoSobre evento) {
        publicados.add(new Publicado(topico, clave, evento));
        try {
            log.info("[EVENTO -> {} | key={}] {}", topico, clave, mapper.writeValueAsString(evento));
        } catch (Exception e) {
            log.warn("No se pudo serializar el evento {}", evento.eventType(), e);
        }
    }

    public List<Publicado> publicados() {
        return List.copyOf(publicados);
    }

    public void limpiar() {
        publicados.clear();
    }
}
