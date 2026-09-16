package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Cuanto saco el alumno en cada item.
 *
 * CI-28: esto NO viaja en el evento hacia el Tema 03 (a el le alcanza la nota
 * para su XP). Se lo servimos al front, que es quien tiene al alumno adelante.
 */
@Entity
@Table(name = "evaluacion_detalle")
public class EvaluacionDetalleEntity {

    @Id
    private UUID id;

    @Column(name = "evaluacion_id", nullable = false)
    private UUID evaluacionId;

    @Column(name = "item_version_id", nullable = false)
    private UUID itemVersionId;

    @Column(nullable = false)
    private int orden;

    @Column(nullable = false)
    private int puntaje;

    /**
     * NULL significa "todavia lo tiene que mirar un humano". No hay una columna
     * `pendiente` aparte a proposito: dos campos que dicen lo mismo se terminan
     * contradiciendo.
     */
    @Column
    private Integer obtenido;

    @Column(name = "corregido_por")
    private UUID corregidoPor;

    @Column(name = "corregido_en")
    private Instant corregidoEn;

    protected EvaluacionDetalleEntity() {
    }

    public EvaluacionDetalleEntity(UUID id, UUID evaluacionId, UUID itemVersionId,
                                   int orden, int puntaje, Integer obtenido) {
        this.id = id;
        this.evaluacionId = evaluacionId;
        this.itemVersionId = itemVersionId;
        this.orden = orden;
        this.puntaje = puntaje;
        this.obtenido = obtenido;
    }

    public UUID getId() { return id; }
    public UUID getEvaluacionId() { return evaluacionId; }
    public UUID getItemVersionId() { return itemVersionId; }
    public int getOrden() { return orden; }
    public int getPuntaje() { return puntaje; }
    public Integer getObtenido() { return obtenido; }
    public UUID getCorregidoPor() { return corregidoPor; }
    public Instant getCorregidoEn() { return corregidoEn; }

    public boolean estaPendiente() { return obtenido == null; }

    /**
     * CI-44: una correccion no se pisa. Si ya tiene puntaje, este metodo no es
     * el camino —para eso existe el recalculo (CI-50), que esta en backlog.
     */
    public void puntuar(int obtenido, UUID profesorId, Instant cuando) {
        this.obtenido = obtenido;
        this.corregidoPor = profesorId;
        this.corregidoEn = cuando;
    }
}
