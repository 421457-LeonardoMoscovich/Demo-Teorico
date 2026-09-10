package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContenidoRepository extends Repository<ContenidoEntity, UUID> {

    ContenidoEntity save(ContenidoEntity contenido);

    Optional<ContenidoEntity> findByIdAndBajaLogicaIsNull(UUID id);

    Optional<ContenidoEntity> findByIdAndProfesorIdAndBajaLogicaIsNull(UUID id, UUID profesorId);

    List<ContenidoEntity> findByProfesorIdAndBajaLogicaIsNullOrderByCreadoEnDesc(UUID profesorId);
}
