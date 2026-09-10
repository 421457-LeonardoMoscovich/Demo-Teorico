import { Component, OnDestroy, inject, signal } from '@angular/core';
import { JsonPipe } from '@angular/common';
import { ApiService } from '../core/api.service';
import { EventoPublicado } from '../core/modelos';

/**
 * ANDAMIAJE DE LA DEMO, y dice que lo es.
 *
 * Junta las dos cosas que hasta ahora había que mostrar desde una terminal:
 * los eventos que salen hacia el Tema 03, y el interruptor que lo apaga.
 *
 * Está separada del resto y rotulada en grande a propósito: nadie tiene que
 * poder confundir esto con una pantalla del producto.
 */
@Component({
  selector: 'app-demo',
  standalone: true,
  imports: [JsonPipe],
  template: `
    <section class="tarjeta ancho">
      <header class="cabecera-cuestionario">
        <div>
          <h2>Panel de demo</h2>
          <span class="etiqueta diferida">no es parte del producto</span>
        </div>
        <button type="button" class="secundario" (click)="cargar()">Actualizar</button>
      </header>

      <p class="ayuda">
        Estas dos cosas existen para mostrar decisiones de diseño que de otro modo sólo se ven en
        el log de Docker. Se borran cuando entre Kafka y cuando el Tema 03 sea un servicio real.
      </p>

      <article class="unidad">
        <h3><span class="posicion">1</span> El Tema 03</h3>
        <p class="ayuda descripcion-unidad">
          CI-03 elige cómo fallar: el front compone el contenido acá primero y recién después crea
          el desafío allá. La consecuencia es que <strong>se puede armar un cuestionario aunque el
          Tema 03 esté caído</strong>. Apagalo y probá publicar: el contenido se guarda igual y lo
          único que falla es el desafío.
        </p>

        <div class="tipos-desafio">
          <p class="ayuda">
            Estado del Tema 03:
            <strong>{{ caido() ? 'CAÍDO' : 'en línea' }}</strong>
          </p>
          @if (caido()) {
            <button type="button" (click)="encender()">Encender el Tema 03</button>
          } @else {
            <button type="button" class="peligro" (click)="apagar()">Apagar el Tema 03</button>
          }
        </div>
      </article>

      <article class="unidad">
        <h3>
          <span class="posicion">2</span>
          Eventos publicados
          <span class="peso-pregunta">{{ eventos().length }}</span>
        </h3>
        <p class="ayuda descripcion-unidad">
          Lo que saldría a Kafka en el tópico <code>desafios.resultados</code>. Mirá el momento en
          que aparecen: con un cuestionario todo automático, el evento sale junto con la entrega.
          Con una respuesta abierta, el alumno entrega y <strong>acá no pasa nada</strong> hasta
          que la profesora pone el último puntaje. Eso es el contrato asincrónico, visible.
        </p>

        @if (eventos().length === 0) {
          <p class="vacio descripcion-unidad">
            Todavía no salió ningún evento. Hacé que un alumno entregue un cuestionario.
          </p>
        }

        @for (e of eventos(); track e.eventId) {
          <div class="evento">
            <p class="ayuda">
              <span class="fuente">{{ e.topico }}</span>
              <span class="etiqueta">{{ e.eventType }}</span>
              <span class="version">key = {{ e.clave }}</span>
            </p>
            <pre>{{ e.payload | json }}</pre>
          </div>
        }
      </article>
    </section>
  `,
})
export class DemoPage implements OnDestroy {
  private readonly api = inject(ApiService);

  readonly eventos = signal<EventoPublicado[]>([]);
  readonly caido = signal(false);

  private timer: ReturnType<typeof setInterval> | null = null;

  constructor() {
    this.cargar();
    // Sondeo corto: en esta pantalla se está mirando algo aparecer, así que la
    // latencia importa más que en el badge de la barra.
    this.timer = setInterval(() => this.cargar(), 3000);
  }

  ngOnDestroy(): void {
    if (this.timer !== null) clearInterval(this.timer);
  }

  cargar(): void {
    this.api.eventos().subscribe({ next: (e) => this.eventos.set(e), error: () => {} });
    this.api.temaCaido().subscribe({ next: (r) => this.caido.set(r.caido), error: () => {} });
  }

  apagar(): void {
    this.api.apagarTema03().subscribe(() => this.caido.set(true));
  }

  encender(): void {
    this.api.encenderTema03().subscribe(() => this.caido.set(false));
  }
}
