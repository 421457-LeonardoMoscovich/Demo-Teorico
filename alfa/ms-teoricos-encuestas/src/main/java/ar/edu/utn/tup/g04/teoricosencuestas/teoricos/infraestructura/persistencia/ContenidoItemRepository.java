package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

public interface ContenidoItemRepository extends Repository<ContenidoItemEntity, ContenidoItemEntity.Clave> {

    ContenidoItemEntity save(ContenidoItemEntity linea);

    List<ContenidoItemEntity> findByClaveContenidoIdOrderByOrdenAsc(UUID contenidoId);

    /**
     * Si el item cuelga de algun cuestionario. Lo necesita CI-59: como la
     * referencia al contenido es flotante (CI-13), un item que ya esta compuesto
     * y vuelve a borrador le seguiria llegando al alumno igual — el borrador
     * seria invisible justo donde tenia que frenar.
     */
    boolean existsByClaveItemId(UUID itemId);
}
