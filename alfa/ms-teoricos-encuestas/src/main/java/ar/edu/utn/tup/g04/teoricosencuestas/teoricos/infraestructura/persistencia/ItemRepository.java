package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia;

import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.TipoDeItem;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Extiende Repository y no JpaRepository A PROPOSITO.
 *
 * JpaRepository trae deleteById heredado, y RF-NFR-01 prohibe el borrado fisico
 * en toda la plataforma. Confiar en "no lo llamamos" no alcanza: la forma limpia
 * es no declarar el metodo, asi no se puede llamar.
 *
 * Todos los buscadores filtran por profesorId y por baja logica: el banco ajeno
 * no se puede consultar ni por accidente (RF-USR-07).
 */
public interface ItemRepository extends Repository<ItemEntity, UUID> {

    ItemEntity save(ItemEntity item);

    Optional<ItemEntity> findByIdAndProfesorIdAndBajaLogicaIsNull(UUID id, UUID profesorId);

    Optional<ItemEntity> findByIdAndBajaLogicaIsNull(UUID id);

    List<ItemEntity> findByProfesorIdAndBajaLogicaIsNullOrderByTipoAsc(UUID profesorId);

    List<ItemEntity> findByProfesorIdAndTipoAndBajaLogicaIsNullOrderByTipoAsc(UUID profesorId,
                                                                             TipoDeItem tipo);
}
