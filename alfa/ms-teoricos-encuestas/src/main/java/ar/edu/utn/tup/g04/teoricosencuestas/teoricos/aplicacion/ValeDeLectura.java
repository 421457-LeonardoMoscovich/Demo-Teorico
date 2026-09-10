package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.aplicacion;

import ar.edu.utn.tup.g04.teoricosencuestas.comun.error.ClaveError;
import ar.edu.utn.tup.g04.teoricosencuestas.comun.error.ExcepcionDeNegocio;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * El vale de lectura de CI-18.
 *
 * El problema que resuelve: nuestro endpoint recibe un contenidoId, no un
 * desafioId, y eso es justamente lo que nos mantiene libres del Tema 03. Pero
 * deja una pregunta sin dueno: quien verifica que este alumno puede leer este
 * contenido AHORA. Las ventanas de apertura y cierre son del 03 y decidimos no
 * mirarlas (CI-01), asi que sin vale un alumno que consiga el contenidoId puede
 * leer las preguntas antes de que el desafio abra.
 *
 * La solucion: al abrir el desafio el 03 crea el intento y firma un vale de
 * corta vida. Nosotros validamos la firma SIN LLAMAR A NADIE, que es lo que
 * evita meter una llamada sincronica al 03 en el camino caliente y convertir
 * su caida en la nuestra.
 *
 * Lo unico que hay que acordar con el 03 es el esquema de firma y el secreto.
 * Es el unico acuerdo criptografico de todo nuestro contrato, y el Tema 05
 * necesita exactamente el mismo para proteger sus consignas.
 */
@Component
public class ValeDeLectura {

    public static final String HEADER = "X-Vale-Lectura";

    private final byte[] secreto;

    public ValeDeLectura(@Value("${app.seguridad.secreto-vale}") String secreto) {
        this.secreto = secreto.getBytes(StandardCharsets.UTF_8);
    }

    public record Contenido(UUID contenidoId, UUID alumnoId) {}

    public Contenido validar(String vale, UUID contenidoEsperado) {
        if (vale == null || vale.isBlank()) {
            throw new ExcepcionDeNegocio(ClaveError.VALE_REQUERIDO, HEADER, HttpStatus.FORBIDDEN);
        }
        JWTClaimsSet claims;
        try {
            SignedJWT jwt = SignedJWT.parse(vale);
            if (!jwt.verify(new MACVerifier(secreto))) {
                throw new ExcepcionDeNegocio(ClaveError.VALE_INVALIDO, HEADER, HttpStatus.FORBIDDEN);
            }
            claims = jwt.getJWTClaimsSet();
        } catch (ExcepcionDeNegocio e) {
            throw e;
        } catch (Exception e) {
            throw new ExcepcionDeNegocio(ClaveError.VALE_INVALIDO, HEADER, HttpStatus.FORBIDDEN);
        }

        Date expira = claims.getExpirationTime();
        if (expira == null || expira.toInstant().isBefore(Instant.now())) {
            throw new ExcepcionDeNegocio(ClaveError.VALE_VENCIDO, HEADER, HttpStatus.FORBIDDEN);
        }

        UUID contenidoId = leerUuid(claims, "contenidoId");
        UUID alumnoId = leerUuid(claims, "alumnoId");

        if (!contenidoId.equals(contenidoEsperado)) {
            throw new ExcepcionDeNegocio(ClaveError.VALE_DE_OTRO_CONTENIDO, HEADER, HttpStatus.FORBIDDEN);
        }
        return new Contenido(contenidoId, alumnoId);
    }

    private UUID leerUuid(JWTClaimsSet claims, String claim) {
        try {
            return UUID.fromString(claims.getStringClaim(claim));
        } catch (Exception e) {
            throw new ExcepcionDeNegocio(ClaveError.VALE_INVALIDO, HEADER, HttpStatus.FORBIDDEN);
        }
    }
}
