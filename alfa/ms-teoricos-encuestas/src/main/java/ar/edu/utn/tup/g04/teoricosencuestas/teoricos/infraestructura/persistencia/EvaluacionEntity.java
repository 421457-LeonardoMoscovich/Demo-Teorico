package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Una entrega despachada por el Tema 03 (CI-22) y su correccion.
 *
 * entrega_id es UNIQUE en la base: la idempotencia de CI-25 la garantiza el
 * motor y no un if. Si el 03 reintenta el despacho no corregimos dos veces ni
 * emitimos dos eventos.
 *
 * CI-44: se guarda una correccion por entrega y NUNCA se pisa. Todos los
 * intentos se conservan.
 */
@Entity
@Table(name = "evaluacion")
public class EvaluacionEntity {

    @Id
    private UUID id;

    @Column(name = "entrega_id", nullable = false, unique = true)
    private UUID entregaId;

    @Column(name = "desafio_id", nullable = false)
    private UUID desafioId;

    @Column(name = "alumno_id", nullable = false)
    private UUID alumnoId;

    @Column(name = "curso_cohorte_id", nullable = false)
    private UUID cursoCohorteId;

    @Column(nullable = false)
    private int intento;

    @Column(name = "contenido_id", nullable = false)
    private UUID contenidoId;

    @Column
    private Integer nota;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(length = 16)
    private String corrector;

    @Column(nullable = false)
    private int revision;

    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn;

    protected EvaluacionEntity() {
    }

    public EvaluacionEntity(UUID id, UUID entregaId, UUID desafioId, UUID alumnoId,
                            UUID cursoCohorteId, int intento, UUID contenidoId, Instant creadaEn) {
        this.id = id;
        this.entregaId = entregaId;
        this.desafioId = desafioId;
        this.alumnoId = alumnoId;
        this.cursoCohorteId = cursoCohorteId;
        this.intento = intento;
        this.contenidoId = contenidoId;
        this.estado = "EN_CURSO";
        this.revision = 1;
        this.creadaEn = creadaEn;
    }

    public UUID getId() { return id; }
    public UUID getEntregaId() { return entregaId; }
    public UUID getDesafioId() { return desafioId; }
    public UUID getAlumnoId() { return alumnoId; }
    public UUID getCursoCohorteId() { return cursoCohorteId; }
    public int getIntento() { return intento; }
    public UUID getContenidoId() { return contenidoId; }
    public Integer getNota() { return nota; }
    public String getEstado() { return estado; }
    public String getCorrector() { return corrector; }
    public int getRevision() { return revision; }
    public Instant getCreadaEn() { return creadaEn; }

    public void cerrar(int nota, String corrector) {
        this.nota = nota;
        this.corrector = corrector;
        this.estado = "FINAL";
    }
}
