import {
  ApplicationConfig,
  provideBrowserGlobalErrorListeners,
  provideZoneChangeDetection,
} from '@angular/core';
import { provideRouter, withViewTransitions } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { routes } from './app.routes';
import { authInterceptor } from './core/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    // Sin esto la pantalla que se va ya esta desmontada cuando la que entra
    // empieza a aparecer, y queda un frame de pagina vacia en el medio. La View
    // Transitions API deja que las dos convivan ese instante; el cruce en si se
    // temporiza en el CSS (`::view-transition-*`). `skipInitialTransition`
    // porque la primera carga no viene DE ninguna pantalla: no hay que cruzar.
    provideRouter(routes, withViewTransitions({ skipInitialTransition: true })),
    provideHttpClient(withInterceptors([authInterceptor])),
  ],
};
