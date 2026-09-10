import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { SesionService } from './sesion.service';
import { URL_TEORICOS } from './api.service';

/** Login y refresh son anónimos por definición: nunca llevan token. */
const ANONIMAS = [`${URL_TEORICOS}/auth/`];

/**
 * Adjunta el token de sesión solo a las llamadas a NUESTRO servicio.
 *
 * Al stub del Tema 03 no se lo mandamos: es otro grupo, con su propia
 * autenticación, y mandarle nuestro token sería filtrarle una credencial que
 * no tiene por qué ver.
 *
 * Y NO se lo mandamos a `/auth/login`. Parece un detalle y es una trampa: si
 * la sesión guardada quedó vieja —token vencido, o el backend reinició—, el
 * filtro de seguridad rechaza el login con 401 antes de que el endpoint llegue
 * a mirar usuario y clave. El front lee ese 401 como "credenciales
 * incorrectas", el usuario tipea bien la clave una y otra vez, y la única
 * salida es borrar el localStorage a mano.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const sesion = inject(SesionService);
  const router = inject(Router);

  const anonima = ANONIMAS.some((prefijo) => req.url.startsWith(prefijo));
  const token = sesion.token();
  const pedido =
    !token || anonima || !req.url.startsWith(URL_TEORICOS)
      ? req
      : req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });

  return next(pedido).pipe(
    catchError((error) => {
      // 401 con sesión abierta = la sesión murió. Se cierra y se vuelve al
      // login, en vez de dejar que cada pantalla invente su propio mensaje de
      // error de dominio para lo que en realidad es un token vencido.
      // El 403 del vale de lectura NO entra acá: ese es un permiso puntual y
      // lo maneja la pantalla del alumno.
      if (error.status === 401 && !anonima && sesion.autenticado()) {
        sesion.salir();
        router.navigateByUrl('/login');
      }
      return throwError(() => error);
    }),
  );
};
