package ar.edu.utn.tup.g04.teoricosencuestas.comun.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * El despacho de entregas (CI-22) lo llama un SERVICIO, no un alumno: el Tema 03
 * ya valido la entrega y nos la reenvia. Por eso no lleva token de usuario sino
 * una credencial de servicio.
 *
 * En la alfa es una clave compartida. En el sprint esto se reemplaza por lo que
 * decida la plataforma para llamadas servicio-a-servicio por el gateway.
 */
@Component
public class ClaveDeDespachoFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Despacho-Key";
    public static final String ROL_SERVICIO = "ROLE_SERVICIO";

    private final byte[] claveEsperada;

    public ClaveDeDespachoFilter(@Value("${app.seguridad.clave-despacho}") String clave) {
        this.claveEsperada = clave.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String recibida = req.getHeader(HEADER);
        if (recibida != null
                && MessageDigest.isEqual(recibida.getBytes(StandardCharsets.UTF_8), claveEsperada)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            var auth = new UsernamePasswordAuthenticationToken(
                    "tema-03", null, List.of(new SimpleGrantedAuthority(ROL_SERVICIO)));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(req, res);
    }
}
