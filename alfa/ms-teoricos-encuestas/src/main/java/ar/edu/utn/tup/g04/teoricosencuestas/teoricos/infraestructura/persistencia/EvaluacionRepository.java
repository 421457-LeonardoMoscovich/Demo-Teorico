package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EvaluacionRepository extends Repository<EvaluacionEntity, UUID> {

    EvaluacionEntity save(EvaluacionEntity evaluacion);

    Optional<EvaluacionEntity> findByEntregaId(UUID entregaId);

    Optional<EvaluacionEntity> findById(UUID id);

    List<EvaluacionEntity> findByAlumnoIdOrderByCreadaEnDesc(UUID alumnoId);
}
