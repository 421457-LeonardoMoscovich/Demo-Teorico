import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { SesionService } from './core/sesion.service';
import { TemaService } from './core/tema.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <nav class="barra">
      <span class="marca">Teóricos · G04 <em>alfa</em></span>

      @if (sesion.autenticado()) {
        @if (sesion.esProfesor()) {
          <a routerLink="/profesor/cursos" routerLinkActive="activo">Cursos</a>
          <a routerLink="/profesor/banco" routerLinkActive="activo">Banco</a>
        } @else {
          <a routerLink="/alumno/cursos" routerLinkActive="activo">Cursos</a>
        }
      }

      <span class="espacio"></span>

      @if (sesion.autenticado()) {
        <span class="quien">
          {{ sesion.sesion()!.nombre }}
          <span class="etiqueta">{{ sesion.sesion()!.rol }}</span>
        </span>
      }

      <button type="button" class="secundario" (click)="tema.alternar()">
        {{ tema.tema() === 'dark' ? 'Modo claro' : 'Modo oscuro' }}
      </button>

      @if (sesion.autenticado()) {
        <button type="button" class="secundario" (click)="salir()">Salir</button>
      }
    </nav>

    <main>
      <router-outlet />
    </main>
  `,
  styleUrl: './app.css',
})
export class App {
  protected readonly sesion = inject(SesionService);
  protected readonly tema = inject(TemaService);
  private readonly router = inject(Router);

  salir(): void {
    this.sesion.salir();
    this.router.navigateByUrl('/login');
  }
}
