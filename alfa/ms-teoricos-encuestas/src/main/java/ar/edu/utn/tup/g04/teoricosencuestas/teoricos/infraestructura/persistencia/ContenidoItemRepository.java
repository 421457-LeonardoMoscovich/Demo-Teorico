package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

public interface ContenidoItemRepository extends Repository<ContenidoItemEntity, ContenidoItemEntity.Clave> {

    ContenidoItemEntity save(ContenidoItemEntity linea);

    List<ContenidoItemEntity> findByClaveContenidoIdOrderByOrdenAsc(UUID contenidoId);
}
