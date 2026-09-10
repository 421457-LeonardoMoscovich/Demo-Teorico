package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * La linea del cuestionario.
 *
 * CI-13: guarda item_id, NO item_version_id. La referencia es FLOTANTE: el
 * cuestionario sirve siempre la ultima version del item, porque RF-CUR-05
 * existe para que el profesor pueda arreglar un error mientras los alumnos
 * trabajan. Lo que protege un examen ya rendido es la estampa que guarda la
 * respuesta del alumno, no un pinneo aca.
 */
@Entity
@Table(name = "contenido_item")
public class ContenidoItemEntity {

    @Embeddable
    public static class Clave implements Serializable {

        @Column(name = "contenido_id", nullable = false)
        private UUID contenidoId;

        @Column(name = "item_id", nullable = false)
        private UUID itemId;

        protected Clave() {
        }

        public Clave(UUID contenidoId, UUID itemId) {
            this.contenidoId = contenidoId;
            this.itemId = itemId;
        }

        public UUID getContenidoId() { return contenidoId; }
        public UUID getItemId() { return itemId; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Clave otra)) return false;
            return Objects.equals(contenidoId, otra.contenidoId) && Objects.equals(itemId, otra.itemId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(contenidoId, itemId);
        }
    }

    @EmbeddedId
    private Clave clave;

    @Column(nullable = false)
    private int orden;

    /** Con escala PORCENTUAL esto es el peso en puntos porcentuales. */
    @Column(nullable = false)
    private int puntaje;

    protected ContenidoItemEntity() {
    }

    public ContenidoItemEntity(UUID contenidoId, UUID itemId, int orden, int puntaje) {
        this.clave = new Clave(contenidoId, itemId);
        this.orden = orden;
        this.puntaje = puntaje;
    }

    public Clave getClave() { return clave; }
    public UUID getContenidoId() { return clave.getContenidoId(); }
    public UUID getItemId() { return clave.getItemId(); }
    public int getOrden() { return orden; }
    public int getPuntaje() { return puntaje; }
}
