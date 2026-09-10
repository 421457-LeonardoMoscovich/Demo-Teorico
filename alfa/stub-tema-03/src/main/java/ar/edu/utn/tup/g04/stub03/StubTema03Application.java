package ar.edu.utn.tup.g04.stub03;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

/**
 * STUB DEL TEMA 03. No es nuestro codigo y no comparte una sola clase con
 * ms-teoricos-encuestas: corre en su propio proceso, en su propio puerto, y lo
 * unico que los une son los dos contratos que acordamos (el vale firmado y el
 * mensaje de despacho).
 *
 * Existe para probar que ese contrato cierra de verdad. Si viviera adentro de
 * nuestro servicio, cualquier problema de integracion se podria "resolver"
 * inyectando un bean del otro lado, y la demo no probaria nada.
 *
 * Todo en memoria. Se cae con el proceso, y esta bien que asi sea.
 */
@SpringBootApplication
public class StubTema03Application {

    public static void main(String[] args) {
        SpringApplication.run(StubTema03Application.class, args);
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}
