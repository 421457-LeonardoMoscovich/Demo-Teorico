package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Las etiquetas de los items.
 *
 * Es el unico repositorio del esquema con un delete, y esta acotado a reemplazar
 * las etiquetas de UN item: reetiquetar es quitar y poner. No contradice
 * RF-NFR-01 —que prohibe el borrado fisico de lo que es historia— porque una
 * etiqueta no es historia: es un rotulo del presente. La version y la
 * correccion, que si lo son, siguen sin tener borrado en ningun lado.
 *
 * Toda consulta que salga de un item concreto pasa por `item` y filtra por
 * profesor: una etiqueta no puede ser la puerta al banco ajeno (RF-USR-07).
 */
public interface ItemEtiquetaRepository extends Repository<ItemEtiquetaEntity, ItemEtiquetaEntity.Clave> {

    ItemEtiquetaEntity save(ItemEtiquetaEntity etiqueta);

    List<ItemEtiquetaEntity> findByItemIdIn(Collection<UUID> itemIds);

    List<ItemEtiquetaEntity> findByItemIdOrderByEtiquetaAsc(UUID itemId);

    void deleteByItemId(UUID itemId);

    /**
     * El vocabulario del profesor, con cuantos items vigentes usa cada etiqueta.
     * Alimenta el autocompletado y el filtro: sin el conteo, el filtro ofrece
     * etiquetas que no traen nada.
     */
    @Query("""
            select e.etiqueta as etiqueta, count(e) as cuantos
              from ItemEtiquetaEntity e
              join ItemEntity i on i.id = e.itemId
             where i.profesorId = :profesorId
               and i.bajaLogica is null
             group by e.etiqueta
             order by count(e) desc, e.etiqueta asc
            """)
    List<EtiquetaConUso> contarPorProfesor(@Param("profesorId") UUID profesorId);

    /** Proyeccion de la consulta de arriba. */
    interface EtiquetaConUso {
        String getEtiqueta();
        long getCuantos();
    }
}
