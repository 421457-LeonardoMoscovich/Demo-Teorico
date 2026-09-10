import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService } from '../core/api.service';
import { SesionService } from '../core/sesion.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="centro">
      <form class="tarjeta login" (ngSubmit)="entrar()" #f="ngForm">
        <h1>Teóricos · Grupo 4</h1>
        <p class="ayuda">
          Alfa del desafío teórico. La identidad es del Tema 01; acá la resuelve un adaptador falso
          con dos usuarios.
        </p>

        <label>
          Usuario
          <input name="usuario" [(ngModel)]="usuario" required autocomplete="username" />
        </label>

        <label>
          Clave
          <input
            name="clave"
            type="password"
            [(ngModel)]="clave"
            required
            autocomplete="current-password"
          />
        </label>

        @if (error()) {
          <p class="error">{{ error() }}</p>
        }

        <button type="submit" [disabled]="f.invalid || cargando()">
          {{ cargando() ? 'Entrando…' : 'Entrar' }}
        </button>

        <div class="atajos">
          <button type="button" class="secundario" (click)="usar('profe')">
            Entrar como profesora
          </button>
          <button type="button" class="secundario" (click)="usar('alumno')">
            Entrar como alumno
          </button>
        </div>
      </form>
    </div>
  `,
})
export class LoginPage {
  private readonly api = inject(ApiService);
  private readonly sesion = inject(SesionService);
  private readonly router = inject(Router);

  usuario = '';
  clave = '';
  readonly error = signal('');
  readonly cargando = signal(false);

  usar(quien: string): void {
    this.usuario = quien;
    this.clave = quien;
    this.entrar();
  }

  entrar(): void {
    if (!this.usuario || !this.clave) return;
    this.cargando.set(true);
    this.error.set('');
    this.api.login(this.usuario, this.clave).subscribe({
      next: (s) => {
        this.sesion.entrar(s);
        this.router.navigateByUrl(s.rol === 'PROFESOR' ? '/profesor/cursos' : '/alumno/cursos');
      },
      error: () => {
        this.cargando.set(false);
        this.error.set('Usuario o clave incorrectos.');
      },
    });
  }
}
