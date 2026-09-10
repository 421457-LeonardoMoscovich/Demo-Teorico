import { Routes } from '@angular/router';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { SesionService } from './core/sesion.service';
import { Rol } from './core/modelos';

/**
 * El guard del front es comodidad, no seguridad: quien decide de verdad es el
 * backend, que resuelve el profesorId del token y nunca de un parámetro.
 * Saltearse esta ruta a mano no habilita nada.
 */
function exigeRol(rol: Rol) {
  return () => {
    const sesion = inject(SesionService);
    const router = inject(Router);
    if (!sesion.autenticado()) return router.parseUrl('/login');
    if (sesion.sesion()!.rol !== rol) {
      return router.parseUrl(sesion.esProfesor() ? '/profesor/cursos' : '/alumno/cursos');
    }
    return true;
  };
}

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  {
    path: 'login',
    loadComponent: () => import('./paginas/login').then((m) => m.LoginPage),
  },

  // ---- profesora ----
  {
    path: 'profesor/cursos',
    pathMatch: 'full',
    canActivate: [exigeRol('PROFESOR')],
    loadComponent: () => import('./paginas/cursos').then((m) => m.CursosPage),
  },
  {
    path: 'profesor/cursos/:cursoId',
    canActivate: [exigeRol('PROFESOR')],
    loadComponent: () => import('./paginas/profesor-curso').then((m) => m.ProfesorCursoPage),
  },
  {
    path: 'profesor/banco',
    canActivate: [exigeRol('PROFESOR')],
    loadComponent: () => import('./paginas/profesor-banco').then((m) => m.ProfesorBancoPage),
  },
  {
    // Se llega desde una unidad del roadmap, con ?curso= y ?unidad=.
    path: 'profesor/armar',
    canActivate: [exigeRol('PROFESOR')],
    loadComponent: () => import('./paginas/profesor-armar').then((m) => m.ProfesorArmarPage),
  },

  // ---- alumno ----
  {
    path: 'alumno/cursos',
    pathMatch: 'full',
    canActivate: [exigeRol('ALUMNO')],
    loadComponent: () => import('./paginas/cursos').then((m) => m.CursosPage),
  },
  {
    path: 'alumno/cursos/:cursoId',
    canActivate: [exigeRol('ALUMNO')],
    loadComponent: () => import('./paginas/alumno-curso').then((m) => m.AlumnoCursoPage),
  },
  {
    path: 'alumno/responder',
    canActivate: [exigeRol('ALUMNO')],
    loadComponent: () => import('./paginas/alumno-responder').then((m) => m.AlumnoResponderPage),
  },
  {
    path: 'alumno/resultado',
    canActivate: [exigeRol('ALUMNO')],
    loadComponent: () => import('./paginas/alumno-resultado').then((m) => m.AlumnoResultadoPage),
  },

  { path: '**', redirectTo: 'login' },
];
