export type Rol = 'PROFESOR' | 'ALUMNO';

export type TipoDeItem =
  | 'OPCION_MULTIPLE'
  | 'VERDADERO_FALSO'
  | 'EMPAREJAR'
  | 'ORDENAR'
  /** La unica que no se corrige sola: abre la cola del profesor (D-01). */
  | 'ABIERTA';

export const TIPOS: { valor: TipoDeItem; etiqueta: string }[] = [
  { valor: 'OPCION_MULTIPLE', etiqueta: 'Opción múltiple' },
  { valor: 'VERDADERO_FALSO', etiqueta: 'Verdadero / falso' },
  { valor: 'EMPAREJAR', etiqueta: 'Emparejar conceptos' },
  { valor: 'ORDENAR', etiqueta: 'Ordenar secuencia' },
  { valor: 'ABIERTA', etiqueta: 'Respuesta abierta' },
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

export type EstadoDeItem = 'BORRADOR' | 'LISTO';

export interface ItemResumen {
  id: string;
  tipo: TipoDeItem;
  enunciado: string;
  version: number;
  autocorregible: boolean;
  /** Un BORRADOR no entra a ningún cuestionario (CI-59). */
  estado: EstadoDeItem;
}

/**
 * El ítem como lo va a ver el alumno. Es la MISMA forma que sirve el examen, no
 * una maqueta: por eso la vista previa vale como prueba de que el criterio no
 * viaja (CI-17).
 */
export interface VistaPreviaItem {
  itemVersionId: string;
  tipo: TipoDeItem;
  enunciado: string;
  orden: number;
  puntaje: number;
  payload: any;
}

export interface ItemDetalle extends ItemResumen {
  itemVersionId: string;
  payload: any;
  criterio: any;
  /** La retroalimentación completa: este DTO es solo del profesor (CI-58). */
  devolucion: Devolucion | null;
}

/** Lo que el alumno lee DESPUÉS de que su respuesta ya fue corregida (CI-58). */
export interface Devolucion {
  general: string | null;
  porOpcion: { id: string; texto: string }[] | null;
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
  /** El payload de la versión que vio el alumno: con esto los ids se vuelven texto. */
  payload: any;
  puntaje: number;
  /** Null mientras lo espere un humano. No es 0: es todavia-no-se. */
  obtenido: number | null;
  correcto: boolean | null;
  pendiente: boolean;
  respuesta: any;
  /**
   * Ya viene recortada por el backend a lo que este alumno marcó: la del resto
   * de las opciones diría cuál era la correcta. Null si el ítem no tiene o si
   * todavía lo espera un humano.
   */
  devolucion: Devolucion | null;
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

/** Un item esperando que la profesora lo puntue (D-01). */
export interface Pendiente {
  detalleId: string;
  evaluacionId: string;
  alumnoId: string;
  intento: number;
  cuestionario: string | null;
  enunciado: string | null;
  consigna: string | null;
  rubrica: string | null;
  puntaje: number;
  respuesta: string | null;
  entregadaEn: string;
}

/** Un cuestionario del profesor, para reutilizarlo. NO es la ficha: lleva título. */
export interface MiContenido {
  contenidoId: string;
  titulo: string;
  cursoCohorteId: string;
  escala: string;
  creadoEn: string;
  ficha: ContenidoRef;
}

/** Una línea del historial del alumno (CI-44: cada intento se conserva). */
export interface EntregaDelAlumno {
  entregaId: string;
  desafioId: string;
  cuestionario: string | null;
  intento: number;
  nota: number | null;
  estado: string;
  corrector: string | null;
  entregadaEn: string;
}

/** Un envelope publicado. Andamiaje de la demo: se borra cuando entre Kafka. */
export interface EventoPublicado {
  topico: string;
  clave: string;
  eventId: string;
  eventType: string;
  timestamp: string;
  producer: string;
  payload: any;
}

export interface ErrorApi {
  clave: string;
  campo: string | null;
  mensaje: string;
}
