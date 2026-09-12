package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Una etiqueta puesta sobre un item. La clave es el par entero: la tabla no
 * tiene identidad propia que valga la pena nombrar.
 *
 * Igual que con las versiones, NO hay un @OneToMany desde ItemEntity: una
 * coleccion con cascade invita a que alguien la manipule y se lleve puesto algo
 * sin querer. Se navega por el repositorio.
 */
@Entity
@Table(name = "item_etiqueta")
@IdClass(ItemEtiquetaEntity.Clave.class)
public class ItemEtiquetaEntity {

    @Id
    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Id
    @Column(nullable = false, length = 40)
    private String etiqueta;

    protected ItemEtiquetaEntity() {
    }

    public ItemEtiquetaEntity(UUID itemId, String etiqueta) {
        this.itemId = itemId;
        this.etiqueta = etiqueta;
    }

    public UUID getItemId() { return itemId; }
    public String getEtiqueta() { return etiqueta; }

    /** La clave compuesta, como la exige JPA. */
    public static class Clave implements Serializable {
        private UUID itemId;
        private String etiqueta;

        public Clave() {
        }

        public Clave(UUID itemId, String etiqueta) {
            this.itemId = itemId;
            this.etiqueta = etiqueta;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Clave otra)) return false;
            return Objects.equals(itemId, otra.itemId) && Objects.equals(etiqueta, otra.etiqueta);
        }

        @Override
        public int hashCode() {
            return Objects.hash(itemId, etiqueta);
        }
    }
}
