package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EvaluacionDetalleRepository extends Repository<EvaluacionDetalleEntity, UUID> {

    EvaluacionDetalleEntity save(EvaluacionDetalleEntity detalle);

    List<EvaluacionDetalleEntity> findByEvaluacionIdOrderByOrdenAsc(UUID evaluacionId);

    Optional<EvaluacionDetalleEntity> findById(UUID id);

    /**
     * La cola de correccion del profesor (D-01).
     *
     * El filtro por profesorId sale del CONTENIDO, no de un parametro: un
     * profesor solo corrige lo suyo, igual que solo ve su banco. Se une por id
     * y no por asociacion JPA porque estas entidades no se referencian entre si
     * a proposito —son agregados distintos y queremos que se note.
     */
    @Query("""
           select d
             from EvaluacionDetalleEntity d, EvaluacionEntity e, ContenidoEntity c
            where d.evaluacionId = e.id
              and e.contenidoId = c.id
              and c.profesorId = :profesorId
              and d.obtenido is null
            order by e.creadaEn asc, d.orden asc
           """)
    List<EvaluacionDetalleEntity> pendientesDelProfesor(@Param("profesorId") UUID profesorId);

    @Query("""
           select count(d)
             from EvaluacionDetalleEntity d, EvaluacionEntity e, ContenidoEntity c
            where d.evaluacionId = e.id
              and e.contenidoId = c.id
              and c.profesorId = :profesorId
              and d.obtenido is null
           """)
    long cuantasPendientes(@Param("profesorId") UUID profesorId);
}
