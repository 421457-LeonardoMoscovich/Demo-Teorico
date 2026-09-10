export type Rol = 'PROFESOR' | 'ALUMNO';

export type TipoDeItem = 'OPCION_MULTIPLE' | 'VERDADERO_FALSO' | 'EMPAREJAR' | 'ORDENAR';

export const TIPOS: { valor: TipoDeItem; etiqueta: string }[] = [
  { valor: 'OPCION_MULTIPLE', etiqueta: 'Opción múltiple' },
  { valor: 'VERDADERO_FALSO', etiqueta: 'Verdadero / falso' },
  { valor: 'EMPAREJAR', etiqueta: 'Emparejar conceptos' },
  { valor: 'ORDENAR', etiqueta: 'Ordenar secuencia' },
];

export interface Sesion {
  token: string;
  id: string;
  nombre: string;
  rol: Rol;
}

export interface Opcion {
  id: string;
  texto: string;
}

export interface ItemResumen {
  id: string;
  tipo: TipoDeItem;
  enunciado: string;
  version: number;
  autocorregible: boolean;
}

export interface ItemDetalle extends ItemResumen {
  itemVersionId: string;
  payload: any;
  criterio: any;
}

/** La ficha de cinco campos que va al Tema 03 (CI-04). */
export interface ContenidoRef {
  tipo: string;
  contenidoId: string;
  version: number;
  resumen: string;
  correccion: 'INMEDIATA' | 'DIFERIDA';
}

export interface ItemParaAlumno {
  itemVersionId: string;
  tipo: TipoDeItem;
  enunciado: string;
  orden: number;
  puntaje: number;
  payload: any;
}

export interface VistaAlumno {
  contenidoId: string;
  titulo: string;
  version: number;
  puntajeTotal: number;
  items: ItemParaAlumno[];
}

export interface Desafio {
  desafioId: string;
  titulo: string;
  cursoCohorteId: string;
  /** El nodo del roadmap donde vive el desafio (CI-09). Es dato del Tema 03. */
  unidadId: string;
  contenidoRef: ContenidoRef;
  abierto: boolean;
}

/**
 * Curso, cohorte y roadmap son del Tema 02 (RF-CUR-01/06). Los modelamos aca
 * porque el front los consume, pero no existen en nuestro microservicio.
 */
export interface Unidad {
  unidadId: string;
  orden: number;
  titulo: string;
  descripcion: string;
}

export interface Curso {
  cursoCohorteId: string;
  nombre: string;
  periodo: string;
  docente: string;
  unidades: Unidad[];
}

export interface Apertura {
  entregaId: string;
  intento: number;
  desafioId: string;
  titulo: string;
  contenidoRef: ContenidoRef;
  vale: string;
}

export interface ItemCorregido {
  itemVersionId: string;
  orden: number;
  enunciado: string;
  puntaje: number;
  obtenido: number;
  correcto: boolean;
  respuesta: any;
}

export interface Resultado {
  entregaId: string;
  desafioId: string;
  alumnoId: string;
  intento: number;
  nota: number | null;
  estado: string;
  corrector: string | null;
  revision: number;
  detalle: ItemCorregido[];
}

export interface ErrorApi {
  clave: string;
  campo: string | null;
  mensaje: string;
}
