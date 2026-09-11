import { Component, effect, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { SesionService } from './core/sesion.service';
import { TemaService } from './core/tema.service';
import { PendientesService } from './core/pendientes.service';

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
          <a routerLink="/profesor/correcciones" routerLinkActive="activo" class="con-badge">
            Por corregir
            @if (pendientes.hay()) {
              <span
                class="badge"
                [class.pulso]="pulso()"
                (animationend)="pulso.set(false)"
                role="status"
                aria-live="polite"
              >
                {{ pendientes.pendientes() }}
                <span class="sr-solo">
                  {{
                    pendientes.pendientes() === 1
                      ? 'corrección esperando'
                      : 'correcciones esperando'
                  }}
                </span>
              </span>
            }
          </a>
          <a routerLink="/profesor/demo" routerLinkActive="activo">Demo</a>
        } @else {
          <a routerLink="/alumno/cursos" routerLinkActive="activo">Cursos</a>
          <a routerLink="/alumno/historial" routerLinkActive="activo">Mis entregas</a>
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
  protected readonly pendientes = inject(PendientesService);
  private readonly router = inject(Router);

  /**
   * Un latido del badge cuando el numero SUBE. Nada cuando baja: que bajen las
   * pendientes es consecuencia de lo que la profesora acaba de hacer, y ya lo
   * vio irse de la cola. Lo que no vio es lo que entro mientras miraba otra
   * pantalla — eso es lo unico que merece llamarle la atencion.
   */
  protected readonly pulso = signal(false);
  private cuantasAntes = 0;

  constructor() {
    // El sondeo sigue a la sesion: arranca cuando entra una profesora y se corta
    // cuando sale. Al alumno no se le sondea nada — no porque sea caro, sino
    // porque CI-37 prohibe avisarle a el.
    effect(() => {
      const cuantas = this.pendientes.pendientes();
      if (cuantas > this.cuantasAntes) {
        this.pulso.set(true);
      }
      this.cuantasAntes = cuantas;
    });

    effect(() => {
      if (this.sesion.esProfesor()) {
        this.pendientes.arrancar();
      } else {
        this.pendientes.detener();
      }
    });
  }

  salir(): void {
    this.pendientes.detener();
    this.sesion.salir();
    this.router.navigateByUrl('/login');
  }
}
