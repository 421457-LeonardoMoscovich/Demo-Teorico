package ar.edu.utn.tup.g04.teoricosencuestas.comun.identidad;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/** Emite el token de sesion del adaptador falso. Se va con el adaptador. */
@Component
public class EmisorDeTokens {

    private final byte[] secreto;

    public EmisorDeTokens(@Value("${app.seguridad.secreto-sesion}") String secreto) {
        this.secreto = secreto.getBytes(StandardCharsets.UTF_8);
    }

    public String emitir(Usuario usuario) {
        try {
            Instant ahora = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(usuario.id().toString())
                    .claim("rol", usuario.rol().name())
                    .claim("nombre", usuario.nombre())
                    .issueTime(Date.from(ahora))
                    .expirationTime(Date.from(ahora.plus(8, ChronoUnit.HOURS)))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(secreto));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("No se pudo firmar el token de sesion", e);
        }
    }
}
