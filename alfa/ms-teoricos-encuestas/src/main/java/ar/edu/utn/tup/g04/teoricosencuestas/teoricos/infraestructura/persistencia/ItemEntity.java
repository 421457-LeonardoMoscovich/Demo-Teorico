package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia;

import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.EstadoDeItem;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.TipoDeItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * La identidad estable del item. Lo que cambia vive en ItemVersionEntity.
 *
 * A proposito NO hay un @OneToMany hacia las versiones: una coleccion mapeada
 * con cascade y orphanRemoval borra versiones anteriores cuando alguien la
 * manipula. Se navega por ItemVersionRepository.
 */
@Entity
@Table(name = "item")
public class ItemEntity {

    @Id
    private UUID id;

    @Column(name = "profesor_id", nullable = false)
    private UUID profesorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private TipoDeItem tipo;

    @Column(name = "version_actual", nullable = false)
    private int versionActual;

    @Column(name = "baja_logica")
    private Instant bajaLogica;

    /** BORRADOR | LISTO (CI-59). Del item y no de la version: ver EstadoDeItem. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EstadoDeItem estado;

    protected ItemEntity() {
    }

    public ItemEntity(UUID id, UUID profesorId, TipoDeItem tipo, EstadoDeItem estado) {
        this.id = id;
        this.profesorId = profesorId;
        this.tipo = tipo;
        this.versionActual = 0;
        this.estado = estado == null ? EstadoDeItem.LISTO : estado;
    }

    public UUID getId() { return id; }
    public UUID getProfesorId() { return profesorId; }
    public TipoDeItem getTipo() { return tipo; }
    public int getVersionActual() { return versionActual; }
    public Instant getBajaLogica() { return bajaLogica; }
    public EstadoDeItem getEstado() { return estado; }

    public void marcarVersionActual(int version) { this.versionActual = version; }

    public void cambiarEstado(EstadoDeItem nuevo) { this.estado = nuevo; }

    /** RF-NFR-01: nunca hard delete. */
    public void darDeBaja(Instant cuando) { this.bajaLogica = cuando; }
}
