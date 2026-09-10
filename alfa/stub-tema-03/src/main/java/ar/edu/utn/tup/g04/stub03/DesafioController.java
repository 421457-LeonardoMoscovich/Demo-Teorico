package ar.edu.utn.tup.g04.stub03;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Los cuatro momentos del ciclo del desafio que la alfa necesita del Tema 03.
 *
 * Lo importante de este archivo no es lo que hace sino lo que NO hace: en
 * ningun lado abre el contenido del cuestionario ni mira adentro de la
 * respuesta del alumno. Para el son dos cajas opacas (CI-01) — la ficha de
 * cinco campos la guarda sin parsearla, y el bloque de respuestas lo reenvia
 * tal cual.
 */
@RestController
@RequestMapping("/desafios")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:8080"})
public class DesafioController {

    private final RestTemplate http;
    private final String urlTeoricos;
    private final String claveDespacho;
    private final byte[] secretoVale;
    private final int minutosDelVale;

    private final Map<UUID, Desafio> desafios = new ConcurrentHashMap<>();
    private final Map<UUID, Intento> intentos = new ConcurrentHashMap<>();
    private final Map<String, Integer> intentosPorAlumno = new ConcurrentHashMap<>();

    public DesafioController(RestTemplate http,
                             @Value("${stub.url-teoricos}") String urlTeoricos,
                             @Value("${stub.clave-despacho}") String claveDespacho,
                             @Value("${stub.secreto-vale}") String secretoVale,
                             @Value("${stub.minutos-vale:15}") int minutosDelVale) {
        this.http = http;
        this.urlTeoricos = urlTeoricos;
        this.claveDespacho = claveDespacho;
        this.secretoVale = secretoVale.getBytes(StandardCharsets.UTF_8);
        this.minutosDelVale = minutosDelVale;
    }

    public record ContenidoRef(String tipo, UUID contenidoId, int version,
                               String resumen, String correccion) {}

    /**
     * `unidadId` es lo que ubica el desafio en el nodo del roadmap (CI-09).
     * Es del Tema 03 guardarlo; el Tema 04 no lo conoce ni le hace falta.
     */
    public record Desafio(UUID desafioId, String titulo, UUID cursoCohorteId, String unidadId,
                          ContenidoRef contenidoRef, boolean abierto) {}

    public record Intento(UUID entregaId, UUID desafioId, UUID alumnoId, int intento) {}

    public record CrearDesafioRequest(String titulo, UUID cursoCohorteId, String unidadId,
                                      ContenidoRef contenidoRef) {}

    /**
     * El front compone primero el contenido en el Tema 04 y despues crea el
     * desafio aca, con la referencia ya formada (CI-03). El bloque
     * `contenidoRef` se guarda SIN parsearlo: de sus cinco campos, el unico que
     * este servicio interpreta es `tipo`, para saber a quien pedirle el
     * contenido despues.
     */
    @PostMapping
    public Desafio crear(@RequestBody CrearDesafioRequest req) {
        Desafio desafio = new Desafio(UUID.randomUUID(), req.titulo(), req.cursoCohorteId(),
                req.unidadId(), req.contenidoRef(), true);
        desafios.put(desafio.desafioId(), desafio);
        return desafio;
    }

    @GetMapping
    public List<Desafio> abiertos(@RequestParam(required = false) UUID curso,
                                  @RequestParam(required = false) String unidad) {
        List<Desafio> resultado = new ArrayList<>();
        for (Desafio d : desafios.values()) {
            boolean delCurso = curso == null || curso.equals(d.cursoCohorteId());
            boolean deLaUnidad = unidad == null || unidad.equals(d.unidadId());
            if (d.abierto() && delCurso && deLaUnidad) {
                resultado.add(d);
            }
        }
        return resultado;
    }

    public record AbrirRequest(UUID alumnoId) {}

    /**
     * CI-20 y CI-21: al abrir, el 03 valida ("puede empezar?"), crea el intento
     * y devuelve la referencia al contenido FIRMADA. El vale de lectura no es
     * un mecanismo aparte: es el valor de retorno natural de "empezar el
     * desafio", que es lo que lo abarato hasta hacerlo obvio.
     */
    @PostMapping("/{id}/intentos")
    public Map<String, Object> abrir(@PathVariable UUID id, @RequestBody AbrirRequest req) {
        Desafio desafio = exigir(id);
        if (!desafio.abierto()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El desafio no esta abierto");
        }

        String clave = id + ":" + req.alumnoId();
        int numero = intentosPorAlumno.merge(clave, 1, Integer::sum);
        Intento intento = new Intento(UUID.randomUUID(), id, req.alumnoId(), numero);
        intentos.put(intento.entregaId(), intento);

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("entregaId", intento.entregaId());
        respuesta.put("intento", intento.intento());
        respuesta.put("desafioId", id);
        respuesta.put("titulo", desafio.titulo());
        respuesta.put("contenidoRef", desafio.contenidoRef());
        respuesta.put("vale", firmarVale(desafio.contenidoRef().contenidoId(), req.alumnoId()));
        return respuesta;
    }

    public record EntregarRequest(UUID entregaId, List<Map<String, Object>> respuestas) {}

    /**
     * CI-22: el boton Entregar llega ACA, no al Tema 04.
     *
     * El argumento fuerte es que validar y registrar tienen que ser un solo
     * acto: el 03 vuelve a chequear que el intento siga siendo valido ("sigue
     * pudiendo?") y recien entonces nos despacha. Si la entrega llegara primero
     * al 04, habria un momento en el que el alumno cree que entrego y la
     * plataforma todavia no lo sabe.
     *
     * Y el bloque `respuestas` se reenvia OPACO: el stub no lo mira.
     */
    @PostMapping("/{id}/entregas")
    public ResponseEntity<String> entregar(@PathVariable UUID id, @RequestBody EntregarRequest req) {
        Desafio desafio = exigir(id);
        Intento intento = intentos.get(req.entregaId());
        if (intento == null || !intento.desafioId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No hay intento abierto para esa entrega");
        }
        if (!desafio.abierto()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El desafio ya no acepta entregas");
        }

        Map<String, Object> binding = new LinkedHashMap<>();
        binding.put("contenidoId", desafio.contenidoRef().contenidoId());
        binding.put("version", desafio.contenidoRef().version());

        Map<String, Object> despacho = new LinkedHashMap<>();
        despacho.put("entregaId", intento.entregaId());
        despacho.put("desafioId", intento.desafioId());
        despacho.put("alumnoId", intento.alumnoId());
        despacho.put("cursoCohorteId", desafio.cursoCohorteId());
        despacho.put("intento", intento.intento());
        despacho.put("contenidoBinding", binding);
        despacho.put("respuestas", req.respuestas());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Despacho-Key", claveDespacho);

        return http.postForEntity(urlTeoricos + "/teoricos/evaluaciones",
                new HttpEntity<>(despacho, headers), String.class);
    }

    private Desafio exigir(UUID id) {
        Desafio desafio = desafios.get(id);
        if (desafio == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Desafio inexistente");
        }
        return desafio;
    }

    private String firmarVale(UUID contenidoId, UUID alumnoId) {
        try {
            Instant ahora = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(alumnoId.toString())
                    .claim("contenidoId", contenidoId.toString())
                    .claim("alumnoId", alumnoId.toString())
                    .issueTime(Date.from(ahora))
                    .expirationTime(Date.from(ahora.plus(minutosDelVale, ChronoUnit.MINUTES)))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(secretoVale));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo firmar el vale de lectura", e);
        }
    }
}
