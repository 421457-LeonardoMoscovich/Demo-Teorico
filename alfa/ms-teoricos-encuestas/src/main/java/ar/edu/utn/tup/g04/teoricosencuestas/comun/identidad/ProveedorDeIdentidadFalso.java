package ar.edu.utn.tup.g04.teoricosencuestas.comun.identidad;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * ADAPTADOR FALSO. Dos usuarios fijos, claves en claro, cero seguridad real.
 * Existe para que la alfa pueda mostrar dos pantallas separadas por rol sin
 * depender del Tema 01. Se borra entero el dia que el Tema 01 exponga su token.
 */
@Component
public class ProveedorDeIdentidadFalso implements ProveedorDeIdentidad {

    public static final UUID ID_PROFESOR = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    public static final UUID ID_ALUMNO   = UUID.fromString("00000000-0000-0000-0000-0000000000b1");

    private final Map<String, String> claves = new LinkedHashMap<>();
    private final Map<String, Usuario> porUsuario = new LinkedHashMap<>();
    private final Map<UUID, Usuario> porId = new LinkedHashMap<>();

    public ProveedorDeIdentidadFalso() {
        registrar(new Usuario(ID_PROFESOR, "profe", "Ana Profesora", Rol.PROFESOR), "profe");
        registrar(new Usuario(ID_ALUMNO, "alumno", "Luis Alumno", Rol.ALUMNO), "alumno");
    }

    private void registrar(Usuario u, String clave) {
        claves.put(u.usuario(), clave);
        porUsuario.put(u.usuario(), u);
        porId.put(u.id(), u);
    }

    @Override
    public Optional<Usuario> autenticar(String usuario, String clave) {
        if (usuario == null || clave == null) return Optional.empty();
        if (!clave.equals(claves.get(usuario))) return Optional.empty();
        return Optional.ofNullable(porUsuario.get(usuario));
    }

    @Override
    public Optional<Usuario> porId(UUID id) {
        return Optional.ofNullable(porId.get(id));
    }
}
