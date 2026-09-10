package ar.edu.utn.tup.g04.stub03;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

/**
 * STUB DEL TEMA 02 — Cursos, cohortes y roadmap.
 *
 * Vive en el mismo proceso que el stub del Tema 03 por comodidad de la alfa,
 * pero es OTRO GRUPO: por eso es otro controlador, con otro prefijo y con este
 * comentario. En la plataforma real son dos microservicios distintos.
 *
 * Que el curso y el roadmap sean del 02 sale del PRD: RF-CUR-01/06 ponen el
 * roadmap y sus secciones en la seccion de Cursos.
 *
 * ⚠ Hallazgo H-02, sin resolver: **el material de la unidad no tiene dueno
 * asignado**. El PDF de arquitectura le da el grafo de contenidos al Tema 10 y
 * el PRD mete el roadmap en Cursos (02). Lo necesitamos nosotros para generar
 * items y lo va a necesitar el Tema 05 para generar consignas sobre la misma
 * unidad. Por eso `Unidad` de aca abajo NO tiene materiales: no sabemos de
 * quien son, y preferimos que se note el hueco antes que inventarle un dueno.
 */
@RestController
@RequestMapping("/cursos")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:8080"})
public class CursoController {

    public record Unidad(String unidadId, int orden, String titulo, String descripcion) {}

    public record Curso(UUID cursoCohorteId, String nombre, String periodo,
                        String docente, List<Unidad> unidades) {}

    /**
     * Datos fijos: es un stub. Los ids de cohorte son estables entre arranques
     * para que un cuestionario compuesto ayer siga apuntando al mismo curso.
     */
    private static final List<Curso> CURSOS = List.of(
            new Curso(
                    UUID.fromString("11111111-1111-1111-1111-111111111111"),
                    "Programación IV", "2026 · 2º cuatrimestre", "Ana Profesora",
                    List.of(
                            new Unidad("u01", 1, "Introducción a las arquitecturas distribuidas",
                                    "Monolito, monolito modular y microservicios. Cuándo conviene cada uno."),
                            new Unidad("u02", 2, "Arquitectura de microservicios",
                                    "Descubrimiento, gateway, base por servicio, patrones de resiliencia."),
                            new Unidad("u03", 3, "Comunicación entre servicios",
                                    "Sincrónico contra asincrónico, mensajería, eventos y consistencia."),
                            new Unidad("u04", 4, "Persistencia y transacciones distribuidas",
                                    "Saga, outbox, idempotencia y claves de deduplicación."))),
            new Curso(
                    UUID.fromString("22222222-2222-2222-2222-222222222222"),
                    "Bases de Datos II", "2026 · 2º cuatrimestre", "Ana Profesora",
                    List.of(
                            new Unidad("u01", 1, "Modelo relacional avanzado",
                                    "Normalización, restricciones y claves foráneas."),
                            new Unidad("u02", 2, "Índices y planes de ejecución",
                                    "B-tree, hash, cobertura y lectura de un EXPLAIN."),
                            new Unidad("u03", 3, "Control de concurrencia",
                                    "Niveles de aislamiento, bloqueos y anomalías."))));

    @GetMapping
    public List<Curso> mios() {
        return CURSOS;
    }

    @GetMapping("/{cursoCohorteId}")
    public Curso uno(@PathVariable UUID cursoCohorteId) {
        return CURSOS.stream()
                .filter(c -> c.cursoCohorteId().equals(cursoCohorteId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso inexistente"));
    }
}
