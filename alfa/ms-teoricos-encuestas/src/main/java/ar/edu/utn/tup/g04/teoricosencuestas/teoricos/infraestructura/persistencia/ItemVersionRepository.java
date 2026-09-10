package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia;

import org.springframework.data.repository.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemVersionRepository extends Repository<ItemVersionEntity, UUID> {

    ItemVersionEntity save(ItemVersionEntity version);

    Optional<ItemVersionEntity> findById(UUID id);

    Optional<ItemVersionEntity> findByItemIdAndVersion(UUID itemId, int version);

    List<ItemVersionEntity> findByItemIdOrderByVersionDesc(UUID itemId);

    List<ItemVersionEntity> findByIdIn(Collection<UUID> ids);
}
