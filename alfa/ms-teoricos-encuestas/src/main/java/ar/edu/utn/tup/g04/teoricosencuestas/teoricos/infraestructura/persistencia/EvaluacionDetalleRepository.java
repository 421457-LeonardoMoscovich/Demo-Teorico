package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

public interface EvaluacionDetalleRepository extends Repository<EvaluacionDetalleEntity, UUID> {

    EvaluacionDetalleEntity save(EvaluacionDetalleEntity detalle);

    List<EvaluacionDetalleEntity> findByEvaluacionIdOrderByOrdenAsc(UUID evaluacionId);
}
