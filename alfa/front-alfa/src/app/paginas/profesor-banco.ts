import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../core/api.service';
import { ErrorApi, ItemDetalle, ItemResumen, TIPOS, TipoDeItem } from '../core/modelos';

interface FilaSimple {
  id: string;
  texto: string;
  correcta: boolean;
}

interface FilaPar {
  id: string;
  texto: string;
  /** id del elemento de la derecha con el que empareja */
  parId: string;
}

@Component({
  selector: 'app-profesor-banco',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="columnas">
      <section class="tarjeta">
        <h2>Banco de ítems</h2>
        <p class="ayuda">
          Cada ítem tiene identidad propia y contenido versionado: editar publica una versión nueva
          y nunca toca lo ya respondido.
        </p>

        <label class="filtro">
          Filtrar por tipo
          <select [(ngModel)]="filtro" name="filtro" (ngModelChange)="cargar()">
            <option value="">Todos</option>
            @for (t of tipos; track t.valor) {
              <option [value]="t.valor">{{ t.etiqueta }}</option>
            }
          </select>
        </label>

        @if (items().length === 0) {
          <p class="vacio">
            Todavía no hay ítems. Cargá el primero con el formulario de la derecha.
          </p>
        }

        <ul class="lista">
          @for (i of items(); track i.id) {
            <li>
              <div>
                <span class="etiqueta">{{ etiqueta(i.tipo) }}</span>
                <span class="version">v{{ i.version }}</span>
                <p>{{ i.enunciado }}</p>
              </div>
              <div class="acciones-fila">
                <button type="button" class="secundario" (click)="editar(i)">Editar</button>
                <button class="peligro" type="button" (click)="darDeBaja(i)">Dar de baja</button>
              </div>
            </li>
          }
        </ul>
      </section>

      <section class="tarjeta">
        @if (editando(); as e) {
          <h2>Editar ítem</h2>
          <p class="ayuda">
            Guardar <strong>no modifica</strong> la versión {{ e.version }}: publica la
            {{ e.version + 1 }} y deja la anterior intacta. Quien ya respondió sobre la
            {{ e.version }} se sigue corrigiendo con esa.
          </p>
          <button type="button" class="secundario" (click)="cancelarEdicion()">
            Cancelar y crear uno nuevo
          </button>
        } @else {
          <h2>Nuevo ítem</h2>
        }

        <form (ngSubmit)="guardar()" #f="ngForm">
          <label>
            Tipo
            <select
              name="tipo"
              [(ngModel)]="tipo"
              (ngModelChange)="reiniciarPorTipo()"
              [disabled]="editando() !== null"
            >
              @for (t of tipos; track t.valor) {
                <option [value]="t.valor">{{ t.etiqueta }}</option>
              }
            </select>
          </label>

          <label>
            Enunciado
            <textarea
              name="enunciado"
              rows="2"
              [(ngModel)]="enunciado"
              placeholder="La consigna que lee el alumno"
            ></textarea>
          </label>

          @if (tipo === 'OPCION_MULTIPLE') {
            <label class="check">
              <input type="checkbox" name="multiple" [(ngModel)]="multiple" />
              Admite más de una respuesta correcta
            </label>

            @for (o of opciones; track o.id; let i = $index) {
              <div class="fila">
                <input [name]="'op' + i" [(ngModel)]="o.texto" placeholder="Opción {{ i + 1 }}" />
                <label class="check">
                  <input
                    [type]="multiple ? 'checkbox' : 'radio'"
                    [name]="multiple ? 'ok' + i : 'ok'"
                    [checked]="o.correcta"
                    (change)="marcar(o)"
                  />
                  correcta
                </label>
                <button type="button" class="secundario" (click)="quitar(opciones, i)">×</button>
              </div>
            }
            <button type="button" class="secundario" (click)="agregar(opciones, 'o')">
              + agregar opción
            </button>
          }

          @if (tipo === 'VERDADERO_FALSO') {
            <label>
              Afirmación
              <input
                name="afirmacion"
                [(ngModel)]="afirmacion"
                placeholder="La afirmación a juzgar"
              />
            </label>
            <label>
              Respuesta correcta
              <select name="esVerdadero" [(ngModel)]="esVerdadero">
                <option [ngValue]="true">Verdadero</option>
                <option [ngValue]="false">Falso</option>
              </select>
            </label>
          }

          @if (tipo === 'EMPAREJAR') {
            <p class="ayuda">Cargá primero la columna derecha; después emparejá cada concepto.</p>
            <h3>Derecha</h3>
            @for (d of derecha; track d.id; let i = $index) {
              <div class="fila">
                <input
                  [name]="'der' + i"
                  [(ngModel)]="d.texto"
                  placeholder="Definición {{ i + 1 }}"
                />
                <button type="button" class="secundario" (click)="quitar(derecha, i)">×</button>
              </div>
            }
            <button type="button" class="secundario" (click)="agregar(derecha, 'd')">
              + agregar definición
            </button>

            <h3>Izquierda</h3>
            @for (z of izquierda; track z.id; let i = $index) {
              <div class="fila">
                <input
                  [name]="'izq' + i"
                  [(ngModel)]="z.texto"
                  placeholder="Concepto {{ i + 1 }}"
                />
                <select [name]="'par' + i" [(ngModel)]="z.parId">
                  <option value="">empareja con…</option>
                  @for (d of derecha; track d.id) {
                    <option [value]="d.id">{{ d.texto || d.id }}</option>
                  }
                </select>
                <button type="button" class="secundario" (click)="quitar(izquierda, i)">×</button>
              </div>
            }
            <button type="button" class="secundario" (click)="agregar(izquierda, 'i')">
              + agregar concepto
            </button>
          }

          @if (tipo === 'ORDENAR') {
            <p class="ayuda">Cargalos en el orden correcto. Al alumno se le muestran mezclados.</p>
            @for (e of elementos; track e.id; let i = $index) {
              <div class="fila">
                <span class="posicion">{{ i + 1 }}</span>
                <input [name]="'el' + i" [(ngModel)]="e.texto" placeholder="Paso {{ i + 1 }}" />
                <button type="button" class="secundario" (click)="quitar(elementos, i)">×</button>
              </div>
            }
            <button type="button" class="secundario" (click)="agregar(elementos, 'e')">
              + agregar paso
            </button>
          }

          @if (error()) {
            <p class="error">
              {{ error()!.mensaje }}
              @if (error()!.campo) {
                <span class="campo">({{ error()!.campo }})</span>
              }
            </p>
          }
          @if (ok()) {
            <p class="ok">{{ ok() }}</p>
          }

          <button type="submit" [disabled]="guardando()">
            {{
              guardando()
                ? 'Guardando…'
                : editando()
                  ? 'Publicar versión nueva'
                  : 'Guardar ítem'
            }}
          </button>
        </form>
      </section>
    </div>
  `,
})
export class ProfesorBancoPage {
  private readonly api = inject(ApiService);

  readonly tipos = TIPOS;
  readonly items = signal<ItemResumen[]>([]);
  readonly error = signal<ErrorApi | null>(null);
  readonly ok = signal('');
  readonly guardando = signal(false);

  filtro: TipoDeItem | '' = '';
  tipo: TipoDeItem = 'OPCION_MULTIPLE';
  enunciado = '';

  multiple = false;
  opciones: FilaSimple[] = [];

  afirmacion = '';
  esVerdadero = true;

  izquierda: FilaPar[] = [];
  derecha: FilaSimple[] = [];

  elementos: FilaSimple[] = [];

  private contador = 0;

  /**
   * El item que se esta editando, o null si se esta creando uno nuevo.
   *
   * Editar NO muta: publica la version siguiente y no toca ninguna anterior
   * (D-04). Por eso la pantalla lo dice con todas las letras — es la regla mas
   * facil de malinterpretar de todo el modulo.
   */
  readonly editando = signal<ItemDetalle | null>(null);

  constructor() {
    this.reiniciarPorTipo();
    this.cargar();
  }

  etiqueta(tipo: TipoDeItem): string {
    return TIPOS.find((t) => t.valor === tipo)?.etiqueta ?? tipo;
  }

  cargar(): void {
    this.api.items(this.filtro).subscribe((i) => this.items.set(i));
  }

  reiniciarPorTipo(): void {
    this.error.set(null);
    this.ok.set('');
    this.contador = 0;
    this.opciones = [this.nueva('o'), this.nueva('o')];
    this.derecha = [this.nueva('d'), this.nueva('d')];
    this.izquierda = [this.nuevoPar(), this.nuevoPar()];
    this.elementos = [this.nueva('e'), this.nueva('e')];
  }

  private nueva(prefijo: string): FilaSimple {
    return { id: prefijo + ++this.contador, texto: '', correcta: false };
  }

  private nuevoPar(): FilaPar {
    return { id: 'i' + ++this.contador, texto: '', parId: '' };
  }

  agregar(lista: any[], prefijo: string): void {
    lista.push(prefijo === 'i' ? this.nuevoPar() : this.nueva(prefijo));
  }

  quitar(lista: any[], i: number): void {
    lista.splice(i, 1);
  }

  /**
   * Carga un item del banco en el formulario para publicar una version nueva.
   *
   * El `tipo` NO se puede cambiar: vive en `item` y no en `item_version`, asi
   * que es inmutable entre versiones por modelo. El select se deshabilita en
   * vez de validarlo despues.
   */
  editar(item: ItemResumen): void {
    this.error.set(null);
    this.ok.set('');
    this.api.item(item.id).subscribe({
      next: (d) => {
        this.editando.set(d);
        this.tipo = d.tipo;
        this.enunciado = d.enunciado;
        // Alto para que los ids nuevos no choquen con los que ya trae el item.
        this.contador = 1000;
        this.cargarFormulario(d);
      },
      error: () =>
        this.error.set({
          clave: 'ERROR',
          campo: null,
          mensaje: 'No se pudo abrir el ítem para editar.',
        }),
    });
  }

  cancelarEdicion(): void {
    this.editando.set(null);
    this.enunciado = '';
    this.afirmacion = '';
    this.reiniciarPorTipo();
  }

  private cargarFormulario(d: ItemDetalle): void {
    const p = d.payload ?? {};
    const c = d.criterio ?? {};

    switch (d.tipo) {
      case 'OPCION_MULTIPLE': {
        const correctas: string[] = c.correctas ?? [];
        this.multiple = !!p.multiple;
        this.opciones = (p.opciones ?? []).map((o: any) => ({
          id: o.id,
          texto: o.texto,
          correcta: correctas.includes(o.id),
        }));
        break;
      }
      case 'VERDADERO_FALSO': {
        this.afirmacion = p.afirmacion ?? '';
        this.esVerdadero = c.esVerdadero === true;
        break;
      }
      case 'EMPAREJAR': {
        const pares: string[][] = c.pares ?? [];
        this.derecha = (p.derecha ?? []).map((x: any) => ({
          id: x.id,
          texto: x.texto,
          correcta: false,
        }));
        this.izquierda = (p.izquierda ?? []).map((z: any) => ({
          id: z.id,
          texto: z.texto,
          parId: pares.find((par) => par[0] === z.id)?.[1] ?? '',
        }));
        break;
      }
      case 'ORDENAR': {
        // El payload viaja MEZCLADO; el orden correcto es el del criterio, y es
        // el que el profesor tiene que ver para poder corregirlo.
        const secuencia: string[] = c.secuencia ?? [];
        const porId = new Map<string, any>((p.elementos ?? []).map((e: any) => [e.id, e]));
        this.elementos = secuencia
          .map((id) => porId.get(id))
          .filter(Boolean)
          .map((e: any) => ({ id: e.id, texto: e.texto, correcta: false }));
        break;
      }
    }
  }

  marcar(o: FilaSimple): void {
    if (this.multiple) {
      o.correcta = !o.correcta;
      return;
    }
    this.opciones.forEach((x) => (x.correcta = false));
    o.correcta = true;
  }

  guardar(): void {
    this.error.set(null);
    this.ok.set('');
    this.guardando.set(true);

    const cuerpo = {
      tipo: this.tipo,
      enunciado: this.enunciado,
      ...this.payloadYCriterio(),
    };

    const enEdicion = this.editando();
    const peticion = enEdicion
      ? this.api.publicarVersion(enEdicion.id, cuerpo as any)
      : this.api.crearItem(cuerpo as any);

    peticion.subscribe({
      next: (guardado) => {
        this.guardando.set(false);
        this.ok.set(
          enEdicion
            ? `Publicada la versión ${guardado.version}. La ${enEdicion.version} queda intacta.`
            : 'Ítem guardado.',
        );
        this.editando.set(null);
        this.enunciado = '';
        this.afirmacion = '';
        this.reiniciarPorTipo();
        this.cargar();
      },
      error: (e) => {
        this.guardando.set(false);
        this.error.set(
          e.error?.mensaje
            ? e.error
            : { clave: 'ERROR', campo: null, mensaje: 'No se pudo guardar.' },
        );
      },
    });
  }

  private payloadYCriterio(): { payload: any; criterio: any } {
    switch (this.tipo) {
      case 'OPCION_MULTIPLE':
        return {
          payload: {
            opciones: this.opciones.map((o) => ({ id: o.id, texto: o.texto })),
            multiple: this.multiple,
          },
          criterio: { correctas: this.opciones.filter((o) => o.correcta).map((o) => o.id) },
        };

      case 'VERDADERO_FALSO':
        return {
          payload: { afirmacion: this.afirmacion },
          criterio: { esVerdadero: this.esVerdadero },
        };

      case 'EMPAREJAR':
        return {
          payload: {
            izquierda: this.izquierda.map((z) => ({ id: z.id, texto: z.texto })),
            derecha: this.derecha.map((d) => ({ id: d.id, texto: d.texto })),
          },
          criterio: { pares: this.izquierda.map((z) => [z.id, z.parId]) },
        };

      case 'ORDENAR':
        return {
          // El payload va MEZCLADO: si mandáramos los elementos en el orden en
          // que los cargó el profesor, el orden correcto viajaría dentro de la
          // vista del alumno sin ser formalmente la clave de corrección.
          payload: {
            elementos: mezclar(this.elementos.map((e) => ({ id: e.id, texto: e.texto }))),
          },
          criterio: { secuencia: this.elementos.map((e) => e.id) },
        };
    }
  }

  darDeBaja(item: ItemResumen): void {
    this.api.bajaItem(item.id).subscribe(() => this.cargar());
  }
}

function mezclar<T>(lista: T[]): T[] {
  const copia = [...lista];
  for (let i = copia.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [copia[i], copia[j]] = [copia[j], copia[i]];
  }
  return copia;
}
