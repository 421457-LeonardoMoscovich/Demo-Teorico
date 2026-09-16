package ar.edu.utn.tup.g04.stub03;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ANDAMIAJE DE LA DEMO. Apaga el Tema 03 sin apagar el proceso.
 *
 * Existe por una razon concreta: CI-03 elige deliberadamente un modo de fallar
 * —se puede componer contenido aunque el Tema 03 este caido, porque el front
 * llama primero al 04 y despues al 03— y ese camino esta programado del lado
 * del front desde el principio, con su mensaje de error y todo. Hasta ahora
 * nadie podia verlo sin matar un contenedor a mano en el medio de la demo.
 *
 * Con esto se apaga en vivo, se muestra que el cuestionario igual se guarda, y
 * se vuelve a encender. Es la diferencia entre afirmar que elegimos como fallar
 * y mostrarlo.
 *
 * Devuelve 503 y no 500 a proposito: 503 es "no estoy disponible ahora", que es
 * lo que un servicio caido de verdad contesta, y es reintentable.
 */
@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:8080"})
public class ModoCaidoController {

    /** Compartido entre el controlador y el filtro. */
    @Component
    public static class Estado {
        private final AtomicBoolean caido = new AtomicBoolean(false);

        public boolean estaCaido() { return caido.get(); }
        public void poner(boolean valor) { caido.set(valor); }
    }

    private final Estado estado;

    public ModoCaidoController(Estado estado) {
        this.estado = estado;
    }

    @GetMapping("/caido")
    public Map<String, Boolean> ver() {
        return Map.of("caido", estado.estaCaido());
    }

    @PostMapping("/caido")
    public Map<String, Boolean> apagar() {
        estado.poner(true);
        return Map.of("caido", true);
    }

    @DeleteMapping("/caido")
    public Map<String, Boolean> encender() {
        estado.poner(false);
        return Map.of("caido", false);
    }

    /**
     * Tumba SOLO lo del Tema 03, o sea `/desafios/**`. Los cursos siguen en pie.
     *
     * Esto no es un detalle de implementacion: en la plataforma real, cursos y
     * desafios son dos microservicios de dos equipos distintos, y comparten
     * proceso aca solo porque son un stub. Apagar los dos juntos seria simular
     * una caida que no puede pasar — y ademas romperia la demo que este boton
     * existe para hacer, porque sin el roadmap del Tema 02 la profesora no llega
     * a la pantalla de armar y nunca ve que el contenido igual se guarda.
     *
     * `/admin/**` queda afuera o no habria forma de volver a encenderlo.
     */
    @Bean
    public FilterRegistrationBean<Filter> filtroDeCaida(Estado estado) {
        Filter filtro = (ServletRequest req, ServletResponse res, FilterChain chain) -> {
            HttpServletRequest peticion = (HttpServletRequest) req;
            HttpServletResponse respuesta = (HttpServletResponse) res;
            String ruta = peticion.getRequestURI();

            boolean delTema03 = ruta.startsWith("/desafios");
            boolean preflight = "OPTIONS".equals(peticion.getMethod());
            if (estado.estaCaido() && delTema03 && !preflight) {
                respuesta.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
                respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
                respuesta.setCharacterEncoding("UTF-8");
                respuesta.setHeader("Access-Control-Allow-Origin", "http://localhost:4200");
                respuesta.getWriter().write(
                        "{\"error\":\"El Tema 03 está caído (simulado desde el panel de demo)\"}");
                return;
            }
            chain.doFilter(req, res);
        };

        FilterRegistrationBean<Filter> registro = new FilterRegistrationBean<>(filtro);
        registro.addUrlPatterns("/*");
        registro.setOrder(1);
        return registro;
    }
}
