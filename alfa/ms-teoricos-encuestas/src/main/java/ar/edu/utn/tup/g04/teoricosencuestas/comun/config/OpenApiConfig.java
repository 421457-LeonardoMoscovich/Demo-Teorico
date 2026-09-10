package ar.edu.utn.tup.g04.teoricosencuestas.comun.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * El contrato, en forma consumible.
 *
 * `CONTRATO-INTEGRACION-G04.md` explica POR QUE cada decision es como es, y es
 * lo que hay que leer para discutirla. Esto es la otra mitad: la forma exacta
 * de cada mensaje, que un grupo puede bajarse y generarse un cliente sin
 * hablar con nosotros. Las dos cosas tienen que decir lo mismo — si alguna vez
 * se contradicen, manda el documento, porque ahi esta el argumento.
 *
 * Las tres credenciales que aparecen aca no son tres formas de lo mismo, y esa
 * distincion ES el contrato:
 *
 *  - el TOKEN dice quien sos (Tema 01, hoy un adaptador falso);
 *  - el VALE dice que el Tema 03 te habilito a leer este cuestionario ahora
 *    (CI-18), y se valida sin llamar a nadie;
 *  - la CLAVE DE DESPACHO dice que quien entrega es un servicio y no una
 *    persona con un token robado.
 */
@Configuration
public class OpenApiConfig {

    private static final String DESCRIPCION = """
            Microservicio del **Tema 04 — Teóricos y Encuestas**, Grupo 04.

            Somos dueños de una sola cosa: **el contenido del cuestionario y su corrección**.
            Todo lo demás de la plataforma —cursos, cohortes, roadmap, unidades, desafíos,
            intentos, entregas, XP, vidas— es de otros grupos y no vive acá.

            ### Lo que cruza la frontera

            De nosotros hacia afuera viajan exactamente dos cosas:

            1. **La ficha de contenido**, cinco campos, que el front le pasa al Tema 03 al crear
               el desafío. El único campo que el 03 interpreta es `tipo`; los otros cuatro los
               guarda sin abrirlos. Cada campo de más sería conocimiento de contenido
               filtrándose fuera de nuestra caja.
            2. **El evento `TEORICO_CORREGIDO`**, con la nota y nada más. El desglose por
               pregunta NO viaja: al Tema 03 le alcanza la nota para su economía (CI-28, CI-38).

            De afuera hacia nosotros viaja **el despacho de la entrega** (CI-22) y **el vale de
            lectura** que firma el Tema 03 al abrir un intento (CI-18).

            ### Dos cosas que sorprenden y son a propósito

            - **El acuse del despacho nunca lleva la nota**, ni siquiera cuando la corrección es
              inmediata (CI-23). Si la llevara a veces, el Tema 03 tendría que ramificar sobre
              nuestro dominio.
            - **No decimos si el alumno aprobó.** El umbral no lo define ningún documento de la
              plataforma y, como maneja XP y vidas, es economía: la decide el Tema 03 (H-16,
              CI-39).

            ### Estado

            Alfa ejecutable, no el Sprint 1. El módulo `encuestas` y su aislamiento de esquemas
            (G04-HU06) todavía no están. El evento se publica contra un puerto con adaptador en
            memoria; el de Kafka entra sin cambiar quién publica.
            """;

    @Bean
    public OpenAPI apiDeTeoricos() {
        return new OpenAPI()
                .info(new Info()
                        .title("Tema 04 · Teóricos y Encuestas — Grupo 04")
                        .version("0.2.0-alfa")
                        .description(DESCRIPCION)
                        .contact(new Contact().name("Grupo 04 — Programación IV, UTN TUP"))
                        .license(new License().name("Uso académico")))
                .servers(List.of(
                        new Server().url("http://localhost:8081").description("Alfa local (docker compose)")))
                .tags(List.of(
                        new Tag().name("Banco de ítems")
                                .description("El banco del profesor. Cada ítem tiene identidad propia y "
                                        + "contenido versionado: editar publica una versión nueva y nunca "
                                        + "toca lo ya respondido (D-04)."),
                        new Tag().name("Composición")
                                .description("Armar el cuestionario y emitir la ficha de cinco campos "
                                        + "que viaja al Tema 03."),
                        new Tag().name("Lectura del alumno")
                                .description("Lo que el alumno ve. Es un DTO aparte y no la vista del "
                                        + "profesor filtrada por rol: es una frontera de seguridad, no de "
                                        + "estilo (CI-17)."),
                        new Tag().name("Evaluación")
                                .description("El despacho de la entrega desde el Tema 03 y el resultado "
                                        + "con su desglose."),
                        new Tag().name("Corrección humana")
                                .description("La cola del profesor para los ítems que no se corrigen "
                                        + "solos (D-01). Mientras quede uno pendiente no hay nota, y el "
                                        + "evento hacia el Tema 03 no sale."),
                        new Tag().name("Identidad")
                                .description("Adaptador falso, se borra cuando exista el Tema 01.")))
                .components(new Components()
                        .addSecuritySchemes("token", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Quién sos. Lo emite el Tema 01; en la alfa, un adaptador "
                                        + "falso con dos usuarios. El profesorId y el alumnoId salen "
                                        + "SIEMPRE de acá y nunca de un parámetro."))
                        .addSecuritySchemes("vale", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Vale-Lectura")
                                .description("CI-18. JWT de corta vida que firma el Tema 03 al abrir el "
                                        + "intento, con `contenidoId` y `alumnoId`. Lo validamos por firma "
                                        + "SIN llamar a nadie: así una caída del Tema 03 no se convierte "
                                        + "en una caída nuestra. Es el único acuerdo criptográfico de todo "
                                        + "el contrato, y el Tema 05 necesita exactamente el mismo."))
                        .addSecuritySchemes("despacho", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Despacho-Key")
                                .description("Clave compartida servicio-a-servicio. Solo el Tema 03 "
                                        + "despacha entregas; un alumno con un token válido no puede.")));
    }
}
