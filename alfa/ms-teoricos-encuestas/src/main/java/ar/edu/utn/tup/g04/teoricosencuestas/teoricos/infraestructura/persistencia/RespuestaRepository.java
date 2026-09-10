package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

public interface RespuestaRepository extends Repository<RespuestaEntity, UUID> {

    RespuestaEntity save(RespuestaEntity respuesta);

    List<RespuestaEntity> findByEvaluacionId(UUID evaluacionId);
}
