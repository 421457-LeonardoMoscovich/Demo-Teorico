package ar.edu.utn.tup.g04.teoricosencuestas.comun.identidad;

import ar.edu.utn.tup.g04.teoricosencuestas.comun.error.ClaveError;
import ar.edu.utn.tup.g04.teoricosencuestas.comun.error.ExcepcionDeNegocio;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Login del ADAPTADOR FALSO. Cuando exista el Tema 01, este controlador
 * desaparece y el front va a pedirle el token a ellos.
 */
@RestController
@RequestMapping("/auth")
public class SesionController {

    private final ProveedorDeIdentidad identidades;
    private final EmisorDeTokens emisor;

    public SesionController(ProveedorDeIdentidad identidades, EmisorDeTokens emisor) {
        this.identidades = identidades;
        this.emisor = emisor;
    }

    public record LoginRequest(@NotBlank String usuario, @NotBlank String clave) {}

    public record LoginResponse(String token, UUID id, String nombre, Rol rol) {}

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        Usuario u = identidades.autenticar(req.usuario(), req.clave())
                .orElseThrow(() -> new ExcepcionDeNegocio(
                        ClaveError.NO_AUTORIZADO, "usuario", HttpStatus.UNAUTHORIZED));
        return new LoginResponse(emisor.emitir(u), u.id(), u.nombre(), u.rol());
    }
}
