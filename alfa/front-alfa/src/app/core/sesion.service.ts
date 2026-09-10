import { Injectable, signal, computed } from '@angular/core';
import { Sesion } from './modelos';

const CLAVE = 'g04-alfa-sesion';

/**
 * La sesión del adaptador falso de identidad.
 *
 * Cuando exista el Tema 01, lo único que cambia acá es de dónde sale el token:
 * el resto del front pregunta por `rol()` y por `id()`, no por cómo se
 * autenticó.
 */
@Injectable({ providedIn: 'root' })
export class SesionService {
  private readonly actual = signal<Sesion | null>(leerGuardada());

  readonly sesion = this.actual.asReadonly();
  readonly autenticado = computed(() => this.actual() !== null);
  readonly esProfesor = computed(() => this.actual()?.rol === 'PROFESOR');
  readonly esAlumno = computed(() => this.actual()?.rol === 'ALUMNO');

  entrar(sesion: Sesion): void {
    this.actual.set(sesion);
    localStorage.setItem(CLAVE, JSON.stringify(sesion));
  }

  salir(): void {
    this.actual.set(null);
    localStorage.removeItem(CLAVE);
  }

  token(): string | null {
    return this.actual()?.token ?? null;
  }
}

function leerGuardada(): Sesion | null {
  try {
    const crudo = localStorage.getItem(CLAVE);
    return crudo ? (JSON.parse(crudo) as Sesion) : null;
  } catch {
    return null;
  }
}
