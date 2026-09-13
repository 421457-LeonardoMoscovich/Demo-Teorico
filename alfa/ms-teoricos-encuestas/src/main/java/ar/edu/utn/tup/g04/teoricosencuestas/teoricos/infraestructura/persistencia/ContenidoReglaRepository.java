package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia;

import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

public interface ContenidoReglaRepository extends Repository<ContenidoReglaEntity, UUID> {

    ContenidoReglaEntity save(ContenidoReglaEntity regla);

    Optional<ContenidoReglaEntity> findByContenidoId(UUID contenidoId);
}
