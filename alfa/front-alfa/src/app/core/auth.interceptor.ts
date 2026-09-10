import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { SesionService } from './sesion.service';
import { URL_TEORICOS } from './api.service';

/**
 * Adjunta el token de sesión solo a las llamadas a NUESTRO servicio.
 *
 * Al stub del Tema 03 no se lo mandamos: es otro grupo, con su propia
 * autenticación, y mandarle nuestro token sería filtrarle una credencial que
 * no tiene por qué ver.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = inject(SesionService).token();
  if (!token || !req.url.startsWith(URL_TEORICOS)) {
    return next(req);
  }
  return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};
