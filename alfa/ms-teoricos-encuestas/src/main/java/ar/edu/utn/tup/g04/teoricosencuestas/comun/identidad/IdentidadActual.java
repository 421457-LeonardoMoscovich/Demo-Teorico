package ar.edu.utn.tup.g04.teoricosencuestas.comun.identidad;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Quien esta llamando, resuelto del token y NUNCA de un parametro.
 * Un GET con el id de otro profesor no se puede ni siquiera expresar.
 */
@Component
public class IdentidadActual {

    public UUID id() {
        return UUID.fromString(jwt().getSubject());
    }

    public Rol rol() {
        return Rol.valueOf(jwt().getClaimAsString("rol"));
    }

    /**
     * El despacho del Tema 03 no llega con un token de usuario sino con una
     * credencial de servicio, asi que no tiene ni id ni rol de persona.
     */
    public boolean esServicio() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && !(auth.getPrincipal() instanceof Jwt);
    }

    private Jwt jwt() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Jwt jwt) return jwt;
        throw new IllegalStateException("No hay identidad en el contexto");
    }
}
