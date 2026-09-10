package ar.edu.utn.tup.g04.teoricosencuestas.comun.error;

import org.springframework.http.HttpStatus;

/**
 * No tiene constructor que reciba un String suelto: si no existe, nadie lo usa.
 * El campo tiene que ser navegable (criterio.correctas[0] y no criterio):
 * el front lo usa para marcar el input exacto.
 */
public class ExcepcionDeNegocio extends RuntimeException {

    private final ClaveError clave;
    private final String campo;
    private final HttpStatus estado;

    public ExcepcionDeNegocio(ClaveError clave, String campo) {
        this(clave, campo, HttpStatus.BAD_REQUEST);
    }

    public ExcepcionDeNegocio(ClaveError clave, String campo, HttpStatus estado) {
        super(clave.name() + (campo == null ? "" : " @ " + campo));
        this.clave = clave;
        this.campo = campo;
        this.estado = estado;
    }

    public ClaveError clave() { return clave; }
    public String campo() { return campo; }
    public HttpStatus estado() { return estado; }
}
