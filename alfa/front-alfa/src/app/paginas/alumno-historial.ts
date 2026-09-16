import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { DatePipe, LowerCasePipe } from '@angular/common';
import { ApiService } from '../core/api.service';
import { EntregaDelAlumno } from '../core/modelos';

/**
 * Todo lo que el alumno entregó.
 *
 * Existe para hacer visible CI-44: se guarda una corrección por entrega y
 * **nunca se pisa**. Hasta ahora el modelo lo garantizaba y nadie podía verlo:
 * el front sólo mostraba el resultado del intento en curso.
 *
 * Lo que se ve cuando hay dos intentos del mismo desafío y el profesor editó
 * un ítem en el medio es el argumento entero del versionado en una pantalla:
 * cada intento conserva el enunciado contra el que se lo corrigió.
 */
@Component({
  selector: 'app-alumno-historial',
  standalone: true,
  imports: [DatePipe, LowerCasePipe],
  template: `
    <section class="tarjeta ancho">
      <header class="cabecera-cuestionario">
        <div>
          <h2>Mis entregas</h2>
          <span class="etiqueta">{{ entregas().length }} en total</span>
        </div>
        <button type="button" class="secundario" (click)="volver()">Volver a mis cursos</button>
      </header>

      <p class="ayuda">
        Cada intento queda guardado y ninguno pisa al anterior. Si rendiste dos veces el mismo
        desafío, vas a ver los dos — y cada uno corregido contra la versión de las preguntas que vos
        viste ese día.
      </p>

      @if (cargando()) {
        <p class="vacio">Buscando tus entregas…</p>
      } @else if (entregas().length === 0) {
        <p class="vacio">Todavía no entregaste ningún cuestionario.</p>
      }

      @if (error()) {
        <p class="error" role="alert">{{ error() }}</p>
      }

      <ul class="lista">
        @for (e of entregas(); track e.entregaId; let idx = $index) {
          <li [style.--orden]="idx">
            <div>
              <p class="titulo-desafio">{{ e.cuestionario || 'Cuestionario dado de baja' }}</p>
              <span class="etiqueta">intento {{ e.intento }}</span>
              <span class="etiqueta">{{ e.entregadaEn | date: 'dd/MM/yyyy HH:mm' }}</span>
              @if (e.corrector) {
                <span class="etiqueta">corregido por {{ e.corrector | lowercase }}</span>
              }
            </div>
            <div class="acciones-fila">
              @if (e.estado === 'EN_ESPERA') {
                <span class="marca-espera">En espera</span>
              } @else {
                <span class="peso-pregunta">{{ e.nota }}/100</span>
              }
              <button type="button" class="secundario" (click)="ver(e)">Ver el desglose</button>
            </div>
          </li>
        }
      </ul>
    </section>
  `,
})
export class AlumnoHistorialPage {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);

  readonly entregas = signal<EntregaDelAlumno[]>([]);
  readonly cargando = signal(true);
  readonly error = signal('');

  constructor() {
    this.api.historial().subscribe({
      next: (e) => {
        this.entregas.set(e);
        this.cargando.set(false);
      },
      error: () => {
        this.cargando.set(false);
        this.error.set('No se pudo traer tu historial.');
      },
    });
  }

  /**
   * Se navega con la entregaId en la URL y no por el intento en curso: estas
   * son entregas viejas, y el vale que las abrio vencio hace rato. El resultado
   * no lo necesita — el vale protege la LECTURA DEL CUESTIONARIO, no la de una
   * nota que ya es tuya.
   */
  ver(e: EntregaDelAlumno): void {
    this.router.navigate(['/alumno/resultado'], { queryParams: { entrega: e.entregaId } });
  }

  volver(): void {
    this.router.navigateByUrl('/alumno/cursos');
  }
}
